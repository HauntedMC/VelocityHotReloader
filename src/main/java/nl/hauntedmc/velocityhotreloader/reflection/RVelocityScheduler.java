package nl.hauntedmc.velocityhotreloader.reflection;

import com.google.common.collect.Multimap;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

public class RVelocityScheduler {

    private RVelocityScheduler() {}

    public static Multimap<Object, ScheduledTask> getTasksByPlugin(Scheduler scheduler) {
        return Reflect.getFieldValue(scheduler, "tasksByPlugin");
    }
}
