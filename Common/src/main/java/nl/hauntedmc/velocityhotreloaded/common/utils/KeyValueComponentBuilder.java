package nl.hauntedmc.velocityhotreloaded.common.utils;

import nl.hauntedmc.velocityhotreloaded.common.config.MessagesResource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

public class KeyValueComponentBuilder {

    private final MessagesResource.Message format;
    private final List<Pair<TagResolver, TagResolver>> tagResolvers;
    @TagPattern
    private final String keyPlaceholder;
    @TagPattern
    private final String valuePlaceholder;

    private KeyValueComponentBuilder(
            MessagesResource.Message format,
            @TagPattern String keyPlaceholder,
            @TagPattern String valuePlaceholder
    ) {
        this.format = format;
        this.tagResolvers = new ArrayList<>();
        this.keyPlaceholder = keyPlaceholder;
        this.valuePlaceholder = valuePlaceholder;
    }

    /**
     * Constructs a new KeyValueComponentBuilder.
     */
    public static KeyValueComponentBuilder create(
            MessagesResource.Message format,
            @TagPattern String keyPlaceholder,
            String valuePlaceholder
    ) {
        return new KeyValueComponentBuilder(format, keyPlaceholder, valuePlaceholder);
    }

    public KeyValueComponentBuilder.KeyValuePair key(String key) {
        return new KeyValuePair(key);
    }

    public KeyValueComponentBuilder.KeyValuePair key(Component key) {
        return new KeyValuePair(key);
    }


    private KeyValueComponentBuilder add(Pair<TagResolver, TagResolver> tagResolver) {
        this.tagResolvers.add(tagResolver);
        return this;
    }

    /**
     * Builds the current ListMessageBuilder instance into a Component.
     */
    public List<Component> build() {
        List<Component> components = new ArrayList<>(tagResolvers.size());

        for (Pair<TagResolver, TagResolver> resolver : tagResolvers) {
            components.add(format.toComponent(resolver.first(), resolver.second()));
        }

        return components;
    }

    public class KeyValuePair {

        private final TagResolver key;

        private KeyValuePair(String key) {
            this.key = Placeholder.unparsed(keyPlaceholder, key);
        }

        private KeyValuePair(Component key) {
            this.key = Placeholder.component(keyPlaceholder, key);
        }

        public KeyValueComponentBuilder value(String value) {
            if (value == null) return KeyValueComponentBuilder.this;
            return add(new Pair<>(this.key, Placeholder.unparsed(valuePlaceholder, value)));
        }

        public KeyValueComponentBuilder value(Component value) {
            if (value == null) return KeyValueComponentBuilder.this;
            return add(new Pair<>(this.key, Placeholder.component(valuePlaceholder, value)));
        }
    }
}
