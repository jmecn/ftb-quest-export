package io.github.jmecn.ftbquestexport.export.scan;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.StageTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads FTB Quests runtime graph into scan result + JSON-friendly maps. */
public final class QuestFileScanner {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

    public record ScanBundle(
            QuestScanResult scan,
            Map<String, Object> index,
            Map<String, Map<String, Object>> chapters) {}

    private QuestFileScanner() {}

    public static ScanBundle scan() {
        BaseQuestFile file = FTBQuestsAPI.api().getQuestFile(false);
        QuestScanResult scan = new QuestScanResult();
        Map<String, Object> index = new LinkedHashMap<>();
        Map<String, Map<String, Object>> chapters = new LinkedHashMap<>();

        index.put("title", file.getRawTitle());
        index.put("version", BaseQuestFile.VERSION);
        index.put("progressionMode", file.getProgressionMode().getId());
        index.put("gridScale", file.getGridScale());
        index.put("defaultQuestShape", file.getDefaultQuestShape());
        scan.collectLangFromText(file.getRawTitle());
        scan.addQuestShapeTextures("circle");
        scan.addQuestShapeTextures(file.getDefaultQuestShape());

        List<Map<String, Object>> groups = new ArrayList<>();
        for (ChapterGroup group : file.getChapterGroups()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", QuestObjectBase.getCodeString(group.id));
            g.put("title", group.getRawTitle());
            scan.collectLangFromText(group.getRawTitle());
            groups.add(g);
        }
        index.put("chapterGroups", groups);

        List<Map<String, Object>> chapterIndex = new ArrayList<>();
        List<Chapter> allChapters = file.getAllChapters();
        scan.setChapterCount(allChapters.size());

        for (Chapter chapter : allChapters) {
            String filename = chapter.getFilename();
            Map<String, Object> chapterJson = exportChapter(chapter, scan);
            chapters.put(filename, chapterJson);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", QuestObjectBase.getCodeString(chapter.id));
            summary.put("filename", filename);
            summary.put("group", chapter.hasGroup()
                    ? QuestObjectBase.getCodeString(chapter.getGroup().id)
                    : null);
            summary.put("orderIndex", chapter.getIndex());
            summary.put("icon", QuestDisplayExport.resolveIconRef(chapter.getIcon(), scan));
            String chapterTitle = chapter.getRawTitle();
            if (chapterTitle != null && !chapterTitle.isBlank()) {
                summary.put("title", chapterTitle);
            }
            chapterIndex.add(summary);
        }
        index.put("chapters", chapterIndex);

        LOGGER.info("[scan] {} chapters, {} quests, {} tasks",
                scan.getChapterCount(), scan.getQuestCount(), scan.getTaskCount());
        return new ScanBundle(scan, index, chapters);
    }

    private static Map<String, Object> exportChapter(Chapter chapter, QuestScanResult scan) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", QuestObjectBase.getCodeString(chapter.id));
        root.put("filename", chapter.getFilename());
        root.put("group", chapter.hasGroup()
                ? QuestObjectBase.getCodeString(chapter.getGroup().id)
                : null);
        root.put("icon", QuestDisplayExport.resolveIconRef(chapter.getIcon(), scan));
        root.put("defaultQuestShape", chapter.getDefaultQuestShape());
        root.put("orderIndex", chapter.getIndex());
        exportChapterDisplay(chapter, root, scan);
        scan.addQuestShapeTextures(chapter.getDefaultQuestShape());

        List<Map<String, Object>> quests = new ArrayList<>();
        for (Quest quest : chapter.getQuests()) {
            quests.add(exportQuest(quest, scan));
            scan.incrementQuestCount();
        }
        root.put("quests", quests);

        List<Map<String, Object>> links = new ArrayList<>();
        for (QuestLink link : chapter.getQuestLinks()) {
            Quest linked = link.getQuest().orElse(null);
            if (linked == null) {
                LOGGER.warn("[scan] skipping quest link {} — linked quest not found", link.id);
                continue;
            }
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("id", QuestObjectBase.getCodeString(link.id));
            l.put("linkedQuest", QuestObjectBase.getCodeString(linked.id));
            l.put("x", link.getX());
            l.put("y", link.getY());
            if (link.getShape() != null && !link.getShape().isEmpty()) {
                l.put("shape", link.getShape());
                scan.addQuestShapeTextures(link.getShape());
            }
            if (link.getWidth() != 1D) {
                l.put("size", link.getWidth());
            }
            links.add(l);
        }
        root.put("questLinks", links);

        List<Map<String, Object>> images = new ArrayList<>();
        for (ChapterImage image : chapter.getImages()) {
            Map<String, Object> img = new LinkedHashMap<>();
            String imageRef = QuestDisplayExport.resolveIconRef(image.getImage(), scan);
            img.put("image", imageRef != null ? imageRef : image.getImage().toString());
            img.put("x", image.getX());
            img.put("y", image.getY());
            img.put("width", image.getWidth());
            img.put("height", image.getHeight());
            img.put("rotation", image.getRotation());
            if (image.getClick() != null && !image.getClick().isEmpty()) {
                img.put("click", image.getClick());
            }
            if (image.getOrder() > 0) {
                img.put("order", image.getOrder());
            }
            images.add(img);
        }
        root.put("images", images);
        return root;
    }

    private static void exportChapterDisplay(Chapter chapter, Map<String, Object> root, QuestScanResult scan) {
        String rawTitle = chapter.getRawTitle();
        if (rawTitle != null && !rawTitle.isBlank()) {
            root.put("title", rawTitle);
            scan.collectLangFromText(rawTitle);
        }
        List<String> rawSubtitle = chapter.getRawSubtitle();
        if (!rawSubtitle.isEmpty()) {
            root.put("subtitle", rawSubtitle);
            scan.collectLangFromLines(rawSubtitle);
        }
    }

    private static Map<String, Object> exportQuest(Quest quest, QuestScanResult scan) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("id", QuestObjectBase.getCodeString(quest.id));
        exportQuestVisibilityFlags(quest, q);
        q.put("x", quest.getX());
        q.put("y", quest.getY());
        q.put("size", quest.getSize());
        if (quest.getShape() != null && !quest.getShape().isEmpty()) {
            q.put("shape", quest.getShape());
            scan.addQuestShapeTextures(quest.getShape());
        }
        QuestDisplayExport.applyQuestDisplay(quest, q, scan);
        q.put("subtitle", quest.getRawSubtitle());
        q.put("description", quest.getRawDescription());
        scan.collectLangFromText(quest.getRawSubtitle());
        scan.collectLangFromLines(quest.getRawDescription());

        List<String> deps = new ArrayList<>();
        quest.streamDependencies().forEach(dep -> deps.add(QuestObjectBase.getCodeString(dep.id)));
        if (!deps.isEmpty()) {
            q.put("dependencies", deps);
        }
        q.put("hideDependencyLines", quest.shouldHideDependentLines());

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Task task : quest.getTasks()) {
            tasks.add(exportTask(task, scan));
        }
        q.put("tasks", tasks);

        List<Map<String, Object>> rewards = new ArrayList<>();
        for (Reward reward : quest.getRewards()) {
            rewards.add(exportReward(reward, scan));
        }
        if (!rewards.isEmpty()) {
            q.put("rewards", rewards);
        }
        return q;
    }

    private static Map<String, Object> exportTask(Task task, QuestScanResult scan) {
        String type = task.getType().getTypeForNBT();
        scan.incrementTaskCount(type);

        CompoundTag nbt = QuestNbtExport.writeTask(task);
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", QuestObjectBase.getCodeString(task.id));
        t.put("type", type);
        t.put("title", task.getRawTitle());
        scan.collectLangFromText(task.getRawTitle());
        if (QuestNbtExport.optionalTask(nbt)) {
            t.put("optional", true);
        }

        if (task instanceof ItemTask itemTask) {
            ItemStack stack = itemTask.getItemStack();
            String filterRaw = SmartFilterExpander.filterRawFromStack(stack);
            List<String> items = SmartFilterExpander.expandFromItemStack(stack);
            if (filterRaw != null) {
                t.put("filterRaw", filterRaw);
                SmartFilterExpander.collectFilterRefs(filterRaw, scan);
                List<String> expanded = SmartFilterExpander.expandFilterString(filterRaw);
                scan.putExpandedFilter(filterRaw, expanded);
                scan.putExpandedFilter(QuestObjectBase.getCodeString(task.id), expanded);
                if (items.isEmpty() && !expanded.isEmpty()) {
                    items = expanded;
                }
            }
            if (!items.isEmpty()) {
                t.put("items", items);
            } else if (stack != null && !stack.isEmpty()) {
                String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                t.put("items", List.of(id));
                scan.addItem(id);
            }
        } else if (task instanceof FluidTask fluidTask) {
            var fluid = fluidTask.getFluid();
            if (fluid != null) {
                String id = ForgeRegistries.FLUIDS.getKey(fluid).toString();
                t.put("fluid", id);
                scan.addFluid(id);
            }
        } else if (task instanceof StageTask stageTask) {
            CompoundTag stageTag = QuestNbtExport.writeTask(stageTask);
            String stage = QuestNbtExport.string(stageTag, "stage");
            if (stage != null) {
                t.put("stage", stage);
            }
        } else {
            ResourceLocation entity = QuestNbtExport.resource(nbt, "entity");
            if (entity != null) {
                t.put("entity", entity.toString());
                scan.addEntity(entity.toString());
            }
            ResourceLocation dimension = QuestNbtExport.resource(nbt, "dimension");
            if (dimension != null) {
                t.put("dimension", dimension.toString());
            }
            ResourceLocation biome = QuestNbtExport.resource(nbt, "biome");
            if (biome != null) {
                t.put("biome", biome.toString());
            }
            String observe = QuestNbtExport.string(nbt, "to_observe");
            if (observe != null) {
                t.put("toObserve", observe);
                ObserveRefCollector.collect(observe, scan);
            }
            long value = QuestNbtExport.longVal(nbt, "value");
            if (value > 0) {
                t.put("value", value);
            }
        }
        return t;
    }

    private static Map<String, Object> exportReward(Reward reward, QuestScanResult scan) {
        String type = reward.getType().getTypeForNBT();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", QuestObjectBase.getCodeString(reward.id));
        r.put("type", type);
        scan.collectLangFromText(reward.getRawTitle());

        if (reward instanceof ItemReward itemReward) {
            ItemStack stack = itemReward.getItem();
            if (stack != null && !stack.isEmpty()) {
                List<String> items = SmartFilterExpander.expandFromItemStack(stack);
                if (!items.isEmpty()) {
                    r.put("items", items);
                    items.forEach(scan::addItem);
                }
                r.put("count", itemReward.getCount());
            }
        }
        return r;
    }

    /** SNBT {@code invisible} — hidden in-game until completed; not progression-gated. */
    private static void exportQuestVisibilityFlags(Quest quest, Map<String, Object> q) {
        CompoundTag tag = new CompoundTag();
        quest.writeData(tag);
        if (tag.getBoolean("invisible")) {
            q.put("invisible", true);
        }
    }

}
