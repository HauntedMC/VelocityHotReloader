package nl.hauntedmc.velocityhotreloader.common.commands.arguments;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloader.velocity.VHR;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import java.util.Optional;

public class PluginParser implements
        ArgumentParser<VelocityAudience, PluginContainer>,
        BlockingSuggestionProvider.Strings<VelocityAudience> {

    private final VHR plugin;

    public PluginParser(VHR plugin) {
        this.plugin = plugin;
    }

    public static ParserDescriptor<VelocityAudience, PluginContainer> pluginParser(
            VHR plugin
    ) {
        return ParserDescriptor.of(new PluginParser(plugin), PluginContainer.class);
    }

    @Override
    @NonNull
    public ArgumentParseResult<PluginContainer> parse(
            @NonNull CommandContext<VelocityAudience> context,
            @NonNull CommandInput commandInput
    ) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Missing input for plugin!"));
        }
        String pluginName = commandInput.peekString();
        Optional<PluginContainer> pluginOptional = plugin.getPluginManager().getPlugin(pluginName);
        if (pluginOptional.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Plugin '" + pluginName + "' does not exist!"));
        }

        commandInput.readString();
        return ArgumentParseResult.success(pluginOptional.get());
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(
                                                                @NonNull CommandContext<VelocityAudience> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getPluginNames();
    }
}
