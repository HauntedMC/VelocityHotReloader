package nl.hauntedmc.velocityhotreloader.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.hauntedmc.velocityhotreloader.entities.VelocityPluginDescription;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class VelocityPluginManagerTest {

    @Test
    void determineLoadOrderShouldRespectDependencies() {
        VelocityPluginManager manager = new VelocityPluginManager(
                mock(ProxyServer.class),
                mock(Logger.class),
                mock(VelocityPluginCommandManager.class)
        );

        VelocityPluginDescription pluginA = description("a", "b", "c");
        VelocityPluginDescription pluginB = description("b", "d");
        VelocityPluginDescription pluginC = description("c");
        VelocityPluginDescription pluginD = description("d");

        List<VelocityPluginDescription> ordered = manager.determineLoadOrder(List.of(pluginA, pluginB, pluginC, pluginD));
        Map<String, Integer> positions = indexById(ordered);

        assertTrue(positions.get("d") < positions.get("b"));
        assertTrue(positions.get("b") < positions.get("a"));
        assertTrue(positions.get("c") < positions.get("a"));
    }

    @Test
    void determineLoadOrderShouldThrowForCircularDependencies() {
        VelocityPluginManager manager = new VelocityPluginManager(
                mock(ProxyServer.class),
                mock(Logger.class),
                mock(VelocityPluginCommandManager.class)
        );

        VelocityPluginDescription a = description("a", "b");
        VelocityPluginDescription b = description("b", "c");
        VelocityPluginDescription c = description("c", "a");

        assertThrows(IllegalStateException.class, () -> manager.determineLoadOrder(List.of(a, b, c)));
    }

    @Test
    void getPluginFileShouldResolveSourceFromPluginDescription() {
        VelocityPluginManager manager = new VelocityPluginManager(
                mock(ProxyServer.class),
                mock(Logger.class),
                mock(VelocityPluginCommandManager.class)
        );
        PluginContainer withFile = pluginContainerWithSource(Optional.of(Path.of("plugins/test.jar")));
        PluginContainer withoutFile = pluginContainerWithSource(Optional.empty());

        File file = manager.getPluginFile(withFile);

        assertEquals("test.jar", file.getName());
        assertNull(manager.getPluginFile(withoutFile));
    }

    @Test
    void determineLoadOrderForContainersShouldMirrorDescriptionOrder() {
        PluginContainer a = pluginContainerWithSource(Optional.of(Path.of("plugins/a.jar")));
        PluginContainer b = pluginContainerWithSource(Optional.of(Path.of("plugins/b.jar")));
        PluginContainer c = pluginContainerWithSource(Optional.of(Path.of("plugins/c.jar")));

        VelocityPluginDescription descA = description("a", "b");
        VelocityPluginDescription descB = description("b", "c");
        VelocityPluginDescription descC = description("c");

        MappedDescriptionManager manager = new MappedDescriptionManager(Map.of(
                a, descA,
                b, descB,
                c, descC
        ));

        List<PluginContainer> ordered = manager.determineLoadOrder(List.of(a, b, c));
        assertEquals(List.of(c, b, a), ordered);
    }

    @Test
    void checkPluginStatesShouldReturnFirstPluginWithUnexpectedState() {
        PluginContainer a = pluginContainerWithSource(Optional.of(Path.of("plugins/a.jar")));
        PluginContainer b = pluginContainerWithSource(Optional.of(Path.of("plugins/b.jar")));
        TestablePluginManager manager = new TestablePluginManager(Map.of(a, true, b, false));

        assertSame(b, manager.check(List.of(a, b), true).orElse(null));
        assertTrue(manager.check(List.of(a), true).isEmpty());
    }

    private static VelocityPluginDescription description(String id, String... dependencies) {
        PluginDescription description = mock(PluginDescription.class);
        when(description.getId()).thenReturn(id);
        when(description.getName()).thenReturn(Optional.of(id.toUpperCase()));
        when(description.getVersion()).thenReturn(Optional.of("1.0.0"));
        when(description.getAuthors()).thenReturn(List.of("author"));
        when(description.getSource()).thenReturn(Optional.of(Path.of("plugins/" + id + ".jar")));
        Set<PluginDependency> dependencySet = Arrays.stream(dependencies)
                .map(depId -> new PluginDependency(depId, null, false))
                .collect(Collectors.toSet());
        when(description.getDependencies()).thenReturn(dependencySet);
        return new VelocityPluginDescription(description);
    }

    private static PluginContainer pluginContainerWithSource(Optional<Path> source) {
        PluginDescription description = mock(PluginDescription.class);
        when(description.getSource()).thenReturn(source);
        when(description.getId()).thenReturn(source.map(Path::getFileName).map(Path::toString).orElse("missing"));
        PluginContainer container = mock(PluginContainer.class);
        when(container.getDescription()).thenReturn(description);
        return container;
    }

    private static Map<String, Integer> indexById(List<VelocityPluginDescription> descriptions) {
        return java.util.stream.IntStream.range(0, descriptions.size())
                .boxed()
                .collect(Collectors.toMap(i -> descriptions.get(i).getId(), i -> i));
    }

    private static final class TestablePluginManager extends VelocityPluginManager {

        private final Map<PluginContainer, Boolean> enabledState;

        private TestablePluginManager(Map<PluginContainer, Boolean> enabledState) {
            super(mock(ProxyServer.class), mock(Logger.class), mock(VelocityPluginCommandManager.class));
            this.enabledState = enabledState;
        }

        @Override
        public boolean isPluginEnabled(PluginContainer plugin) {
            return enabledState.getOrDefault(plugin, false);
        }

        private Optional<PluginContainer> check(List<PluginContainer> plugins, boolean expectedEnabled) {
            return super.checkPluginStates(plugins, expectedEnabled);
        }
    }

    private static final class MappedDescriptionManager extends VelocityPluginManager {

        private final Map<PluginContainer, VelocityPluginDescription> descriptions;

        private MappedDescriptionManager(Map<PluginContainer, VelocityPluginDescription> descriptions) {
            super(mock(ProxyServer.class), mock(Logger.class), mock(VelocityPluginCommandManager.class));
            this.descriptions = descriptions;
        }

        @Override
        public VelocityPluginDescription getLoadedPluginDescription(PluginContainer plugin) {
            return descriptions.get(plugin);
        }
    }
}
