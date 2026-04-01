package nl.hauntedmc.velocityhotreloader.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nl.hauntedmc.velocityhotreloader.providers.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VHRConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void setShouldReplaceInvalidParentSections() {
        JsonConfig config = new JsonConfig(new JsonObject());
        config.setUnsafe("parent", "leaf");

        config.set("parent.child", 9);

        assertInstanceOf(JsonConfig.class, config.get("parent"));
        assertEquals(9, config.getInt("parent.child"));
    }

    @Test
    void addDefaultsShouldPopulateMissingValues() {
        JsonConfig defaults = new JsonConfig(JsonParser.parseString("""
                {
                  "enabled": true,
                  "count": 3,
                  "nested": {"value": "x"},
                  "list": ["a", "b"]
                }
                """).getAsJsonObject());
        JsonConfig conf = new JsonConfig(new JsonObject());

        VHRConfig.addDefaults(defaults, conf);

        assertTrue(conf.getBoolean("enabled"));
        assertEquals(3, conf.getInt("count"));
        assertEquals("x", conf.getString("nested.value"));
        assertEquals(List.of("a", "b"), conf.getStringList("list"));
    }

    @Test
    void removeOldKeysShouldDeleteUnknownEntriesRecursively() {
        JsonConfig defaults = new JsonConfig(JsonParser.parseString("""
                {"keep": 1, "section": {"keep2": 2}}
                """).getAsJsonObject());
        JsonConfig conf = new JsonConfig(JsonParser.parseString("""
                {"keep": 1, "drop": 0, "section": {"keep2": 2, "drop2": 3}}
                """).getAsJsonObject());

        VHRConfig.removeOldKeys(defaults, conf);

        assertEquals(1, conf.getInt("keep"));
        assertNull(conf.getJsonElement("drop"));
        assertEquals(2, conf.getInt("section.keep2"));
        assertNull(conf.getJsonElement("section.drop2"));
    }

    @Test
    void initShouldWrapSaveExceptions() throws IOException {
        VHRConfig defaults = mock(VHRConfig.class);
        when(defaults.getKeys()).thenReturn(List.of());

        VHRConfig conf = mock(VHRConfig.class);
        when(conf.getKeys()).thenReturn(List.of());
        when(conf.get("any")).thenReturn(null);
        when(conf.getMap("any")).thenReturn(Map.of());
        when(conf.getStringList("any")).thenReturn(List.of());
        when(conf.getString("any")).thenReturn(null);
        when(conf.getBoolean("any")).thenReturn(false);
        when(conf.getInt("any")).thenReturn(-1);
        org.mockito.Mockito.doThrow(new IOException("boom")).when(conf).save();

        assertThrows(UncheckedIOException.class, () -> VHRConfig.init(defaults, conf));
    }

    @Test
    void initWithProviderShouldCreateConfigFileAndLoadIt() {
        VHRConfig defaults = mock(VHRConfig.class);
        when(defaults.getKeys()).thenReturn(List.of());

        VHRConfig loaded = mock(VHRConfig.class);
        when(loaded.getKeys()).thenReturn(List.of());

        ResourceProvider provider = mock(ResourceProvider.class);
        when(provider.load(org.mockito.ArgumentMatchers.any(File.class))).thenReturn(loaded);

        Path path = tempDir.resolve("nested").resolve("config.json");
        VHRConfig result = VHRConfig.init(defaults, provider, path);

        assertTrue(path.toFile().exists());
        assertEquals(loaded, result);
        verify(provider).load(path.toFile());
    }
}
