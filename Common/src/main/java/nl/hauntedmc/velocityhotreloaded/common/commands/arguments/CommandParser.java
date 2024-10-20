package nl.hauntedmc.velocityhotreloaded.common.commands.arguments;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import java.util.Optional;

public class CommandParser<C extends VHRAudience<?>> implements ArgumentParser<C, String>, BlockingSuggestionProvider.Strings<C> {

    private final VHRPlugin<?, ?, C, ?, ?> plugin;

    public CommandParser(VHRPlugin<?, ?, C, ?, ?> plugin) {
        this.plugin = plugin;
    }

    public static <C extends VHRAudience<?>> ParserDescriptor<C, String> commandParser(
            VHRPlugin<?, ?, C, ?, ?> plugin
    ) {
        return ParserDescriptor.of(new CommandParser<>(plugin), String.class);
    }

    @Override
    @NonNull
    public ArgumentParseResult<String> parse(@NonNull CommandContext<C> context, @NonNull CommandInput commandInput) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Missing input for command!"));
        }

        String commandName = commandInput.peekString();
        Optional<String> commandOptional = plugin.getPluginManager().getCommands().stream()
                .filter(command -> command.equalsIgnoreCase(commandName))
                .findFirst();

        if (commandOptional.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Command '" + commandName + "' does not exist!"));
        }

        commandInput.readString();
        return ArgumentParseResult.success(commandOptional.get());
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getCommands();
    }
}