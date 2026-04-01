package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CloseablePluginResultTest {

    @Test
    void closeShouldCloseAllResources() throws IOException {
        Closeable a = mock(Closeable.class);
        Closeable b = mock(Closeable.class);

        CloseablePluginResult<String> result = new CloseablePluginResult<>(
                "plugin",
                "instance",
                Result.SUCCESS,
                List.of(a, b)
        );

        result.close();

        verify(a).close();
        verify(b).close();
    }

    @Test
    void tryCloseShouldSuppressIoExceptions() throws IOException {
        Closeable failing = mock(Closeable.class);
        doThrow(new IOException("boom")).when(failing).close();

        CloseablePluginResult<String> result = new CloseablePluginResult<>(
                "plugin",
                "instance",
                Result.SUCCESS,
                List.of(failing)
        );

        assertDoesNotThrow(result::tryClose);
        verify(failing).close();
    }
}
