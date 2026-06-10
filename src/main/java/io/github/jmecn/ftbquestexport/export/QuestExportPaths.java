package io.github.jmecn.ftbquestexport.export;

import java.nio.file.Path;

public final class QuestExportPaths {

    public static final String QUEST_SUBDIR = "quest-export";
    public static final String EXPORT_ROOT_PROPERTY = "quest.export.outputDir";
    public static final String EXPORT_FOLDER_PROPERTY = "quest.exportFolder";

    private QuestExportPaths() {}

    public static Path resolveExportRoot(Path gameDirectory) {
        String out = System.getProperty(EXPORT_ROOT_PROPERTY);
        if (out != null && !out.isBlank()) {
            return Path.of(out.trim());
        }
        String folder = System.getProperty(EXPORT_FOLDER_PROPERTY);
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
        String folder = System.getProperty(EXPORT_FOLDER_PROPERTY);
        if (folder != null && !folder.isBlank()) {
            return Path.of(folder.trim());
        }
        String out = System.getProperty(EXPORT_ROOT_PROPERTY);
        if (out != null && !out.isBlank()) {
            Path p = Path.of(out.trim());
            if (p.getFileName() != null && QUEST_SUBDIR.equals(p.getFileName().toString())) {
                return p;
            }
            return p.resolve(QUEST_SUBDIR);
        }
        return resolveExportRoot(gameDirectory).resolve(QUEST_SUBDIR);
    }

    public static Path questDirectoryFromExportRoot(Path exportRoot) {
        String folder = System.getProperty(EXPORT_FOLDER_PROPERTY);
        if (folder != null && !folder.isBlank()) {
            return Path.of(folder.trim());
        }
        if (exportRoot.getFileName() != null && QUEST_SUBDIR.equals(exportRoot.getFileName().toString())) {
            return exportRoot;
        }
        return exportRoot.resolve(QUEST_SUBDIR);
    }
}
