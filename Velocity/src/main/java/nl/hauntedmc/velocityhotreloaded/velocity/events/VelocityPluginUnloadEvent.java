package nl.hauntedmc.velocityhotreloaded.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloaded.common.events.PluginUnloadEvent;

public class VelocityPluginUnloadEvent extends VelocityPluginEvent implements PluginUnloadEvent<PluginContainer> {

    public VelocityPluginUnloadEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
