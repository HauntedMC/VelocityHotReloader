package nl.hauntedmc.velocityhotreloader.entities;

import java.io.File;
import java.io.InputStream;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;
import nl.hauntedmc.velocityhotreloader.providers.ResourceProvider;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;

public class VelocityResourceProvider implements ResourceProvider {

    private final VelocityHotReloaded plugin;

    public VelocityResourceProvider(VelocityHotReloaded plugin) {
        this.plugin = plugin;
    }

    @Override
    public InputStream getRawResource(String resource) {
        return plugin.getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public VHRConfig load(File file) {
        return new VelocityTomlConfig(file);
    }

    @Override
    public String getResourceExtension() {
        return ".toml";
    }
}
