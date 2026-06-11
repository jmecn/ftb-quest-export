package io.github.jmecn.ftbquestexport.ci;

import io.github.jmecn.ftbquestexport.QuestExportPaths;
import io.github.jmecn.ftbquestexport.QuestExportPipeline;
import io.github.jmecn.ftbquestexport.QuestExportProperties;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;

/** Client tick state machine for headless CI export. */
final class QuestExportTickHandler {

    private CiExportPhase phase = CiExportPhase.ARMED;
    private boolean worldRequestSent;
    private int worldDelayTicks;
    private int warmupTicks;
    private long startNanos;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == CiExportPhase.DONE) {
            return;
        }
        if (!QuestExportProperties.runExportAndExit()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (phase == CiExportPhase.ARMED) {
            startNanos = System.nanoTime();
            phase = CiExportPhase.WORLD_OPENING;
            FtbQuestExportMod.LOGGER.info("runExportAndExit: armed, waiting for idle menu...");
        }

        if (QuestExportCiDriver.isFatalMenuScreen(client)) {
            phase = CiExportPhase.DONE;
            FtbQuestExportMod.LOGGER.error(
                    "fatal menu screen ({}); aborting export",
                    client.screen.getClass().getName());
            Runtime.getRuntime().halt(1);
            return;
        }
        if (QuestExportProperties.timedOut(startNanos)) {
            phase = CiExportPhase.DONE;
            FtbQuestExportMod.LOGGER.error(
                    "export timed out after {}s (phase={})",
                    QuestExportProperties.exportTimeoutSeconds(),
                    phase);
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
            phase = CiExportPhase.WARMUP;
            warmupTicks = 0;
            FtbQuestExportMod.LOGGER.info(
                    "player + level present, warming up {} ticks",
                    QuestExportProperties.exportWarmupTicks());
            return;
        }

        if (!worldRequestSent) {
            if (!QuestExportCiDriver.isIdleMenuReady(client)) {
                return;
            }
            boolean reuse = QuestExportWorldCreator.saveExists(client);
            int delay = reuse ? 0 : QuestExportProperties.exportWorldDelayTicks();
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
            phase = CiExportPhase.WORLD_OPENING;
            worldRequestSent = true;
            return;
        }

        if (!QuestExportCiDriver.isQuestFileReady()) {
            warmupTicks = 0;
            return;
        }

        if (warmupTicks < QuestExportProperties.exportWarmupTicks()) {
            warmupTicks++;
            return;
        }

        phase = CiExportPhase.DONE;
        Path questDir = QuestExportPaths.questDirectory(client.gameDirectory.toPath());
        FtbQuestExportMod.LOGGER.info("running quest export to {} ...", questDir.toAbsolutePath());
        try {
            QuestExportPipeline.run(questDir);
            FtbQuestExportMod.LOGGER.info("quest export finished, halting JVM");
            Runtime.getRuntime().halt(0);
        } catch (Exception e) {
            FtbQuestExportMod.LOGGER.error("quest export failed for {}", questDir.toAbsolutePath(), e);
            Runtime.getRuntime().halt(1);
        }
    }
}
