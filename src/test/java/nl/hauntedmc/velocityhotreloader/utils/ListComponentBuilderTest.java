package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ListComponentBuilderTest {

    @Test
    void buildShouldReturnEmptyValueWhenListIsEmpty() {
        Component empty = Component.text("<none>");
        Component result = ListComponentBuilder.<String>create()
                .format(Component::text)
                .emptyValue(empty)
                .build();

        assertEquals(empty, result);
    }

    @Test
    void buildShouldFormatSingleElement() {
        Component result = ListComponentBuilder.create("alpha")
                .format(value -> Component.text(value.toUpperCase()))
                .build();

        assertEquals(Component.text("ALPHA"), result);
    }

    @Test
    void buildShouldUseSeparatorsForMultipleElements() {
        Component result = ListComponentBuilder.create("a", "b", "c")
                .format(Component::text)
                .separator(Component.text(", "))
                .lastSeparator(Component.text(" and "))
                .build();

        assertEquals(Component.text().append(
                Component.text("a"),
                Component.text(", "),
                Component.text("b"),
                Component.text(" and "),
                Component.text("c")
        ).build(), result);
    }
}
