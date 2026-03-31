package nl.hauntedmc.velocityhotreloader.reflection;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.proxy.ConsoleCommandSource;

public class RVelocityConsole {

    private RVelocityConsole() {}

    public static void setPermissionFunction(ConsoleCommandSource velocityConsole, PermissionFunction function) {
        Reflect.setFieldValue(velocityConsole, "permissionFunction", function);
    }
}
