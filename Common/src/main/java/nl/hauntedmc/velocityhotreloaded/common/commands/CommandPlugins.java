package nl.hauntedmc.velocityhotreloaded.common.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.incendo.cloud.context.CommandContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.hauntedmc.velocityhotreloaded.common.config.MessageKey;
import nl.hauntedmc.velocityhotreloaded.common.config.MessagesResource;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPlugin;
import nl.hauntedmc.velocityhotreloaded.common.entities.VHRPluginDescription;
import nl.hauntedmc.velocityhotreloaded.common.managers.AbstractPluginManager;
import nl.hauntedmc.velocityhotreloaded.common.utils.ListComponentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

@SuppressWarnings("LineLength")
public abstract class CommandPlugins<U extends VHRPlugin<P, ?, C, ?, D>, P, C extends VHRAudience<?>, D extends VHRPluginDescription>
        extends VHRCommand<U, C> {

    protected CommandPlugins(U plugin) {
        super(plugin, "plugins");
    }

    protected abstract void handlePlugins(CommandContext<C> context);

    /**
     * Sends a plugin list to the receiver.
     * @param sender The receiver of the plugin list.
     * @param plugins The plugins to be sent.
     * @param hasVersionFlag Whether to include the plugin version in the format
     */
    protected void handlePlugins(C sender, List<P> plugins, boolean hasVersionFlag) {
        List<P> filteredPlugins = new ArrayList<>(plugins.size());
        Set<String> hiddenPlugins = new HashSet<>(plugin.getConfigResource().getConfig().getStringList(
                "hide-plugins-from-plugins-command"
        ));
        AbstractPluginManager<P, D> pluginManager = plugin.getPluginManager();
        for (P plugin : plugins) {
            if (!hiddenPlugins.contains(pluginManager.getPluginId(plugin))) {
                filteredPlugins.add(plugin);
            }
        }

        MessagesResource messages = plugin.getMessagesResource();

        sender.sendMessage(messages.get(MessageKey.PLUGINS_HEADER).toComponent());
        TextComponent.Builder builder = Component.text();
        builder.append(messages.get(MessageKey.PLUGINS_PREFIX).toComponent(
                Placeholder.unparsed("count", String.valueOf(filteredPlugins.size()))
        ));
        builder.append(ListComponentBuilder.create(filteredPlugins)
                .separator(messages.get(MessageKey.PLUGINS_SEPARATOR).toComponent())
                .lastSeparator(messages.get(MessageKey.PLUGINS_LAST_SEPARATOR).toComponent())
                .format(plugin -> {
                    D description = pluginManager.getLoadedPluginDescription(plugin);

                    TextComponent.Builder formatBuilder = Component.text();
                    MessageKey formatKey = pluginManager.isPluginEnabled(plugin)
                            ? MessageKey.PLUGINS_FORMAT
                            : MessageKey.PLUGINS_FORMAT_DISABLED;
                    formatBuilder.append(messages.get(formatKey).toComponent(
                            Placeholder.unparsed("plugin", description.getName())
                    ));
                    if (hasVersionFlag) {
                        formatBuilder.append(messages.get(MessageKey.PLUGINS_VERSION).toComponent(
                                Placeholder.unparsed("version", description.getVersion())
                        ));
                    }

                    return formatBuilder.build();
                })
                .build());
        sender.sendMessage(builder.build());
        sender.sendMessage(messages.get(MessageKey.PLUGINS_FOOTER).toComponent());
    }
}
