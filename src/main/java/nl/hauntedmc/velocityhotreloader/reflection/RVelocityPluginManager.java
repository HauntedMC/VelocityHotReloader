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

    /**
     * Retrieves the plugin map. Key is the id of the plugin.
     */
    public static Map<String, PluginContainer> getPlugins(PluginManager manager) {
        return Reflect.getFieldValue(PLUGINS_BY_ID_FIELD, manager);
    }

    public static Map<Object, PluginContainer> getPluginInstances(PluginManager manager) {
        return Reflect.getFieldValue(PLUGIN_INSTANCES_FIELD, manager);
    }

    public static void registerPlugin(PluginManager manager, PluginContainer container) {
        Reflect.invoke(REGISTER_PLUGIN_METHOD, manager, container);
    }
}
