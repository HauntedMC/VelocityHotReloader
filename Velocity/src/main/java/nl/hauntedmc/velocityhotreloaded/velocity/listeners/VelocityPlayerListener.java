package nl.hauntedmc.velocityhotreloaded.velocity.listeners;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloaded.common.listeners.PlayerListener;
import nl.hauntedmc.velocityhotreloaded.velocity.entities.VelocityAudience;
import nl.hauntedmc.velocityhotreloaded.velocity.entities.VelocityPlugin;

public class VelocityPlayerListener extends PlayerListener<VelocityPlugin, PluginContainer, VelocityAudience> {

    public VelocityPlayerListener(VelocityPlugin plugin) {
        super(plugin);
    }

}
