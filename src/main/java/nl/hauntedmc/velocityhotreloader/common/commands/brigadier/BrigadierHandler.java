package nl.hauntedmc.velocityhotreloader.common.commands.brigadier;

import nl.hauntedmc.velocityhotreloader.common.commands.arguments.JarFilesParser;
import nl.hauntedmc.velocityhotreloader.common.commands.arguments.PluginsParser;
import nl.hauntedmc.velocityhotreloader.velocity.entities.VelocityAudience;

import org.incendo.cloud.brigadier.CloudBrigadierManager;

import com.mojang.brigadier.arguments.StringArgumentType;

import io.leangen.geantyref.TypeToken;

public class BrigadierHandler {

    private final CloudBrigadierManager<VelocityAudience, ?> brigadierManager;

    public BrigadierHandler(CloudBrigadierManager<VelocityAudience, ?> brigadierManager) {
        this.brigadierManager = brigadierManager;
    }

    /**
     * Registers types with the cloud brigadier manager.
     */
    public void registerTypes() {
        brigadierManager.registerMapping(
                new TypeToken<JarFilesParser>() {},
                builder -> builder
                        .cloudSuggestions()
                        .toConstant(StringArgumentType.greedyString())
        );
        brigadierManager.registerMapping(
                new TypeToken<PluginsParser>() {},
                builder -> builder
                        .cloudSuggestions()
                        .toConstant(StringArgumentType.greedyString())
        );
    }
}
