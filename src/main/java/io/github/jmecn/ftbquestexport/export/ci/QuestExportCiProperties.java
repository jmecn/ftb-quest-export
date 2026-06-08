package io.github.jmecn.ftbquestexport.export.ci;

/** System properties for headless CI export ({@code -Dquest.export.*}). */
public final class QuestExportCiProperties {

    public static final String RUN_EXPORT_AND_EXIT = "quest.export.runAndExit";
    public static final String EXPORT_TIMEOUT_SECONDS = "quest.exportTimeoutSeconds";
    public static final String EXPORT_WORLD_NAME = "quest.exportWorldName";
    public static final String EXPORT_WORLD_DELAY_TICKS = "quest.exportWorldDelayTicks";
    public static final String EXPORT_WARMUP_TICKS = "quest.exportWarmupTicks";

    private static final int DEFAULT_WARMUP = 100;
    private static final int DEFAULT_WORLD_DELAY = 600;
    private static final int DEFAULT_TIMEOUT = 7200;
    private static final String DEFAULT_WORLD = "quest-export";

    private QuestExportCiProperties() {}

    public static boolean runExportAndExit() {
        return Boolean.getBoolean(RUN_EXPORT_AND_EXIT);
    }

    public static int exportWarmupTicks() {
        return Math.max(0, Integer.getInteger(EXPORT_WARMUP_TICKS, DEFAULT_WARMUP));
    }

    public static int exportWorldDelayTicks() {
        return Math.max(0, Integer.getInteger(EXPORT_WORLD_DELAY_TICKS, DEFAULT_WORLD_DELAY));
    }

    public static int exportTimeoutSeconds() {
        return Integer.getInteger(EXPORT_TIMEOUT_SECONDS, DEFAULT_TIMEOUT);
    }

    public static String exportWorldName() {
        String raw = System.getProperty(EXPORT_WORLD_NAME, DEFAULT_WORLD).trim();
        return raw.isEmpty() ? DEFAULT_WORLD : raw;
    }

    public static boolean timedOut(long startNanos) {
        int sec = exportTimeoutSeconds();
        if (sec <= 0) {
            return false;
        }
        return (System.nanoTime() - startNanos) >= sec * 1_000_000_000L;
    }
}
