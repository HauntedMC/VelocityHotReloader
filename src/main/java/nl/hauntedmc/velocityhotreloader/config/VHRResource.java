package nl.hauntedmc.velocityhotreloader.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;

public abstract class VHRResource {

    protected final VelocityHotReloaded plugin;
    protected final VHRConfig config;
    protected final JsonConfig defaultConfig;

    protected VHRResource(
            VelocityHotReloaded plugin,
            VHRConfig config,
            JsonConfig defaultConfig
    ) {
        this.plugin = plugin;
        this.config = config;
        this.defaultConfig = defaultConfig;
    }

    protected VHRResource(VelocityHotReloaded plugin, String resourceName) {
        this.plugin = plugin;
        this.defaultConfig = JsonConfig.load(plugin.getResourceProvider(), resourceName);
        this.config = VHRConfig.init(
                this.defaultConfig,
                plugin.getResourceProvider(),
                plugin.getDataFolder().toPath().resolve(
                        resourceName + plugin.getResourceProvider().getResourceExtension()
                )
        );
        this.migrate();
    }

    public VHRConfig getConfig() {
        return config;
    }

    /**
     * Migrates values in the config.
     */
    public void migrate() {
        config.set("config-version", defaultConfig.getInt("config-version"));
        try {
            config.save();
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to save migrated configuration", ex);
        }
    }

}
