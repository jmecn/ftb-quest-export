package io.github.jmecn.ftbquestexport.export.write;

import io.github.jmecn.ftbquestexport.export.QuestExportJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Writes {@code quests/index.json} and {@code quests/chapters/*.json}. */
public final class QuestJsonWriter {

    private QuestJsonWriter() {}

    public static void write(Path outputDir, Map<String, Object> index, Map<String, Map<String, Object>> chapters)
            throws IOException {
        Path questsRoot = outputDir.resolve("quests");
        Files.createDirectories(questsRoot.resolve("chapters"));
        Files.writeString(questsRoot.resolve("index.json"), QuestExportJson.PRETTY.toJson(index));
        for (Map.Entry<String, Map<String, Object>> entry : chapters.entrySet()) {
            Path chapterFile = questsRoot.resolve("chapters").resolve(entry.getKey() + ".json");
            Files.writeString(chapterFile, QuestExportJson.PRETTY.toJson(entry.getValue()));
        }
    }
}
