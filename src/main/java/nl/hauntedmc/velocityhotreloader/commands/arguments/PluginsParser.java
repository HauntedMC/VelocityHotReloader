package nl.hauntedmc.velocityhotreloader.commands.arguments;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloader.VHR;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PluginsParser implements
        ArgumentParser<VelocityAudience, PluginContainer[]>,
        BlockingSuggestionProvider.Strings<VelocityAudience> {

    private static final PluginContainer[] EMPTY = new PluginContainer[0];

    private final VHR plugin;
    private final String commandConfigPath;

    public PluginsParser(VHR plugin, String commandConfigPath) {
        this.plugin = plugin;
        this.commandConfigPath = commandConfigPath;
    }

    public static ParserDescriptor<VelocityAudience, PluginContainer[]> pluginsParser(
            VHR plugin
    ) {
        return pluginsParser(plugin, null);
    }

    public static ParserDescriptor<VelocityAudience, PluginContainer[]> pluginsParser(
            VHR plugin,
            String commandConfigPath
    ) {
        return ParserDescriptor.of(new PluginsParser(plugin, commandConfigPath), PluginContainer[].class);
    }


    @Override
    @NonNull
    public ArgumentParseResult<PluginContainer[]> parse(
            @NonNull CommandContext<VelocityAudience> context,
            @NonNull CommandInput commandInput
    ) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.success(EMPTY);
        }
        Set<String> flags = plugin.getCommandsResource().getAllFlagAliases(commandConfigPath + ".flags.force");
        List<PluginContainer> plugins = new ArrayList<>();
        while (commandInput.hasRemainingInput()) {
            String pluginName = commandInput.peekString();
            if (flags.contains(pluginName)) {
                commandInput.readString();
                continue;
            }
            PluginContainer parsedPlugin = plugin.getPluginManager().getPlugin(pluginName).orElse(null);
            if (parsedPlugin == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Plugin '" + pluginName + "' does not exist!"));
            }
            commandInput.readString();
            plugins.add(parsedPlugin);
        }
        return ArgumentParseResult.success(plugins.toArray(PluginContainer[]::new));
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(
                                                                @NonNull CommandContext<VelocityAudience> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getPluginNames();
    }
}
