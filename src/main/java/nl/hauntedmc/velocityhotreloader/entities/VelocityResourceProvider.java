package nl.hauntedmc.velocityhotreloader.entities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;
import nl.hauntedmc.velocityhotreloader.providers.ResourceProvider;
import nl.hauntedmc.velocityhotreloader.VHR;

public class VelocityResourceProvider implements ResourceProvider {

    private final VHR plugin;

    public VelocityResourceProvider(VHR plugin) {
        this.plugin = plugin;
    }

    @Override
    public InputStream getRawResource(String resource) {
        return plugin.getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public VHRConfig load(InputStream is) {
        try {
            Path tmpFile = Files.createTempFile(null, null);
            Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);

            VelocityTomlConfig config = new VelocityTomlConfig(tmpFile.toFile());
            Files.delete(tmpFile);

            return config;
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to load configuration from resource stream", ex);
        }
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
