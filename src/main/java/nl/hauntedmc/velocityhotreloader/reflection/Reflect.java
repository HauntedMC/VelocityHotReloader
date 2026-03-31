package nl.hauntedmc.velocityhotreloader.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Small internal helper for reflective access to Velocity internals.
 */
public final class Reflect {

    private Reflect() {}

    public static Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to resolve class: " + className, ex);
        }
    }

    public static Field getAccessibleField(Class<?> type, String fieldName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                //
            }
        }
        throw new IllegalStateException("Unable to find field '" + fieldName + "' in " + type.getName());
    }

    public static Method getAccessibleMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                //
            }
        }
        throw new IllegalStateException("Unable to find method '" + methodName + "' in " + type.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object target, String fieldName) {
        try {
            return (T) getAccessibleField(target.getClass(), fieldName).get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to read field '" + fieldName + "'", ex);
        }
    }

    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            getAccessibleField(target.getClass(), fieldName).set(target, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to write field '" + fieldName + "'", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = getAccessibleMethod(target.getClass(), methodName, parameterTypes);
            return (T) method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Unable to invoke method '" + methodName + "'", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> type, Class<?>[] parameterTypes, Object... args) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to construct instance for " + type.getName(), ex);
        }
    }
}
