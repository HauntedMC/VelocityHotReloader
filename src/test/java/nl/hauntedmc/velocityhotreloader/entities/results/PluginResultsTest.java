package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import org.junit.jupiter.api.Test;

class PluginResultsTest {

    @Test
    void shouldCollectSuccessfulPlugins() {
        PluginResults<String> results = new PluginResults<>();
        results.addResult("a", "A");
        results.addResult("b", "B");

        assertTrue(results.isSuccess());
        assertEquals(List.of("A", "B"), results.getPlugins());
        assertEquals("a", results.first().getPluginId());
        assertEquals("b", results.last().getPluginId());
    }

    @Test
    void getPluginsShouldFailWhenAnyResultFailed() {
        PluginResults<String> results = new PluginResults<>();
        results.addResult("a", "A");
        results.addResult("b", Result.ERROR);

        assertThrows(IllegalArgumentException.class, results::getPlugins);
    }

    @Test
    void sendToShouldSendOnlyLastWhenNotSuccessful() {
        @SuppressWarnings("unchecked")
        PluginResult<String> ok = mock(PluginResult.class);
        @SuppressWarnings("unchecked")
        PluginResult<String> failed = mock(PluginResult.class);
        when(ok.isSuccess()).thenReturn(true);
        when(failed.isSuccess()).thenReturn(false);

        PluginResults<String> results = new PluginResults<>();
        results.addResult(ok);
        results.addResult(failed);

        results.sendTo(mock(VHRAudience.class), MessageKey.LOADPLUGIN);

        verify(failed).sendTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(MessageKey.LOADPLUGIN));
        verify(ok, never()).sendTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendToShouldSendAllWhenAllSuccessful() {
        @SuppressWarnings("unchecked")
        PluginResult<String> a = mock(PluginResult.class);
        @SuppressWarnings("unchecked")
        PluginResult<String> b = mock(PluginResult.class);
        when(a.isSuccess()).thenReturn(true);
        when(b.isSuccess()).thenReturn(true);

        PluginResults<String> results = new PluginResults<>();
        results.addResult(a);
        results.addResult(b);

        results.sendTo(mock(VHRAudience.class), MessageKey.LOADPLUGIN);

        verify(a).sendTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(MessageKey.LOADPLUGIN));
        verify(b).sendTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(MessageKey.LOADPLUGIN));
    }
}
