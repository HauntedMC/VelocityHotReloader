package nl.hauntedmc.velocityhotreloaded.common;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPluginDescription;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

public class VHRApp<U extends VHRPlugin<P, T, C, S, D>, P, T, C extends VHRAudience<S>, S, D extends VHRPluginDescription> {

    public static final String VERSION = "{version}";

    private final Object platformPlugin;
    private final U plugin;

    @SuppressWarnings("rawtypes")
    private static VHRApp instance;

    private VHRApp(Object platformPlugin, U plugin) {
        this.platformPlugin = platformPlugin;
        this.plugin = plugin;
    }

    public static <
            U extends VHRPlugin<P, T, C, S, D>,
            P,
            T,
            C extends VHRAudience<S>,
            S,
            D extends VHRPluginDescription
        > void init(
            Object platformPlugin,
            U plugin
    ) {
        instance = new VHRApp<>(platformPlugin, plugin);
    }

    public static Object getPlatformPlugin() {
        return instance.platformPlugin;
    }

    @SuppressWarnings("unchecked")
    public static <
            U extends VHRPlugin<P, T, C, S, D>,
            P,
            T,
            C extends VHRAudience<S>,
            S,
            D extends VHRPluginDescription
        > U getPlugin() {
        return (U) instance.plugin;
    }
}
