package nl.hauntedmc.velocityhotreloader.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import nl.hauntedmc.velocityhotreloader.VHR;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudience;
import nl.hauntedmc.velocityhotreloader.entities.VelocityPluginDescription;
import nl.hauntedmc.velocityhotreloader.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResult;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginWatchResults;
import nl.hauntedmc.velocityhotreloader.entities.results.Result;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityCommandManager;
import nl.hauntedmc.velocityhotreloader.utils.KeyValueComponentBuilder;
import nl.hauntedmc.velocityhotreloader.utils.ListComponentBuilder;

public class CommandVHR {

    private static final String ROOT_COMMAND = "velocityhotreloader";
    private static final String ROOT_ALIAS = "vhr";

    private static final String PERM_HELP = "velocityhotreloader.help";
    private static final String PERM_RELOAD = "velocityhotreloader.reload";
    private static final String PERM_RESTART = "velocityhotreloader.restart";
    private static final String PERM_LOAD_PLUGIN = "velocityhotreloader.loadplugin";
    private static final String PERM_UNLOAD_PLUGIN = "velocityhotreloader.unloadplugin";
    private static final String PERM_RELOAD_PLUGIN = "velocityhotreloader.reloadplugin";
    private static final String PERM_WATCH_PLUGIN = "velocityhotreloader.watchplugin";
    private static final String PERM_PLUGIN_INFO = "velocityhotreloader.plugininfo";
    private static final String PERM_COMMAND_INFO = "velocityhotreloader.commandinfo";
    private static final String PERM_PLUGINS = "velocityhotreloader.plugins";
    private static final String PERM_PLUGINS_VERSION = "velocityhotreloader.plugins.version";

    private static final Set<String> FORCE_FLAGS = Set.of("--force", "-f");
    private static final Set<String> VERSION_FLAGS = Set.of("--version", "-v");

    private final VHR plugin;

    public CommandVHR(VHR plugin) {
        this.plugin = plugin;
    }

    public void register() {
        CommandManager commandManager = plugin.getProxy().getCommandManager();
        com.velocitypowered.api.command.BrigadierCommand brigadier =
                new com.velocitypowered.api.command.BrigadierCommand(buildTree());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases(ROOT_ALIAS)
                .plugin(plugin)
                .build();
        commandManager.register(meta, brigadier);
    }

    private LiteralCommandNode<CommandSource> buildTree() {
        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.<CommandSource>literal(ROOT_COMMAND)
                .executes(context -> {
                    if (hasPermission(context.getSource(), PERM_HELP)) {
                        handleHelpCommand(plugin.getChatProvider().get(context.getSource()));
                    }
                    return 1;
                });

        addSubcommand(root, "help", List.of(), PERM_HELP, literal -> literal.executes(context -> {
            handleHelpCommand(plugin.getChatProvider().get(context.getSource()));
            return 1;
        }));

        addSubcommand(root, "reload", List.of(), PERM_RELOAD, literal -> literal.executes(context -> {
            handleReload(plugin.getChatProvider().get(context.getSource()));
            return 1;
        }));

        addSubcommand(root, "restart", List.of(), PERM_RESTART, literal -> {
            literal.executes(context -> {
                handleRestart(
                        plugin.getChatProvider().get(context.getSource()),
                        false,
                        context.getInput()
                );
                return 1;
            });

            for (String flag : FORCE_FLAGS) {
                literal.then(LiteralArgumentBuilder.<CommandSource>literal(flag)
                        .requires(source -> hasPermission(source, PERM_RESTART))
                        .executes(context -> {
                            handleRestart(
                                    plugin.getChatProvider().get(context.getSource()),
                                    true,
                                    context.getInput()
                            );
                            return 1;
                        }));
            }
            return literal;
        });

        addSubcommand(root, "loadplugin", List.of(), PERM_LOAD_PLUGIN, literal -> literal
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("jarFiles", StringArgumentType.greedyString())
                        .suggests((context, builder) -> suggestJarFiles(builder))
                        .executes(context -> {
                            VelocityAudience sender = plugin.getChatProvider().get(context.getSource());
                            String rawFiles = StringArgumentType.getString(context, "jarFiles");
                            ParsedJarFiles parsed = parseJarFiles(rawFiles);
                            if (parsed.invalidJarFile() != null) {
                                sendError(sender, "Jar file '" + parsed.invalidJarFile() + "' does not exist.");
                                return 0;
                            }
                            if (parsed.files().isEmpty()) {
                                sendError(sender, "Please specify at least one jar file.");
                                return 0;
                            }

                            handleLoadPlugin(sender, parsed.files());
                            return 1;
                        })));

        addPluginBatchSubcommand(root, "unloadplugin", List.of(), PERM_UNLOAD_PLUGIN, this::handleUnloadPlugin);
        addPluginBatchSubcommand(root, "reloadplugin", List.of(), PERM_RELOAD_PLUGIN, this::handleReloadPlugin);
        addPluginBatchSubcommand(root, "watchplugin", List.of(), PERM_WATCH_PLUGIN, this::handleWatchPlugin);

        addSubcommand(root, "unwatchplugin", List.of(), PERM_WATCH_PLUGIN, literal -> literal
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("plugin", StringArgumentType.word())
                        .suggests((context, builder) -> suggestPlugins(builder))
                        .executes(context -> {
                            VelocityAudience sender = plugin.getChatProvider().get(context.getSource());
                            String pluginName = StringArgumentType.getString(context, "plugin");
                            PluginContainer pluginContainer = parsePlugin(pluginName);
                            if (pluginContainer == null) {
                                sendPluginNotExists(sender, pluginName);
                                return 0;
                            }

                            handleUnwatchPlugin(sender, pluginContainer);
                            return 1;
                        })));

        addSubcommand(root, "plugininfo", List.of(), PERM_PLUGIN_INFO, literal -> literal
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("plugin", StringArgumentType.word())
                        .suggests((context, builder) -> suggestPlugins(builder))
                        .executes(context -> {
                            VelocityAudience sender = plugin.getChatProvider().get(context.getSource());
                            String pluginName = StringArgumentType.getString(context, "plugin");
                            PluginContainer pluginContainer = parsePlugin(pluginName);
                            if (pluginContainer == null) {
                                sendPluginNotExists(sender, pluginName);
                                return 0;
                            }

                            handlePluginInfo(sender, pluginContainer);
                            return 1;
                        })));

        addSubcommand(root, "commandinfo", List.of(), PERM_COMMAND_INFO, literal -> literal
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("command", StringArgumentType.word())
                        .suggests((context, builder) -> suggestCommands(builder))
                        .executes(context -> {
                            VelocityAudience sender = plugin.getChatProvider().get(context.getSource());
                            String commandName = StringArgumentType.getString(context, "command");
                            handleCommandInfo(sender, commandName);
                            return 1;
                        })));

        addSubcommand(root, "plugins", List.of(), PERM_PLUGINS, literal -> {
            literal.executes(context -> {
                handlePlugins(plugin.getChatProvider().get(context.getSource()), false);
                return 1;
            });

            for (String flag : VERSION_FLAGS) {
                literal.then(LiteralArgumentBuilder.<CommandSource>literal(flag)
                        .requires(source -> hasPermission(source, PERM_PLUGINS_VERSION))
                        .executes(context -> {
                            handlePlugins(plugin.getChatProvider().get(context.getSource()), true);
                            return 1;
                        }));
            }
            return literal;
        });

        return root.build();
    }

    private void addPluginBatchSubcommand(
            LiteralArgumentBuilder<CommandSource> root,
            String name,
            List<String> aliases,
            String permission,
            PluginBatchHandler handler
    ) {
        addSubcommand(root, name, aliases, permission, literal -> literal
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("plugins", StringArgumentType.greedyString())
                        .suggests((context, builder) -> suggestPluginsAndFlags(builder, FORCE_FLAGS))
                        .executes(context -> {
                            VelocityAudience sender = plugin.getChatProvider().get(context.getSource());
                            String rawPlugins = StringArgumentType.getString(context, "plugins");
                            ParsedPlugins parsed = parsePlugins(rawPlugins, FORCE_FLAGS);
                            if (parsed.invalidPlugin() != null) {
                                sendPluginNotExists(sender, parsed.invalidPlugin());
                                return 0;
                            }
                            if (parsed.plugins().isEmpty()) {
                                sendError(sender, "Please specify at least one plugin.");
                                return 0;
                            }

                            handler.handle(sender, parsed.plugins(), parsed.force(), context.getInput());
                            return 1;
                        })));
    }

    private void addSubcommand(
            LiteralArgumentBuilder<CommandSource> root,
            String name,
            List<String> aliases,
            String permission,
            Function<LiteralArgumentBuilder<CommandSource>, LiteralArgumentBuilder<CommandSource>> builderFunction
    ) {
        root.then(builderFunction.apply(baseLiteral(name, permission)));
        for (String alias : aliases) {
            root.then(builderFunction.apply(baseLiteral(alias, permission)));
        }
    }

    private LiteralArgumentBuilder<CommandSource> baseLiteral(String name, String permission) {
        return LiteralArgumentBuilder.<CommandSource>literal(name)
                .requires(source -> hasPermission(source, permission));
    }

    private boolean hasPermission(CommandSource source, String permission) {
        return permission == null || permission.isBlank() || source.hasPermission(permission);
    }

    private void handleHelpCommand(VelocityAudience sender) {
        MessagesResource messages = plugin.getMessagesResource();
        sender.sendMessage(messages.get(MessageKey.HELP_HEADER).toComponent());

        MessagesResource.Message helpFormatMessage = messages.get(MessageKey.HELP_FORMAT);
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " help", "Geeft de hulppagina weer.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " reload", "Herladen van de VHR-plugin.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " restart", "Herstart de VHR-plugin.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " loadplugin", "Laadt de opgegeven jar-bestanden.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " unloadplugin", "Schakelt de opgegeven plugin(s) uit en laadt ze uit.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " reloadplugin", "Herladen van de opgegeven plugin(s).");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " watchplugin", "Volgt de opgegeven plugin(s) op wijzigingen.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " unwatchplugin", "Stopt met het volgen van de opgegeven plugin voor wijzigingen.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " plugininfo", "Toont informatie over de opgegeven plugin.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " commandinfo", "Toont informatie over het opgegeven commando.");
        sendHelpLine(helpFormatMessage, sender, ROOT_ALIAS + " plugins", "Geeft de ingeschakelde plugins weer.");

        sender.sendMessage(messages.get(MessageKey.HELP_FOOTER).toComponent());
    }

    private void sendHelpLine(
            MessagesResource.Message helpFormatMessage,
            VelocityAudience sender,
            String command,
            String description
    ) {
        sender.sendMessage(helpFormatMessage.toComponent(
                Placeholder.unparsed("command", command),
                Placeholder.unparsed("help", description)
        ));
    }

    private void handleReload(VelocityAudience sender) {
        plugin.reload();
        plugin.getMessagesResource().get(MessageKey.RELOAD).sendTo(sender);
    }

    private void handleRestart(VelocityAudience sender, boolean force, String rawInput) {
        List<PluginContainer> plugins = List.of(plugin.getPlugin());
        checkDependingPlugins(sender, plugins, force, rawInput);
    }

    private void handleLoadPlugin(VelocityAudience sender, List<File> jarFiles) {
        VelocityPluginManager pluginManager = plugin.getPluginManager();
        PluginResults<PluginContainer> loadResults = pluginManager.loadPlugins(jarFiles);
        if (!loadResults.isSuccess()) {
            PluginResult<PluginContainer> failedResult = loadResults.last();
            failedResult.sendTo(sender, null);
            return;
        }

        PluginResults<PluginContainer> enableResults = pluginManager.enablePlugins(loadResults.getPlugins());
        enableResults.sendTo(sender, MessageKey.LOADPLUGIN);
    }

    private void handleUnloadPlugin(VelocityAudience sender, List<PluginContainer> plugins, boolean force, String rawInput) {
        if (checkDependingPlugins(sender, plugins, force, rawInput)) {
            return;
        }

        PluginResults<PluginContainer> disableResults = plugin.getPluginManager().disablePlugins(plugins);
        for (PluginResult<PluginContainer> disableResult : disableResults.getResults()) {
            if (!disableResult.isSuccess() && disableResult.getResult() != Result.ALREADY_DISABLED) {
                disableResult.sendTo(sender, null);
                return;
            }
        }

        CloseablePluginResults<PluginContainer> unloadResults = plugin.getPluginManager().unloadPlugins(plugins);
        unloadResults.tryClose();
        unloadResults.sendTo(sender, MessageKey.UNLOADPLUGIN);
    }

    private void handleReloadPlugin(VelocityAudience sender, List<PluginContainer> plugins, boolean force, String rawInput) {
        if (checkDependingPlugins(sender, plugins, force, rawInput)) {
            return;
        }

        if (checkVHR(sender, plugins, rootLiteral(rawInput))) {
            return;
        }

        PluginResults<PluginContainer> reloadResults = plugin.getPluginManager().reloadPlugins(plugins);
        reloadResults.sendTo(sender, MessageKey.RELOADPLUGIN_SUCCESS);
    }

    private void handleWatchPlugin(VelocityAudience sender, List<PluginContainer> plugins, boolean force, String rawInput) {
        if (checkDependingPlugins(sender, plugins, force, rawInput)) {
            return;
        }

        if (checkVHR(sender, plugins, rootLiteral(rawInput))) {
            return;
        }

        PluginWatchResults watchResults = plugin.getWatchManager().watchPlugins(sender, plugins);
        watchResults.sendTo(sender);
    }

    private boolean checkDependingPlugins(
            VelocityAudience sender,
            List<PluginContainer> plugins,
            boolean force,
            String rawInput
    ) {
        if (force) {
            return false;
        }

        VelocityPluginManager pluginManager = plugin.getPluginManager();
        MessagesResource messages = plugin.getMessagesResource();

        boolean hasDependingPlugins = false;
        for (PluginContainer pluginContainer : plugins) {
            String pluginId = pluginManager.getPluginId(pluginContainer);
            List<PluginContainer> dependingPlugins = pluginManager.getPluginsDependingOn(pluginId);
            if (dependingPlugins.isEmpty()) {
                continue;
            }

            TextComponent.Builder builder = Component.text();
            builder.append(messages.get(MessageKey.DEPENDING_PLUGINS_PREFIX).toComponent(
                    Placeholder.unparsed("plugin", pluginId)
            ));
            builder.append(ListComponentBuilder.create(dependingPlugins)
                    .format(p -> messages.get(MessageKey.DEPENDING_PLUGINS_FORMAT).toComponent(
                            Placeholder.unparsed("plugin", pluginManager.getPluginId(p))
                    ))
                    .separator(messages.get(MessageKey.DEPENDING_PLUGINS_SEPARATOR).toComponent())
                    .lastSeparator(messages.get(MessageKey.DEPENDING_PLUGINS_LAST_SEPARATOR).toComponent())
                    .build());
            sender.sendMessage(builder.build());
            hasDependingPlugins = true;
        }

        if (hasDependingPlugins) {
            sender.sendMessage(messages.get(MessageKey.DEPENDING_PLUGINS_OVERRIDE).toComponent(
                    Placeholder.unparsed("command", rawInput + " -f")
            ));
        }
        return hasDependingPlugins;
    }

    private boolean checkVHR(VelocityAudience sender, List<PluginContainer> plugins, String commandRoot) {
        for (PluginContainer loadedPlugin : plugins) {
            if (plugin.getPlugin() != loadedPlugin) {
                continue;
            }
            Component component = plugin.getMessagesResource().get(MessageKey.RELOADPLUGIN_VHR).toComponent(
                    Placeholder.unparsed("command", commandRoot + " restart")
            );
            sender.sendMessage(component);
            return true;
        }
        return false;
    }

    private void handleUnwatchPlugin(VelocityAudience sender, PluginContainer pluginArg) {
        String pluginId = plugin.getPluginManager().getPluginId(pluginArg);
        PluginWatchResults watchResults = plugin.getWatchManager().unwatchPluginsAssociatedWith(pluginId);
        watchResults.sendTo(sender);
    }

    private void handlePluginInfo(VelocityAudience sender, PluginContainer pluginArg) {
        createInfo(sender, "plugininfo", pluginArg, this::createPluginInfo);
    }

    private KeyValueComponentBuilder createPluginInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            PluginContainer pluginArg
    ) {
        PluginDescription desc = pluginArg.getDescription();

        return builder
                .key("Id").value(desc.getId())
                .key("Name").value(desc.getName().orElse(null))
                .key("Version").value(desc.getVersion().orElse("<UNKNOWN>"))
                .key("Author" + (desc.getAuthors().size() == 1 ? "" : "s"))
                .value(listBuilderFunction.apply(b -> b.addAll(desc.getAuthors())))
                .key("Description").value(desc.getDescription().orElse(null))
                .key("URL").value(desc.getUrl().orElse(null))
                .key("Source").value(desc.getSource().map(Path::toString).orElse(null))
                .key("Dependencies")
                .value(listBuilderFunction.apply(b -> b.addAll(desc.getDependencies().stream()
                        .map(PluginDependency::getId)
                        .collect(Collectors.toList()))));
    }

    private void handleCommandInfo(VelocityAudience sender, String commandName) {
        String parsedCommand = parseCommand(commandName);
        if (parsedCommand == null) {
            plugin.getMessagesResource().get(MessageKey.COMMANDINFO_NOT_EXISTS).sendTo(sender);
            return;
        }

        createInfo(sender, "commandinfo", parsedCommand, this::createCommandInfo);
    }

    private KeyValueComponentBuilder createCommandInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            String commandName
    ) {
        VHR proxyPlugin = VHR.getInstance();
        com.velocitypowered.api.command.CommandManager proxyCommandManager = proxyPlugin.getProxy().getCommandManager();
        CommandDispatcher<CommandSource> dispatcher = RVelocityCommandManager.getDispatcher(proxyCommandManager);

        String commandNodeName = commandName;
        if (dispatcher.getRoot().getChild(commandName) != null) {
            commandNodeName = dispatcher.getRoot().getChild(commandName).getName();
        }
        builder.key("Name").value(commandNodeName);

        CommandMeta meta = null;
        try {
            meta = proxyCommandManager.getCommandMeta(commandName);
        } catch (Throwable ignored) {
            //
        }

        String pluginName = null;
        if (meta != null) {
            if (meta.getPlugin() != null) {
                pluginName = proxyPlugin.getProxy().getPluginManager().fromInstance(meta.getPlugin())
                        .map(c -> c.getDescription().getId())
                        .orElse(null);
            }
            CommandMeta finalMeta = meta;
            builder.key("Aliases").value(listBuilderFunction.apply(b -> b.addAll(finalMeta.getAliases())));
        }

        if (pluginName == null) {
            pluginName = proxyPlugin.getPluginCommandManager().findPluginId(commandName).orElse("<UNKNOWN>");
        }

        return builder.key("Plugin").value(pluginName);
    }

    private <T> void createInfo(VelocityAudience sender, String command, T item, InfoCreator<T> creator) {
        MessagesResource messages = plugin.getMessagesResource();
        MessagesResource.Message formatMessage = messages.get(command + ".format");
        MessagesResource.Message listFormatMessage = messages.get(command + ".list-format");
        Component separator = messages.get(command + ".list-separator").toComponent();
        Component lastSeparator = messages.get(command + ".list-last-separator").toComponent();

        sender.sendMessage(messages.get(command + ".header").toComponent());
        creator.createInfo(
                KeyValueComponentBuilder.create(formatMessage, "key", "value"),
                listBuilderConsumer -> {
                    ListComponentBuilder<String> listBuilder = ListComponentBuilder.<String>create()
                            .format(str -> listFormatMessage.toComponent(Placeholder.unparsed("value", str)))
                            .separator(separator)
                            .lastSeparator(lastSeparator)
                            .emptyValue(null);
                    listBuilderConsumer.accept(listBuilder);
                    return listBuilder.build();
                },
                item
        ).build().forEach(sender::sendMessage);
        sender.sendMessage(messages.get(command + ".footer").toComponent());
    }

    private void handlePlugins(VelocityAudience sender, boolean hasVersionFlag) {
        List<PluginContainer> plugins = plugin.getPluginManager().getPluginsSorted();
        VelocityPluginManager pluginManager = plugin.getPluginManager();
        MessagesResource messages = plugin.getMessagesResource();

        sender.sendMessage(messages.get(MessageKey.PLUGINS_HEADER).toComponent());
        TextComponent.Builder builder = Component.text();
        builder.append(messages.get(MessageKey.PLUGINS_PREFIX).toComponent(
                Placeholder.unparsed("count", String.valueOf(plugins.size()))
        ));
        builder.append(ListComponentBuilder.create(plugins)
                .separator(messages.get(MessageKey.PLUGINS_SEPARATOR).toComponent())
                .lastSeparator(messages.get(MessageKey.PLUGINS_LAST_SEPARATOR).toComponent())
                .format(pluginContainer -> {
                    VelocityPluginDescription description = pluginManager.getLoadedPluginDescription(pluginContainer);

                    TextComponent.Builder formatBuilder = Component.text();
                    MessageKey formatKey = pluginManager.isPluginEnabled(pluginContainer)
                            ? MessageKey.PLUGINS_FORMAT
                            : MessageKey.PLUGINS_FORMAT_DISABLED;
                    formatBuilder.append(messages.get(formatKey).toComponent(
                            Placeholder.unparsed("plugin", description.getName())
                    ));
                    if (hasVersionFlag) {
                        formatBuilder.append(messages.get(MessageKey.PLUGINS_VERSION).toComponent(
                                Placeholder.unparsed("version", description.getVersion())
                        ));
                    }
                    return formatBuilder.build();
                })
                .build());
        sender.sendMessage(builder.build());
        sender.sendMessage(messages.get(MessageKey.PLUGINS_FOOTER).toComponent());
    }

    private PluginContainer parsePlugin(String input) {
        PluginContainer exact = plugin.getPluginManager().getPlugin(input).orElse(null);
        if (exact != null) {
            return exact;
        }

        for (PluginContainer loadedPlugin : plugin.getPluginManager().getPlugins()) {
            String pluginId = plugin.getPluginManager().getPluginId(loadedPlugin);
            if (pluginId.equalsIgnoreCase(input)) {
                return loadedPlugin;
            }
        }
        return null;
    }

    private String parseCommand(String input) {
        return plugin.getPluginManager().getCommands().stream()
                .filter(command -> command.equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    private ParsedPlugins parsePlugins(String rawInput, Set<String> forceFlags) {
        List<String> tokens = parseTokens(rawInput);
        if (tokens.isEmpty()) {
            return new ParsedPlugins(List.of(), false, null);
        }

        boolean force = false;
        List<PluginContainer> plugins = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            if (forceFlags.contains(token)) {
                force = true;
                continue;
            }
            PluginContainer pluginContainer = parsePlugin(token);
            if (pluginContainer == null) {
                return new ParsedPlugins(List.of(), force, token);
            }
            plugins.add(pluginContainer);
        }
        return new ParsedPlugins(plugins, force, null);
    }

    private ParsedJarFiles parseJarFiles(String rawInput) {
        List<String> tokens = parseTokens(rawInput);
        if (tokens.isEmpty()) {
            return new ParsedJarFiles(List.of(), null);
        }

        Set<String> pluginFiles = new HashSet<>(plugin.getPluginManager().getPluginFileNames());
        List<File> parsed = new ArrayList<>(tokens.size());
        File pluginsFolder = plugin.getPluginManager().getPluginsFolder();
        for (String token : tokens) {
            if (!pluginFiles.contains(token) || pluginsFolder == null) {
                return new ParsedJarFiles(List.of(), token);
            }
            parsed.add(new File(pluginsFolder, token));
        }
        return new ParsedJarFiles(parsed, null);
    }

    private List<String> parseTokens(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                continue;
            }

            if (!quoted && Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private CompletableFuture<Suggestions> suggestPlugins(SuggestionsBuilder builder) {
        return suggestSimple(builder, plugin.getPluginManager().getPluginNames());
    }

    private CompletableFuture<Suggestions> suggestCommands(SuggestionsBuilder builder) {
        return suggestSimple(builder, plugin.getPluginManager().getCommands());
    }

    private CompletableFuture<Suggestions> suggestJarFiles(SuggestionsBuilder builder) {
        return suggestMultipleTokens(builder, plugin.getPluginManager().getPluginFileNames());
    }

    private CompletableFuture<Suggestions> suggestPluginsAndFlags(SuggestionsBuilder builder, Set<String> flags) {
        List<String> suggestions = new ArrayList<>(plugin.getPluginManager().getPluginNames());
        suggestions.addAll(flags);
        return suggestMultipleTokens(builder, suggestions);
    }

    private CompletableFuture<Suggestions> suggestSimple(SuggestionsBuilder builder, Iterable<String> values) {
        String remaining = builder.getRemainingLowerCase();
        for (String value : values) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.startsWith(remaining)) {
                builder.suggest(value);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestMultipleTokens(SuggestionsBuilder builder, Iterable<String> values) {
        String remaining = builder.getRemaining();
        int splitIndex = remaining.lastIndexOf(' ');

        String prefix = splitIndex == -1 ? "" : remaining.substring(0, splitIndex + 1);
        String token = splitIndex == -1 ? remaining : remaining.substring(splitIndex + 1);

        Set<String> usedTokens = parseTokens(prefix.trim()).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getStart() + prefix.length());
        String tokenLower = token.toLowerCase(Locale.ROOT);
        for (String value : values) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (!tokenLower.isEmpty() && !lower.startsWith(tokenLower)) {
                continue;
            }
            if (usedTokens.contains(lower)) {
                continue;
            }
            offsetBuilder.suggest(value);
        }
        return offsetBuilder.buildFuture();
    }

    private void sendPluginNotExists(VelocityAudience audience, String pluginId) {
        audience.sendMessage(plugin.getMessagesResource().get(MessageKey.GENERIC_NOT_EXISTS).toComponent(
                Placeholder.unparsed("plugin", pluginId)
        ));
    }

    private void sendError(VelocityAudience audience, String message) {
        audience.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private String rootLiteral(String input) {
        if (input == null || input.isBlank()) {
            return ROOT_COMMAND;
        }
        int firstSpace = input.indexOf(' ');
        return firstSpace == -1 ? input : input.substring(0, firstSpace);
    }

    private interface PluginBatchHandler {
        void handle(VelocityAudience sender, List<PluginContainer> plugins, boolean force, String rawInput);
    }

    private interface InfoCreator<T> {
        KeyValueComponentBuilder createInfo(
                KeyValueComponentBuilder builder,
                Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
                T item
        );
    }

    private record ParsedPlugins(List<PluginContainer> plugins, boolean force, String invalidPlugin) {}

    private record ParsedJarFiles(List<File> files, String invalidJarFile) {}
}
