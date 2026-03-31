package nl.hauntedmc.velocityhotreloader.commands;

import com.velocitypowered.api.plugin.PluginContainer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.hauntedmc.velocityhotreloader.config.MessageKey;
import nl.hauntedmc.velocityhotreloader.config.MessagesResource;
import nl.hauntedmc.velocityhotreloader.utils.ListComponentBuilder;
import nl.hauntedmc.velocityhotreloader.VHR;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudience;
import nl.hauntedmc.velocityhotreloader.entities.VelocityPluginDescription;
import nl.hauntedmc.velocityhotreloader.managers.VelocityPluginManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class CommandPlugins extends VHRCommand {

    public CommandPlugins(VHR plugin) {
        super(plugin, "plugins");
    }

    @Override
    protected void register(
            CommandManager<VelocityAudience> manager,
            Command.Builder<VelocityAudience> builder
    ) {
        manager.command(builder
                .flag(parseFlag("version"))
                .handler(this::handlePlugins));
    }

    private void handlePlugins(CommandContext<VelocityAudience> context) {
        VelocityAudience sender = context.sender();
        boolean hasVersionFlag = context.flags().contains("version");
        handlePlugins(sender, plugin.getPluginManager().getPluginsSorted(), hasVersionFlag);
    }

    /**
     * Sends a plugin list to the receiver.
     * @param sender The receiver of the plugin list.
     * @param plugins The plugins to be sent.
     * @param hasVersionFlag Whether to include the plugin version in the format
     */
    protected void handlePlugins(VelocityAudience sender, List<PluginContainer> plugins, boolean hasVersionFlag) {
        List<PluginContainer> filteredPlugins = new ArrayList<>(plugins.size());
        Set<String> hiddenPlugins = new HashSet<>(plugin.getConfigResource().getConfig().getStringList(
                "hide-plugins-from-plugins-command"
        ));
        VelocityPluginManager pluginManager = plugin.getPluginManager();
        for (PluginContainer pluginContainer : plugins) {
            if (!hiddenPlugins.contains(pluginManager.getPluginId(pluginContainer))) {
                filteredPlugins.add(pluginContainer);
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
                .format(pluginContainer -> {
                    VelocityPluginDescription description = pluginManager.getLoadedPluginDescription(pluginContainer);

                    TextComponent.Builder formatBuilder = Component.text();
                    MessageKey formatKey = pluginManager.isPluginEnabled(pluginContainer)
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
