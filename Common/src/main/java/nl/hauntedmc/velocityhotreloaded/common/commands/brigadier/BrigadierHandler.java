package nl.hauntedmc.velocityhotreloaded.common.commands.brigadier;

import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.JarFilesParser;
import nl.hauntedmc.velocityhotreloaded.common.commands.arguments.PluginsParser;

import org.incendo.cloud.brigadier.CloudBrigadierManager;

import com.mojang.brigadier.arguments.StringArgumentType;

import io.leangen.geantyref.TypeToken;

import nl.hauntedmc.velocityhotreloaded.common.entities.VHRAudience;

public class BrigadierHandler<C extends VHRAudience<?>, P> {

    private final CloudBrigadierManager<C, ?> brigadierManager;

    public BrigadierHandler(CloudBrigadierManager<C, ?> brigadierManager) {
        this.brigadierManager = brigadierManager;
    }

    /**
     * Registers types with the cloud brigadier manager.
     */
    public void registerTypes() {
        brigadierManager.registerMapping(
                new TypeToken<JarFilesParser<C>>() {},
                builder -> builder
                        .cloudSuggestions()
                        .toConstant(StringArgumentType.greedyString())
        );
        brigadierManager.registerMapping(
                new TypeToken<PluginsParser<C, P>>() {},
                builder -> builder
                        .cloudSuggestions()
                        .toConstant(StringArgumentType.greedyString())
        );
    }
}
