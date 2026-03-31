package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;

public class RVelocityPluginContainer {

    private static final Class<?> VELOCITY_PLUGIN_CONTAINER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer");

    private RVelocityPluginContainer() {}

    public static PluginContainer newInstance(PluginDescription description) {
        return Reflect.newInstance(
                VELOCITY_PLUGIN_CONTAINER_CLASS,
                new Class<?>[]{PluginDescription.class},
                description
        );
    }
}
