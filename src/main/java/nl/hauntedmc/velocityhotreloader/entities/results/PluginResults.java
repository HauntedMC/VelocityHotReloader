package nl.hauntedmc.velocityhotreloader.entities.results;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nl.hauntedmc.velocityhotreloader.config.ConfigKey;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.NonNull;

public class PluginResults<T> implements Iterable<PluginResult<T>> {

    private static final TagResolver[] EMPTY = new TagResolver[0];

    protected final List<PluginResult<T>> results;

    public PluginResults() {
        this.results = new ArrayList<>();
    }

    public PluginResults<T> addResult(String pluginId, Result result) {
        addResult(pluginId, null, result, EMPTY);
        return this;
    }

    public PluginResults<T> addResult(String pluginId, Result result, TagResolver... placeholders) {
        addResult(pluginId, null, result, placeholders);
        return this;
    }

    public PluginResults<T> addResult(String pluginId, T plugin, TagResolver... placeholders) {
        addResult(pluginId, plugin, Result.SUCCESS, placeholders);
        return this;
    }

    protected PluginResults<T> addResult(String pluginId, T plugin, Result result, TagResolver... placeholders) {
        addResult(new PluginResult<>(pluginId, plugin, result, placeholders));
        return this;
    }

    public PluginResults<T> addResult(PluginResult<T> pluginResult) {
        this.results.add(pluginResult);
        return this;
    }

    public boolean isSuccess() {
        return results.stream().allMatch(PluginResult::isSuccess);
    }

    public List<PluginResult<T>> getResults() {
        return results;
    }

    /**
     * Creates an array of all plugins.
     * @throws IllegalArgumentException Iff a result was not successful (check {@link PluginResults#isSuccess()} first!)
     */
    public List<T> getPlugins() {
        List<T> plugins = new ArrayList<>(results.size());
        for (PluginResult<T> result : results) {
            if (!result.isSuccess()) throw new IllegalArgumentException(
                    "Result after handling plugin '" + result.getPluginId() + "' was not successful!"
            );
            plugins.add(result.getPlugin());
        }
        return plugins;
    }

    public PluginResult<T> first() {
        return results.getFirst();
    }

    public PluginResult<T> last() {
        return results.getLast();
    }

    /**
     * Sends the result(s) to the given sender.
     */
    public void sendTo(VHRAudience<?> sender, ConfigKey successKey) {
        if (!isSuccess()) {
            last().sendTo(sender, successKey);
            return;
        }

        for (PluginResult<T> result : results) {
            result.sendTo(sender, successKey);
        }
    }

    @Override
    public @NonNull Iterator<PluginResult<T>> iterator() {
        return results.iterator();
    }
}
