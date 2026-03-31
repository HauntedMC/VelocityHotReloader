package nl.hauntedmc.velocityhotreloader.managers;

import com.velocitypowered.api.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.hauntedmc.velocityhotreloader.entities.AbstractTask;
import nl.hauntedmc.velocityhotreloader.VHR;

/**
 * Tracks scheduled tasks so they can be cancelled individually or in bulk on shutdown/reload.
 */
public class VelocityTaskManager {

    private final VHR plugin;
    private final List<ScheduledTask> serverTasks;
    private final Map<String, RunningTask> tasks;

    public VelocityTaskManager(VHR plugin) {
        this.plugin = plugin;
        this.serverTasks = new ArrayList<>();
        this.tasks = new HashMap<>();
    }

    public ScheduledTask runTask(Runnable runnable) {
        return addTask(runTaskAsynchronously(runnable));
    }

    public ScheduledTask runTask(String key, AbstractTask abstractTask) {
        ScheduledTask task = runTask(abstractTask);
        tasks.put(key, new RunningTask(task, abstractTask));
        return task;
    }

    public ScheduledTask runTaskLater(Runnable runnable, long delay) {
        return addTask(plugin.getProxy().getScheduler()
                .buildTask(plugin, runnable)
                .delay(Duration.ofMillis(delay * 50))
                .schedule());
    }

    public ScheduledTask runTaskAsynchronously(Runnable runnable) {
        return addTask(plugin.getProxy().getScheduler()
                .buildTask(plugin, runnable)
                .schedule());
    }

    public ScheduledTask runTaskAsynchronously(String key, AbstractTask abstractTask) {
        ScheduledTask task = runTaskAsynchronously(abstractTask);
        tasks.put(key, new RunningTask(task, abstractTask));
        return task;
    }

    private ScheduledTask addTask(ScheduledTask task) {
        serverTasks.add(task);
        return task;
    }

    public void cancelTask(ScheduledTask task) {
        task.cancel();
    }

    public boolean cancelTask(String key) {
        RunningTask task = tasks.remove(key);
        if (task == null) {
            return false;
        }

        task.cancel();
        return true;
    }

    public void cancelAllTasks() {
        for (RunningTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();

        for (ScheduledTask task : serverTasks) {
            task.cancel();
        }
        serverTasks.clear();
    }

    private final class RunningTask {

        private final ScheduledTask task;
        private final AbstractTask abstractTask;

        private RunningTask(ScheduledTask task, AbstractTask abstractTask) {
            this.task = task;
            this.abstractTask = abstractTask;
        }

        public void cancel() {
            cancelTask(task);
            abstractTask.cancel();
        }
    }
}
