package nl.hauntedmc.velocityhotreloader.entities.results;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseablePluginResult<T> extends PluginResult<T> implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseablePluginResult.class);
    private final List<Closeable> closeables;

    public CloseablePluginResult(String pluginId, Result result) {
        super(pluginId, result);
        this.closeables = Collections.emptyList();
    }

    public CloseablePluginResult(
            String pluginId,
            T plugin,
            Result result,
            List<Closeable> closeables,
            TagResolver... placeholders
    ) {
        super(pluginId, plugin, result, placeholders);
        this.closeables = closeables;
    }

    /**
     * Attempts to close the closable, essentially wrapping it with try-catch.
     */
    public void tryClose() {
        if (closeables == null) return;
        try {
            close();
        } catch (IOException ex) {
            LOGGER.warn("Failed to close plugin resources for '{}'", getPluginId(), ex);
        }
    }

    /**
     * Closes the closable.
     */
    @Override
    public void close() throws IOException {
        for (Closeable closeable : closeables) {
            closeable.close();
        }
    }
}
