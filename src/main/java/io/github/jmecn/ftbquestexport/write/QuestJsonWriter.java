package io.github.jmecn.ftbquestexport.write;

import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.model.QuestIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class QuestJsonWriter {

    private QuestJsonWriter() {}

    public static void write(Path outputDir, QuestIndex index, Map<String, ChapterData> chapters)
            throws IOException {
        Path questsRoot = outputDir.resolve("quests");
        Files.createDirectories(questsRoot.resolve("chapters"));
        Files.writeString(questsRoot.resolve("index.json"), QuestExportJson.PRETTY.toJson(index));
        writeChapters(outputDir, chapters);
    }

    public static void writeChapters(Path outputDir, Map<String, ChapterData> chapters) throws IOException {
        Path chaptersRoot = outputDir.resolve("quests/chapters");
        Files.createDirectories(chaptersRoot);
        for (Map.Entry<String, ChapterData> entry : chapters.entrySet()) {
            Path chapterFile = chaptersRoot.resolve(entry.getKey() + ".json");
            Files.writeString(chapterFile, QuestExportJson.PRETTY.toJson(entry.getValue()));
        }
    }
}
