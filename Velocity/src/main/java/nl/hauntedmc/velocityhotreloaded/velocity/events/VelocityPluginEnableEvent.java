package nl.hauntedmc.velocityhotreloaded.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloaded.common.events.PluginEnableEvent;

public class VelocityPluginEnableEvent extends VelocityPluginEvent implements PluginEnableEvent<PluginContainer> {

    public VelocityPluginEnableEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
