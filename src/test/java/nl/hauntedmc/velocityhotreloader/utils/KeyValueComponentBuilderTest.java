package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KeyValueComponentBuilderTest {

    @Test
    void buildShouldFormatEachNonNullEntry() {
        MessagesResource.Message format = Mockito.mock(MessagesResource.Message.class);
        when(format.toComponent(any(TagResolver.class), any(TagResolver.class)))
                .thenReturn(Component.text("entry"));

        List<Component> components = KeyValueComponentBuilder.create(format, "key", "value")
                .key("A").value("1")
                .key("B").value((String) null)
                .key(Component.text("C")).value(Component.text("3"))
                .build();

        assertEquals(List.of(Component.text("entry"), Component.text("entry")), components);
        verify(format, times(2)).toComponent(any(TagResolver.class), any(TagResolver.class));
    }
}
