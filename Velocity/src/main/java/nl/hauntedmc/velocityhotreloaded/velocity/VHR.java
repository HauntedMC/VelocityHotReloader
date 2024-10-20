package nl.hauntedmc.velocityhotreloaded.velocity;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;
import nl.hauntedmc.velocityhotreloaded.common.VHRApp;
import nl.hauntedmc.velocityhotreloaded.velocity.entities.VelocityPlugin;
import nl.hauntedmc.velocityhotreloaded.velocity.managers.VelocityPluginCommandManager;
import nl.hauntedmc.velocityhotreloaded.velocity.reflection.RVelocityCommandManager;
import org.slf4j.Logger;

@Plugin(
        id = "velocityhotreload",
        name = "VelocityHotReload",
        version = "{version}",
        description = "Velocity Plugin Reloader",
        url = "https://www.hauntedmc.nl",
        authors = "remymine"
)
public class VHR {

    private static VHR instance;
    private static final String PLUGIN_COMMANDS_CACHE = ".pluginCommandsCache.json";

    private VelocityPlugin plugin;

    @Inject
    private ProxyServer proxy;

    @Inject
    private Logger logger;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    @Inject
    @Named("velocityhotreload")
    private PluginContainer pluginContainer;

    private final VelocityPluginCommandManager pluginCommandManager;

    /**
     * Initialises VHR.
     */
    @Inject
    public VHR(ProxyServer proxy, @DataDirectory Path dataDirectory) {
        instance = this;
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

    /**
     * Initialises and enables VHR.
     */
    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        this.plugin = new VelocityPlugin(this);
        VHRApp.init(this, plugin);

        plugin.enable();
    }

    /**
     * De-initialises and disables VHR.
     */
    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
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

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public VelocityPlugin getPlugin() {
        return plugin;
    }

    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    public VelocityPluginCommandManager getPluginCommandManager() {
        return pluginCommandManager;
    }
}
