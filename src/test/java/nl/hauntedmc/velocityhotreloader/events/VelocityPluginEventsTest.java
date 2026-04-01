package nl.hauntedmc.velocityhotreloader.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.velocitypowered.api.plugin.PluginContainer;
import org.junit.jupiter.api.Test;

class VelocityPluginEventsTest {

    @Test
    void eventsShouldKeepPluginAndStage() {
        PluginContainer plugin = mock(PluginContainer.class);

        VelocityPluginEvent load = new VelocityPluginLoadEvent(plugin, VelocityPluginEvent.Stage.PRE);
        VelocityPluginEvent unload = new VelocityPluginUnloadEvent(plugin, VelocityPluginEvent.Stage.POST);
        VelocityPluginEvent enable = new VelocityPluginEnableEvent(plugin, VelocityPluginEvent.Stage.PRE);
        VelocityPluginEvent disable = new VelocityPluginDisableEvent(plugin, VelocityPluginEvent.Stage.POST);

        assertInstanceOf(VelocityPluginLoadEvent.class, load);
        assertInstanceOf(VelocityPluginUnloadEvent.class, unload);
        assertInstanceOf(VelocityPluginEnableEvent.class, enable);
        assertInstanceOf(VelocityPluginDisableEvent.class, disable);
        assertSame(plugin, load.getPlugin());
        assertSame(plugin, unload.getPlugin());
        assertEquals(VelocityPluginEvent.Stage.PRE, load.getStage());
        assertEquals(VelocityPluginEvent.Stage.POST, unload.getStage());
    }
}
