package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;

public class RJavaPluginLoader {

    private static final Class<?> JAVA_PLUGIN_LOADER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.plugin.loader.java.JavaPluginLoader");

    private RJavaPluginLoader() {}

    /**
     * Constructs a new instance of a JavaPluginLoader.
     */
    public static Object newInstance(ProxyServer proxy, Path baseDirectory) {
        return Reflect.newInstance(
                JAVA_PLUGIN_LOADER_CLASS,
                new Class<?>[]{ProxyServer.class, Path.class},
                proxy,
                baseDirectory
        );
    }

    /**
     * Loads a candidate description from the given source.
     */
    public static PluginDescription loadPluginDescription(Object javaPluginLoader, Path source) {
        String methodName = "loadCandidate";
        try {
            Reflect.getAccessibleMethod(JAVA_PLUGIN_LOADER_CLASS, methodName, Path.class);
        } catch (IllegalStateException ex) {
            methodName = "loadPluginDescription";
        }

        return Reflect.invoke(javaPluginLoader, methodName, new Class<?>[]{Path.class}, source);
    }

    /**
     * Loads the plugin from their candidate PluginDescription.
     */
    public static PluginDescription loadPlugin(Object javaPluginLoader, PluginDescription candidate) {
        String methodName = "createPluginFromCandidate";
        try {
            Reflect.getAccessibleMethod(JAVA_PLUGIN_LOADER_CLASS, methodName, PluginDescription.class);
        } catch (IllegalStateException ex) {
            methodName = "loadPlugin";
        }

        return Reflect.invoke(
                javaPluginLoader,
                methodName,
                new Class<?>[]{PluginDescription.class},
                candidate
        );
    }

    public static Module createModule(Object javaPluginLoader, PluginContainer container) {
        return Reflect.invoke(
                javaPluginLoader,
                "createModule",
                new Class<?>[]{PluginContainer.class},
                container
        );
    }

    /**
     * Creates the plugin.
     */
    public static void createPlugin(Object javaPluginLoader, PluginContainer container, Module... modules) {
        Reflect.invoke(
                javaPluginLoader,
                "createPlugin",
                new Class<?>[]{PluginContainer.class, Module[].class},
                container,
                modules
        );
    }
}
