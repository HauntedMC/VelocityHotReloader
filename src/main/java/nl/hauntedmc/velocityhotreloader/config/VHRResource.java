package nl.hauntedmc.velocityhotreloader.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import nl.hauntedmc.velocityhotreloader.VHR;

public abstract class VHRResource {

    protected final VHR plugin;
    protected final VHRConfig config;
    protected final JsonConfig defaultConfig;

    protected VHRResource(
            VHR plugin,
            VHRConfig config,
            JsonConfig defaultConfig
    ) {
        this.plugin = plugin;
        this.config = config;
        this.defaultConfig = defaultConfig;
    }

    protected VHRResource(VHR plugin, String resourceName) {
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

    public VHRConfig getDefaultConfig() {
        return defaultConfig;
    }

    protected void reset(String path) {
        config.set(path, JsonConfig.toObjectValue(defaultConfig.getJsonElement(path)));
    }

    /**
     * Migrates values in the config.
     */
    public void migrate() {
        migrate(config.getInt("config-version"));
        config.set("config-version", defaultConfig.getInt("config-version"));
        try {
            config.save();
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to save migrated configuration", ex);
        }
    }

    public abstract void migrate(int currentConfigVersion);
}
