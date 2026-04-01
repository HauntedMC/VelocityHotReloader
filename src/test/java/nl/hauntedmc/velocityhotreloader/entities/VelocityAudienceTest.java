package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class VelocityAudienceTest {

    @Test
    void shouldDetectPlayerAndDelegatePermissionAndMessagesToSource() {
        Audience audience = mock(Audience.class);
        CommandSource source = mock(CommandSource.class);
        when(source.hasPermission("perm.test")).thenReturn(true);

        VelocityAudience velocityAudience = new VelocityAudience(audience, source);
        Component message = Component.text("ping");
        velocityAudience.sendMessage(message);

        assertFalse(velocityAudience.isPlayer());
        assertTrue(velocityAudience.hasPermission("perm.test"));
        verify(source).sendMessage(message);
        verifyNoInteractions(audience);
    }

    @Test
    void isPlayerShouldReturnTrueForPlayerSource() {
        Player player = mock(Player.class);
        VelocityAudience velocityAudience = new VelocityAudience(player, player);

        assertTrue(velocityAudience.isPlayer());
    }
}
