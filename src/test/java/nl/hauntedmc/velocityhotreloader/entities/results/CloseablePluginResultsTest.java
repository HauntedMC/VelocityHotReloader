package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CloseablePluginResultsTest {

    @Test
    void addResultShouldWrapRegularPluginResult() {
        PluginResult<String> base = new PluginResult<>("plugin", "instance", Result.SUCCESS);
        CloseablePluginResults<String> results = new CloseablePluginResults<>();

        results.addResult(base);

        assertInstanceOf(CloseablePluginResult.class, results.first());
        assertInstanceOf(CloseablePluginResult.class, results.last());
    }

    @Test
    void closeShouldCloseContainedCloseableResults() throws IOException {
        Closeable closeable = mock(Closeable.class);
        CloseablePluginResults<String> results = new CloseablePluginResults<>();
        results.addResult("plugin", "instance", List.of(closeable));

        results.close();

        verify(closeable).close();
    }

    @Test
    void tryCloseShouldSuppressIoExceptions() throws IOException {
        Closeable failing = mock(Closeable.class);
        doThrow(new IOException("boom")).when(failing).close();

        CloseablePluginResults<String> results = new CloseablePluginResults<>();
        results.addResult("plugin", "instance", List.of(failing));

        assertDoesNotThrow(results::tryClose);
        verify(failing).close();
    }
}
