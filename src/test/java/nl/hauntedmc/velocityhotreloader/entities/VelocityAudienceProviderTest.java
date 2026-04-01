package nl.hauntedmc.velocityhotreloader.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.List;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class VelocityAudienceProviderTest {

    @Test
    void shouldCreateAudienceWrappersAndBroadcastByPermission() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        ProxyServer proxy = mock(ProxyServer.class);
        ConsoleCommandSource console = mock(ConsoleCommandSource.class);
        Player allowed = mock(Player.class);
        Player denied = mock(Player.class);
        when(plugin.getProxy()).thenReturn(proxy);
        when(proxy.getConsoleCommandSource()).thenReturn(console);
        when(proxy.getAllPlayers()).thenReturn(List.of(allowed, denied));
        when(allowed.hasPermission("vhr.notify")).thenReturn(true);
        when(denied.hasPermission("vhr.notify")).thenReturn(false);

        VelocityAudienceProvider provider = new VelocityAudienceProvider(plugin);
        VelocityAudience consoleAudience = provider.getConsoleServerAudience();
        VelocityAudience wrapped = provider.get(console);

        assertEquals(console, consoleAudience.getSource());
        assertEquals(console, wrapped.getSource());

        Component message = Component.text("reload");
        provider.broadcast(message, "vhr.notify");
        verify(allowed).sendMessage(message);
        verify(denied, never()).sendMessage(message);
    }
}
