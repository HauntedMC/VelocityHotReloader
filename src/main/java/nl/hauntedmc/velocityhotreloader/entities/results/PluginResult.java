package nl.hauntedmc.velocityhotreloader.entities.results;

import nl.hauntedmc.velocityhotreloader.config.ConfigKey;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.VHR;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class PluginResult<T> implements AbstractResult {

    private final String pluginId;
    private final T plugin;
    private final Result result;
    private final TagResolver[] placeholders;

    public PluginResult(String pluginId, Result result) {
        this(pluginId, null, result);
    }

    /**
     * Constructs a new PluginResult.
     */
    public PluginResult(String pluginId, T plugin, Result result, TagResolver... placeholders) {
        this.pluginId = pluginId;
        this.plugin = plugin;
        this.result = result;
        this.placeholders = new TagResolver[placeholders.length + 1];
        this.placeholders[0] = Placeholder.unparsed("plugin", pluginId);
        System.arraycopy(placeholders, 0, this.placeholders, 1, placeholders.length);
    }

    public String getPluginId() {
        return pluginId;
    }

    public T getPlugin() {
        return plugin;
    }

    public Result getResult() {
        return result;
    }

    public TagResolver[] getPlaceholders() {
        return placeholders;
    }

    public boolean isSuccess() {
        return plugin != null && result == Result.SUCCESS;
    }

    public void sendTo(VHRAudience<?> sender, ConfigKey successKey) {
        ConfigKey key = isSuccess() ? successKey : result.getKey();
        sender.sendMessage(VHR.getInstance().getMessagesResource().get(key).toComponent(placeholders));
    }

    @Override
    public ConfigKey getKey() {
        return result.getKey();
    }
}
