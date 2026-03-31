package nl.hauntedmc.velocityhotreloader.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import nl.hauntedmc.velocityhotreloader.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MessagesResource extends VHRResource {

    public static final String MESSAGES_RESOURCE = "messages";

    private final Map<ConfigKey, Message> messageMap;
    private final MiniMessage miniMessage;

    /**
     * Constructs a new MessagesResource.
     */
    public MessagesResource(VelocityHotReloaded plugin) {
        super(plugin, MESSAGES_RESOURCE);
        this.messageMap = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Message get(String path) {
        return get(MessageKey.fromPath(path));
    }

    public Message get(ConfigKey key) {
        return messageMap.get(key);
    }

    /**
     * Loads message keys and pre-compiles them if possible.
     */
    public void load(Collection<? extends PlaceholderConfigKey> keys) {
        for (PlaceholderConfigKey key : keys) {
            this.messageMap.put(key, new Message(key));
        }
    }

    public class Message {

        private final PlaceholderConfigKey key;
        private final String messageString;
        private final Component component;

        /**
         * Constructs a new Message.
         */
        public Message(PlaceholderConfigKey key) {
            this.key = key;
            this.messageString = getConfig().getString("messages." + key.getPath());
            this.component = key.hasPlaceholders() ? null : miniMessage.deserialize(messageString);
        }

        /**
         * Creates a {@link Component}.
         */
        public Component toComponent() {
            return this.component == null ? miniMessage.deserialize(messageString) : this.component;
        }

        public Component toComponent(TagResolver... tags) {
            return this.component == null ? miniMessage.deserialize(messageString, tags) : this.component;
        }

        public Component toComponent(@TagPattern String placeholderName, String value) {
            return toComponent(Placeholder.unparsed(placeholderName, value));
        }


        public void sendTo(VHRAudience<?> serverAudience, TagResolver... tags) {
            serverAudience.sendMessage(toComponent(tags));
        }

        public void sendTo(VHRAudience<?> serverAudience, @TagPattern String tag, String value) {
            serverAudience.sendMessage(toComponent(tag, value));
        }
    }

    @Override
    public void migrate(int currentConfigVersion) {

    }
}
