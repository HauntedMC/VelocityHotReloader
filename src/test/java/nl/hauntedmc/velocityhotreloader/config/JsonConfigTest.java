package nl.hauntedmc.velocityhotreloader.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nl.hauntedmc.velocityhotreloader.providers.ResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadNestedValuesAndSupportMutations() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, """
                {
                  "name": "test",
                  "enabled": true,
                  "list": ["a", "b"],
                  "section": { "number": 7 }
                }
                """);

        JsonConfig config = new JsonConfig(file.toFile());
        assertEquals("test", config.getString("name"));
        assertTrue(config.getBoolean("enabled"));
        assertEquals(7, config.getInt("section.number"));
        assertInstanceOf(JsonConfig.class, config.get("section"));
        assertEquals(List.of("a", "b"), config.getStringList("list"));
        assertEquals(7.0d, config.getMap("section").get("number"));

        config.setUnsafe("section.extra.value", 99);
        assertEquals(99, config.getInt("section.extra.value"));
        config.remove("section.extra.value");
        assertEquals(-1, config.getInt("section.extra.value"));

        config.save();
        String saved = Files.readString(file);
        assertTrue(saved.contains("\"name\":\"test\""));
    }

    @Test
    void toObjectValueShouldHandlePrimitivesAndArrays() {
        assertEquals(true, JsonConfig.toObjectValue(JsonParser.parseString("true")));
        assertEquals(3, JsonConfig.toObjectValue(JsonParser.parseString("3")));
        assertEquals(3.5d, JsonConfig.toObjectValue(JsonParser.parseString("3.5")));
        assertEquals("hello", JsonConfig.toObjectValue(JsonParser.parseString("\"hello\"")));
        assertEquals(
                List.of("1", "true", "hello"),
                JsonConfig.toObjectValue(JsonParser.parseString("[1, true, \"hello\"]"))
        );
        assertNull(JsonConfig.toObjectValue(null));
    }

    @Test
    void getShouldReturnJsonElementForPrimitivePath() {
        JsonObject root = JsonParser.parseString("{\"value\": 10}").getAsJsonObject();
        JsonConfig config = new JsonConfig(root);

        Object value = config.get("value");
        assertInstanceOf(JsonElement.class, value);
        assertEquals(10, ((JsonElement) value).getAsInt());
    }

    @Test
    void loadShouldMergeVelocityDefaultsWithoutOverridingGeneralValues() {
        InMemoryProvider provider = new InMemoryProvider(Map.of(
                "sample.json", """
                        {"a":1,"nested":{"v":"general"}}
                        """,
                "velocity-sample.json", """
                        {"nested":{"v":"velocity"},"extra":"fromVelocity"}
                        """
        ));

        JsonConfig loaded = JsonConfig.load(provider, "sample");

        assertEquals(1, loaded.getInt("a"));
        assertEquals("general", loaded.getString("nested.v"));
        assertEquals("fromVelocity", loaded.getString("extra"));
    }

    @Test
    void loadShouldThrowWhenGeneralResourceIsMissing() {
        InMemoryProvider provider = new InMemoryProvider(Map.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> JsonConfig.load(provider, "missing")
        );
        assertTrue(ex.getMessage().contains("missing.json"));
    }

    @Test
    void missingValuesShouldUseFallbackDefaults() throws IOException {
        Path file = tempDir.resolve("empty.json");
        Files.writeString(file, "{}");

        JsonConfig config = new JsonConfig(file.toFile());
        assertNull(config.getString("missing"));
        assertFalse(config.getBoolean("missing"));
        assertEquals(-1, config.getInt("missing"));
    }

    private record InMemoryProvider(Map<String, String> resources) implements ResourceProvider {

        @Override
            public InputStream getRawResource(String resource) {
                String value = resources.get(resource);
                if (value == null) {
                    return null;
                }
                return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public VHRConfig load(File file) {
                throw new UnsupportedOperationException("Not needed for this test");
            }

            @Override
            public String getResourceExtension() {
                return ".json";
            }
        }
}
