package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.util.Map;

public class RVelocityPluginManager {

    private RVelocityPluginManager() {}

    /**
     * Retrieves the plugin map. Key is the id of the plugin.
     */
    public static Map<String, PluginContainer> getPlugins(PluginManager manager) {
        String fieldName = "pluginsById";
        try {
            Reflect.getAccessibleField(manager.getClass(), fieldName);
        } catch (IllegalStateException ex) {
            fieldName = "plugins";
        }

        return Reflect.getFieldValue(manager, fieldName);
    }

    public static Map<Object, PluginContainer> getPluginInstances(PluginManager manager) {
        return Reflect.getFieldValue(manager, "pluginInstances");
    }

    public static void registerPlugin(PluginManager manager, PluginContainer container) {
        Reflect.invoke(
                manager,
                "registerPlugin",
                new Class<?>[]{PluginContainer.class},
                container
        );
    }
}
