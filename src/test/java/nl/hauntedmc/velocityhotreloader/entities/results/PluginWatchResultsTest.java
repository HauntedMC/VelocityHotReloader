package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.entities.VelocityResourceProvider;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PluginWatchResultsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreResultsAndIterateInInsertionOrder() {
        PluginWatchResults results = new PluginWatchResults();
        results.add(WatchResult.START).add(WatchResult.STOPPED);

        List<WatchResult> values = new ArrayList<>();
        for (PluginWatchResult result : results) {
            values.add(result.result());
        }

        assertEquals(List.of(WatchResult.START, WatchResult.STOPPED), values);
    }

    @Test
    void sendToShouldResolveAndSendAllComponents() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        VelocityResourceProvider provider = mock(VelocityResourceProvider.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getResourceProvider()).thenReturn(provider);
        when(plugin.getSlf4jLogger()).thenReturn(mock(org.slf4j.Logger.class));
        when(provider.getResourceExtension()).thenReturn(".json");
        when(provider.getRawResource("messages.json")).thenReturn(new ByteArrayInputStream("""
                {
                  "config-version": 1,
                  "messages": {
                    "watchplugin": {
                      "start": "<green>start",
                      "stopped": "<red>stop"
                    }
                  }
                }
                """.getBytes(StandardCharsets.UTF_8)));
        when(provider.getRawResource("velocity-messages.json")).thenReturn(null);
        when(provider.load(org.mockito.ArgumentMatchers.any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            try {
                return new nl.hauntedmc.velocityhotreloader.config.JsonConfig(file);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });

        MessagesResource messages = new MessagesResource(plugin);
        messages.load(List.of(MessageKey.WATCHPLUGIN_START, MessageKey.WATCHPLUGIN_STOPPED));
        when(plugin.getMessagesResource()).thenReturn(messages);

        PluginWatchResults results = new PluginWatchResults()
                .add(WatchResult.START)
                .add(WatchResult.STOPPED);
        CapturingAudience sender = new CapturingAudience();

        try (MockedStatic<VelocityHotReloaded> staticMock = Mockito.mockStatic(VelocityHotReloaded.class)) {
            staticMock.when(VelocityHotReloaded::getInstance).thenReturn(plugin);
            results.sendTo(sender);
        }

        assertEquals(
                List.of(
                        messages.get(MessageKey.WATCHPLUGIN_START).toComponent(),
                        messages.get(MessageKey.WATCHPLUGIN_STOPPED).toComponent()
                ),
                sender.messages
        );
    }

    private static final class CapturingAudience extends VHRAudience<Object> {

        private final List<Component> messages = new ArrayList<>();

        private CapturingAudience() {
            super(mock(Audience.class), new Object());
        }

        @Override
        public boolean isPlayer() {
            return false;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(Component component) {
            messages.add(component);
        }
    }
}
