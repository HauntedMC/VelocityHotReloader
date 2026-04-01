package nl.hauntedmc.velocityhotreloader.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageKeyTest {

    @Test
    void fromPathShouldMapDotsAndDashes() {
        assertEquals(MessageKey.GENERIC_NOT_EXISTS, MessageKey.fromPath("generic.not-exists"));
        assertEquals(MessageKey.HELP_HEADER, MessageKey.fromPath("help.header"));
    }

    @Test
    void shouldExposePathAndPlaceholderFlag() {
        assertEquals("reload", MessageKey.RELOAD.getPath());
        assertTrue(MessageKey.LOADPLUGIN.hasPlaceholders());
        assertFalse(MessageKey.RELOAD.hasPlaceholders());
        assertFalse(MessageKey.GENERIC_PREFIX.hasPlaceholders());
    }
}
