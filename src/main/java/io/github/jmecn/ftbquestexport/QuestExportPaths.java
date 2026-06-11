package io.github.jmecn.ftbquestexport;

import java.nio.file.Path;

public final class QuestExportPaths {

    private QuestExportPaths() {}

    public static Path resolveExportRoot(Path gameDirectory) {
        String out = System.getProperty(QuestExportConstants.EXPORT_ROOT_PROPERTY);
        if (out != null && !out.isBlank()) {
            return Path.of(out.trim());
        }
        String folder = System.getProperty(QuestExportConstants.EXPORT_FOLDER_PROPERTY);
        if (folder != null && !folder.isBlank()) {
            Path quest = Path.of(folder.trim());
            Path parent = quest.getParent();
            if (parent != null) {
                return parent;
            }
            return quest;
        }
        return gameDirectory.resolve("export");
    }

    public static Path questDirectory(Path gameDirectory) {
        String folder = System.getProperty(QuestExportConstants.EXPORT_FOLDER_PROPERTY);
        if (folder != null && !folder.isBlank()) {
            return Path.of(folder.trim());
        }
        String out = System.getProperty(QuestExportConstants.EXPORT_ROOT_PROPERTY);
        if (out != null && !out.isBlank()) {
            Path p = Path.of(out.trim());
            if (p.getFileName() != null && QuestExportConstants.QUEST_SUBDIR.equals(p.getFileName().toString())) {
                return p;
            }
            return p.resolve(QuestExportConstants.QUEST_SUBDIR);
        }
        return resolveExportRoot(gameDirectory).resolve(QuestExportConstants.QUEST_SUBDIR);
    }

    public static Path questDirectoryFromExportRoot(Path exportRoot) {
        String folder = System.getProperty(QuestExportConstants.EXPORT_FOLDER_PROPERTY);
        if (folder != null && !folder.isBlank()) {
            return Path.of(folder.trim());
        }
        if (exportRoot.getFileName() != null
                && QuestExportConstants.QUEST_SUBDIR.equals(exportRoot.getFileName().toString())) {
            return exportRoot;
        }
        return exportRoot.resolve(QuestExportConstants.QUEST_SUBDIR);
    }
}
