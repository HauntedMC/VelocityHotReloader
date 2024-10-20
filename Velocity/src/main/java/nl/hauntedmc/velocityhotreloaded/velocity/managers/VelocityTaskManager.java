package nl.hauntedmc.velocityhotreloaded.velocity.managers;

import com.velocitypowered.api.scheduler.ScheduledTask;
import java.time.Duration;
import nl.hauntedmc.velocityhotreloaded.common.managers.AbstractTaskManager;
import nl.hauntedmc.velocityhotreloaded.velocity.VHR;


public class VelocityTaskManager extends AbstractTaskManager<ScheduledTask> {

    private final VHR plugin;

    public VelocityTaskManager(VHR plugin) {
        super(ScheduledTask::cancel);
        this.plugin = plugin;
    }



    @Override
    protected ScheduledTask runTaskImpl(Runnable runnable) {
        return runTaskAsynchronously(runnable);
    }

    @Override
    public ScheduledTask runTaskLater(Runnable runnable, long delay) {
        return plugin.getProxy().getScheduler()
                .buildTask(plugin, runnable)
                .delay(Duration.ofMillis(delay * 50))
                .schedule();
    }

    @Override
    protected ScheduledTask runTaskAsynchronouslyImpl(Runnable runnable) {
        return plugin.getProxy().getScheduler()
                .buildTask(plugin, runnable)
                .schedule();
    }

    @Override
    public void cancelTask(ScheduledTask task) {
        task.cancel();
    }
}
