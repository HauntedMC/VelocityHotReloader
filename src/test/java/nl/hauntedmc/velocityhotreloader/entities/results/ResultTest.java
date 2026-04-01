package nl.hauntedmc.velocityhotreloader.entities.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void shouldExposeConfiguredKeys() {
        assertEquals(MessageKey.GENERIC_ERROR, Result.ERROR.getKey());
        assertNull(Result.SUCCESS.getKey());
    }
}
