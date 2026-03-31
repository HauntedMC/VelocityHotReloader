package nl.hauntedmc.velocityhotreloader.velocity;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import nl.hauntedmc.velocityhotreloader.common.commands.CommandPlugins;
import nl.hauntedmc.velocityhotreloader.common.commands.CommandVHR;
import nl.hauntedmc.velocityhotreloader.common.commands.brigadier.BrigadierHandler;
import nl.hauntedmc.velocityhotreloader.common.config.CommandsResource;
import nl.hauntedmc.velocityhotreloader.common.config.ConfigResource;
import nl.hauntedmc.velocityhotreloader.common.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.common.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.common.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloader.common.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.common.managers.WatchManager;
import nl.hauntedmc.velocityhotreloader.common.utils.FileUtils;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityAudience;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityAudienceProvider;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityPluginDescription;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityResourceProvider;
import nl.hauntedmc.velocityhotreloader.velocity.managers.VelocityPluginCommandManager;
import nl.hauntedmc.velocityhotreloader.velocity.managers.VelocityPluginManager;
import nl.hauntedmc.velocityhotreloader.velocity.managers.VelocityTaskManager;
import nl.hauntedmc.velocityhotreloader.velocity.reflection.RVelocityCommandManager;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;
import org.slf4j.Logger;

@Plugin(
        id = "velocityhotreloader",
        name = "VelocityHotReloader",
        version = "{version}",
        description = "Velocity Plugin Reloader",
        url = "https://www.hauntedmc.nl",
        authors = "remymine"
)
public class VHR {

    private static VHR instance;
    private static final String PLUGIN_COMMANDS_CACHE = ".pluginCommandsCache.json";

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
    private CommandsResource commandsResource;
    private ConfigResource configResource;
    private MessagesResource messagesResource;
    private CommandManager<VelocityAudience> commandManager;

    @Inject
    public VHR(
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
            throw new RuntimeException(ex);
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
            ex.printStackTrace();
        }
    }

    public static VHR getInstance() {
        return instance;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Path getDataDirectory() {
        return dataDirectory;
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

    public java.util.logging.Logger getLogger() {
        return java.util.logging.Logger.getLogger(slf4jLogger.getName());
    }

    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    public WatchManager getWatchManager() {
        return watchManager;
    }

    public CommandsResource getCommandsResource() {
        return commandsResource;
    }

    public ConfigResource getConfigResource() {
        return configResource;
    }

    public MessagesResource getMessagesResource() {
        return messagesResource;
    }

    public Collection<Command<VelocityAudience>> getCommands() {
        return commandManager.commands();
    }

    public void createDataFolderIfNotExists() {
        if (getDataFolder().exists()) return;
        getDataFolder().mkdirs();
    }

    public File copyResourceIfNotExists(String targetName, String resource) {
        createDataFolderIfNotExists();

        File file = new File(getDataFolder(), targetName);
        if (!file.exists()) {
            getLogger().info("'" + targetName + "' not found, creating!");
            try {
                FileUtils.saveResource(getResourceProvider().getResource(resource), file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return file;
    }

    private VelocityCommandManager<VelocityAudience> newCommandManager() {
        VelocityCommandManager<VelocityAudience> commandManager = new VelocityCommandManager<>(
                pluginContainer,
                proxy,
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(chatProvider::get, VelocityAudience::getSource)
        );
        handleBrigadier(commandManager.brigadierManager());
        return commandManager;
    }

    private void registerCommands(CommandManager<VelocityAudience> commandManager) {
        new CommandPlugins(this).register(commandManager);
        new CommandVHR(this).register(commandManager);
    }

    private void handleBrigadier(CloudBrigadierManager<VelocityAudience, ?> brigadierManager) {
        BrigadierHandler handler = new BrigadierHandler(brigadierManager);
        handler.registerTypes();
    }

    private void unloadConfiguredPlugins() {
        List<String> pluginIds = configResource.getConfig().getStringList("unload-after-startup.plugins");
        List<PluginContainer> plugins = new ArrayList<>(pluginIds.size());
        for (String pluginId : pluginIds) {
            Optional<PluginContainer> pluginOptional = getPluginManager().getPlugin(pluginId);
            if (!pluginOptional.isPresent()) {
                getLogger().warning(
                        "Plugin '" + pluginId + "' defined in config.yml 'unload-after-startup' is not loaded!"
                );
                continue;
            }
            plugins.add(pluginOptional.get());
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
                ex.printStackTrace();
            }
        }

        reload();
        getTaskManager().runTaskLater(
                this::unloadConfiguredPlugins,
                configResource.getConfig().getInt("unload-after-startup.delay-ticks")
        );
    }

    public void disable() {
        if (taskManager != null) {
            taskManager.cancelAllTasks();
        }
    }

    public void reload() {
        this.commandsResource = new CommandsResource(this);
        this.configResource = new ConfigResource(this);
        this.messagesResource = new MessagesResource(this);
        this.messagesResource.load(Arrays.asList(MessageKey.values()));
        if (this.commandManager == null) {
            this.commandManager = newCommandManager();
            registerCommands(this.commandManager);
        }
    }

    public VelocityPluginCommandManager getPluginCommandManager() {
        return pluginCommandManager;
    }

    public Logger getSlf4jLogger() {
        return slf4jLogger;
    }
}
