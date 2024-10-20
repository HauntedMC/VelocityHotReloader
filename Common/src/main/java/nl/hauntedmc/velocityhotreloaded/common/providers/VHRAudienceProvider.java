package nl.hauntedmc.velocityhotreloaded.common.providers;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import net.kyori.adventure.text.Component;

public interface VHRAudienceProvider<S> {

    /**
     * Retrieves the console ServerAudience.
     */
    VHRAudience<S> getConsoleServerAudience();

    /**
     * Converts the given source (specific to impl) to an ServerAudience.
     */
    VHRAudience<S> get(S source);

    /**
     * Broadcasts a message to all with given permission.
     */
    void broadcast(Component component, String permission);
}
