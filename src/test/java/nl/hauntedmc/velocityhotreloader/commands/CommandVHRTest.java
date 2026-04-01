package nl.hauntedmc.velocityhotreloader.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.plugin.PluginContainer;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandVHRTest {

    private VelocityPluginManager pluginManager;
    private CommandVHR command;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        pluginManager = mock(VelocityPluginManager.class);
        when(plugin.getPluginManager()).thenReturn(pluginManager);
        command = new CommandVHR(plugin);
    }

    @Test
    void parseTokensShouldHandleQuotedAndUnquotedParts() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) invoke("parseTokens", new Class<?>[]{String.class},
                "alpha \"beta gamma\" delta");
        assertEquals(List.of("alpha", "beta gamma", "delta"), tokens);
    }

    @Test
    void parsePluginShouldFallbackToCaseInsensitiveLookup() throws Exception {
        PluginContainer target = mock(PluginContainer.class);
        when(pluginManager.getPlugin("TeSt")).thenReturn(Optional.empty());
        when(pluginManager.getPlugins()).thenReturn(List.of(target));
        when(pluginManager.getPluginId(target)).thenReturn("test");

        Object parsed = invoke("parsePlugin", new Class<?>[]{String.class}, "TeSt");
        assertSame(target, parsed);
    }

    @Test
    void parseCommandShouldMatchCaseInsensitive() throws Exception {
        when(pluginManager.getCommands()).thenReturn(Set.of("ReloadPlugin"));
        Object parsed = invoke("parseCommand", new Class<?>[]{String.class}, "reloadplugin");
        assertEquals("ReloadPlugin", parsed);
    }

    @Test
    void parsePluginsShouldReturnForceFlagAndInvalidPlugin() throws Exception {
        PluginContainer p1 = mock(PluginContainer.class);
        when(pluginManager.getPlugin("A")).thenReturn(Optional.of(p1));
        when(pluginManager.getPlugin("Missing")).thenReturn(Optional.empty());
        when(pluginManager.getPlugins()).thenReturn(List.of(p1));
        when(pluginManager.getPluginId(p1)).thenReturn("A");

        Object parsed = invoke("parsePlugins", new Class<?>[]{String.class, Set.class}, "A --force Missing", Set.of("--force"));
        @SuppressWarnings("unchecked")
        List<PluginContainer> plugins = (List<PluginContainer>) recordValue(parsed, "plugins");
        boolean force = (boolean) recordValue(parsed, "force");
        String invalid = (String) recordValue(parsed, "invalidPlugin");

        assertEquals(List.of(), plugins);
        assertTrue(force);
        assertEquals("Missing", invalid);
    }

    @Test
    void parseJarFilesShouldValidateAgainstKnownFiles() throws Exception {
        when(pluginManager.getPluginFileNames()).thenReturn(List.of("a.jar", "b.jar"));
        when(pluginManager.getPluginsFolder()).thenReturn(tempDir.toFile());

        Object parsed = invoke("parseJarFiles", new Class<?>[]{String.class}, "a.jar b.jar");
        @SuppressWarnings("unchecked")
        List<File> files = (List<File>) recordValue(parsed, "files");
        String invalid = (String) recordValue(parsed, "invalidJarFile");

        assertEquals(List.of(new File(tempDir.toFile(), "a.jar"), new File(tempDir.toFile(), "b.jar")), files);
        assertNull(invalid);
    }

    @Test
    void rootLiteralShouldReturnFirstTokenOrDefault() throws Exception {
        assertEquals("vhr", invoke("rootLiteral", new Class<?>[]{String.class}, "vhr reload --force"));
        assertEquals("velocityhotreloader", invoke("rootLiteral", new Class<?>[]{String.class}, " "));
    }

    @Test
    void suggestSimpleShouldFilterByPrefix() throws Exception {
        SuggestionsBuilder builder = new SuggestionsBuilder("re", 0);
        @SuppressWarnings("unchecked")
        CompletableFuture<Suggestions> future = (CompletableFuture<Suggestions>) invoke(
                "suggestSimple",
                new Class<?>[]{SuggestionsBuilder.class, Iterable.class},
                builder,
                List.of("reload", "restart", "plugins")
        );

        List<String> suggestions = future.join().getList().stream().map(Suggestion::getText).toList();
        assertEquals(List.of("reload", "restart"), suggestions);
    }

    @Test
    void suggestMultipleTokensShouldSkipAlreadyUsedValues() throws Exception {
        SuggestionsBuilder builder = new SuggestionsBuilder("one t", 0);
        @SuppressWarnings("unchecked")
        CompletableFuture<Suggestions> future = (CompletableFuture<Suggestions>) invoke(
                "suggestMultipleTokens",
                new Class<?>[]{SuggestionsBuilder.class, Iterable.class},
                builder,
                List.of("one", "two", "three")
        );

        List<String> suggestions = future.join().getList().stream().map(Suggestion::getText).toList();
        assertEquals(Set.of("two", "three"), Set.copyOf(suggestions));
    }

    private Object invoke(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = CommandVHR.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(command, args);
    }

    private static Object recordValue(Object record, String accessor) throws Exception {
        Method method = record.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(record);
    }
}
