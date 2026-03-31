package nl.hauntedmc.velocityhotreloader.entities.results;

import nl.hauntedmc.velocityhotreloader.config.ConfigKey;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public record PluginWatchResult(WatchResult result, TagResolver... placeholders) implements AbstractResult {

    @Override
    public ConfigKey getKey() {
        return result.getKey();
    }
}
