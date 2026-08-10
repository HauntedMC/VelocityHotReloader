package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class RVelocityPluginManager {

    private static final Class<?> VELOCITY_PLUGIN_MANAGER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.plugin.VelocityPluginManager");
    private static final Field PLUGINS_BY_ID_FIELD = Reflect.getAccessibleField(
            VELOCITY_PLUGIN_MANAGER_CLASS,
            "pluginsById"
    );
    private static final Field PLUGIN_INSTANCES_FIELD = Reflect.getAccessibleField(
            VELOCITY_PLUGIN_MANAGER_CLASS,
            "pluginInstances"
    );
    private static final Method REGISTER_PLUGIN_METHOD = Reflect.getAccessibleMethod(
            VELOCITY_PLUGIN_MANAGER_CLASS,
            "registerPlugin",
            PluginContainer.class
    );

    private RVelocityPluginManager() {}

    public static void registerPlugin(PluginManager manager, PluginContainer container) {
        Reflect.invoke(REGISTER_PLUGIN_METHOD, manager, container);
    }

    /**
     * Removes a plugin from every registry maintained by Velocity's plugin manager.
     *
     * <p>Velocity 4.1 derives its public plugin collection from these maps. Removing an unloaded
     * container from both registries keeps it out of subsequent plugin injections.</p>
     */
    public static void unregisterPlugin(PluginManager manager, PluginContainer container) {
        Map<String, PluginContainer> pluginsById = Reflect.getFieldValue(PLUGINS_BY_ID_FIELD, manager);
        pluginsById.entrySet().removeIf(entry -> entry.getValue() == container);

        Map<Object, PluginContainer> pluginInstances = Reflect.getFieldValue(PLUGIN_INSTANCES_FIELD, manager);
        pluginInstances.entrySet().removeIf(entry -> entry.getValue() == container);
    }
}
