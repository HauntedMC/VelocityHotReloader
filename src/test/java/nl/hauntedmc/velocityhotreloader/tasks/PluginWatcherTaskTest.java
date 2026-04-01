package nl.hauntedmc.velocityhotreloader.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.plugin.PluginContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginWatcherTaskTest {

    @TempDir
    Path tempDir;

    @Test
    void constructorShouldTrackPluginsWithExistingFilesOnly() throws Exception {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        VelocityPluginManager pluginManager = mock(VelocityPluginManager.class);
        PluginContainer pluginA = mock(PluginContainer.class);
        PluginContainer pluginB = mock(PluginContainer.class);
        Path file = tempDir.resolve("tracked.jar");
        java.nio.file.Files.writeString(file, "content");
        when(plugin.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPluginFile(pluginA)).thenReturn(file.toFile());
        when(pluginManager.getPluginFile(pluginB)).thenReturn(null);
        when(pluginManager.getPluginId(pluginA)).thenReturn("plugin-a");

        PluginWatcherTask watcher = new PluginWatcherTask(plugin, mock(VHRAudience.class), List.of(pluginA, pluginB));

        @SuppressWarnings("unchecked")
        Map<String, ?> trackedByFile = (Map<String, ?>) field(watcher, "fileNameToWatchEntryMap");
        @SuppressWarnings("unchecked")
        Map<String, ?> trackedById = (Map<String, ?>) field(watcher, "pluginIdToWatchEntryMap");
        assertEquals(1, trackedByFile.size());
        assertTrue(trackedByFile.containsKey("tracked.jar"));
        assertTrue(trackedById.isEmpty());
    }

    @Test
    void cancelShouldStopLoopEvenWithoutWatchService() throws Exception {
        PluginWatcherTask watcher = new PluginWatcherTask(
                mock(VelocityHotReloaded.class),
                mock(VHRAudience.class),
                List.of()
        );

        watcher.cancel();
        AtomicBoolean run = (AtomicBoolean) field(watcher, "run");
        assertFalse(run.get());
    }

    @Test
    void cancelShouldCloseWatchServiceWhenPresent() throws Exception {
        PluginWatcherTask watcher = new PluginWatcherTask(
                mock(VelocityHotReloaded.class),
                mock(VHRAudience.class),
                List.of()
        );
        WatchService watchService = mock(WatchService.class);
        setField(watcher, "watchService", watchService);

        watcher.cancel();

        verify(watchService).close();
        AtomicBoolean run = (AtomicBoolean) field(watcher, "run");
        assertFalse(run.get());
    }

    @Test
    void cancelShouldIgnoreCloseFailures() throws Exception {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        when(plugin.getSlf4jLogger()).thenReturn(mock(org.slf4j.Logger.class));
        PluginWatcherTask watcher = new PluginWatcherTask(plugin, mock(VHRAudience.class), List.of());
        WatchService watchService = mock(WatchService.class);
        org.mockito.Mockito.doThrow(new IOException("boom")).when(watchService).close();
        setField(watcher, "watchService", watchService);

        watcher.cancel();

        AtomicBoolean run = (AtomicBoolean) field(watcher, "run");
        assertFalse(run.get());
    }

    private static Object field(Object target, String name) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
