package nl.hauntedmc.velocityhotreloader.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;

public class VelocityPluginEnableEvent extends VelocityPluginEvent {

    public VelocityPluginEnableEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
