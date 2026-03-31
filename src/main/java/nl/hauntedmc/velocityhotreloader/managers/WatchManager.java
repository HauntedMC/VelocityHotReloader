package nl.hauntedmc.velocityhotreloader.managers;

import com.velocitypowered.api.plugin.PluginContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginWatchResults;
import nl.hauntedmc.velocityhotreloader.entities.results.WatchResult;
import nl.hauntedmc.velocityhotreloader.tasks.PluginWatcherTask;
import nl.hauntedmc.velocityhotreloader.VHR;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class WatchManager {

    private final VHR plugin;
    private final Map<String, WatchTask> watchTasks;

    public WatchManager(VHR plugin) {
        this.plugin = plugin;
        this.watchTasks = new HashMap<>();
    }

    /**
     * Starts watching the specified plugin and reloads it when a change is detected.
     */
    public PluginWatchResults watchPlugins(VHRAudience<?> sender, List<PluginContainer> plugins) {
        List<String> pluginIds = new ArrayList<>(plugins.size());
        for (PluginContainer watchPlugin : plugins) {
            String pluginId = plugin.getPluginManager().getPluginId(watchPlugin);
            if (watchTasks.containsKey(pluginId)) {
                return new PluginWatchResults().add(WatchResult.ALREADY_WATCHING, Placeholder.unparsed("plugin", pluginId));
            }

            pluginIds.add(plugin.getPluginManager().getPluginId(watchPlugin));
        }

        UUID key = UUID.randomUUID();
        plugin.getTaskManager().runTaskAsynchronously(
                key.toString(),
                new PluginWatcherTask(plugin, sender, plugins)
        );

        WatchTask watchTask = new WatchTask(key, pluginIds);
        for (String pluginId : pluginIds) {
            watchTasks.put(pluginId, watchTask);
        }

        PluginWatchResults watchResults = new PluginWatchResults();
        for (String pluginId : pluginIds) {
            watchResults.add(WatchResult.START, Placeholder.unparsed("plugin", pluginId));
        }
        return watchResults;
    }

    /**
     * Stops watching plugins for changes.
     */
    public PluginWatchResults unwatchPluginsAssociatedWith(String associatedPluginId) {
        WatchTask task = watchTasks.get(associatedPluginId);
        if (task != null && plugin.getTaskManager().cancelTask(task.key.toString())) {
            task.pluginIds.forEach(watchTasks::remove);

            PluginWatchResults watchResults = new PluginWatchResults();
            for (String pluginId : task.pluginIds) {
                watchResults.add(WatchResult.STOPPED, Placeholder.unparsed("plugin", pluginId));
            }
            return watchResults;
        }
        return new PluginWatchResults().add(WatchResult.NOT_WATCHING, Placeholder.unparsed("plugin", associatedPluginId));
    }

    private static final class WatchTask {

        private final UUID key;
        private final List<String> pluginIds;

        private WatchTask(UUID key, List<String> pluginIds) {
            this.key = key;
            this.pluginIds = pluginIds;
        }
    }
}
