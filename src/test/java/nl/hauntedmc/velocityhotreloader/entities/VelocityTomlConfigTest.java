package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class VelocityTomlConfigTest {

    @Test
    void shouldProvideUnifiedAccessorsAndMutators() {
        CommentedConfig root = CommentedConfig.inMemory();
        CommentedConfig section = CommentedConfig.inMemory();
        section.set("value", 12);
        root.set("section", section);
        root.set("name", "demo");
        root.set("enabled", true);
        root.set("list", List.of("x", "y"));

        VelocityTomlConfig config = new VelocityTomlConfig(root, new File("ignored.toml"));

        assertInstanceOf(VelocityTomlConfig.class, config.get("section"));
        assertEquals("demo", config.getString("name"));
        assertTrue(config.getBoolean("enabled"));
        assertEquals(12, config.getInt("section.value"));
        assertEquals(List.of("x", "y"), config.getStringList("list"));
        assertTrue(config.getMap("section").containsKey("value"));

        config.setUnsafe("new.path", "value");
        assertEquals("value", config.getString("new.path"));
        config.remove("new.path");
        assertNull(config.get("new.path"));
    }

    @Test
    void saveShouldThrowForNonFileConfig() {
        VelocityTomlConfig config = new VelocityTomlConfig(CommentedConfig.inMemory(), new File("ignored.toml"));
        assertThrows(IOException.class, config::save);
    }

    @Test
    void saveShouldDelegateToUnderlyingFileConfig() throws IOException {
        CommentedFileConfig fileConfig = mock(CommentedFileConfig.class);
        VelocityTomlConfig config = new VelocityTomlConfig(fileConfig, new File("ignored.toml"));

        config.save();

        verify(fileConfig).save();
    }
}
