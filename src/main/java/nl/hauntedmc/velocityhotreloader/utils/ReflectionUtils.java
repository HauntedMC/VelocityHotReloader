package nl.hauntedmc.velocityhotreloader.utils;

import dev.frankheijden.minecraftreflection.Reflection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class ReflectionUtils {

    private static MethodHandle theUnsafeFieldMethodHandle;

    static {
        try {
            theUnsafeFieldMethodHandle = MethodHandles.lookup().unreflectGetter(Reflection.getAccessibleField(
                    Unsafe.class,
                    "theUnsafe"
            ));
        } catch (Throwable th) {
            throw new ExceptionInInitializerError(th);
        }
    }

    private ReflectionUtils() {}

    /**
     * Performs a privileged action while accessing {@link Unsafe}.
     */
    public static void doPrivilegedWithUnsafe(Consumer<Unsafe> privilegedAction) {
        try {
            privilegedAction.accept((Unsafe) theUnsafeFieldMethodHandle.invoke());
        } catch (Throwable th) {
            throw new IllegalStateException("Unable to execute operation with Unsafe", th);
        }
    }
}
