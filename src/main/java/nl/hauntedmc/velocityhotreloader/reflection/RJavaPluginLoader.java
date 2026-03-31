package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class RJavaPluginLoader {

    private static final Class<?> JAVA_PLUGIN_LOADER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.plugin.loader.java.JavaPluginLoader");
    private static final Constructor<?> JAVA_PLUGIN_LOADER_CONSTRUCTOR = Reflect.getAccessibleConstructor(
            JAVA_PLUGIN_LOADER_CLASS,
            ProxyServer.class,
            Path.class
    );
    private static final Method LOAD_CANDIDATE_METHOD = Reflect.getAccessibleMethod(
            JAVA_PLUGIN_LOADER_CLASS,
            "loadCandidate",
            Path.class
    );
    private static final Method CREATE_PLUGIN_FROM_CANDIDATE_METHOD = Reflect.getAccessibleMethod(
            JAVA_PLUGIN_LOADER_CLASS,
            "createPluginFromCandidate",
            PluginDescription.class
    );
    private static final Method CREATE_MODULE_METHOD = Reflect.getAccessibleMethod(
            JAVA_PLUGIN_LOADER_CLASS,
            "createModule",
            PluginContainer.class
    );
    private static final Method CREATE_PLUGIN_METHOD = Reflect.getAccessibleMethod(
            JAVA_PLUGIN_LOADER_CLASS,
            "createPlugin",
            PluginContainer.class,
            Module[].class
    );

    private RJavaPluginLoader() {}

    /**
     * Constructs a new instance of a JavaPluginLoader.
     */
    public static Object newInstance(ProxyServer proxy, Path baseDirectory) {
        return Reflect.newInstance(JAVA_PLUGIN_LOADER_CONSTRUCTOR, proxy, baseDirectory);
    }

    /**
     * Loads a candidate description from the given source.
     */
    public static PluginDescription loadPluginDescription(Object javaPluginLoader, Path source) {
        return Reflect.invoke(LOAD_CANDIDATE_METHOD, javaPluginLoader, source);
    }

    /**
     * Loads the plugin from their candidate PluginDescription.
     */
    public static PluginDescription loadPlugin(Object javaPluginLoader, PluginDescription candidate) {
        return Reflect.invoke(CREATE_PLUGIN_FROM_CANDIDATE_METHOD, javaPluginLoader, candidate);
    }

    public static Module createModule(Object javaPluginLoader, PluginContainer container) {
        return Reflect.invoke(CREATE_MODULE_METHOD, javaPluginLoader, container);
    }

    /**
     * Creates the plugin.
     */
    public static void createPlugin(Object javaPluginLoader, PluginContainer container, Module... modules) {
        Reflect.invoke(CREATE_PLUGIN_METHOD, javaPluginLoader, container, modules);
    }
}
