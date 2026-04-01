package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PluginWatchResultTest {

    @Test
    void getKeyShouldDelegateToWrappedWatchResult() {
        PluginWatchResult result = new PluginWatchResult(WatchResult.START);
        assertEquals(WatchResult.START.getKey(), result.getKey());
    }
}
