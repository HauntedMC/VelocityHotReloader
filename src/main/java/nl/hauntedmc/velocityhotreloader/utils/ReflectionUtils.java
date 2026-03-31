package nl.hauntedmc.velocityhotreloader.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class ReflectionUtils {

    private static final MethodHandle THE_UNSAFE_FIELD_GETTER = createUnsafeFieldGetter();

    private static MethodHandle createUnsafeFieldGetter() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(unsafeField);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private ReflectionUtils() {}

    /**
     * Performs a privileged action while accessing {@link Unsafe}.
     */
    public static void doPrivilegedWithUnsafe(Consumer<Unsafe> privilegedAction) {
        try {
            privilegedAction.accept((Unsafe) THE_UNSAFE_FIELD_GETTER.invoke());
        } catch (Throwable th) {
            throw new IllegalStateException("Unable to execute operation with Unsafe", th);
        }
    }
}
