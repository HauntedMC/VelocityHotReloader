package acceptance;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

/** Minimal dynamically-loaded Velocity plugin used only by the platform acceptance suite. */
public final class AcceptancePlugin {

    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public AcceptancePlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        logger.info("VHR_ACCEPTANCE_PLUGIN_ENABLED id={} marker={}", id(), marker());
        if (commandEnabled()) {
            CommandManager commandManager = proxy.getCommandManager();
            CommandMeta meta = commandManager.metaBuilder("vhracceptance").plugin(this).build();
            commandManager.register(meta, new SampleCommand());
        }
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        logger.info("VHR_ACCEPTANCE_PLUGIN_DISABLED id={}", id());
    }

    private String id() {
        return resource("plugin-id.txt");
    }

    private String marker() {
        return resource("marker.txt");
    }

    private boolean commandEnabled() {
        return Boolean.parseBoolean(resource("command-enabled.txt"));
    }

    private static String resource(String name) {
        try (InputStream stream = AcceptancePlugin.class.getResourceAsStream("/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing acceptance resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read acceptance resource " + name, exception);
        }
    }

    private final class SampleCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            logger.info("VHR_ACCEPTANCE_SAMPLE_COMMAND id={} marker={}", id(), marker());
        }
    }
}
