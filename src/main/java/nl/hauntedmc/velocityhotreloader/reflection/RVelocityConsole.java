package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import java.lang.reflect.Field;

public class RVelocityConsole {

    private static final Class<?> VELOCITY_CONSOLE_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.console.VelocityConsole");
    private static final Field PERMISSION_FUNCTION_FIELD = Reflect.getAccessibleField(
            VELOCITY_CONSOLE_CLASS,
            "permissionFunction"
    );

    private RVelocityConsole() {}

    public static void setPermissionFunction(ConsoleCommandSource velocityConsole, PermissionFunction function) {
        Reflect.setFieldValue(PERMISSION_FUNCTION_FIELD, velocityConsole, function);
    }
}
