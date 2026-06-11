package io.github.jmecn.ftbquestexport.scan;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import io.github.jmecn.ftbquestexport.icons.ChapterImages;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.pojo.ScanBundle;
import io.github.jmecn.ftbquestexport.model.ChapterGroup;
import io.github.jmecn.ftbquestexport.model.ChapterImage;
import io.github.jmecn.ftbquestexport.model.ChapterSummary;
import io.github.jmecn.ftbquestexport.model.QuestIndex;
import io.github.jmecn.ftbquestexport.model.QuestLink;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import io.github.jmecn.ftbquestexport.model.QuestReward;
import io.github.jmecn.ftbquestexport.model.QuestTask;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.StageTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestFileScanner {

    private QuestFileScanner() {}

    public static ScanBundle scan() {
        BaseQuestFile file = FTBQuestsAPI.api().getQuestFile(false);
        QuestScanResult scan = new QuestScanResult();
        Map<String, ChapterData> chapters = new LinkedHashMap<>();

        String title = file.getRawTitle();
        double gridScale = file.getGridScale();
        String defaultQuestShape = file.getDefaultQuestShape();
        scan.collectLangFromText(title);
        scan.addQuestShapeTextures("circle");
        scan.addQuestShapeTextures(defaultQuestShape);

        List<ChapterGroup> groups = new ArrayList<>();
        int groupIndex = 0;
        for (dev.ftb.mods.ftbquests.quest.ChapterGroup group : file.getChapterGroups()) {
            groups.add(new ChapterGroup(
                    QuestObjectBase.getCodeString(group.id),
                    group.getRawTitle(),
                    groupIndex++));
            scan.collectLangFromText(group.getRawTitle());
        }

        List<ChapterSummary> chapterIndex = new ArrayList<>();
        List<dev.ftb.mods.ftbquests.quest.Chapter> allChapters = file.getAllChapters();
        scan.setChapterCount(allChapters.size());

        for (dev.ftb.mods.ftbquests.quest.Chapter chapter : allChapters) {
            String filename = chapter.getFilename();
            ChapterData chapterData = exportChapter(chapter, scan);
            chapters.put(filename, chapterData);

            String chapterTitle = chapter.getRawTitle();
            List<String> rawSubtitle = chapter.getRawSubtitle();
            if (!rawSubtitle.isEmpty()) {
                scan.collectLangFromLines(rawSubtitle);
            }
            chapterIndex.add(new ChapterSummary(
                    QuestObjectBase.getCodeString(chapter.id),
                    filename,
                    chapter.hasGroup()
                            ? QuestObjectBase.getCodeString(chapter.getGroup().id)
                            : null,
                    chapter.getIndex(),
                    QuestDisplayExport.resolveIconRef(chapter.getIcon(), scan),
                    chapterTitle != null && !chapterTitle.isBlank() ? chapterTitle : null,
                    rawSubtitle.isEmpty() ? null : rawSubtitle));
        }

        QuestIndex index = new QuestIndex(
                title,
                BaseQuestFile.VERSION,
                file.getProgressionMode().getId(),
                gridScale,
                defaultQuestShape,
                groups,
                chapterIndex,
                null);

        FtbQuestExportMod.LOGGER.info("[scan] {} chapters, {} quests, {} tasks",
                scan.getChapterCount(), scan.getQuestCount(), scan.getTaskCount());
        return new ScanBundle(scan, index, chapters);
    }

    private static ChapterData exportChapter(dev.ftb.mods.ftbquests.quest.Chapter chapter, QuestScanResult scan) {
        ChapterData.Builder root = ChapterData.builder()
                .id(QuestObjectBase.getCodeString(chapter.id))
                .filename(chapter.getFilename())
                .group(chapter.hasGroup()
                        ? QuestObjectBase.getCodeString(chapter.getGroup().id)
                        : null)
                .icon(QuestDisplayExport.resolveIconRef(chapter.getIcon(), scan))
                .defaultQuestShape(chapter.getDefaultQuestShape())
                .orderIndex(chapter.getIndex());
        exportChapterDisplay(chapter, root, scan);
        chapter.getAutofocus().ifPresent(movable ->
                root.autofocusId(QuestObjectBase.getCodeString(movable.getMovableID())));
        scan.addQuestShapeTextures(chapter.getDefaultQuestShape());

        List<QuestNode> quests = new ArrayList<>();
        for (Quest quest : chapter.getQuests()) {
            quests.add(exportQuest(quest, scan));
            scan.incrementQuestCount();
        }
        root.quests(quests);

        List<QuestLink> links = new ArrayList<>();
        for (dev.ftb.mods.ftbquests.quest.QuestLink link : chapter.getQuestLinks()) {
            Quest linked = link.getQuest().orElse(null);
            if (linked == null) {
                FtbQuestExportMod.LOGGER.warn("[scan] skipping quest link {} — linked quest not found", link.id);
                continue;
            }
            QuestLink.Builder l = QuestLink.builder()
                    .id(QuestObjectBase.getCodeString(link.id))
                    .linkedQuest(QuestObjectBase.getCodeString(linked.id))
                    .x(link.getX())
                    .y(link.getY());
            if (link.getShape() != null && !link.getShape().isEmpty()) {
                l.shape(link.getShape());
                scan.addQuestShapeTextures(link.getShape());
            }
            if (link.getWidth() != 1D) {
                l.size(link.getWidth());
            }
            links.add(l.build());
        }
        root.questLinks(links);

        List<ChapterImage> images = new ArrayList<>();
        for (dev.ftb.mods.ftbquests.quest.ChapterImage image : chapter.getImages()) {
            images.add(exportChapterImage(image, scan));
        }
        root.images(images);
        return root.build();
    }

    private static void exportChapterDisplay(dev.ftb.mods.ftbquests.quest.Chapter chapter, ChapterData.Builder root, QuestScanResult scan) {
        String rawTitle = chapter.getRawTitle();
        if (rawTitle != null && !rawTitle.isBlank()) {
            root.title(rawTitle);
            scan.collectLangFromText(rawTitle);
        }
        List<String> rawSubtitle = chapter.getRawSubtitle();
        if (!rawSubtitle.isEmpty()) {
            root.subtitle(rawSubtitle);
            scan.collectLangFromLines(rawSubtitle);
        }
    }

    private static QuestNode exportQuest(Quest quest, QuestScanResult scan) {
        QuestNode.Builder q = QuestNode.builder()
                .id(QuestObjectBase.getCodeString(quest.id))
                .x(quest.getX())
                .y(quest.getY())
                .size(quest.getSize());
        exportQuestVisibilityFlags(quest, q);
        if (quest.getShape() != null && !quest.getShape().isEmpty()) {
            q.shape(quest.getShape());
            scan.addQuestShapeTextures(quest.getShape());
        }
        QuestDisplayExport.applyQuestDisplay(quest, q, scan);
        q.subtitle(quest.getRawSubtitle());
        q.description(quest.getRawDescription());
        scan.collectLangFromText(quest.getRawSubtitle());
        scan.collectLangFromLines(quest.getRawDescription());

        String guidePage = quest.getGuidePage();
        if (guidePage != null && !guidePage.isBlank()) {
            q.guidePage(guidePage);
        }

        List<String> deps = new ArrayList<>();
        quest.streamDependencies().forEach(dep -> deps.add(QuestObjectBase.getCodeString(dep.id)));
        if (!deps.isEmpty()) {
            q.dependencies(deps);
        }
        q.hideDependencyLines(quest.shouldHideDependentLines());

        List<QuestTask> tasks = new ArrayList<>();
        for (Task task : quest.getTasks()) {
            tasks.add(exportTask(task, scan));
        }
        q.tasks(tasks);

        List<QuestReward> rewards = new ArrayList<>();
        for (Reward reward : quest.getRewards()) {
            rewards.add(exportReward(reward, scan));
        }
        if (!rewards.isEmpty()) {
            q.rewards(rewards);
        }
        return q.build();
    }

    private static QuestTask exportTask(Task task, QuestScanResult scan) {
        String type = task.getType().getTypeForNBT();
        scan.incrementTaskCount(type);

        CompoundTag nbt = writeTaskNbt(task);
        QuestTask.Builder t = QuestTask.builder()
                .id(QuestObjectBase.getCodeString(task.id))
                .type(type)
                .title(task.getRawTitle());
        scan.collectLangFromText(task.getRawTitle());
        if (optionalTask(nbt)) {
            t.optional(true);
        }

        if (task instanceof ItemTask itemTask) {
            ItemStack stack = itemTask.getItemStack();
            String filterRaw = SmartFilterExpander.filterRawFromStack(stack);
            List<String> items = SmartFilterExpander.expandFromItemStack(stack);
            if (filterRaw != null) {
                t.filterRaw(filterRaw);
                SmartFilterExpander.collectFilterRefs(filterRaw, scan);
                List<String> expanded = SmartFilterExpander.expandFilterString(filterRaw);
                scan.putExpandedFilter(filterRaw, expanded);
                scan.putExpandedFilter(QuestObjectBase.getCodeString(task.id), expanded);
                if (items.isEmpty() && !expanded.isEmpty()) {
                    items = expanded;
                }
            }
            if (!items.isEmpty()) {
                t.items(items);
                items.forEach(scan::addItem);
            } else if (stack != null && !stack.isEmpty()) {
                String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                t.items(List.of(id));
                scan.addItem(id);
            }
        } else if (task instanceof FluidTask fluidTask) {
            Fluid fluid = fluidTask.getFluid();
            if (fluid != null) {
                String id = ForgeRegistries.FLUIDS.getKey(fluid).toString();
                t.fluid(id);
                scan.addFluid(id);
            }
        } else if (task instanceof StageTask stageTask) {
            CompoundTag stageTag = writeTaskNbt(stageTask);
            String stage = nbtString(stageTag, "stage");
            if (stage != null) {
                t.stage(stage);
            }
        } else {
            ResourceLocation entity = nbtResource(nbt, "entity");
            if (entity != null) {
                t.entity(entity.toString());
                scan.addEntity(entity.toString());
            }
            ResourceLocation dimension = nbtResource(nbt, "dimension");
            if (dimension != null) {
                t.dimension(dimension.toString());
            }
            ResourceLocation biome = nbtResource(nbt, "biome");
            if (biome != null) {
                t.biome(biome.toString());
            }
            String observe = nbtString(nbt, "to_observe");
            if (observe != null) {
                t.toObserve(observe);
                collectObserveRef(observe, scan);
            }
            long value = nbtLong(nbt, "value");
            if (value > 0) {
                t.value(value);
            }
        }
        return t.build();
    }

    private static QuestReward exportReward(Reward reward, QuestScanResult scan) {
        String type = reward.getType().getTypeForNBT();
        QuestReward.Builder r = QuestReward.builder()
                .id(QuestObjectBase.getCodeString(reward.id))
                .type(type);
        scan.collectLangFromText(reward.getRawTitle());

        if (reward instanceof ItemReward itemReward) {
            ItemStack stack = itemReward.getItem();
            if (stack != null && !stack.isEmpty()) {
                List<String> items = SmartFilterExpander.expandFromItemStack(stack);
                if (!items.isEmpty()) {
                    r.items(items);
                    items.forEach(scan::addItem);
                }
                r.count(itemReward.getCount());
            }
        }
        return r.build();
    }

    private static ChapterImage exportChapterImage(dev.ftb.mods.ftbquests.quest.ChapterImage image, QuestScanResult scan) {
        String imageRef = QuestDisplayExport.resolveIconRef(image.getImage(), scan);
        ChapterImage.Builder img = ChapterImage.builder()
                .image(imageRef != null ? imageRef : image.getImage().toString())
                .x(image.getX())
                .y(image.getY())
                .width(image.getWidth())
                .height(image.getHeight())
                .rotation(image.getRotation())
                .alpha((double) image.getAlpha());
        if (image.getClick() != null && !image.getClick().isEmpty()) {
            img.click(image.getClick());
        }
        if (image.getOrder() > 0) {
            img.order(image.getOrder());
        }
        exportChapterImageDisplayFields(image, img);
        ChapterImage built = img.build();
        return ChapterImages.applyAnimationMeta(image.getImage(), built);
    }

    private static void exportQuestVisibilityFlags(Quest quest, QuestNode.Builder q) {
        CompoundTag tag = new CompoundTag();
        quest.writeData(tag);
        if (tag.getBoolean("invisible")) {
            q.invisible(true);
        }
    }

    private static void exportChapterImageDisplayFields(dev.ftb.mods.ftbquests.quest.ChapterImage image, ChapterImage.Builder img) {
        if (!image.getColor().equals(Color4I.WHITE)) {
            img.color(image.getColor().rgb());
        }
        if (image.isAlignToCorner()) {
            img.alignToCorner(true);
        }
        CompoundTag tag = new CompoundTag();
        image.writeData(tag);
        if (tag.contains("dependency")) {
            img.dependency(tag.getString("dependency"));
        }
        if (tag.getBoolean("dev")) {
            img.editorsOnly(true);
        }
        ListTag hoverTag = tag.getList("hover", Tag.TAG_STRING);
        if (!hoverTag.isEmpty()) {
            List<String> hover = new ArrayList<>(hoverTag.size());
            for (int i = 0; i < hoverTag.size(); i++) {
                hover.add(hoverTag.getString(i));
            }
            img.hover(hover);
        }
    }

    private static CompoundTag writeTaskNbt(dev.ftb.mods.ftbquests.quest.task.Task task) {
        CompoundTag tag = new CompoundTag();
        task.writeData(tag);
        return tag;
    }

    private static boolean optionalTask(CompoundTag tag) {
        return tag.getBoolean("optional_task");
    }

    private static String nbtString(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : null;
    }

    private static long nbtLong(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : 0L;
    }

    private static ResourceLocation nbtResource(CompoundTag tag, String key) {
        String s = nbtString(tag, key);
        return s != null ? ResourceLocation.tryParse(s) : null;
    }

    private static void collectObserveRef(String ref, QuestScanResult scan) {
        if (ref == null || ref.isBlank()) {
            return;
        }
        String trimmed = ref.trim();
        if (trimmed.startsWith("#")) {
            scan.addTag(trimmed.substring(1));
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(trimmed);
        if (loc == null) {
            FtbQuestExportMod.LOGGER.debug("[observe] skipping unparseable ref: {}", trimmed);
            return;
        }
        if (ForgeRegistries.BLOCKS.containsKey(loc)) {
            scan.addBlock(loc.toString());
            Item blockItem = ForgeRegistries.BLOCKS.getValue(loc).asItem();
            if (blockItem != null && blockItem != Items.AIR) {
                scan.addItem(ForgeRegistries.ITEMS.getKey(blockItem).toString());
            }
            return;
        }
        if (ForgeRegistries.ENTITY_TYPES.containsKey(loc)) {
            scan.addEntity(loc.toString());
            return;
        }
        FtbQuestExportMod.LOGGER.debug("[observe] ref not in block/entity registry, treating as block: {}", trimmed);
        scan.addBlock(trimmed);
    }
}
