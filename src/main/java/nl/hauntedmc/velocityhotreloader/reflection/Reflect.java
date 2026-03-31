package nl.hauntedmc.velocityhotreloader.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.misc.Unsafe;

/**
 * Small internal helper for reflective access to Velocity internals.
 */
public final class Reflect {

    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<FieldKey, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ConstructorKey, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    private static final Unsafe UNSAFE = resolveUnsafe();

    private Reflect() {}

    public static Class<?> classForName(String className) {
        return CLASS_CACHE.computeIfAbsent(className, Reflect::resolveClass);
    }

    public static Field getAccessibleField(Class<?> type, String fieldName) {
        return FIELD_CACHE.computeIfAbsent(new FieldKey(type, fieldName), key -> {
            for (Class<?> current = key.owner(); current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(key.name());
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    //
                }
            }
            throw new IllegalStateException(
                    "Unable to find field '" + key.name() + "' in " + key.owner().getName()
            );
        });
    }

    public static Method getAccessibleMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        MethodKey key = new MethodKey(type, methodName, List.of(parameterTypes.clone()));
        return METHOD_CACHE.computeIfAbsent(key, k -> {
            for (Class<?> current = k.owner(); current != null; current = current.getSuperclass()) {
                try {
                    Method method = current.getDeclaredMethod(k.name(), k.parameterTypes().toArray(Class[]::new));
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    //
                }
            }
            throw new IllegalStateException(
                    "Unable to find method '" + k.name() + "' in " + k.owner().getName()
            );
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Field field, Object target) {
        try {
            return (T) field.get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to read field '" + field.getName() + "'", ex);
        }
    }

    public static void setFieldValue(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to write field '" + field.getName() + "'", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object target, Object... args) {
        try {
            return (T) method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to invoke method '" + method.getName() + "'", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            throw new IllegalStateException("Unable to invoke method '" + method.getName() + "'", cause);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<?> constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Unable to construct instance for " + constructor.getDeclaringClass().getName(),
                    ex
            );
        }
    }

    public static Constructor<?> getAccessibleConstructor(Class<?> type, Class<?>... parameterTypes) {
        ConstructorKey key = new ConstructorKey(type, List.of(parameterTypes.clone()));
        return CONSTRUCTOR_CACHE.computeIfAbsent(key, k -> {
            try {
                Constructor<?> constructor = k.owner().getDeclaredConstructor(
                        k.parameterTypes().toArray(Class[]::new)
                );
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(
                        "Unable to find constructor for " + k.owner().getName()
                                + " with parameter types " + Arrays.toString(k.parameterTypes().toArray()),
                        ex
                );
            }
        });
    }

    /**
     * Writes to an object field via Unsafe. This is used only where Velocity exposes
     * immutable/final internals that cannot be reassigned through regular reflection.
     */
    @SuppressWarnings("deprecation")
    public static void putObjectFieldUnsafe(Object target, Field field, Object value) {
        long offset = UNSAFE.objectFieldOffset(field);
        UNSAFE.putObject(target, offset, value);
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to resolve class: " + className, ex);
        }
    }

    private static Unsafe resolveUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private record FieldKey(Class<?> owner, String name) {}

    private record MethodKey(Class<?> owner, String name, List<Class<?>> parameterTypes) {}

    private record ConstructorKey(Class<?> owner, List<Class<?>> parameterTypes) {}
}
