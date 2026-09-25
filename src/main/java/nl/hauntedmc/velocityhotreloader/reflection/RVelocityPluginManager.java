package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

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
    private static final Field PLUGINS_FIELD = findPluginsField();
    private static final Method REGISTER_PLUGIN_METHOD = Reflect.getAccessibleMethod(
            VELOCITY_PLUGIN_MANAGER_CLASS,
            "registerPlugin",
            PluginContainer.class
    );

    private RVelocityPluginManager() {}

    private static Field findPluginsField() {
        try {
            return Reflect.getAccessibleField(VELOCITY_PLUGIN_MANAGER_CLASS, "plugins");
        } catch (IllegalStateException missingInEarlierVelocity) {
            return null;
        }
    }

    public static void registerPlugin(PluginManager manager, PluginContainer container) {
        Reflect.invoke(REGISTER_PLUGIN_METHOD, manager, container);
    }

    /**
     * Removes a plugin from every registry maintained by Velocity's plugin manager.
     *
     * <p>Velocity 4.2 also keeps a separate collection backing getPlugins(). All three
     * registries must drop the container to prevent stale dependency checks after a reload.</p>
     */
    public static void unregisterPlugin(PluginManager manager, PluginContainer container) {
        Map<String, PluginContainer> pluginsById = Reflect.getFieldValue(PLUGINS_BY_ID_FIELD, manager);
        pluginsById.entrySet().removeIf(entry -> entry.getValue() == container);

        Map<Object, PluginContainer> pluginInstances = Reflect.getFieldValue(PLUGIN_INSTANCES_FIELD, manager);
        pluginInstances.entrySet().removeIf(entry -> entry.getValue() == container);

        if (PLUGINS_FIELD != null) {
            Set<PluginContainer> plugins = Reflect.getFieldValue(PLUGINS_FIELD, manager);
            plugins.remove(container);
        }
    }
}
