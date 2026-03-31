package nl.hauntedmc.velocityhotreloader;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.hauntedmc.velocityhotreloader.commands.CommandVHR;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.managers.WatchManager;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudienceProvider;
import nl.hauntedmc.velocityhotreloader.entities.VelocityResourceProvider;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginCommandManager;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import nl.hauntedmc.velocityhotreloader.managers.VelocityTaskManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityCommandManager;
import org.slf4j.Logger;

/**
 * Main Velocity plugin entrypoint.
 */
@Plugin(
        id = "velocityhotreloader",
        name = "VelocityHotReloader",
        version = "{version}",
        description = "Velocity Plugin Reloader",
        url = "https://www.hauntedmc.nl",
        authors = "remymine"
)
public class VelocityHotReloaded {

    private static VelocityHotReloaded instance;
    private static final String PLUGIN_COMMANDS_CACHE = ".pluginCommandsCache.json";
    private static final int STARTUP_UNLOAD_DELAY_TICKS = 20;
    private static final List<String> STARTUP_UNLOAD_PLUGIN_IDS = List.of();

    private final ProxyServer proxy;
    private final Logger slf4jLogger;
    private final Path dataDirectory;
    private final PluginContainer pluginContainer;
    private final VelocityPluginCommandManager pluginCommandManager;

    private final WatchManager watchManager = new WatchManager(this);
    private VelocityPluginManager pluginManager;
    private VelocityTaskManager taskManager;
    private VelocityResourceProvider resourceProvider;
    private VelocityAudienceProvider chatProvider;
    private MessagesResource messagesResource;
    private boolean commandsRegistered;

    @Inject
    public VelocityHotReloaded(
            ProxyServer proxy,
            Logger slf4jLogger,
            @DataDirectory Path dataDirectory,
            @Named("velocityhotreloader") PluginContainer pluginContainer
    ) {
        instance = this;
        this.proxy = proxy;
        this.slf4jLogger = slf4jLogger;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;

        try {
            this.pluginCommandManager = VelocityPluginCommandManager.load(dataDirectory.resolve(PLUGIN_COMMANDS_CACHE));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load plugin command cache", ex);
        }

        RVelocityCommandManager.proxyRegistrars(
                proxy,
                getClass().getClassLoader(),
                (container, meta) -> pluginCommandManager.getPluginCommands().putAll(
                        container.getDescription().getId(),
                        meta.getAliases()
                )
        );
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        this.pluginManager = new VelocityPluginManager(proxy, slf4jLogger, pluginCommandManager);
        this.taskManager = new VelocityTaskManager(this);
        this.resourceProvider = new VelocityResourceProvider(this);
        this.chatProvider = new VelocityAudienceProvider(this);
        enable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        disable();
        try {
            pluginCommandManager.save();
        } catch (IOException ex) {
            slf4jLogger.error("Failed to save plugin command cache", ex);
        }
    }

    public static VelocityHotReloaded getInstance() {
        return instance;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public PluginContainer getPlugin() {
        return pluginContainer;
    }

    public VelocityPluginManager getPluginManager() {
        return pluginManager;
    }

    public VelocityTaskManager getTaskManager() {
        return taskManager;
    }

    public VelocityResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public VelocityAudienceProvider getChatProvider() {
        return chatProvider;
    }

    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    public WatchManager getWatchManager() {
        return watchManager;
    }

    public MessagesResource getMessagesResource() {
        return messagesResource;
    }

    private void registerCommands() {
        new CommandVHR(this).register();
    }

    private void unloadConfiguredPlugins() {
        List<PluginContainer> plugins = new ArrayList<>(STARTUP_UNLOAD_PLUGIN_IDS.size());
        for (String pluginId : STARTUP_UNLOAD_PLUGIN_IDS) {
            PluginContainer pluginContainer = getPluginManager().getPlugin(pluginId).orElse(null);
            if (pluginContainer == null) {
                slf4jLogger.warn(
                        "Plugin '{}' defined in startup unload list is not loaded!",
                        pluginId
                );
                continue;
            }
            plugins.add(pluginContainer);
        }

        if (plugins.isEmpty()) return;

        PluginResults<PluginContainer> disableResults = getPluginManager().disablePlugins(plugins);
        if (!disableResults.isSuccess()) {
            disableResults.sendTo(getChatProvider().getConsoleServerAudience(), null);
            return;
        }

        CloseablePluginResults<PluginContainer> unloadResults = getPluginManager().unloadPlugins(plugins);
        unloadResults.tryClose();
        unloadResults.sendTo(getChatProvider().getConsoleServerAudience(), MessageKey.UNLOADPLUGIN);
    }

    public void enable() {
        Path dataFolder = getDataFolder().toPath();
        if (Files.notExists(dataFolder)) {
            try {
                Files.createDirectories(dataFolder);
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to create plugin data folder at " + dataFolder, ex);
            }
        }

        reload();
        getTaskManager().runTaskLater(
                this::unloadConfiguredPlugins,
                STARTUP_UNLOAD_DELAY_TICKS
        );
    }

    public void disable() {
        if (taskManager != null) {
            taskManager.cancelAllTasks();
        }
    }

    public void reload() {
        this.messagesResource = new MessagesResource(this);
        this.messagesResource.load(Arrays.asList(MessageKey.values()));
        if (!this.commandsRegistered) {
            registerCommands();
            this.commandsRegistered = true;
        }
    }

    public VelocityPluginCommandManager getPluginCommandManager() {
        return pluginCommandManager;
    }

    public Logger getSlf4jLogger() {
        return slf4jLogger;
    }
}
