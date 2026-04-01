package nl.hauntedmc.velocityhotreloader.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import org.junit.jupiter.api.Test;

class VHRResourceTest {

    @Test
    void migrateShouldWriteConfigVersionAndSave() throws IOException {
        VHRConfig conf = mock(VHRConfig.class);
        JsonConfig defaults = new JsonConfig(JsonParser.parseString("""
                {"config-version": 42}
                """).getAsJsonObject());

        TestResource resource = new TestResource(mock(VelocityHotReloaded.class), conf, defaults);
        resource.migrate();

        verify(conf).set("config-version", 42);
        verify(conf).save();
    }

    @Test
    void migrateShouldWrapIoExceptions() throws IOException {
        VHRConfig conf = mock(VHRConfig.class);
        org.mockito.Mockito.doThrow(new IOException("boom")).when(conf).save();
        JsonConfig defaults = new JsonConfig(JsonParser.parseString("""
                {"config-version": 1}
                """).getAsJsonObject());

        TestResource resource = new TestResource(mock(VelocityHotReloaded.class), conf, defaults);
        assertThrows(UncheckedIOException.class, resource::migrate);
    }

    private static final class TestResource extends VHRResource {

        private TestResource(VelocityHotReloaded plugin, VHRConfig config, JsonConfig defaultConfig) {
            super(plugin, config, defaultConfig);
        }
    }
}
