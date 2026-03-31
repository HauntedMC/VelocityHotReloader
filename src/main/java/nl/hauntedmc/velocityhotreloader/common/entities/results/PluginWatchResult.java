package nl.hauntedmc.velocityhotreloader.common.entities.results;

import nl.hauntedmc.velocityhotreloader.common.config.ConfigKey;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class PluginWatchResult implements AbstractResult {

    private final WatchResult result;
    private final TagResolver[] placeholders;

    public PluginWatchResult(WatchResult result, TagResolver... placeholders) {
        this.result = result;
        this.placeholders = placeholders;
    }

    public WatchResult getResult() {
        return result;
    }

    public TagResolver[] getPlaceholders() {
        return placeholders;
    }

    @Override
    public ConfigKey getKey() {
        return result.getKey();
    }
}
