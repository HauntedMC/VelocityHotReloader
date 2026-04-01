package nl.hauntedmc.velocityhotreloader.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import java.time.Duration;
import nl.hauntedmc.velocityhotreloader.VelocityHotReloaded;
import nl.hauntedmc.velocityhotreloader.entities.AbstractTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VelocityTaskManagerTest {

    private Scheduler.TaskBuilder taskBuilder;
    private ScheduledTask scheduledTask;
    private VelocityTaskManager manager;

    @BeforeEach
    void setUp() {
        VelocityHotReloaded plugin = mock(VelocityHotReloaded.class);
        ProxyServer proxy = mock(ProxyServer.class);
        Scheduler scheduler = mock(Scheduler.class);
        taskBuilder = mock(Scheduler.TaskBuilder.class);
        scheduledTask = mock(ScheduledTask.class);

        when(plugin.getProxy()).thenReturn(proxy);
        when(proxy.getScheduler()).thenReturn(scheduler);
        when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenReturn(taskBuilder);
        when(taskBuilder.delay(any(Duration.class))).thenReturn(taskBuilder);
        when(taskBuilder.schedule()).thenReturn(scheduledTask);

        manager = new VelocityTaskManager(plugin);
    }

    @Test
    void runTaskAsynchronouslyShouldScheduleTask() {
        ScheduledTask task = manager.runTaskAsynchronously(() -> {});
        assertSame(scheduledTask, task);
        verify(taskBuilder).schedule();
    }

    @Test
    void runTaskLaterShouldApplyTickDelay() {
        manager.runTaskLater(() -> {}, 20L);
        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        verify(taskBuilder).delay(captor.capture());
        assertEquals(Duration.ofMillis(1000), captor.getValue());
    }

    @Test
    void cancelTaskByKeyShouldCancelBothScheduledAndAbstractTask() {
        RecordingTask task = new RecordingTask();
        manager.runTaskAsynchronously("watch-key", task);

        boolean cancelled = manager.cancelTask("watch-key");

        assertTrue(cancelled);
        assertTrue(task.cancelled);
        verify(scheduledTask).cancel();
    }

    @Test
    void cancelTaskByUnknownKeyShouldReturnFalse() {
        assertFalse(manager.cancelTask("unknown"));
    }

    @Test
    void cancelAllTasksShouldCancelEverything() {
        manager.runTaskAsynchronously(() -> {});
        manager.runTaskAsynchronously("watch", new RecordingTask());

        manager.cancelAllTasks();

        verify(scheduledTask, atLeastOnce()).cancel();
    }

    private static final class RecordingTask extends AbstractTask {

        private boolean cancelled;

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {
            // no-op
        }
    }
}
