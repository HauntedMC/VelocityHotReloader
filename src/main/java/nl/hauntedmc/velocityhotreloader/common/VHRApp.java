package nl.hauntedmc.velocityhotreloader.common;

import nl.hauntedmc.velocityhotreloader.common.entities.VHRPlugin;
import nl.hauntedmc.velocityhotreloader.velocity.VHR;

public class VHRApp {

    public static final String VERSION = "{version}";

    private final VHR platformPlugin;
    private final VHRPlugin<?, ?, ?, ?, ?> plugin;
    private static VHRApp instance;

    private VHRApp(VHR platformPlugin, VHRPlugin<?, ?, ?, ?, ?> plugin) {
        this.platformPlugin = platformPlugin;
        this.plugin = plugin;
    }

    public static void init(VHR platformPlugin, VHRPlugin<?, ?, ?, ?, ?> plugin) {
        instance = new VHRApp(platformPlugin, plugin);
    }

    public static VHR getPlatformPlugin() {
        return instance.platformPlugin;
    }

    public static VHRPlugin<?, ?, ?, ?, ?> getPlugin() {
        return instance.plugin;
    }
}
