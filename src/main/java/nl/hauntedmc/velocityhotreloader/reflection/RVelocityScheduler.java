package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.common.collect.Multimap;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import java.lang.reflect.Field;

public class RVelocityScheduler {

    private static final Class<?> VELOCITY_SCHEDULER_CLASS =
            Reflect.classForName("com.velocitypowered.proxy.scheduler.VelocityScheduler");
    private static final Field TASKS_BY_PLUGIN_FIELD = Reflect.getAccessibleField(
            VELOCITY_SCHEDULER_CLASS,
            "tasksByPlugin"
    );

    private RVelocityScheduler() {}

    public static Multimap<Object, ScheduledTask> getTasksByPlugin(Scheduler scheduler) {
        return Reflect.getFieldValue(TASKS_BY_PLUGIN_FIELD, scheduler);
    }
}
