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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JarFilesParser<C extends VHRAudience<?>> implements ArgumentParser<C, File[]>, BlockingSuggestionProvider.Strings<C> {

    private static final File[] EMPTY = new File[0];


    private final VHRPlugin<?, ?, C, ?, ?> plugin;

    public JarFilesParser(VHRPlugin<?, ?, C, ?, ?> plugin) {
        this.plugin = plugin;
    }

    public static <C extends VHRAudience<?>> ParserDescriptor<C, File[]> jarFilesParser(VHRPlugin<?, ?, C, ?, ?> plugin) {
        return ParserDescriptor.of(new JarFilesParser<>(plugin), File[].class);
    }


    @Override
    public @NonNull ArgumentParseResult<File @NonNull []> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                                @NonNull CommandInput commandInput) {
        if (!commandInput.hasRemainingInput()) {
            return ArgumentParseResult.success(EMPTY);
        }
        Set<String> pluginFiles = new HashSet<>(plugin.getPluginManager().getPluginFileNames());
        System.out.println(Arrays.toString(pluginFiles.toArray(String[]::new)));
        List<File> files = new ArrayList<>();
        char firstChar = commandInput.peek();
        List<String> fileNames = new ArrayList<>();
        if (firstChar == '"') {
            commandInput.read();
            String remaining = commandInput.readUntil('"');
            fileNames.addAll(Arrays.asList(remaining.split(" ")));
        } else {
            fileNames.addAll(Arrays.asList(commandInput.readStringSkipWhitespace(false).split(" ")));
        }

        for (String pluginFileName : fileNames) {
            if (!pluginFiles.contains(pluginFileName)) {
                return ArgumentParseResult.failure(new IllegalArgumentException(
                        "Jar file for plugin '" + pluginFileName + "' does not exist!"
                ));
            }
            files.add(new File(plugin.getPluginManager().getPluginsFolder(), pluginFileName));
        }

        return ArgumentParseResult.success(files.toArray(File[]::new));
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext,
                                                                @NonNull CommandInput input) {
        return this.plugin.getPluginManager().getPluginFileNames();
    }


}
