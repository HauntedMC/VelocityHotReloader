package nl.hauntedmc.velocityhotreloader.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.VelocityResourceProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

class MessagesResourceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadConfiguredMessagesAndRenderComponents() {
        String resourceJson = """
                {
                  "config-version": 1,
                  "messages": {
                    "generic": {
                      "not-exists": "<red><plugin>",
                      "prefix": "<green>prefix"
                    }
                  }
                }
                """;

        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        VelocityResourceProvider provider = mock(VelocityResourceProvider.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getSlf4jLogger()).thenReturn(mock(Logger.class));
        when(plugin.getResourceProvider()).thenReturn(provider);
        when(provider.getResourceExtension()).thenReturn(".json");
        when(provider.getRawResource("messages.json")).thenReturn(new ByteArrayInputStream(resourceJson.getBytes(StandardCharsets.UTF_8)));
        when(provider.getRawResource("velocity-messages.json")).thenReturn(null);
        when(provider.load(org.mockito.ArgumentMatchers.any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            try {
                return new JsonConfig(file);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });

        MessagesResource resource = new MessagesResource(plugin);
        resource.load(java.util.List.of(MessageKey.GENERIC_NOT_EXISTS, MessageKey.GENERIC_PREFIX));

        assertEquals(
                Component.text("pluginA", NamedTextColor.RED),
                resource.get(MessageKey.GENERIC_NOT_EXISTS).toComponent(Placeholder.unparsed("plugin", "pluginA"))
        );
        assertEquals(
                resource.get(MessageKey.GENERIC_PREFIX).toComponent(),
                resource.get(MessageKey.GENERIC_PREFIX).toComponent(Placeholder.unparsed("ignored", "value"))
        );
    }

    @Test
    void missingMessageKeyShouldUseEmptyFallbackAndLogWarning() {
        String resourceJson = """
                {
                  "config-version": 1,
                  "messages": {}
                }
                """;

        Logger logger = mock(Logger.class);
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        VelocityResourceProvider provider = mock(VelocityResourceProvider.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getSlf4jLogger()).thenReturn(logger);
        when(plugin.getResourceProvider()).thenReturn(provider);
        when(provider.getResourceExtension()).thenReturn(".json");
        when(provider.getRawResource("messages.json")).thenReturn(new ByteArrayInputStream(resourceJson.getBytes(StandardCharsets.UTF_8)));
        when(provider.getRawResource("velocity-messages.json")).thenReturn(null);
        when(provider.load(org.mockito.ArgumentMatchers.any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            try {
                return new JsonConfig(file);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });

        MessagesResource resource = new MessagesResource(plugin);
        PlaceholderConfigKey missingKey = new PlaceholderConfigKey() {
            @Override
            public boolean hasPlaceholders() {
                return true;
            }

            @Override
            public String getPath() {
                return "missing.path";
            }
        };
        resource.load(java.util.List.of(missingKey));

        assertEquals(Component.empty(), resource.get(missingKey).toComponent());
        verify(logger).warn("Missing message key '{}', using empty fallback.", "missing.path");
    }
}
