package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import java.lang.reflect.Constructor;

public class RVelocityPluginContainer {

    private static final Class<?> VELOCITY_PLUGIN_CONTAINER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer");
    private static final Constructor<?> VELOCITY_PLUGIN_CONTAINER_CONSTRUCTOR = Reflect.getAccessibleConstructor(
            VELOCITY_PLUGIN_CONTAINER_CLASS,
            PluginDescription.class
    );

    private RVelocityPluginContainer() {}

    public static PluginContainer newInstance(PluginDescription description) {
        return Reflect.newInstance(VELOCITY_PLUGIN_CONTAINER_CONSTRUCTOR, description);
    }
}
