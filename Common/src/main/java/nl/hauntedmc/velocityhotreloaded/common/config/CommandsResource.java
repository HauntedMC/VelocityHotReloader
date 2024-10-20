package nl.hauntedmc.velocityhotreloaded.common.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

/**
 * The Commands configuration.
 */
public class CommandsResource extends VHRResource {

    private static final String COMMANDS_RESOURCE = "commands";

    public CommandsResource(VHRPlugin<?, ?, ?, ?, ?> plugin) {
        super(plugin, COMMANDS_RESOURCE);
    }

    /**
     * Retrieves all flag aliases for the given flag path.
     */
    public Set<String> getAllFlagAliases(String path) {
        Object flagObject = getConfig().get(path);
        if (flagObject instanceof VHRConfig) {
            VHRConfig flagConfig = (VHRConfig) flagObject;

            Set<String> flagAliases = new HashSet<>();
            flagAliases.add("--" + flagConfig.getString("main"));
            for (String alias : flagConfig.getStringList("aliases")) {
                flagAliases.add("-" + alias);
            }

            return flagAliases;
        }

        return Collections.emptySet();
    }

    /**
     * Retrieves all aliases for the given path.
     */
    public Set<String> getAllAliases(String path) {
        Object object = getConfig().get(path);
        if (object instanceof VHRConfig) {
            VHRConfig config = (VHRConfig) object;

            Set<String> aliases = new HashSet<>();
            aliases.add(config.getString("main"));
            aliases.addAll(config.getStringList("aliases"));
            return aliases;
        }

        return Collections.emptySet();
    }

    @Override
    public void migrate(int currentConfigVersion) {

    }
}
