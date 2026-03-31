package nl.hauntedmc.velocityhotreloader.common.commands.arguments;

import nl.hauntedmc.velocityhotreloader.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.common.entities.VHRPlugin;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

public class PluginsParser<C extends VHRAudience<?>, P> implements ArgumentParser<C, P[]>, BlockingSuggestionProvider.Strings<C> {

    private final P[] empty;

    private final VHRPlugin<P, ?, C, ?, ?> plugin;
    private final IntFunction<P[]> arrayFunction;
    private final String commandConfigPath;

    public PluginsParser(VHRPlugin<P, ?, C, ?, ?> plugin,
                         String commandConfigPath,
                         IntFunction<P[]> arrayFunction) {
        this.plugin = plugin;
        this.empty = arrayFunction.apply(0);
        this.arrayFunction = arrayFunction;
        this.commandConfigPath = commandConfigPath;
    }

    public static <C extends VHRAudience<?>, P> ParserDescriptor<C, P[]> pluginsParser(
            VHRPlugin<P, ?, C, ?, ?> plugin,
            IntFunction<P[]> arrayFunction,
            Class<P> pluginType
    ) {
        return pluginsParser(plugin, null, arrayFunction, pluginType);
    }

    @SuppressWarnings("unchecked")
    public static <C extends VHRAudience<?>, P> ParserDescriptor<C, P[]> pluginsParser(
            VHRPlugin<P, ?, C, ?, ?> plugin,
            String commandConfigPath,
            IntFunction<P[]> arrayFunction,
            Class<P> pluginType
    ) {
        return ParserDescriptor.of(new PluginsParser<>(plugin, commandConfigPath, arrayFunction),
                (Class<P[]>) pluginType.arrayType());
    }


    @Override
    @NonNull
    public ArgumentParseResult<P[]> parse(@NonNull CommandContext<C> context, @NonNull CommandInput commandInput) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.success(this.empty);
        }
        Set<String> flags = plugin.getCommandsResource().getAllFlagAliases(commandConfigPath + ".flags.force");
        List<P> plugins = new ArrayList<>();
        while (commandInput.hasRemainingInput()) {
            String pluginName = commandInput.peekString();
            if (flags.contains(pluginName)) {
                continue;
            }
            Optional<P> pluginOptional = plugin.getPluginManager().getPlugin(pluginName);
            if (pluginOptional.isEmpty()) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Plugin '" + pluginName + "' does not exist!"));
            }
            commandInput.readString();
            plugins.add(pluginOptional.get());
        }
        return ArgumentParseResult.success(plugins.toArray(this.arrayFunction));
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getPluginNames();
    }
}
