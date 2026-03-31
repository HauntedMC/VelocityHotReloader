package nl.hauntedmc.velocityhotreloader.commands;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.permission.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;
import nl.hauntedmc.velocityhotreloader.VHR;
import nl.hauntedmc.velocityhotreloader.entities.VelocityAudience;

public abstract class VHRCommand {

    protected final VHR plugin;
    protected final String commandName;
    protected final VHRConfig commandConfig;
    protected final Map<String, CommandComponent<VelocityAudience>> components;

    protected VHRCommand(VHR plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.commandConfig = (VHRConfig) plugin.getCommandsResource().getConfig()
                .get("commands." + commandName);
        this.components = new HashMap<>();
    }

    /**
     * Registers commands with the given CommandManager.
     */
    public final void register(CommandManager<VelocityAudience> manager) {
        register(
                manager,
                manager.commandBuilder(
                    applyPrefix(commandConfig.getString("main")),
                    commandConfig.getStringList("aliases").stream()
                            .map(this::applyPrefix)
                            .toArray(String[]::new)
                ).permission(commandConfig.getString("permission"))
        );
    }

    protected abstract void register(
            CommandManager<VelocityAudience> manager,
            Command.Builder<VelocityAudience> builder
    );

    public void addComponent(CommandComponent<VelocityAudience> component) {
        this.components.put(component.name(), component);
    }

    public <T> void addRequiredComponent(String name, ParserDescriptor<VelocityAudience, T> parser) {
        var component = CommandComponent.<VelocityAudience, T>builder()
                .name(name)
                .required(true)
                .parser(parser)
                .build();
        addComponent(component);
    }

    public <T> void addOptionalComponent(String name, ParserDescriptor<VelocityAudience, T> parser) {
        var component = CommandComponent.<VelocityAudience, T>builder()
                .name(name)
                .required(false)
                .parser(parser)
                .build();
        addComponent(component);
    }

    public CommandComponent<VelocityAudience> getComponent(String name) {
        return this.components.get(name);
    }

    /**
     * Builds a subcommand from the config.
     */
    public void registerSubcommand(
            CommandManager<VelocityAudience> manager,
            Command.Builder<VelocityAudience> builder,
            String subcommandName,
            UnaryOperator<Command.Builder<VelocityAudience>> builderUnaryOperator
    ) {
        CommandElement subcommand = parseSubcommand(subcommandName);

        Stream.concat(
                Stream.of(subcommand.getMain()),
                Arrays.stream(subcommand.getAliases())
        ).map(cmd -> {
            Command.Builder<VelocityAudience> subcommandBuilder = builder
                    .literal(cmd, subcommand.getDescription())
                    .permission(subcommand.getPermission());
            for (CommandElement flagElement : subcommand.getFlags()) {
                subcommandBuilder = subcommandBuilder.flag(createFlag(flagElement));
            }

            return builderUnaryOperator.apply(subcommandBuilder).build();
        }).forEach(manager::command);
    }

    /**
     * Parses a command from the config.
     */
    public CommandElement parseElement(VHRConfig elementConfig) {
        String main = applyPrefix(elementConfig.getString("main"));
        String descriptionString = elementConfig.getString("description");
        Description description = descriptionString == null ? null : Description.of(descriptionString);
        Permission permission = Permission.of(elementConfig.getString("permission"));
        boolean displayInHelp = elementConfig.getBoolean("display-in-help");
        String[] aliases = elementConfig.getStringList("aliases").stream()
                .map(this::applyPrefix)
                .toArray(String[]::new);

        List<CommandElement> flags = new ArrayList<>();
        Object flagsObject = elementConfig.get("flags");
        if (flagsObject instanceof VHRConfig) {
            VHRConfig flagsConfig = ((VHRConfig) flagsObject);
            for (String flagName : flagsConfig.getKeys()) {
                flags.add(parseElement((VHRConfig) flagsConfig.get(flagName)));
            }
        }

        return new CommandElement(main, description, permission, displayInHelp, aliases, flags);
    }

    /**
     * Parses a subcommand from the config.
     */
    public CommandElement parseSubcommand(String subcommandName) {
        return parseElement((VHRConfig) commandConfig.get("subcommands." + subcommandName));
    }

    public String getRawPath(String subcommandName) {
        return "commands." + commandName + ".subcommands." + subcommandName;
    }

    /**
     * Parses a flag from the config.
     */
    public CommandFlag<Void> parseFlag(String flagName) {
        return createFlag(parseElement((VHRConfig) commandConfig.get("flags." + flagName)));
    }

    /**
     * Creates a flag from a CommandElement.
     */
    public CommandFlag<Void> createFlag(CommandElement flagElement) {
        return CommandFlag.builder(flagElement.getMain())
                .withAliases(flagElement.getAliases())
                .withPermission(flagElement.getPermission())
                .withDescription(flagElement.getDescription())
                .build();
    }

    private String applyPrefix(String str) {
        return str.replace("%prefix%", "v");
    }

    protected static class CommandElement {

        private final String main;
        private final Description description;
        private final Permission permission;
        private final boolean displayInHelp;
        private final String[] aliases;
        private final List<CommandElement> flags;

        public CommandElement(
                String main,
                Description description,
                Permission permission,
                boolean displayInHelp,
                String[] aliases,
                List<CommandElement> flags
        ) {
            this.main = main;
            this.description = description;
            this.permission = permission;
            this.displayInHelp = displayInHelp;
            this.aliases = aliases;
            this.flags = flags;
        }

        public String getMain() {
            return main;
        }

        public Description getDescription() {
            return description;
        }

        public Permission getPermission() {
            return permission;
        }

        public boolean shouldDisplayInHelp() {
            return displayInHelp;
        }

        public String[] getAliases() {
            return aliases;
        }

        public List<CommandElement> getFlags() {
            return flags;
        }
    }
}
