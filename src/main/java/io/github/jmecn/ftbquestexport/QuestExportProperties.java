package io.github.jmecn.ftbquestexport;

/** In-game and CI export toggles ({@code -Dquest.export.*}). */
public final class QuestExportProperties {

    private QuestExportProperties() {}

    public static boolean runExportAndExit() {
        return Boolean.getBoolean(QuestExportConstants.RUN_EXPORT_AND_EXIT);
    }

    public static int exportWarmupTicks() {
        return Math.max(0, Integer.getInteger(QuestExportConstants.EXPORT_WARMUP_TICKS, QuestExportConstants.DEFAULT_WARMUP_TICKS));
    }

    public static int exportWorldDelayTicks() {
        return Math.max(0, Integer.getInteger(QuestExportConstants.EXPORT_WORLD_DELAY_TICKS, QuestExportConstants.DEFAULT_WORLD_DELAY_TICKS));
    }

    public static int exportTimeoutSeconds() {
        return Integer.getInteger(QuestExportConstants.EXPORT_TIMEOUT_SECONDS, QuestExportConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    public static String exportWorldName() {
        String raw = System.getProperty(QuestExportConstants.EXPORT_WORLD_NAME, QuestExportConstants.DEFAULT_WORLD_NAME).trim();
        return raw.isEmpty() ? QuestExportConstants.DEFAULT_WORLD_NAME : raw;
    }

    public static boolean timedOut(long startNanos) {
        int sec = exportTimeoutSeconds();
        if (sec <= 0) {
            return false;
        }
        return (System.nanoTime() - startNanos) >= sec * 1_000_000_000L;
    }
}
