package nl.hauntedmc.velocityhotreloaded.common.events;

public interface PluginEvent<T> {

    enum Stage {
        PRE,
        POST
    }

    T getPlugin();

    Stage getStage();

}
