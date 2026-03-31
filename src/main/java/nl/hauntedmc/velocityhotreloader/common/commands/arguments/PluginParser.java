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

import java.util.Optional;

public class PluginParser<C extends VHRAudience<?>, P> implements ArgumentParser<C, P>, BlockingSuggestionProvider.Strings<C> {

    private final VHRPlugin<P, ?, C, ?, ?> plugin;

    public PluginParser(VHRPlugin<P, ?, C, ?, ?> plugin) {
        this.plugin = plugin;
    }

    public static <C extends VHRAudience<?>, P> ParserDescriptor<C, P> pluginParser(
            VHRPlugin<P, ?, C, ?, ?> plugin,
            Class<P> pluginType
    ) {
        return ParserDescriptor.of(new PluginParser<>(plugin), pluginType);
    }

    @Override
    @NonNull
    public ArgumentParseResult<P> parse(@NonNull CommandContext<C> context, @NonNull CommandInput commandInput) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Missing input for plugin!"));
        }
        String pluginName = commandInput.peekString();
        Optional<P> pluginOptional = plugin.getPluginManager().getPlugin(pluginName);
        if (pluginOptional.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Plugin '" + pluginName + "' does not exist!"));
        }

        commandInput.readString();
        return ArgumentParseResult.success(pluginOptional.get());
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getPluginNames();
    }
}
