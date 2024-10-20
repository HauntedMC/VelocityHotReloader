package nl.hauntedmc.velocityhotreloaded.common.commands;

import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.CommandParser;
import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.JarFilesParser;
import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.PluginParser;
import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.PluginsParser;
import nl.hauntedmc.velocityhotreloaded.common.config.MessageKey;
import nl.hauntedmc.velocityhotreloaded.common.config.MessagesResource;
import nl.hauntedmc.velocityhotreloaded.common.config.VHRConfig;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;
import nl.hauntedmc.velocityhotreloaded.common.entities.results.CloseablePluginResults;
import nl.hauntedmc.velocityhotreloaded.common.entities.results.PluginResult;
import nl.hauntedmc.velocityhotreloaded.common.entities.results.PluginResults;
import nl.hauntedmc.velocityhotreloaded.common.entities.results.PluginWatchResults;
import nl.hauntedmc.velocityhotreloaded.common.entities.results.Result;
import nl.hauntedmc.velocityhotreloaded.common.managers.AbstractPluginManager;
import nl.hauntedmc.velocityhotreloaded.common.utils.KeyValueComponentBuilder;
import nl.hauntedmc.velocityhotreloaded.common.utils.ListComponentBuilder;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public abstract class CommandVHR<U extends VHRPlugin<P, ?, C, ?, ?>, P, C extends VHRAudience<?>>
        extends VHRCommand<U, C> {

    protected final IntFunction<P[]> arrayCreator;
    protected final Class<P> pluginType;

    protected CommandVHR(U plugin, IntFunction<P[]> arrayCreator, Class<P> pluginType) {
        super(plugin, "velocityhotreloaded");
        this.arrayCreator = arrayCreator;
        this.pluginType = pluginType;
    }

    protected ParserDescriptor<C, P[]> pluginsParser() {
        return pluginsParser(null);
    }

    protected ParserDescriptor<C, P[]> pluginsParser(String path) {
        return PluginsParser.pluginsParser(this.plugin, path, this.arrayCreator, this.pluginType);
    }

    @Override
    public void register(CommandManager<C> manager, Command.Builder<C> builder) {
        addRequiredComponent("jarFiles", JarFilesParser.jarFilesParser(this.plugin));
        addRequiredComponent("plugins", pluginsParser());
        addRequiredComponent("plugin", PluginParser.pluginParser(this.plugin, this.pluginType));
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

    private void handleHelpCommand(CommandContext<C> context) {
        C sender = context.sender();

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

    private void handleReload(CommandContext<C> context) {
        C sender = context.sender();
        plugin.reload();
        plugin.getMessagesResource().get(MessageKey.RELOAD).sendTo(sender);
    }

    private void handleRestart(CommandContext<C> context) {
        C sender = context.sender();

        if (checkDependingPlugins(context, sender, Collections.singletonList(plugin.getPlugin()), "restart")) {
            return;
        }

    }

    private void handleLoadPlugin(CommandContext<C> context) {
        C sender = context.sender();
        List<File> jarFiles = Arrays.asList(context.get("jarFiles"));

        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        PluginResults<P> loadResults = pluginManager.loadPlugins(jarFiles);
        if (!loadResults.isSuccess()) {
            PluginResult<P> failedResult = loadResults.last();
            failedResult.sendTo(sender, null);
            return;
        }

        PluginResults<P> enableResults = pluginManager.enablePlugins(loadResults.getPlugins());
        enableResults.sendTo(sender, MessageKey.LOADPLUGIN);
    }

    private void handleUnloadPlugin(CommandContext<C> context) {
        C sender = context.sender();
        List<P> plugins = Arrays.asList(context.get("plugins"));

        if (checkProtectedPlugins(sender, plugins)) {
            return;
        }

        if (checkDependingPlugins(context, sender, plugins, "unloadplugin")) {
            return;
        }

        PluginResults<P> disableResults = plugin.getPluginManager().disablePlugins(plugins);
        for (PluginResult<P> disableResult : disableResults.getResults()) {
            if (!disableResult.isSuccess() && disableResult.getResult() != Result.ALREADY_DISABLED) {
                disableResult.sendTo(sender, null);
                return;
            }
        }

        CloseablePluginResults<P> unloadResults = plugin.getPluginManager().unloadPlugins(plugins);
        unloadResults.tryClose();
        unloadResults.sendTo(sender, MessageKey.UNLOADPLUGIN);
    }

    private void handleReloadPlugin(CommandContext<C> context) {
        C sender = context.sender();
        List<P> plugins = Arrays.asList(context.get("plugins"));

        if (checkProtectedPlugins(sender, plugins)) {
            return;
        }

        if (checkDependingPlugins(context, sender, plugins, "reloadplugin")) {
            return;
        }

        if (checkVHR(context, sender, plugins)) {
            return;
        }

        PluginResults<P> reloadResults = plugin.getPluginManager().reloadPlugins(plugins);
        reloadResults.sendTo(sender, MessageKey.RELOADPLUGIN_SUCCESS);
    }

    protected boolean checkDependingPlugins(CommandContext<C> context, C sender, List<P> plugins, String subcommand) {
        if (context.flags().contains("force")) return false;

        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        MessagesResource messages = plugin.getMessagesResource();

        boolean hasDependingPlugins = false;
        for (P plugin : plugins) {
            String pluginId = pluginManager.getPluginId(plugin);

            List<P> dependingPlugins = pluginManager.getPluginsDependingOn(pluginId);
            //List<P> dependingPlugins = new ArrayList<>();

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

    protected boolean checkVHR(CommandContext<C> context, C sender, List<P> plugins) {
        for (P loadedPlugin : plugins) {
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

    protected boolean checkProtectedPlugins(C sender, List<P> plugins) {
        List<String> protectedPlugins = plugin.getConfigResource().getConfig().getStringList("protected-plugins");
        AbstractPluginManager<P, ?> pluginManager = plugin.getPluginManager();
        MessagesResource messagesResource = plugin.getMessagesResource();
        for (P plugin : plugins) {
            String pluginId = pluginManager.getPluginId(plugin);
            if (protectedPlugins.contains(pluginId)) {
                sender.sendMessage(messagesResource.get(MessageKey.GENERIC_PROTECTED_PLUGIN).toComponent(
                        Placeholder.unparsed("plugin", pluginId)
                ));
                return true;
            }
        }
        return false;
    }

    private void handleWatchPlugin(CommandContext<C> context) {
        C sender = context.sender();
        List<P> plugins = Arrays.asList(context.get("plugins"));

        if (checkDependingPlugins(context, sender, plugins, "watchplugin")) {
            return;
        }

        if (checkVHR(context, sender, plugins)) {
            return;
        }

        PluginWatchResults watchResults = plugin.getWatchManager().watchPlugins(sender, plugins);
        watchResults.sendTo(sender);
    }

    private void handleUnwatchPlugin(CommandContext<C> context) {
        C sender = context.sender();
        P pluginArg = context.get("plugin");

        String pluginId = plugin.getPluginManager().getPluginId(pluginArg);
        PluginWatchResults watchResults = plugin.getWatchManager().unwatchPluginsAssociatedWith(pluginId);
        watchResults.sendTo(sender);
    }

    private void handlePluginInfo(CommandContext<C> context) {
        C sender = context.sender();
        P pluginArg = context.get("plugin");

        createInfo(sender, "plugininfo", pluginArg, this::createPluginInfo);
    }

    protected abstract KeyValueComponentBuilder createPluginInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            P pluginArg
    );

    private void handleCommandInfo(CommandContext<C> context) {
        C sender = context.sender();
        String commandName = context.get("command");

        if (!plugin.getPluginManager().getCommands().contains(commandName)) {
            plugin.getMessagesResource().get(MessageKey.COMMANDINFO_NOT_EXISTS).sendTo(sender);
            return;
        }

        createInfo(sender, "commandinfo", commandName, this::createCommandInfo);
    }

    protected abstract KeyValueComponentBuilder createCommandInfo(
            KeyValueComponentBuilder builder,
            Function<Consumer<ListComponentBuilder<String>>, Component> listBuilderFunction,
            String commandName
    );

    private <T> void createInfo(C sender, String command, T item, InfoCreator<T> creator) {
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
