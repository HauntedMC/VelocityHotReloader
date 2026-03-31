package nl.hauntedmc.velocityhotreloader.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;

public class VelocityPluginDisableEvent extends VelocityPluginEvent {

    public VelocityPluginDisableEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
