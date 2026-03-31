package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.common.collect.Multimap;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginContainer;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RVelocityEventManager {

    private static final Class<?> HANDLER_REGISTRATION_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.event.VelocityEventManager$HandlerRegistration");

    private RVelocityEventManager() {}

    @SuppressWarnings("unchecked")
    public static Multimap<Class<?>, Object> getHandlersByType(EventManager manager) {
        return Reflect.getFieldValue(manager, "handlersByType");
    }

    /**
     * Retrieves the registrations from a plugin for a specific event.
     */
    public static List<Object> getRegistrationsByPlugins(
            EventManager manager,
            List<Object> plugins,
            Class<?> eventClass
    ) {
        Comparator<Object> comparator = Reflect.getFieldValue(manager, "handlerComparator");
        return getHandlersByType(manager).get(eventClass).stream()
                .filter(r -> plugins.contains(RHandlerRegistration.getPlugin(r).getInstance().orElse(null)))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Registers the listener for a given plugin.
     */
    public static void registerInternally(EventManager manager, PluginContainer container, Object listener) {
        Reflect.invoke(
                manager,
                "registerInternally",
                new Class<?>[]{PluginContainer.class, Object.class},
                container,
                listener
        );
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

        Object registrationsEmptyArray = Array.newInstance(HANDLER_REGISTRATION_CLASS, 0);
        Class<?> registrationsArrayClass = registrationsEmptyArray.getClass();

        Reflect.invoke(
                manager,
                "fire",
                new Class<?>[]{
                        CompletableFuture.class,
                        Object.class,
                        int.class,
                        boolean.class,
                        registrationsArrayClass
                },
                future,
                event,
                0,
                true,
                registrations.toArray((Object[]) registrationsEmptyArray)
        );

        return future;
    }

    public static class RHandlerRegistration {

        private RHandlerRegistration() {}

        public static PluginContainer getPlugin(Object registration) {
            return Reflect.getFieldValue(registration, "plugin");
        }

        public static EventHandler<Object> getEventHandler(Object registration) {
            return Reflect.getFieldValue(registration, "handler");
        }
    }
}
