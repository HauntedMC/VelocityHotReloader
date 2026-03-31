package nl.hauntedmc.velocityhotreloader.entities;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import net.kyori.adventure.text.Component;

public class VelocityAudienceProvider {

    private final VelocityHotReloaded plugin;
    private final VelocityAudience consoleServerAudience;

    /**
     * Constructs a new VelocityAudienceProvider.
     */
    public VelocityAudienceProvider(VelocityHotReloaded plugin) {
        this.plugin = plugin;
        this.consoleServerAudience = new VelocityAudience(
                plugin.getProxy().getConsoleCommandSource(),
                plugin.getProxy().getConsoleCommandSource()
        );
    }

    public VelocityAudience getConsoleServerAudience() {
        return consoleServerAudience;
    }

    public VelocityAudience get(CommandSource source) {
        return new VelocityAudience(source, source);
    }

    public void broadcast(Component component, String permission) {
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }
}
