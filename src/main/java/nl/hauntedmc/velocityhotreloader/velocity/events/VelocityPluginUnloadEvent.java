package nl.hauntedmc.velocityhotreloader.velocity.events;

import com.velocitypowered.api.plugin.PluginContainer;

public class VelocityPluginUnloadEvent extends VelocityPluginEvent {

    public VelocityPluginUnloadEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
