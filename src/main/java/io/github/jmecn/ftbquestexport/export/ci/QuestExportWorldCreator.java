package io.github.jmecn.ftbquestexport.export.ci;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Creates or reopens the void world used by {@link QuestExportCiDriver}. */
public final class QuestExportWorldCreator {


    private QuestExportWorldCreator() {}

    public static String saveName() {
        return QuestExportCiProperties.exportWorldName();
    }

    public static boolean saveExists(Minecraft mc) {
        try {
            return mc.getLevelSource().levelExists(saveName());
        } catch (Exception e) {
            FtbQuestExportMod.LOGGER.warn("levelExists({}) threw; assuming missing", saveName(), e);
            return false;
        }
    }

    public static void openExisting(Minecraft mc) {
        FtbQuestExportMod.LOGGER.info("opening existing world '{}'", saveName());
        mc.createWorldOpenFlows().loadLevel(mc.screen, saveName());
    }

    public static void createAndLoad(Minecraft mc) {
        FtbQuestExportMod.LOGGER.info("creating fresh void creative world '{}'", saveName());

        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DOFIRETICK).set(false, null);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(false, null);
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);

        LevelSettings settings = new LevelSettings(
                saveName(),
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                rules,
                WorldDataConfiguration.DEFAULT);

        mc.createWorldOpenFlows().createFreshLevel(
                saveName(),
                settings,
                new WorldOptions(0L, false, false),
                QuestExportWorldCreator::buildVoidDimensions);
    }

    private static WorldDimensions buildVoidDimensions(RegistryAccess registries) {
        FlatLevelGeneratorSettings voidSettings = registries
                .registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                .getHolderOrThrow(FlatLevelGeneratorPresets.THE_VOID)
                .value()
                .settings();
        ChunkGenerator voidGen = new FlatLevelSource(voidSettings);
        WorldPreset normal = registries
                .registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.NORMAL)
                .value();
        return normal.createWorldDimensions().replaceOverworldGenerator(registries, voidGen);
    }
}
