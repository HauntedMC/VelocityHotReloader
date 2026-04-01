package nl.hauntedmc.velocityhotreloader.entities.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InvalidPluginDescriptionExceptionTest {

    @Test
    void constructorsShouldPreserveMessageAndCause() {
        InvalidPluginDescriptionException empty = new InvalidPluginDescriptionException();
        InvalidPluginDescriptionException withMessage = new InvalidPluginDescriptionException("invalid");
        RuntimeException cause = new RuntimeException("cause");
        InvalidPluginDescriptionException withCause = new InvalidPluginDescriptionException(cause);

        assertNull(empty.getMessage());
        assertEquals("invalid", withMessage.getMessage());
        assertEquals(cause, withCause.getCause());
    }
}
