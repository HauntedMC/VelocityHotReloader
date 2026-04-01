package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class PluginResultTest {

    @Test
    void shouldExposeStateAndInjectPluginPlaceholder() {
        PluginResult<String> result = new PluginResult<>(
                "test-plugin",
                "plugin-instance",
                Result.SUCCESS,
                Placeholder.unparsed("key", "value")
        );

        assertEquals("test-plugin", result.getPluginId());
        assertEquals("plugin-instance", result.getPlugin());
        assertEquals(Result.SUCCESS, result.getResult());
        assertTrue(result.isSuccess());
        assertEquals(2, result.getPlaceholders().length);
        assertNotNull(result.getPlaceholders()[0]);
        assertNotNull(result.getPlaceholders()[1]);
    }

    @Test
    void shouldReportNonSuccessWhenPluginOrResultDoesNotMatchSuccess() {
        assertFalse(new PluginResult<>("a", Result.SUCCESS).isSuccess());
        assertFalse(new PluginResult<>("a", "x", Result.ERROR).isSuccess());
    }

    @Test
    void sendToShouldResolveSuccessAndFailureKeys() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        MessagesResource messages = mock(MessagesResource.class);
        MessagesResource.Message successMessage = mock(MessagesResource.Message.class);
        MessagesResource.Message errorMessage = mock(MessagesResource.Message.class);
        VHRAudience<?> sender = mock(VHRAudience.class);
        Component successComponent = Component.text("success");
        Component errorComponent = Component.text("error");
        when(plugin.getMessagesResource()).thenReturn(messages);
        when(messages.get(MessageKey.LOADPLUGIN)).thenReturn(successMessage);
        when(messages.get(MessageKey.GENERIC_ERROR)).thenReturn(errorMessage);
        when(successMessage.toComponent(Mockito.any())).thenReturn(successComponent);
        when(errorMessage.toComponent(Mockito.any())).thenReturn(errorComponent);

        try (MockedStatic<VelocityHotReloaded> staticMock = Mockito.mockStatic(VelocityHotReloaded.class)) {
            staticMock.when(VelocityHotReloaded::getInstance).thenReturn(plugin);

            PluginResult<String> ok = new PluginResult<>("ok", "instance", Result.SUCCESS);
            PluginResult<String> failed = new PluginResult<>("fail", Result.ERROR);

            ok.sendTo(sender, MessageKey.LOADPLUGIN);
            failed.sendTo(sender, MessageKey.LOADPLUGIN);
        }

        verify(messages).get(MessageKey.LOADPLUGIN);
        verify(messages).get(MessageKey.GENERIC_ERROR);
        verify(sender).sendMessage(successComponent);
        verify(sender).sendMessage(errorComponent);
    }
}
