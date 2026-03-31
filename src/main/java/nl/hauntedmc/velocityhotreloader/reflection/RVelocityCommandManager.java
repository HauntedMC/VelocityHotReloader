package nl.hauntedmc.velocityhotreloader.reflection;

import com.mojang.brigadier.CommandDispatcher;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import nl.hauntedmc.velocityhotreloader.VHR;

public class RVelocityCommandManager {

    private static final Class<?> VELOCITY_COMMAND_MANAGER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.command.VelocityCommandManager");
    private static final Class<?> COMMAND_REGISTRAR_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.command.registrar.CommandRegistrar");
    private static final Field DISPATCHER_FIELD = Reflect.getAccessibleField(
            VELOCITY_COMMAND_MANAGER_CLASS,
            "dispatcher"
    );
    private static final Field REGISTRARS_FIELD = Reflect.getAccessibleField(
            VELOCITY_COMMAND_MANAGER_CLASS,
            "registrars"
    );
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final ClassLoader CURRENT_PLUGIN_CLASS_LOADER = RVelocityCommandManager.class.getClassLoader();

    private RVelocityCommandManager() {}

    public static CommandDispatcher<CommandSource> getDispatcher(CommandManager manager) {
        return Reflect.getFieldValue(DISPATCHER_FIELD, manager);
    }

    /**
     * Proxies the registrars.
     */
    @SuppressWarnings("deprecation")
    public static void proxyRegistrars(
            ProxyServer proxy,
            ClassLoader loader,
            BiConsumer<PluginContainer, CommandMeta> registrationConsumer
    ) {
        CommandManager commandManager = proxy.getCommandManager();
        List<Object> registrars = Reflect.getFieldValue(REGISTRARS_FIELD, commandManager);
        List<Object> proxiedRegistrars = new ArrayList<>(registrars.size());
        for (Object registrar : registrars) {
            proxiedRegistrars.add(Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{ COMMAND_REGISTRAR_CLASS },
                    new CommandRegistrarInvocationHandler(
                            proxy,
                            registrar,
                            registrationConsumer
                    )
            ));
        }

        // Velocity keeps this field final and immutable, so we replace it through Unsafe once at startup.
        Reflect.putObjectFieldUnsafe(commandManager, REGISTRARS_FIELD, List.copyOf(proxiedRegistrars));
    }

    public static final class CommandRegistrarInvocationHandler implements InvocationHandler {

        private final ProxyServer proxy;
        private final Object commandRegistrar;
        private final BiConsumer<PluginContainer, CommandMeta> registrationConsumer;

        /**
         * Constructs a new {@link CommandRegistrarInvocationHandler}.
         */
        public CommandRegistrarInvocationHandler(
                ProxyServer proxy,
                Object commandRegistrar,
                BiConsumer<PluginContainer, CommandMeta> registrationConsumer
        ) {
            this.proxy = proxy;
            this.commandRegistrar = commandRegistrar;
            this.registrationConsumer = registrationConsumer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object obj = method.invoke(commandRegistrar, args);
            if ("register".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof CommandMeta commandMeta) {
                handleRegisterMethod(commandMeta);
            }
            return obj;
        }

        private void handleRegisterMethod(CommandMeta commandMeta) {
            Map<ClassLoader, PluginContainer> pluginsByLoader = new HashMap<>();
            for (PluginContainer container : proxy.getPluginManager().getPlugins()) {
                Object instance = container.getInstance().orElse(null);
                if (instance != null) {
                    pluginsByLoader.put(instance.getClass().getClassLoader(), container);
                }
            }

            PluginContainer container = STACK_WALKER.walk(frames -> frames
                    .map(frame -> frame.getDeclaringClass().getClassLoader())
                    .filter(Objects::nonNull)
                    .dropWhile(classLoader -> classLoader == CURRENT_PLUGIN_CLASS_LOADER)
                    .map(pluginsByLoader::get)
                    .filter(found -> found != null)
                    .findFirst()
                    .orElse(null));
            if (container != null) {
                registrationConsumer.accept(container, commandMeta);
                return;
            }

            VHR.getInstance().getSlf4jLogger().warn(
                    "Couldn't find the registering plugin for the following aliases: {}",
                    commandMeta.getAliases()
            );
        }
    }
}
