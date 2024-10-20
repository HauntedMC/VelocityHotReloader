package nl.hauntedmc.velocityhotreloaded.common.listeners;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

public abstract class PlayerListener<U extends VHRPlugin<P, ?, C, ?, ?>, P, C extends VHRAudience<?>>
        extends VHRListener<U, C> {

    protected PlayerListener(U plugin) {
        super(plugin);
    }

}
