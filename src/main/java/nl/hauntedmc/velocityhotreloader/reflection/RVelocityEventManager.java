package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.common.collect.Multimap;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginContainer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RVelocityEventManager {

    private static final Class<?> VELOCITY_EVENT_MANAGER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.event.VelocityEventManager");
    private static final Class<?> HANDLER_REGISTRATION_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");
    private static final Class<?> HANDLER_REGISTRATION_ARRAY_CLASS =
            Array.newInstance(HANDLER_REGISTRATION_CLASS, 0).getClass();
    private static final Field HANDLERS_BY_TYPE_FIELD = Reflect.getAccessibleField(
            VELOCITY_EVENT_MANAGER_CLASS,
            "handlersByType"
    );
    private static final Field HANDLER_COMPARATOR_FIELD = Reflect.getAccessibleField(
            VELOCITY_EVENT_MANAGER_CLASS,
            "handlerComparator"
    );
    private static final Method REGISTER_INTERNALLY_METHOD = Reflect.getAccessibleMethod(
            VELOCITY_EVENT_MANAGER_CLASS,
            "registerInternally",
            PluginContainer.class,
            Object.class
    );
    private static final Method FIRE_METHOD = Reflect.getAccessibleMethod(
            VELOCITY_EVENT_MANAGER_CLASS,
            "fire",
            CompletableFuture.class,
            Object.class,
            int.class,
            boolean.class,
            HANDLER_REGISTRATION_ARRAY_CLASS
    );
    private static final Field HANDLER_REGISTRATION_PLUGIN_FIELD = Reflect.getAccessibleField(
            HANDLER_REGISTRATION_CLASS,
            "plugin"
    );
    private static final Field HANDLER_REGISTRATION_HANDLER_FIELD = Reflect.getAccessibleField(
            HANDLER_REGISTRATION_CLASS,
            "handler"
    );

    private RVelocityEventManager() {}

    public static Multimap<Class<?>, Object> getHandlersByType(EventManager manager) {
        return Reflect.getFieldValue(HANDLERS_BY_TYPE_FIELD, manager);
    }

    /**
     * Retrieves the registrations from a plugin for a specific event.
     */
    public static List<Object> getRegistrationsByPlugins(
            EventManager manager,
            List<Object> plugins,
            Class<?> eventClass
    ) {
        Comparator<Object> comparator = Reflect.getFieldValue(HANDLER_COMPARATOR_FIELD, manager);
        return getHandlersByType(manager).get(eventClass).stream()
                .filter(r -> plugins.contains(RHandlerRegistration.getPlugin(r).getInstance().orElse(null)))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Registers the listener for a given plugin.
     */
    public static void registerInternally(EventManager manager, PluginContainer container, Object listener) {
        Reflect.invoke(REGISTER_INTERNALLY_METHOD, manager, container, listener);
    }

    /**
     * Fires an event specifically for one plugin.
     */
    public static <E> CompletableFuture<E> fireForPlugins(
            EventManager manager,
            E event,
            List<Object> pluginInstances
    ) {
        List<Object> registrations = getRegistrationsByPlugins(manager, pluginInstances, event.getClass());
        CompletableFuture<E> future = new CompletableFuture<>();
        Object[] registrationsArray = registrations.toArray(
                (Object[]) Array.newInstance(HANDLER_REGISTRATION_CLASS, registrations.size())
        );

        Reflect.invoke(
                FIRE_METHOD,
                manager,
                future,
                event,
                0,
                true,
                registrationsArray
        );

        return future;
    }

    public static class RHandlerRegistration {

        private RHandlerRegistration() {}

        public static PluginContainer getPlugin(Object registration) {
            return Reflect.getFieldValue(HANDLER_REGISTRATION_PLUGIN_FIELD, registration);
        }

        public static EventHandler<Object> getEventHandler(Object registration) {
            return Reflect.getFieldValue(HANDLER_REGISTRATION_HANDLER_FIELD, registration);
        }
    }
}
