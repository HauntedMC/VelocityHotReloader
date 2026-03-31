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
import java.util.List;
import java.util.function.BiConsumer;
import nl.hauntedmc.velocityhotreloader.utils.ReflectionUtils;
import nl.hauntedmc.velocityhotreloader.VHR;

public class RVelocityCommandManager {

    private RVelocityCommandManager() {}

    public static CommandDispatcher<CommandSource> getDispatcher(CommandManager manager) {
        return Reflect.getFieldValue(manager, "dispatcher");
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
        List<Object> proxiedRegistrars = new ArrayList<>();

        Class<?> commandRegistrarClass;
        try {
            commandRegistrarClass = Class.forName("com.velocitypowered.proxy.command.registrar.CommandRegistrar");
        } catch (ClassNotFoundException ex) {
            VHR.getInstance().getSlf4jLogger().error("Unable to load Velocity command registrar class", ex);
            return;
        }

        List<Object> registrars = Reflect.getFieldValue(commandManager, "registrars");
        for (Object registrar : registrars) {
            proxiedRegistrars.add(Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{ commandRegistrarClass },
                    new CommandRegistrarInvocationHandler(
                            proxy,
                            registrar,
                            registrationConsumer
                    )
            ));
        }

        Field registrarsField = Reflect.getAccessibleField(commandManager.getClass(), "registrars");
        ReflectionUtils.doPrivilegedWithUnsafe(unsafe -> {
            long offset = unsafe.objectFieldOffset(registrarsField);
            unsafe.putObject(commandManager, offset, proxiedRegistrars);
        });
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
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();

            // Skip the first four elements, which is our overhead here
            for (int i = 4; i < elements.length; i++) {
                Class<?> clazz;
                try {
                    clazz = Class.forName(elements[i].getClassName());
                } catch (ClassNotFoundException ex) {
                    continue;
                }

                ClassLoader classLoader = clazz.getClassLoader();
                for (PluginContainer container : proxy.getPluginManager().getPlugins()) {
                    if (container.getInstance().filter(o -> o.getClass().getClassLoader() == classLoader).isPresent()) {
                        registrationConsumer.accept(container, commandMeta);
                        return;
                    }
                }
            }

            VHR.getInstance().getSlf4jLogger().warn(
                    "Couldn't find the registering plugin for the following aliases: {}",
                    commandMeta.getAliases()
            );
        }
    }
}
