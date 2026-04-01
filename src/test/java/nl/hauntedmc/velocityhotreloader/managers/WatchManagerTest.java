package nl.hauntedmc.velocityhotreloader.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginWatchResult;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginWatchResults;
import nl.hauntedmc.velocityhotreloader.entities.results.WatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatchManagerTest {

    private VelocityTaskManager taskManager;
    private WatchManager watchManager;
    private PluginContainer pluginA;
    private PluginContainer pluginB;

    @BeforeEach
    void setUp() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        VelocityPluginManager pluginManager = mock(VelocityPluginManager.class);
        taskManager = mock(VelocityTaskManager.class);
        pluginA = mock(PluginContainer.class);
        pluginB = mock(PluginContainer.class);

        when(plugin.getPluginManager()).thenReturn(pluginManager);
        when(plugin.getTaskManager()).thenReturn(taskManager);
        when(pluginManager.getPluginId(pluginA)).thenReturn("plugin-a");
        when(pluginManager.getPluginId(pluginB)).thenReturn("plugin-b");
        when(taskManager.runTaskAsynchronously(anyString(), any())).thenReturn(mock(ScheduledTask.class));

        watchManager = new WatchManager(plugin);
    }

    @Test
    void watchPluginsShouldStartWatchingAndReturnStartResults() {
        PluginWatchResults results = watchManager.watchPlugins(mock(VHRAudience.class), List.of(pluginA, pluginB));
        List<WatchResult> actual = extract(results);

        assertEquals(List.of(WatchResult.START, WatchResult.START), actual);
        verify(taskManager).runTaskAsynchronously(anyString(), any());
    }

    @Test
    void watchPluginsShouldReturnAlreadyWatchingWhenPluginIsAlreadyWatched() {
        watchManager.watchPlugins(mock(VHRAudience.class), List.of(pluginA));

        PluginWatchResults results = watchManager.watchPlugins(mock(VHRAudience.class), List.of(pluginA));
        List<WatchResult> actual = extract(results);

        assertEquals(List.of(WatchResult.ALREADY_WATCHING), actual);
        verify(taskManager, times(1)).runTaskAsynchronously(anyString(), any());
    }

    @Test
    void unwatchPluginsAssociatedWithShouldStopAllAssociatedPlugins() {
        watchManager.watchPlugins(mock(VHRAudience.class), List.of(pluginA, pluginB));
        when(taskManager.cancelTask(anyString())).thenReturn(true);

        PluginWatchResults results = watchManager.unwatchPluginsAssociatedWith("plugin-a");
        List<WatchResult> actual = extract(results);

        assertEquals(List.of(WatchResult.STOPPED, WatchResult.STOPPED), actual);
        assertTrue(actual.stream().allMatch(result -> result == WatchResult.STOPPED));
    }

    @Test
    void unwatchPluginsAssociatedWithShouldReturnNotWatchingWhenUnknown() {
        PluginWatchResults results = watchManager.unwatchPluginsAssociatedWith("missing");
        assertEquals(List.of(WatchResult.NOT_WATCHING), extract(results));
    }

    private static List<WatchResult> extract(PluginWatchResults results) {
        List<WatchResult> extracted = new ArrayList<>();
        for (PluginWatchResult result : results) {
            extracted.add(result.result());
        }
        return extracted;
    }
}
