package nl.hauntedmc.velocityhotreloader.tasks;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.entities.AbstractTask;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.entities.exceptions.InvalidPluginDescriptionException;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResult;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.WatchResult;
import nl.hauntedmc.velocityhotreloader.utils.FileUtils;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.VelocityPluginDescription;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PluginWatcherTask extends AbstractTask {

    private static final WatchEvent.Kind<?>[] EVENTS = new WatchEvent.Kind[]{
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    };

    private final VelocityHotReloaded plugin;
    private final VHRAudience<?> sender;
    private final Map<String, WatchEntry> fileNameToWatchEntryMap;
    private final Map<String, WatchEntry> pluginIdToWatchEntryMap;

    private final AtomicBoolean run = new AtomicBoolean(true);
    private WatchService watchService;
    private ScheduledTask task = null;

    /**
     * Constructs a new PluginWatcherTask for the specified plugin.
     */
    public PluginWatcherTask(VelocityHotReloaded plugin, VHRAudience<?> sender, List<PluginContainer> plugins) {
        this.plugin = plugin;
        this.sender = sender;
        this.fileNameToWatchEntryMap = new HashMap<>();
        this.pluginIdToWatchEntryMap = new HashMap<>();

        VelocityPluginManager pluginManager = plugin.getPluginManager();
        for (PluginContainer watchPlugin : plugins) {
            File file = pluginManager.getPluginFile(watchPlugin);
            if (file == null) {
                continue;
            }

            WatchEntry entry = new WatchEntry(pluginManager.getPluginId(watchPlugin));
            entry.update(file);

            this.fileNameToWatchEntryMap.put(file.getName(), entry);
        }
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            this.watchService = watchService;

            VelocityPluginManager pluginManager = plugin.getPluginManager();
            Path basePath = pluginManager.getPluginsFolder().toPath();
            basePath.register(watchService, EVENTS);

            while (run.get()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path path = basePath.resolve((Path) event.context());

                    if (!Files.isDirectory(path)) {
                        handleWatchEvent(path);
                    }
                }

                if ((fileNameToWatchEntryMap.isEmpty() && pluginIdToWatchEntryMap.isEmpty()) || !key.reset()) {
                    send(WatchResult.STOPPED);
                    break;
                }
            }
        } catch (IOException ex) {
            plugin.getSlf4jLogger().error("File watcher failed unexpectedly", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            //
        }
    }

    private void handleWatchEvent(Path path) {
        String fileName = path.getFileName().toString();
        WatchEntry entry = fileNameToWatchEntryMap.get(fileName);

        if (entry == null && Files.exists(path)) {
            Optional<VelocityPluginDescription> descriptionOptional;
            try {
                descriptionOptional = plugin.getPluginManager().getPluginDescription(path.toFile());
            } catch (InvalidPluginDescriptionException ignored) {
                return;
            }

            descriptionOptional.ifPresent(description -> {
                WatchEntry foundEntry = pluginIdToWatchEntryMap.remove(description.getId());
                if (foundEntry != null) {
                    send(WatchResult.DELETED_FILE_IS_CREATED, Placeholder.unparsed("plugin", foundEntry.pluginId));
                    fileNameToWatchEntryMap.put(fileName, foundEntry);

                    if (pluginIdToWatchEntryMap.isEmpty()) {
                        checkWatchEntry(foundEntry, fileName);
                    }
                }
            });
            return;
        }

        if (entry != null) {
            checkWatchEntry(entry, fileName);
        }
    }

    private void checkWatchEntry(WatchEntry entry, String fileName) {
        if (task != null) {
            plugin.getTaskManager().cancelTask(task);
        }

        VelocityPluginManager pluginManager = plugin.getPluginManager();
        File pluginFile = pluginManager.getPluginFile(entry.pluginId).orElse(null);
        if (pluginFile == null) {
            send(WatchResult.FILE_DELETED, Placeholder.unparsed("plugin", entry.pluginId));

            fileNameToWatchEntryMap.remove(fileName);
            pluginIdToWatchEntryMap.put(entry.pluginId, entry);
            return;
        }

        String previousHash = entry.hash;
        long previousTimestamp = entry.timestamp;
        entry.update(pluginFile);

        // Debounce multiple rapid file system events into a single reload operation.
        task = plugin.getTaskManager().runTaskLater(() -> {
            if (entry.hash.equals(previousHash) || previousTimestamp < entry.timestamp - 1000L) {
                send(WatchResult.CHANGE);

                List<PluginContainer> plugins = new ArrayList<>(fileNameToWatchEntryMap.size());
                Map<String, WatchEntry> retainedWatchEntries = new HashMap<>();
                for (WatchEntry oldEntry : fileNameToWatchEntryMap.values()) {
                    PluginContainer pluginContainer = pluginManager.getPlugin(oldEntry.pluginId).orElse(null);
                    if (pluginContainer == null) {
                        continue;
                    }

                    plugins.add(pluginContainer);
                    retainedWatchEntries.put(oldEntry.pluginId, oldEntry);
                }

                fileNameToWatchEntryMap.clear();

                PluginResults<PluginContainer> reloadResults = pluginManager.reloadPlugins(plugins);
                reloadResults.sendTo(sender, MessageKey.RELOADPLUGIN_SUCCESS);

                for (PluginResult<PluginContainer> reloadResult : reloadResults) {
                    if (!reloadResult.isSuccess()) continue;

                    PluginContainer reloadedPlugin = reloadResult.getPlugin();
                    String pluginId = pluginManager.getPluginId(reloadedPlugin);

                    WatchEntry retainedEntry = retainedWatchEntries.get(pluginId);
                    File reloadedPluginFile = pluginManager.getPluginFile(reloadedPlugin);
                    if (reloadedPluginFile == null) {
                        continue;
                    }
                    String pluginFileName = reloadedPluginFile.getName();
                    fileNameToWatchEntryMap.put(pluginFileName, retainedEntry);
                }
            }
        }, 10L);
    }

    private void send(WatchResult result, TagResolver... tagResolvers) {
        result.sendTo(sender, tagResolvers);
        if (sender.isPlayer()) {
            result.sendTo(plugin.getChatProvider().getConsoleServerAudience(), tagResolvers);
        }
    }

    @Override
    public void cancel() {
        run.set(false);
        if (watchService == null) {
            return;
        }

        try {
            watchService.close();
        } catch (IOException ex) {
            plugin.getSlf4jLogger().warn("Failed to close watch service", ex);
        }
    }

    private static final class WatchEntry {

        private final String pluginId;
        private String hash = null;
        private long timestamp = 0L;

        public WatchEntry(String pluginId) {
            this.pluginId = pluginId;
        }

        public void update(File file) {
            this.hash = FileUtils.getHash(file.toPath());
            this.timestamp = System.currentTimeMillis();
        }
    }
}
