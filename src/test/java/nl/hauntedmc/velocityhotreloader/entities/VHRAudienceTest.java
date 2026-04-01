package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class VHRAudienceTest {

    @Test
    void sendMessageShouldDelegateToAudienceAndExposeSource() {
        Audience audience = mock(Audience.class);
        Object source = new Object();
        TestAudience testAudience = new TestAudience(audience, source);

        Component message = Component.text("hello");
        testAudience.sendMessage(message);

        verify(audience).sendMessage(message);
        assertEquals(source, testAudience.getSource());
    }

    private static final class TestAudience extends VHRAudience<Object> {

        private TestAudience(Audience audience, Object source) {
            super(audience, source);
        }

        @Override
        public boolean isPlayer() {
            return false;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }
    }
}
