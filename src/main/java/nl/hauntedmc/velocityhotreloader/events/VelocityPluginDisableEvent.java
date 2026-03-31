package nl.hauntedmc.velocityhotreloader.events;

import com.velocitypowered.api.plugin.PluginContainer;

public class VelocityPluginDisableEvent extends VelocityPluginEvent {

    public VelocityPluginDisableEvent(PluginContainer plugin, Stage stage) {
        super(plugin, stage);
    }
}
