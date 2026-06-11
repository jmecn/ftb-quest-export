package io.github.jmecn.ftbquestexport.pojo;

import java.util.Map;

public record QuestScanStats(
        int chapterCount,
        int questCount,
        int taskCount,
        Map<String, Integer> tasksByType,
        int itemRefs,
        int tagRefs,
        int fluidRefs,
        int blockRefs,
        int entityRefs,
        int textureRefs,
        int langKeys,
        int expandedFilters) {}
