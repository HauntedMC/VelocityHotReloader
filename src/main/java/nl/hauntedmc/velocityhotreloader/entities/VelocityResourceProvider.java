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
        if (is == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile("vhr-", ".tmp");
            try (InputStream input = is) {
                Files.copy(input, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return new VelocityTomlConfig(tmpFile.toFile());
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to load configuration from resource stream", ex);
        } finally {
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException ignored) {
                    //
                }
            }
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
