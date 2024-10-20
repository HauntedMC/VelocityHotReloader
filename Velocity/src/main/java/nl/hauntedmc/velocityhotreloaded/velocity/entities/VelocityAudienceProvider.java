package nl.hauntedmc.velocityhotreloaded.velocity.entities;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import nl.hauntedmc.velocityhotreloaded.common.providers.VHRAudienceProvider;
import nl.hauntedmc.velocityhotreloaded.velocity.VHR;
import net.kyori.adventure.text.Component;

public class VelocityAudienceProvider implements VHRAudienceProvider<CommandSource> {

    private final VHR plugin;
    private final VelocityAudience consoleServerAudience;

    /**
     * Constructs a new VelocityAudienceProvider.
     */
    public VelocityAudienceProvider(VHR plugin) {
        this.plugin = plugin;
        this.consoleServerAudience = new VelocityAudience(
                plugin.getProxy().getConsoleCommandSource(),
                plugin.getProxy().getConsoleCommandSource()
        );
    }

    @Override
    public VelocityAudience getConsoleServerAudience() {
        return consoleServerAudience;
    }

    @Override
    public VelocityAudience get(CommandSource source) {
        return new VelocityAudience(source, source);
    }

    @Override
    public void broadcast(Component component, String permission) {
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }
}
