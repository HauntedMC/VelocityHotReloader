package nl.hauntedmc.velocityhotreloaded.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;
import nl.hauntedmc.velocityhotreloaded.common.events.PluginDisableEvent;

public class VelocityPluginDisableEvent extends VelocityPluginEvent implements PluginDisableEvent<PluginContainer> {

    public VelocityPluginDisableEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
