package io.github.jmecn.ftbquestexport.pojo;

import java.util.List;

public record QuestSearchIndexExportResult(int localesWritten, int questCount, List<String> locales) {
    public static final QuestSearchIndexExportResult EMPTY = new QuestSearchIndexExportResult(0, 0, List.of());
}
