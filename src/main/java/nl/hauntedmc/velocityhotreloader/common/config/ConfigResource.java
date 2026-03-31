package nl.hauntedmc.velocityhotreloader.common.config;

import nl.hauntedmc.velocityhotreloader.common.entities.VHRPlugin;

public class ConfigResource extends VHRResource {

    private static final String CONFIG_RESOURCE = "config";

    public ConfigResource(VHRPlugin<?, ?, ?, ?, ?> plugin) {
        super(plugin, CONFIG_RESOURCE);
    }

    @Override
    public void migrate(int currentConfigVersion) {

    }
}
