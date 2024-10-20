package nl.hauntedmc.velocityhotreloaded.common.listeners;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;

public abstract class VHRListener<U extends VHRPlugin<?, ?, C, ?, ?>, C extends VHRAudience<?>> {

    protected final U plugin;

    protected VHRListener(U plugin) {
        this.plugin = plugin;
    }
}
