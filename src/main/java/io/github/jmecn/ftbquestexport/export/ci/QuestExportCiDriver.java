package io.github.jmecn.ftbquestexport.export.ci;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import io.github.jmecn.ftbquestexport.export.QuestExportPipeline;
import io.github.jmecn.ftbquestexport.export.QuestExportPaths;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;

/** Client tick state machine: menu → void world → warmup → quest export → CI hard exit. */
public final class QuestExportCiDriver {

    private QuestExportCiDriver() {}

    public static void register() {
        FtbQuestExportMod.LOGGER.info(
                "mode=runExportAndExit, world={}, warmupTicks={}, timeoutSeconds={}",
                QuestExportCiProperties.exportWorldName(),
                QuestExportCiProperties.exportWarmupTicks(),
                QuestExportCiProperties.exportTimeoutSeconds());
        MinecraftForge.EVENT_BUS.register(new Handler());
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

    private static final class Handler {

        private enum Phase { ARMED, WORLD_OPENING, WARMUP, DONE }

        private Phase phase = Phase.ARMED;
        private boolean worldRequestSent;
        private int worldDelayTicks;
        private int warmupTicks;
        private long startNanos;

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) {
                return;
            }
            if (!QuestExportCiProperties.runExportAndExit()) {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (phase == Phase.ARMED) {
                startNanos = System.nanoTime();
                phase = Phase.WORLD_OPENING;
                FtbQuestExportMod.LOGGER.info("runExportAndExit: armed, waiting for idle menu...");
            }

            if (isFatalMenuScreen(client)) {
                phase = Phase.DONE;
                FtbQuestExportMod.LOGGER.error(
                        "fatal menu screen ({}); aborting export",
                        client.screen.getClass().getName());
                Runtime.getRuntime().halt(1);
                return;
            }
            if (QuestExportCiProperties.timedOut(startNanos)) {
                phase = Phase.DONE;
                FtbQuestExportMod.LOGGER.error("export timed out after {}s (phase={})",
                        QuestExportCiProperties.exportTimeoutSeconds(), phase);
                Runtime.getRuntime().halt(1);
                return;
            }

            switch (phase) {
                case WORLD_OPENING -> tickWorldOpening(client);
                case WARMUP -> tickWarmup(client);
                default -> {}
            }
        }

        private void tickWorldOpening(Minecraft client) {
            if (client.player != null && client.level != null) {
                phase = Phase.WARMUP;
                warmupTicks = 0;
                FtbQuestExportMod.LOGGER.info("player + level present, warming up {} ticks",
                        QuestExportCiProperties.exportWarmupTicks());
                return;
            }

            if (!worldRequestSent) {
                if (!isIdleMenuReady(client)) {
                    return;
                }
                boolean reuse = QuestExportWorldCreator.saveExists(client);
                int delay = reuse ? 0 : QuestExportCiProperties.exportWorldDelayTicks();
                if (delay > 0 && worldDelayTicks < delay) {
                    worldDelayTicks++;
                    return;
                }
                worldRequestSent = true;
                if (reuse) {
                    QuestExportWorldCreator.openExisting(client);
                } else {
                    QuestExportWorldCreator.createAndLoad(client);
                }
            }
        }

        private void tickWarmup(Minecraft client) {
            if (client.player == null || client.level == null) {
                phase = Phase.WORLD_OPENING;
                worldRequestSent = true;
                return;
            }

            if (!isQuestFileReady()) {
                warmupTicks = 0;
                return;
            }

            if (warmupTicks < QuestExportCiProperties.exportWarmupTicks()) {
                warmupTicks++;
                return;
            }

            phase = Phase.DONE;
            Path questDir = QuestExportPaths.questDirectory(client.gameDirectory.toPath());
            FtbQuestExportMod.LOGGER.info("running quest export to {} ...", questDir.toAbsolutePath());
            try {
                QuestExportPipeline.run(questDir);
                FtbQuestExportMod.LOGGER.info("quest export finished, halting JVM");
                // halt: export is on disk; avoid EMI/TMRV background threads blocking System.exit shutdown.
                Runtime.getRuntime().halt(0);
            } catch (Exception e) {
                FtbQuestExportMod.LOGGER.error("quest export failed for {}", questDir.toAbsolutePath(), e);
                Runtime.getRuntime().halt(1);
            }
        }
    }
}
