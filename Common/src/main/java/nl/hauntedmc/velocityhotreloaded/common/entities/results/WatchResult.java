package nl.hauntedmc.velocityhotreloaded.common.entities.results;

import nl.hauntedmc.velocityhotreloaded.common.VHRApp;
import nl.hauntedmc.velocityhotreloaded.common.config.ConfigKey;
import nl.hauntedmc.velocityhotreloaded.common.config.MessageKey;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public enum WatchResult implements AbstractResult {
    START(MessageKey.WATCHPLUGIN_START),
    CHANGE(MessageKey.WATCHPLUGIN_CHANGE),
    ALREADY_WATCHING(MessageKey.WATCHPLUGIN_ALREADY_WATCHING),
    NOT_WATCHING(MessageKey.WATCHPLUGIN_NOT_WATCHING),
    FILE_DELETED(MessageKey.WATCHPLUGIN_FILE_DELETED),
    DELETED_FILE_IS_CREATED(MessageKey.WATCHPLUGIN_DELETED_FILE_IS_CREATED),
    STOPPED(MessageKey.WATCHPLUGIN_STOPPED),
    ;

    private final ConfigKey key;

    WatchResult(ConfigKey key) {
        this.key = key;
    }

    public void sendTo(VHRAudience<?> sender, TagResolver... tagResolvers) {
        Component component = VHRApp.getPlugin().getMessagesResource().get(key).toComponent(tagResolvers);
        sender.sendMessage(component);
    }

    @Override
    public ConfigKey getKey() {
        return key;
    }
}
