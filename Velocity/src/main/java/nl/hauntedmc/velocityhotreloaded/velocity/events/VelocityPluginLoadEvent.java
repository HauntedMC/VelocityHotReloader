package nl.hauntedmc.velocityhotreloaded.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloaded.common.events.PluginLoadEvent;

public class VelocityPluginLoadEvent extends VelocityPluginEvent implements PluginLoadEvent<PluginContainer> {

    public VelocityPluginLoadEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
