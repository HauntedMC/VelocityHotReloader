package nl.hauntedmc.velocityhotreloader.entities;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class VelocityAudience extends VHRAudience<CommandSource> {

    protected VelocityAudience(Audience audience, CommandSource source) {
        super(audience, source);
    }

    @Override
    public boolean isPlayer() {
        return source instanceof Player;
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(Component component) {
        source.sendMessage(component);
    }
}
