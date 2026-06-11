package io.github.jmecn.ftbquestexport.scan;

import io.github.jmecn.ftbquestexport.pojo.QuestRefsSnapshot;
import io.github.jmecn.ftbquestexport.pojo.QuestScanStats;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Aggregated refs from {@link QuestFileScanner}. */
public final class QuestScanResult {

    private int chapterCount;
    private int questCount;
    private int taskCount;

    private final Map<String, Integer> tasksByType = new TreeMap<>();
    private final Set<String> items = new TreeSet<>();
    private final Set<String> tags = new TreeSet<>();
    private final Set<String> fluids = new TreeSet<>();
    private final Set<String> blocks = new TreeSet<>();
    private final Set<String> entities = new TreeSet<>();
    private final Set<String> textures = new TreeSet<>();
    private final Set<String> questShapeIds = new TreeSet<>();
    private final Set<String> langKeys = new TreeSet<>();
    private final Map<String, java.util.List<String>> expandedFilters = new TreeMap<>();

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    public int getQuestCount() {
        return questCount;
    }

    public void incrementQuestCount() {
        questCount++;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void incrementTaskCount(String type) {
        taskCount++;
        tasksByType.merge(type, 1, Integer::sum);
    }

    public Map<String, Integer> getTasksByType() {
        return tasksByType;
    }

    public Set<String> getItems() {
        return items;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<String> getFluids() {
        return fluids;
    }

    public Set<String> getBlocks() {
        return blocks;
    }

    public Set<String> getEntities() {
        return entities;
    }

    public Set<String> getTextures() {
        return textures;
    }

    public Set<String> getQuestShapeIds() {
        return questShapeIds;
    }

    public Set<String> getLangKeys() {
        return langKeys;
    }

    public Map<String, java.util.List<String>> getExpandedFilters() {
        return expandedFilters;
    }

    public void addItem(String id) {
        if (id != null && !id.isBlank() && id.contains(":")) {
            items.add(stripItem(id));
        }
    }

    public void addTag(String id) {
        if (id != null && !id.isBlank()) {
            tags.add(id.startsWith("#") ? id.substring(1) : id);
        }
    }

    public void addFluid(String id) {
        if (id != null && !id.isBlank() && id.contains(":")) {
            fluids.add(id);
        }
    }

    public void addBlock(String id) {
        if (id != null && !id.isBlank() && id.contains(":")) {
            blocks.add(id);
        }
    }

    public void addEntity(String id) {
        if (id != null && !id.isBlank() && id.contains(":")) {
            entities.add(id);
        }
    }

    public void addTexture(String ref) {
        if (ref != null && !ref.isBlank()) {
            textures.add(ref.trim());
        }
    }

    public void addQuestShapeTextures(String shape) {
        String shapeId = normalizeQuestShapeId(shape);
        if ("none".equals(shapeId)) {
            return;
        }
        questShapeIds.add(shapeId);
    }

    private static String normalizeQuestShapeId(String shape) {
        if (shape == null || shape.isBlank() || "default".equals(shape)) {
            return "circle";
        }
        return shape.trim();
    }

    public void collectLangFromLines(java.util.List<String> lines) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            collectLangFromText(line);
        }
    }

    public void addLangKey(String key) {
        if (key != null && !key.isBlank() && key.contains(".")) {
            langKeys.add(key);
        }
    }

    public void collectLangFromText(String text) {
        QuestRichTextScan.collectFromText(this, text);
    }

    public void putExpandedFilter(String key, java.util.List<String> itemIds) {
        if (key != null && itemIds != null && !itemIds.isEmpty()) {
            expandedFilters.put(key, itemIds);
            itemIds.forEach(this::addItem);
        }
    }

    public QuestScanStats toStats() {
        return new QuestScanStats(
                chapterCount,
                questCount,
                taskCount,
                new LinkedHashMap<>(tasksByType),
                items.size(),
                tags.size(),
                fluids.size(),
                blocks.size(),
                entities.size(),
                textures.size(),
                langKeys.size(),
                expandedFilters.size());
    }

    public QuestRefsSnapshot toRefs() {
        return new QuestRefsSnapshot(
                new java.util.ArrayList<>(items),
                new java.util.ArrayList<>(tags),
                new java.util.ArrayList<>(fluids),
                new java.util.ArrayList<>(blocks),
                new java.util.ArrayList<>(entities),
                new java.util.ArrayList<>(textures),
                new java.util.ArrayList<>(langKeys));
    }

    private static String stripItem(String id) {
        int bracket = id.indexOf('[');
        return bracket > 0 ? id.substring(0, bracket) : id;
    }
}
