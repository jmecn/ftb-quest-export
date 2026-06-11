package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record QuestIndex(
        String title,
        int version,
        String progressionMode,
        double gridScale,
        String defaultQuestShape,
        List<ChapterGroup> chapterGroups,
        List<ChapterSummary> chapters,
        ShapeAtlas shapeAtlas) {

    public QuestIndex withShapeAtlas(ShapeAtlas shapeAtlas) {
        return new QuestIndex(
                title,
                version,
                progressionMode,
                gridScale,
                defaultQuestShape,
                chapterGroups,
                chapters,
                shapeAtlas);
    }
}
