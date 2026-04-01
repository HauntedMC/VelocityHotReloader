package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VelocityResourceProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExposeTomlLoaderAndExtension() throws IOException {
        VelocityResourceProvider provider = new VelocityResourceProvider(mock(VelocityHotReloaded.class));
        Path file = tempDir.resolve("config.toml");
        Files.writeString(file, "name = \"value\"");

        VHRConfig config = provider.load(file.toFile());

        assertInstanceOf(VelocityTomlConfig.class, config);
        assertEquals(".toml", provider.getResourceExtension());
    }

    @Test
    void shouldLoadBundledResourcesFromPluginClassLoader() throws IOException {
        VelocityResourceProvider provider = new VelocityResourceProvider(mock(VelocityHotReloaded.class));
        try (InputStream stream = provider.getRawResource("messages.json")) {
            assertNotNull(stream);
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("\"config-version\""));
        }
    }
}
