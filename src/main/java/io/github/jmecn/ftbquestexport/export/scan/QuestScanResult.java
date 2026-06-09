package io.github.jmecn.ftbquestexport.export.scan;

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

    /** FTB {@code QuestShape} PNG layers for web quest nodes ({@code background} + {@code outline}). */
    public void addQuestShapeTextures(String shape) {
        String shapeId = normalizeQuestShapeId(shape);
        if (shapeId == null || "none".equals(shapeId)) {
            return;
        }
        String base = "ftbquests:textures/shapes/" + shapeId + "/";
        addTexture(base + "background.png");
        addTexture(base + "outline.png");
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

    public void collectLangFromText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        int start = 0;
        while (true) {
            int open = text.indexOf('{', start);
            if (open < 0) {
                break;
            }
            int close = text.indexOf('}', open + 1);
            if (close < 0) {
                break;
            }
            String key = text.substring(open + 1, close);
            if (!key.isEmpty() && !key.startsWith("@") && key.contains(".")) {
                langKeys.add(key);
            }
            start = close + 1;
        }
    }

    public void putExpandedFilter(String key, java.util.List<String> itemIds) {
        if (key != null && itemIds != null && !itemIds.isEmpty()) {
            expandedFilters.put(key, itemIds);
            itemIds.forEach(this::addItem);
        }
    }

    public Map<String, Object> toStatsMap() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("chapterCount", chapterCount);
        stats.put("questCount", questCount);
        stats.put("taskCount", taskCount);
        stats.put("tasksByType", new LinkedHashMap<>(tasksByType));
        stats.put("itemRefs", items.size());
        stats.put("tagRefs", tags.size());
        stats.put("fluidRefs", fluids.size());
        stats.put("blockRefs", blocks.size());
        stats.put("entityRefs", entities.size());
        stats.put("textureRefs", textures.size());
        stats.put("langKeys", langKeys.size());
        stats.put("expandedFilters", expandedFilters.size());
        return stats;
    }

    public Map<String, Object> toRefsMap() {
        Map<String, Object> refs = new LinkedHashMap<>();
        refs.put("items", new java.util.ArrayList<>(items));
        refs.put("tags", new java.util.ArrayList<>(tags));
        refs.put("fluids", new java.util.ArrayList<>(fluids));
        refs.put("blocks", new java.util.ArrayList<>(blocks));
        refs.put("entities", new java.util.ArrayList<>(entities));
        refs.put("textures", new java.util.ArrayList<>(textures));
        refs.put("langKeys", new java.util.ArrayList<>(langKeys));
        return refs;
    }

    private static String stripItem(String id) {
        int bracket = id.indexOf('[');
        return bracket > 0 ? id.substring(0, bracket) : id;
    }
}
