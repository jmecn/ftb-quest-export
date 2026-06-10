package io.github.jmecn.ftbquestexport.export.ci;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import io.github.jmecn.ftbquestexport.export.QuestExportProperties;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraftforge.common.MinecraftForge;

/** Registers the CI export tick handler ({@code quest.export.runAndExit}). */
public final class QuestExportCiDriver {

    private QuestExportCiDriver() {}

    public static void register() {
        FtbQuestExportMod.LOGGER.info(
                "mode=runExportAndExit, world={}, warmupTicks={}, timeoutSeconds={}",
                QuestExportProperties.exportWorldName(),
                QuestExportProperties.exportWarmupTicks(),
                QuestExportProperties.exportTimeoutSeconds());
        MinecraftForge.EVENT_BUS.register(new QuestExportTickHandler());
    }

    static boolean isFatalMenuScreen(Minecraft client) {
        if (client.screen == null) {
            return false;
        }
        String simple = client.screen.getClass().getSimpleName();
        return switch (simple) {
            case "LoadingErrorScreen", "ErrorScreen", "KubeJSErrorScreen", "DisconnectedScreen" -> true;
            default -> false;
        };
    }

    static boolean isIdleMenuReady(Minecraft client) {
        if (client.getOverlay() instanceof LoadingOverlay) {
            return false;
        }
        if (client.screen == null || client.level != null || client.player != null) {
            return false;
        }
        return !isFatalMenuScreen(client);
    }

    static boolean isQuestFileReady() {
        try {
            var file = FTBQuestsAPI.api().getQuestFile(false);
            return file != null && !file.getAllChapters().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
