package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VelocityPluginDescriptionTest {

    @Test
    void shouldExposeMetadataFromPluginDescription() {
        PluginDependency depA = mock(PluginDependency.class);
        PluginDependency depB = mock(PluginDependency.class);
        when(depA.getId()).thenReturn("economy");
        when(depB.getId()).thenReturn("chat");

        PluginDescription description = mock(PluginDescription.class);
        when(description.getId()).thenReturn("example");
        when(description.getName()).thenReturn(Optional.of("Example Plugin"));
        when(description.getVersion()).thenReturn(Optional.of("1.0.0"));
        when(description.getAuthors()).thenReturn(List.of("Alice", "Bob"));
        when(description.getDependencies()).thenReturn(Set.of(depA, depB));
        when(description.getSource()).thenReturn(Optional.of(Path.of("plugins/example.jar")));

        VelocityPluginDescription wrapped = new VelocityPluginDescription(description);

        assertEquals("example", wrapped.getId());
        assertEquals("Example Plugin", wrapped.getName());
        assertEquals("1.0.0", wrapped.getVersion());
        assertEquals("Alice, Bob", wrapped.getAuthor());
        assertEquals("example.jar", wrapped.getFile().getName());
        assertTrue(wrapped.getDependencies().containsAll(Set.of("economy", "chat")));
        assertEquals(description, wrapped.getDescription());
    }
}
