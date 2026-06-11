package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record QuestIndex(
        String title,
        int version,
        String progressionMode,
        double gridScale,
        String defaultQuestShape,
        List<ChapterGroup> chapterGroups,
        List<ChapterSummary> chapters) {
}
