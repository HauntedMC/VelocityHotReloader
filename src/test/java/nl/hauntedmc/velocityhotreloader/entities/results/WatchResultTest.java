package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class WatchResultTest {

    @Test
    void shouldExposeKeys() {
        assertEquals(MessageKey.WATCHPLUGIN_START, WatchResult.START.getKey());
        assertEquals(MessageKey.WATCHPLUGIN_STOPPED, WatchResult.STOPPED.getKey());
    }

    @Test
    void sendToShouldResolveMessageAndForwardToAudience() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        MessagesResource messages = mock(MessagesResource.class);
        MessagesResource.Message message = mock(MessagesResource.Message.class);
        VHRAudience<?> sender = mock(VHRAudience.class);
        Component component = Component.text("watch");
        when(plugin.getMessagesResource()).thenReturn(messages);
        when(messages.get(WatchResult.CHANGE.getKey())).thenReturn(message);
        when(message.toComponent(Mockito.any())).thenReturn(component);

        try (MockedStatic<VelocityHotReloaded> staticMock = Mockito.mockStatic(VelocityHotReloaded.class)) {
            staticMock.when(VelocityHotReloaded::getInstance).thenReturn(plugin);
            WatchResult.CHANGE.sendTo(sender, Placeholder.unparsed("plugin", "demo"));
        }

        verify(sender).sendMessage(component);
    }
}
