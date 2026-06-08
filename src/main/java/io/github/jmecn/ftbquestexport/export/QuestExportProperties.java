package io.github.jmecn.ftbquestexport.export;

/** System properties for quest export ({@code -Dquest.export.*}). */
public final class QuestExportProperties {

    public static final String RUN_EXPORT_AND_EXIT = "quest.export.runAndExit";

    private QuestExportProperties() {}

    public static boolean runExportAndExit() {
        return Boolean.getBoolean(RUN_EXPORT_AND_EXIT);
    }
}
