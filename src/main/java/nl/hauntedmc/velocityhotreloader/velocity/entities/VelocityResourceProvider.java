package nl.hauntedmc.velocityhotreloader.velocity.entities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import nl.hauntedmc.velocityhotreloader.common.config.VHRConfig;
import nl.hauntedmc.velocityhotreloader.common.providers.ResourceProvider;
import nl.hauntedmc.velocityhotreloader.velocity.VHR;

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
            ex.printStackTrace();
        }
        return null;
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
