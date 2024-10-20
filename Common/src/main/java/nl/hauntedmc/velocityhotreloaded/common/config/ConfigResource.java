package nl.hauntedmc.velocityhotreloaded.common.config;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

public class ConfigResource extends VHRResource {

    private static final String CONFIG_RESOURCE = "config";

    public ConfigResource(VHRPlugin<?, ?, ?, ?, ?> plugin) {
        super(plugin, CONFIG_RESOURCE);
    }

    @Override
    public void migrate(int currentConfigVersion) {

    }
}
