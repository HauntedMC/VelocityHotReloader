package nl.hauntedmc.velocityhotreloader.events;

import com.velocitypowered.api.plugin.PluginContainer;

public abstract class VelocityPluginEvent {

    public enum Stage {
        PRE,
        POST
    }

    private final PluginContainer plugin;
    private final Stage stage;

    protected VelocityPluginEvent(PluginContainer plugin, Stage stage) {
        this.plugin = plugin;
        this.stage = stage;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    public Stage getStage() {
        return stage;
    }
}
