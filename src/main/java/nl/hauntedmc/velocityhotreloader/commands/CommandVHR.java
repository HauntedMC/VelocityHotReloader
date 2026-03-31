package nl.hauntedmc.velocityhotreloader.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import nl.hauntedmc.velocityhotreloader.commands.arguments.CommandParser;
import nl.hauntedmc.velocityhotreloader.commands.arguments.JarFilesParser;
import nl.hauntedmc.velocityhotreloader.commands.arguments.PluginParser;
import nl.hauntedmc.velocityhotreloader.commands.arguments.PluginsParser;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;
import nl.hauntedmc.velocityhotreloader.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResult;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloader.entities.results.PluginWatchResults;
import nl.hauntedmc.velocityhotreloader.entities.results.Result;
import nl.hauntedmc.velocityhotreloader.utils.KeyValueComponentBuilder;
import nl.hauntedmc.velocityhotreloader.utils.ListComponentBuilder;
import nl.hauntedmc.velocityhotreloader.VHR;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudience;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import nl.hauntedmc.velocityhotreloader.reflection.RVelocityCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandVHR extends VHRCommand {

    public CommandVHR(VHR plugin) {
        super(plugin, "velocityhotreloader");
    }

    protected ParserDescriptor<VelocityAudience, PluginContainer[]> pluginsParser() {
        return pluginsParser(null);
    }

    protected ParserDescriptor<VelocityAudience, PluginContainer[]> pluginsParser(String path) {
        return PluginsParser.pluginsParser(this.plugin, path);
    }

    @Override
    public void register(
            CommandManager<VelocityAudience> manager,
            Command.Builder<VelocityAudience> builder
    ) {
        addRequiredComponent("jarFiles", JarFilesParser.jarFilesParser(this.plugin));
        addRequiredComponent("plugins", pluginsParser());
        addRequiredComponent("plugin", PluginParser.pluginParser(this.plugin));
        addRequiredComponent("command", CommandParser.commandParser(this.plugin));

        manager.command(builder
                .handler(this::handleHelpCommand));
        registerSubcommand(manager, builder, "help", subcommandBuilder -> subcommandBuilder
                .handler(this::handleHelpCommand));
        registerSubcommand(manager, builder, "reload", subcommandBuilder -> subcommandBuilder
                .handler(this::handleReload));
        registerSubcommand(manager, builder, "restart", subcommandBuilder -> subcommandBuilder
                .handler(this::handleRestart));
        registerSubcommand(manager, builder, "loadplugin", subcommandBuilder -> subcommandBuilder
                .argument(getComponent("jarFiles"))
                .handler(this::handleLoadPlugin));
        registerSubcommand(manager, builder, "unloadplugin", subcommandBuilder -> subcommandBuilder
                .required("plugins", pluginsParser(getRawPath("unloadplugin")))
                .handler(this::handleUnloadPlugin));
        registerSubcommand(manager, builder, "reloadplugin", subcommandBuilder -> subcommandBuilder
                .required("plugins", pluginsParser(getRawPath("reloadplugin")))
                .handler(this::handleReloadPlugin));
        registerSubcommand(manager, builder, "watchplugin", subcommandBuilder -> subcommandBuilder
                .required("plugins", pluginsParser(getRawPath("watchplugin")))
                .handler(this::handleWatchPlugin));
        registerSubcommand(manager, builder, "unwatchplugin", subcommandBuilder -> subcommandBuilder
                .argument(getComponent("plugin"))
                .handler(this::handleUnwatchPlugin));
        registerSubcommand(manager, builder, "plugininfo", subcommandBuilder -> subcommandBuilder
                .argument(getComponent("plugin"))
                .handler(this::handlePluginInfo));
        registerSubcommand(manager, builder, "commandinfo", subcommandBuilder -> subcommandBuilder
                .argument(getComponent("command"))
                .handler(this::handleCommandInfo));
    }

    private void handleHelpCommand(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();

        MessagesResource messages = plugin.getMessagesResource();
        sender.sendMessage(messages.get(MessageKey.HELP_HEADER).toComponent());

        MessagesResource.Message helpFormatMessage = messages.get(MessageKey.HELP_FORMAT);

        VHRConfig config = (VHRConfig) plugin.getCommandsResource().getConfig().get("commands");
        for (String commandName : config.getKeys()) {
            VHRConfig commandConfig = (VHRConfig) config.get(commandName);
            CommandElement commandElement = parseElement(commandConfig);
            String shortestCommandAlias = determineShortestAlias(commandElement);

            if (commandElement.shouldDisplayInHelp()) {
                sender.sendMessage(helpFormatMessage.toComponent(
                        Placeholder.unparsed("command", shortestCommandAlias),
                        Placeholder.unparsed("help", commandElement.getDescription().textDescription()))
                );
            }

            Object subcommandsObject = commandConfig.get("subcommands");
            if (subcommandsObject instanceof VHRConfig) {
                VHRConfig subcommandsConfig = (VHRConfig) subcommandsObject;

                for (String subcommandName : subcommandsConfig.getKeys()) {
                    VHRConfig subcommandConfig = (VHRConfig) subcommandsConfig.get(subcommandName);
                    CommandElement subcommandElement = parseElement(subcommandConfig);
                    if (subcommandElement.shouldDisplayInHelp()) {
                        String shortestSubcommandAlias = determineShortestAlias(subcommandElement);
                        sender.sendMessage(helpFormatMessage.toComponent(
                                Placeholder.unparsed("command", shortestCommandAlias + ' ' + shortestSubcommandAlias),
                                Placeholder.unparsed("help", subcommandElement.getDescription().textDescription())
                        ));
                    }
                }
            }

            Object flagsObject = commandConfig.get("flags");
            if (flagsObject instanceof VHRConfig) {
                VHRConfig flagsConfig = (VHRConfig) flagsObject;

                for (String flagName : flagsConfig.getKeys()) {
                    VHRConfig flagConfig = (VHRConfig) flagsConfig.get(flagName);
                    CommandElement flagElement = parseElement(flagConfig);
                    if (flagElement.shouldDisplayInHelp()) {
                        String shortestFlagAlias = determineShortestAlias(flagElement);
                        String flagPrefix = "-" + (flagElement.getMain().equals(shortestFlagAlias) ? "_" : "");
                        sender.sendMessage(helpFormatMessage.toComponent(
                                Placeholder.unparsed("command",
                                        shortestCommandAlias + ' ' + flagPrefix + shortestFlagAlias),
                                Placeholder.unparsed("help", flagElement.getDescription().textDescription())
                        ));
                    }
                }
            }
        }

        sender.sendMessage(messages.get(MessageKey.HELP_FOOTER).toComponent());
    }

    private String determineShortestAlias(CommandElement element) {
        String shortestAlias = element.getMain();
        for (String alias : element.getAliases()) {
            if (alias.length() < shortestAlias.length()) {
                shortestAlias = alias;
            }
        }
        return shortestAlias;
    }

    private void handleReload(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        plugin.reload();
        plugin.getMessagesResource().get(MessageKey.RELOAD).sendTo(sender);
    }

    private void handleRestart(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();

        if (checkDependingPlugins(context, sender, Collections.singletonList(plugin.getPlugin()), "restart")) {
            return;
        }

    }

    private void handleLoadPlugin(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        List<File> jarFiles = Arrays.asList(context.get("jarFiles"));

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

    private void handleUnloadPlugin(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        List<PluginContainer> plugins = Arrays.asList(context.get("plugins"));

        if (checkProtectedPlugins(sender, plugins)) {
            return;
        }

        if (checkDependingPlugins(context, sender, plugins, "unloadplugin")) {
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

    private void handleReloadPlugin(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        List<PluginContainer> plugins = Arrays.asList(context.get("plugins"));

        if (checkProtectedPlugins(sender, plugins)) {
            return;
        }

        if (checkDependingPlugins(context, sender, plugins, "reloadplugin")) {
            return;
        }

        if (checkVHR(context, sender, plugins)) {
            return;
        }

        PluginResults<PluginContainer> reloadResults = plugin.getPluginManager().reloadPlugins(plugins);
        reloadResults.sendTo(sender, MessageKey.RELOADPLUGIN_SUCCESS);
    }

    protected boolean checkDependingPlugins(
            CommandContext<VelocityAudience> context,
            VelocityAudience sender,
            List<PluginContainer> plugins,
            String subcommand
    ) {
        if (context.flags().contains("force")) return false;

        VelocityPluginManager pluginManager = plugin.getPluginManager();
        MessagesResource messages = plugin.getMessagesResource();

        boolean hasDependingPlugins = false;
        for (PluginContainer pluginContainer : plugins) {
            String pluginId = pluginManager.getPluginId(pluginContainer);

            List<PluginContainer> dependingPlugins = pluginManager.getPluginsDependingOn(pluginId);

            if (!dependingPlugins.isEmpty()) {
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
        }

        if (hasDependingPlugins) {
            String flagPath = getRawPath(subcommand) + ".flags.force";
            String forceFlag = plugin.getCommandsResource().getAllFlagAliases(flagPath).stream()
                    .min(Comparator.comparingInt(String::length))
                    .orElse("-f");

            sender.sendMessage(messages.get(MessageKey.DEPENDING_PLUGINS_OVERRIDE).toComponent(
                    Placeholder.unparsed("command", context.rawInput() + " " + forceFlag)
            ));
        }

        return hasDependingPlugins;
    }

    protected boolean checkVHR(
            CommandContext<VelocityAudience> context,
            VelocityAudience sender,
            List<PluginContainer> plugins
    ) {
        for (PluginContainer loadedPlugin : plugins) {
            if (plugin.getPlugin() == loadedPlugin) {
                String restartCommand = plugin.getCommandsResource().getAllAliases(getRawPath("restart")).stream()
                        .min(Comparator.comparingInt(String::length))
                        .orElse("restart");
                Component component = plugin.getMessagesResource().get(MessageKey.RELOADPLUGIN_VHR).toComponent(
                        Placeholder.unparsed("command",
                                context.rawInput().cursor(0).peekString() + " " + restartCommand)
                );
                sender.sendMessage(component);
                return true;
            }
        }

        return false;
    }

    protected boolean checkProtectedPlugins(VelocityAudience sender, List<PluginContainer> plugins) {
        List<String> protectedPlugins = plugin.getConfigResource().getConfig().getStringList("protected-plugins");
        VelocityPluginManager pluginManager = plugin.getPluginManager();
        MessagesResource messagesResource = plugin.getMessagesResource();
        for (PluginContainer pluginContainer : plugins) {
            String pluginId = pluginManager.getPluginId(pluginContainer);
            if (protectedPlugins.contains(pluginId)) {
                sender.sendMessage(messagesResource.get(MessageKey.GENERIC_PROTECTED_PLUGIN).toComponent(
                        Placeholder.unparsed("plugin", pluginId)
                ));
                return true;
            }
        }
        return false;
    }

    private void handleWatchPlugin(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        List<PluginContainer> plugins = Arrays.asList(context.get("plugins"));

        if (checkDependingPlugins(context, sender, plugins, "watchplugin")) {
            return;
        }

        if (checkVHR(context, sender, plugins)) {
            return;
        }

        PluginWatchResults watchResults = plugin.getWatchManager().watchPlugins(sender, plugins);
        watchResults.sendTo(sender);
    }

    private void handleUnwatchPlugin(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        PluginContainer pluginArg = context.get("plugin");

        String pluginId = plugin.getPluginManager().getPluginId(pluginArg);
        PluginWatchResults watchResults = plugin.getWatchManager().unwatchPluginsAssociatedWith(pluginId);
        watchResults.sendTo(sender);
    }

    private void handlePluginInfo(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        PluginContainer pluginArg = context.get("plugin");

        createInfo(sender, "plugininfo", pluginArg, this::createPluginInfo);
    }

    @SuppressWarnings("unchecked")
    protected KeyValueComponentBuilder createPluginInfo(
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

    private void handleCommandInfo(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        String commandName = context.get("command");

        if (!plugin.getPluginManager().getCommands().contains(commandName)) {
            plugin.getMessagesResource().get(MessageKey.COMMANDINFO_NOT_EXISTS).sendTo(sender);
            return;
        }

        createInfo(sender, "commandinfo", commandName, this::createCommandInfo);
    }

    protected KeyValueComponentBuilder createCommandInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            String commandName
    ) {
        VHR proxyPlugin = VHR.getInstance();
        com.velocitypowered.api.command.CommandManager proxyCommandManager = proxyPlugin.getProxy().getCommandManager();
        CommandDispatcher<CommandSource> dispatcher = RVelocityCommandManager.getDispatcher(proxyCommandManager);

        builder.key("Name").value(dispatcher.getRoot().getChild(commandName).getName());

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

        builder.key("Plugin").value(pluginName);

        return builder;
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

    private interface InfoCreator<T> {

        KeyValueComponentBuilder createInfo(
                KeyValueComponentBuilder builder,
                Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
                T item
        );
    }
}
