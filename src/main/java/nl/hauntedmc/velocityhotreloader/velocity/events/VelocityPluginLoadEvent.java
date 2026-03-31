package nl.hauntedmc.velocityhotreloader.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;

public class VelocityPluginLoadEvent extends VelocityPluginEvent {

    public VelocityPluginLoadEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
