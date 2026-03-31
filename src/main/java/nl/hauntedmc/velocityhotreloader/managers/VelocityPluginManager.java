package nl.hauntedmc.velocityhotreloader.managers;

import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.hauntedmc.velocityhotreloader.entities.results.CloseablePluginResult;
import nl.hauntedmc.velocityhotreloader.entities.exceptions.InvalidPluginDescriptionException;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResult;
import nl.hauntedmc.velocityhotreloader.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.Result;
import nl.hauntedmc.velocityhotreloader.utils.DependencyUtils;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.VelocityPluginDescription;
import nl.hauntedmc.velocityhotreloader.events.VelocityPluginDisableEvent;
import nl.hauntedmc.velocityhotreloader.events.VelocityPluginEnableEvent;
import nl.hauntedmc.velocityhotreloader.events.VelocityPluginEvent;
import nl.hauntedmc.velocityhotreloader.events.VelocityPluginLoadEvent;
import nl.hauntedmc.velocityhotreloader.events.VelocityPluginUnloadEvent;
import nl.hauntedmc.velocityhotreloader.reflection.RJavaPluginLoader;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityCommandManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityConsole;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityEventManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityPluginContainer;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityPluginManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;

/**
 * Handles plugin lifecycle operations (load/enable/disable/reload/unload) for Velocity plugins.
 */
public class VelocityPluginManager {

    private final ProxyServer proxy;
    private final Logger logger;
    private final VelocityPluginCommandManager pluginCommandManager;

    /**
     * Constructs a new VelocityPluginManager.
     */
    public VelocityPluginManager(ProxyServer proxy, Logger logger, VelocityPluginCommandManager pluginCommandManager) {
        this.proxy = proxy;
        this.logger = logger;
        this.pluginCommandManager = pluginCommandManager;
    }

    public File getPluginsFolder() {
        return VelocityHotReloaded.getInstance().getDataFolder().getParentFile();
    }

    public List<PluginContainer> getPlugins() {
        return new ArrayList<>(proxy.getPluginManager().getPlugins());
    }

    public List<PluginContainer> getPluginsSorted() {
        List<PluginContainer> plugins = getPlugins();
        plugins.sort(Comparator.comparing(this::getPluginId));
        return plugins;
    }

    public List<String> getPluginNames() {
        return getPlugins().stream()
                .map(this::getPluginId)
                .collect(Collectors.toList());
    }

    public String getPluginId(PluginContainer plugin) {
        return plugin.getDescription().getId();
    }

    public File getPluginFile(PluginContainer plugin) {
        return plugin.getDescription().getSource()
                .map(Path::toFile)
                .orElse(null);
    }

    public Optional<File> getPluginFile(String pluginName) {
        File pluginsFolder = getPluginsFolder();
        if (pluginsFolder == null) {
            return Optional.empty();
        }

        Object javaPluginLoader = RJavaPluginLoader.newInstance(proxy, pluginsFolder.toPath());

        for (File file : getPluginJars()) {
            PluginDescription desc = RJavaPluginLoader.loadPluginDescription(javaPluginLoader, file.toPath());
            if (desc.getId().equals(pluginName)) {
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }

    public Optional<PluginContainer> getPlugin(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName);
    }

    public List<PluginContainer> getPluginsDependingOn(String pluginId) {
        List<PluginContainer> plugins = new ArrayList<>();
        for (PluginContainer loadedPlugin : getPlugins()) {
            VelocityPluginDescription description = getLoadedPluginDescription(loadedPlugin);
            if (description.getDependencies().contains(pluginId)) {
                plugins.add(loadedPlugin);
            }
        }
        return plugins;
    }

    public VelocityPluginDescription getLoadedPluginDescription(PluginContainer plugin) {
        return new VelocityPluginDescription(plugin.getDescription());
    }

    public Optional<VelocityPluginDescription> getPluginDescription(
            String pluginId
    ) throws InvalidPluginDescriptionException {
        File pluginFile = getPluginFile(pluginId).orElse(null);
        if (pluginFile == null) {
            return Optional.empty();
        }

        return getPluginDescription(pluginFile);
    }

    public Optional<VelocityPluginDescription> getPluginDescription(
            File file
    ) throws InvalidPluginDescriptionException {
        Path source = file.toPath();
        Path baseDirectory = source.getParent();

        try {
            Object javaPluginLoader = RJavaPluginLoader.newInstance(proxy, baseDirectory);
            PluginDescription candidate = RJavaPluginLoader.loadPluginDescription(javaPluginLoader, source);
            return Optional.of(new VelocityPluginDescription(candidate));
        } catch (Exception ex) {
            throw new InvalidPluginDescriptionException(ex);
        }
    }

    public Object getInstance(PluginContainer plugin) {
        return plugin.getInstance().orElse(null);
    }

    public Set<String> getCommands() {
        return RVelocityCommandManager.getDispatcher(proxy.getCommandManager()).getRoot().getChildren().stream()
                .map(CommandNode::getName)
                .collect(Collectors.toSet());
    }

    public File[] getPluginJars() {
        File parent = getPluginsFolder();
        if (parent == null || !parent.exists()) {
            return new File[0];
        }

        File[] files = parent.listFiles(f -> f.getName().endsWith(".jar"));
        return files != null ? files : new File[0];
    }

    public List<String> getPluginFileNames() {
        return Arrays.stream(getPluginJars())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public PluginResult<PluginContainer> loadPlugin(String pluginFile) {
        File file = new File(getPluginsFolder(), pluginFile);
        if (!file.exists()) {
            return new PluginResult<>(pluginFile, Result.NOT_EXISTS);
        }
        return loadPlugin(file);
    }

    public PluginResult<PluginContainer> loadPlugin(File file) {
        return loadPlugins(Collections.singletonList(file)).first();
    }

    public PluginResults<PluginContainer> loadPlugins(List<File> files) {
        List<VelocityPluginDescription> descriptions = new ArrayList<>(files.size());

        for (File file : files) {
            VelocityPluginDescription description;
            try {
                VelocityPluginDescription parsedDescription = getPluginDescription(file).orElse(null);
                if (parsedDescription == null) {
                    return new PluginResults<PluginContainer>().addResult(file.getName(), Result.NOT_EXISTS);
                }

                description = parsedDescription;
            } catch (InvalidPluginDescriptionException ex) {
                logger.warn("Invalid plugin description for '{}'", file, ex);
                return new PluginResults<PluginContainer>().addResult(file.getName(), Result.INVALID_DESCRIPTION);
            }

            if (getPlugin(description.getId()).isPresent()) {
                return new PluginResults<PluginContainer>().addResult(description.getId(), Result.ALREADY_LOADED);
            }

            descriptions.add(description);
        }

        List<VelocityPluginDescription> orderedDescriptions;
        try {
            orderedDescriptions = determineLoadOrder(descriptions);
        } catch (IllegalStateException ex) {
            logger.error("Failed to determine load order for plugin files: {}", files, ex);

            StringBuilder sb = new StringBuilder();
            for (File file : files) {
                sb.append(", ").append(file.getName());
            }

            return new PluginResults<PluginContainer>().addResult(sb.substring(2), Result.ERROR);
        }

        return loadPluginDescriptions(orderedDescriptions);
    }

    public PluginResult<PluginContainer> enablePlugin(String pluginId) {
        return getPlugin(pluginId)
                .map(this::enablePlugin)
                .orElse(new PluginResult<>(pluginId, Result.NOT_EXISTS));
    }

    public PluginResult<PluginContainer> enablePlugin(PluginContainer plugin) {
        return enablePlugins(Collections.singletonList(plugin)).first();
    }

    public PluginResults<PluginContainer> enablePlugins(List<PluginContainer> plugins) {
        PluginContainer invalidStatePlugin = checkPluginStates(plugins, false).orElse(null);
        if (invalidStatePlugin != null) {
            return new PluginResults<PluginContainer>().addResult(getPluginId(invalidStatePlugin), Result.ALREADY_ENABLED);
        }

        return enableOrderedPlugins(determineLoadOrder(plugins));
    }

    public boolean isPluginEnabled(PluginContainer plugin) {
        return isPluginEnabled(getPluginId(plugin));
    }

    public boolean isPluginEnabled(String pluginId) {
        return proxy.getPluginManager().isLoaded(pluginId);
    }

    protected Optional<PluginContainer> checkPluginStates(List<PluginContainer> plugins, boolean enabled) {
        for (PluginContainer plugin : plugins) {
            if (isPluginEnabled(plugin) != enabled) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public PluginResult<PluginContainer> disablePlugin(String pluginId) {
        return getPlugin(pluginId)
                .map(this::disablePlugin)
                .orElse(new PluginResult<>(pluginId, Result.NOT_EXISTS));
    }

    public PluginResult<PluginContainer> disablePlugin(PluginContainer plugin) {
        return disablePlugins(Collections.singletonList(plugin)).first();
    }

    public PluginResults<PluginContainer> disablePlugins(List<PluginContainer> plugins) {
        PluginContainer invalidStatePlugin = checkPluginStates(plugins, true).orElse(null);
        if (invalidStatePlugin != null) {
            return new PluginResults<PluginContainer>().addResult(getPluginId(invalidStatePlugin), Result.ALREADY_DISABLED);
        }

        List<PluginContainer> orderedPlugins;
        try {
            orderedPlugins = determineLoadOrder(plugins);
        } catch (IllegalStateException ex) {
            logger.error("Failed to determine disable order for plugins: {}", plugins, ex);

            StringBuilder sb = new StringBuilder();
            for (PluginContainer plugin : plugins) {
                sb.append(", ").append(getPluginId(plugin));
            }

            return new PluginResults<PluginContainer>().addResult(sb.substring(2), Result.ERROR);
        }

        Collections.reverse(orderedPlugins);
        return disableOrderedPlugins(orderedPlugins);
    }

    public PluginResult<PluginContainer> reloadPlugin(String pluginId) {
        return getPlugin(pluginId)
                .map(this::reloadPlugin)
                .orElse(new PluginResult<>(pluginId, Result.NOT_EXISTS));
    }

    public PluginResult<PluginContainer> reloadPlugin(PluginContainer plugin) {
        return reloadPlugins(Collections.singletonList(plugin)).first();
    }

    public PluginResults<PluginContainer> reloadPlugins(List<PluginContainer> plugins) {
        PluginResults<PluginContainer> disableResults = disablePlugins(plugins);
        for (PluginResult<PluginContainer> disableResult : disableResults.getResults()) {
            if (!disableResult.isSuccess() && disableResult.getResult() != Result.ALREADY_DISABLED) {
                return disableResults;
            }
        }

        List<String> pluginIds = new ArrayList<>(plugins.size());
        for (PluginContainer plugin : plugins) {
            pluginIds.add(getPluginId(plugin));
        }

        CloseablePluginResults<PluginContainer> unloadResults = unloadPlugins(plugins);
        if (!unloadResults.isSuccess()) {
            return unloadResults;
        }
        unloadResults.tryClose();

        List<File> pluginFiles = new ArrayList<>(plugins.size());
        for (String pluginId : pluginIds) {
            File pluginFile = getPluginFile(pluginId).orElse(null);
            if (pluginFile == null) {
                return new PluginResults<PluginContainer>().addResult(pluginId, Result.FILE_DELETED);
            }
            pluginFiles.add(pluginFile);
        }

        PluginResults<PluginContainer> loadResults = loadPlugins(pluginFiles);
        if (!loadResults.isSuccess()) {
            return loadResults;
        }

        List<PluginContainer> loadedPlugins = new ArrayList<>(pluginIds.size());
        for (PluginResult<PluginContainer> loadResult : loadResults) {
            loadedPlugins.add(loadResult.getPlugin());
        }

        return enablePlugins(loadedPlugins);
    }

    public CloseablePluginResult<PluginContainer> unloadPlugin(String pluginId) {
        return getPlugin(pluginId)
                .map(this::unloadPlugin)
                .orElse(new CloseablePluginResult<>(pluginId, Result.NOT_EXISTS));
    }

    public CloseablePluginResult<PluginContainer> unloadPlugin(PluginContainer plugin) {
        return unloadPlugins(Collections.singletonList(plugin)).first();
    }

    public CloseablePluginResults<PluginContainer> unloadPlugins(List<PluginContainer> plugins) {
        List<PluginContainer> orderedPlugins;
        try {
            orderedPlugins = determineLoadOrder(plugins);
        } catch (IllegalStateException ex) {
            logger.error("Failed to determine unload order for plugins: {}", plugins, ex);

            StringBuilder sb = new StringBuilder();
            for (PluginContainer plugin : plugins) {
                sb.append(", ").append(getPluginId(plugin));
            }

            return new CloseablePluginResults<PluginContainer>().addResult(sb.substring(2), Result.ERROR);
        }

        Collections.reverse(orderedPlugins);
        return unloadOrderedPlugins(orderedPlugins);
    }

    /**
     * Determines the load order of a list of plugins.
     */
    public List<PluginContainer> determineLoadOrder(List<PluginContainer> plugins) throws IllegalStateException {
        Map<VelocityPluginDescription, PluginContainer> descriptionMap = new HashMap<>(plugins.size());
        for (PluginContainer plugin : plugins) {
            descriptionMap.put(getLoadedPluginDescription(plugin), plugin);
        }

        List<PluginContainer> orderedPlugins = new ArrayList<>(plugins.size());
        for (VelocityPluginDescription description : determineLoadOrder(descriptionMap.keySet())) {
            orderedPlugins.add(descriptionMap.get(description));
        }
        return orderedPlugins;
    }

    /**
     * Determines the load order for a given collection of descriptions.
     * @throws IllegalStateException iff circular dependency
     */
    public List<VelocityPluginDescription> determineLoadOrder(
            Collection<? extends VelocityPluginDescription> descriptions
    ) throws IllegalStateException {
        Map<String, VelocityPluginDescription> pluginIdToDescriptionMap = new HashMap<>();
        for (VelocityPluginDescription description : descriptions) {
            pluginIdToDescriptionMap.put(description.getId(), description);
        }

        Map<VelocityPluginDescription, Set<VelocityPluginDescription>> dependencyMap = new HashMap<>(descriptions.size());
        for (VelocityPluginDescription description : descriptions) {
            Set<String> dependencyStrings = description.getDependencies();
            Set<VelocityPluginDescription> dependencies = new HashSet<>();

            for (String dependencyString : dependencyStrings) {
                VelocityPluginDescription dependency = pluginIdToDescriptionMap.get(dependencyString);
                if (dependency != null) {
                    dependencies.add(dependency);
                }
            }

            dependencyMap.put(description, dependencies);
        }

        return DependencyUtils.determineOrder(dependencyMap);
    }

    public PluginResults<PluginContainer> loadPluginDescriptions(List<VelocityPluginDescription> descriptions) {
        PluginResults<PluginContainer> loadResults = new PluginResults<>();

        for (VelocityPluginDescription description : descriptions) {
            Path source = description.getFile().toPath();
            Path baseDirectory = source.getParent();

            Object javaPluginLoader = RJavaPluginLoader.newInstance(proxy, baseDirectory);
            PluginDescription candidate = RJavaPluginLoader.loadPluginDescription(javaPluginLoader, source);

            dependencyCheck:
            for (PluginDependency dependency : candidate.getDependencies()) {
                String pluginId = dependency.getId();
                for (VelocityPluginDescription desc : descriptions) {
                    if (desc.getId().equals(pluginId)) continue dependencyCheck;
                }

                if (!dependency.isOptional() && !proxy.getPluginManager().isLoaded(dependency.getId())) {
                    logger.error(
                            "Can't load plugin {} due to missing dependency {}",
                            candidate.getId(),
                            dependency.getId()
                    );
                    return loadResults.addResult(description.getId(), Result.UNKNOWN_DEPENDENCY,
                            Placeholder.unparsed("dependency", dependency.getId())
                    );
                }
            }

            PluginDescription realPlugin = RJavaPluginLoader.loadPlugin(javaPluginLoader, candidate);
            PluginContainer container = RVelocityPluginContainer.newInstance(realPlugin);
            proxy.getEventManager().fire(new VelocityPluginLoadEvent(container, VelocityPluginEvent.Stage.PRE));
            proxy.getEventManager().fire(new VelocityPluginLoadEvent(container, VelocityPluginEvent.Stage.POST));

            loadResults.addResult(description.getId(), container);
        }

        return loadResults;
    }

    public PluginResults<PluginContainer> enableOrderedPlugins(List<PluginContainer> containers) {
        PluginResults<PluginContainer> enableResults = new PluginResults<>();

        List<Object> pluginInstances = new ArrayList<>(containers.size());
        for (PluginContainer container : containers) {
            String pluginId = container.getDescription().getId();
            proxy.getEventManager().fire(new VelocityPluginEnableEvent(container, VelocityPluginEvent.Stage.PRE));
            if (isPluginEnabled(pluginId)) {
                return enableResults.addResult(pluginId, Result.ALREADY_ENABLED);
            }

            Object javaPluginLoader = RJavaPluginLoader.newInstance(
                    proxy,
                    container.getDescription().getSource().map(Path::getParent).orElse(null)
            );
            PluginDescription realPlugin = container.getDescription();
            Module module = RJavaPluginLoader.createModule(javaPluginLoader, container);

            AbstractModule commonModule = new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ProxyServer.class).toInstance(proxy);
                    bind(PluginManager.class).toInstance(proxy.getPluginManager());
                    bind(EventManager.class).toInstance(proxy.getEventManager());
                    bind(CommandManager.class).toInstance(proxy.getCommandManager());
                    for (PluginContainer container : proxy.getPluginManager().getPlugins()) {
                        bind(PluginContainer.class)
                                .annotatedWith(Names.named(container.getDescription().getId()))
                                .toInstance(container);
                    }
                    for (PluginContainer container : containers) {
                        bind(PluginContainer.class)
                                .annotatedWith(Names.named(container.getDescription().getId()))
                                .toInstance(container);
                    }
                }
            };

            try {
                RJavaPluginLoader.createPlugin(javaPluginLoader, container, module, commonModule);
            } catch (Exception ex) {
                logger.error(
                        String.format("Can't create plugin %s", container.getDescription().getId()),
                        ex
                );
                return enableResults.addResult(pluginId, Result.ERROR);
            }

            logger.info(
                    "Loaded plugin {} {} by {}",
                    realPlugin.getId(),
                    realPlugin.getVersion().orElse("<UNKNOWN>"),
                    Joiner.on(", ").join(realPlugin.getAuthors())
            );

            RVelocityPluginManager.registerPlugin(proxy.getPluginManager(), container);
            Object pluginInstance = container.getInstance().orElse(null);
            if (pluginInstance != null) {
                RVelocityEventManager.registerInternally(proxy.getEventManager(), container, pluginInstance);
                pluginInstances.add(pluginInstance);
            }
        }

        RVelocityEventManager.fireForPlugins(
                proxy.getEventManager(),
                new ProxyInitializeEvent(),
                pluginInstances
        ).join();

        ConsoleCommandSource console = proxy.getConsoleCommandSource();
        PermissionsSetupEvent event = new PermissionsSetupEvent(
                console,
                s -> PermissionFunction.ALWAYS_TRUE
        );
        PermissionFunction permissionFunction = RVelocityEventManager.fireForPlugins(
                proxy.getEventManager(),
                event,
                pluginInstances
        ).join().createFunction(console);

        if (permissionFunction == null) {
            logger.error(
                    "A plugin permission provider {} provided an invalid permission function for the console."
                            + " This is a bug in the plugin, not in Velocity."
                            + " Falling back to the default permission function.",
                    event.getProvider().getClass().getName()
            );
            permissionFunction = PermissionFunction.ALWAYS_TRUE;
        }

        RVelocityConsole.setPermissionFunction(console, permissionFunction);

        for (PluginContainer container : containers) {
            proxy.getEventManager().fire(new VelocityPluginEnableEvent(container, VelocityPluginEvent.Stage.POST));
            enableResults.addResult(container.getDescription().getId(), container);
        }

        return enableResults;
    }

    public PluginResults<PluginContainer> disableOrderedPlugins(List<PluginContainer> containers) {
        PluginResults<PluginContainer> disableResults = new PluginResults<>();

        List<Object> pluginInstances = new ArrayList<>(containers.size());
        for (PluginContainer container : containers) {
            proxy.getEventManager().fire(new VelocityPluginDisableEvent(container, VelocityPluginEvent.Stage.PRE));
            String pluginId = getPluginId(container);
            Object pluginInstance = container.getInstance().orElse(null);
            if (pluginInstance == null) {
                return disableResults.addResult(pluginId, Result.ALREADY_DISABLED);
            }

            pluginInstances.add(pluginInstance);
        }

        RVelocityEventManager.fireForPlugins(
                proxy.getEventManager(),
                new ProxyShutdownEvent(),
                pluginInstances
        );

        for (PluginContainer container : containers) {
            proxy.getEventManager().fire(new VelocityPluginDisableEvent(container, VelocityPluginEvent.Stage.POST));
            disableResults.addResult(getPluginId(container), container);
        }

        return disableResults;
    }

    public CloseablePluginResults<PluginContainer> unloadOrderedPlugins(List<PluginContainer> containers) {
        CloseablePluginResults<PluginContainer> unloadResults = new CloseablePluginResults<>();

        for (PluginContainer container : containers) {
            proxy.getEventManager().fire(new VelocityPluginUnloadEvent(container, VelocityPluginEvent.Stage.PRE));
            String pluginId = getPluginId(container);
            Object pluginInstance = container.getInstance().orElse(null);
            if (pluginInstance == null) {
                return unloadResults.addResult(pluginId, Result.INVALID_PLUGIN);
            }

            proxy.getEventManager().unregisterListeners(pluginInstance);
            for (ScheduledTask task : RVelocityScheduler.getTasksByPlugin(proxy.getScheduler())
                    .removeAll(pluginInstance)) {
                task.cancel();
            }

            for (String alias : pluginCommandManager.getPluginCommands().removeAll(pluginId)) {
                proxy.getCommandManager().unregister(alias);
            }

            RVelocityPluginManager.getPlugins(proxy.getPluginManager()).remove(pluginId);
            RVelocityPluginManager.getPluginInstances(proxy.getPluginManager()).remove(pluginInstance);

            List<Closeable> closeables = new ArrayList<>();

            ClassLoader loader = pluginInstance.getClass().getClassLoader();
            if (loader instanceof Closeable) {
                closeables.add((Closeable) loader);
            }

            proxy.getEventManager().fire(new VelocityPluginUnloadEvent(container, VelocityPluginEvent.Stage.POST));
            unloadResults.addResult(pluginId, container, closeables);
        }

        return unloadResults;
    }
}
