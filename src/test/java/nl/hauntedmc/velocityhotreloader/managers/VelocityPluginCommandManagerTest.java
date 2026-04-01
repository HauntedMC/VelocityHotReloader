package nl.hauntedmc.velocityhotreloader.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VelocityPluginCommandManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void loadShouldReturnEmptyManagerWhenFileDoesNotExist() throws IOException {
        VelocityPluginCommandManager manager = VelocityPluginCommandManager.load(tempDir.resolve("missing.json"));
        assertTrue(manager.getPluginCommands().isEmpty());
        assertEquals(Optional.empty(), manager.findPluginId("alias"));
    }

    @Test
    void saveAndLoadShouldPersistPluginCommandMappings() throws IOException {
        Path file = tempDir.resolve("nested").resolve("commands.json");
        VelocityPluginCommandManager manager = new VelocityPluginCommandManager(file);
        manager.getPluginCommands().put("plugin-a", "a");
        manager.getPluginCommands().put("plugin-a", "aa");
        manager.getPluginCommands().put("plugin-b", "b");

        manager.save();
        assertTrue(Files.exists(file));

        VelocityPluginCommandManager loaded = VelocityPluginCommandManager.load(file);
        assertEquals(Optional.of("plugin-a"), loaded.findPluginId("a"));
        assertEquals(Optional.of("plugin-a"), loaded.findPluginId("aa"));
        assertEquals(Optional.of("plugin-b"), loaded.findPluginId("b"));
        assertFalse(loaded.findPluginId("missing").isPresent());
    }
}
