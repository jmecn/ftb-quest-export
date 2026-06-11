package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.model.QuestLink;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import io.github.jmecn.ftbquestexport.model.QuestReward;
import io.github.jmecn.ftbquestexport.model.QuestTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChapterSpriteCollector {

    private ChapterSpriteCollector() {}

    public record SpriteNeed(String ref, int tier, int outerPx, int innerPx) {
        public String spriteId() {
            return ref + "@" + tier;
        }
    }

    public record QuestRefs(List<String> refs, int tier, int outerPx, int innerPx) {}

    public static Map<String, SpriteNeed> collectChapterNeeds(
            ChapterData chapterData,
            Map<String, QuestNode> questById,
            double gridScale,
            Set<String> fluidIds) {
        Map<String, SpriteNeed> needs = new LinkedHashMap<>();

        if (chapterData.icon() != null && !chapterData.icon().isBlank()) {
            upsert(needs, chapterData.icon(), 1.0, gridScale, fluidIds);
        }

        if (chapterData.quests() != null) {
            for (QuestNode quest : chapterData.quests()) {
                collectQuestNodeSprites(quest, needs, gridScale, fluidIds, true);
            }
        }

        if (chapterData.questLinks() != null) {
            for (QuestLink link : chapterData.questLinks()) {
                collectLinkSprites(link, questById, needs, gridScale, fluidIds);
            }
        }

        return needs;
    }

    private static void collectQuestNodeSprites(
            QuestNode quest,
            Map<String, SpriteNeed> needs,
            double gridScale,
            Set<String> fluidIds,
            boolean includeTasksRewards) {
        for (QuestRefs refs : collectRefsForQuest(quest, gridScale)) {
            for (String ref : refs.refs()) {
                upsert(needs, ref, refs.innerPx(), refs.outerPx(), fluidIds);
            }
        }
        if (includeTasksRewards) {
            collectTaskRewardItems(quest, needs, gridScale, fluidIds);
        }
    }

    private static void collectLinkSprites(
            QuestLink link,
            Map<String, QuestNode> questById,
            Map<String, SpriteNeed> needs,
            double gridScale,
            Set<String> fluidIds) {
        if (link.linkedQuest() == null || link.linkedQuest().isBlank()) {
            return;
        }
        QuestNode linkedQuest = questById.get(link.linkedQuest());
        if (linkedQuest == null) {
            return;
        }
        double linkSize = link.size() != null ? link.size() : linkedQuest.size() != null ? linkedQuest.size() : 1.0;
        int outer = QuestIconSizing.questIconPx(linkSize, gridScale);
        int inner = QuestIconSizing.questIconInnerPx(outer);
        List<String> refs = new ArrayList<>();
        if (linkedQuest.icon() != null && !linkedQuest.icon().isBlank()) {
            refs.add(linkedQuest.icon());
        }
        if (linkedQuest.iconItems() != null) {
            refs.addAll(linkedQuest.iconItems());
        }
        if (refs.isEmpty()) {
            for (QuestRefs info : collectRefsForQuest(linkedQuest, gridScale)) {
                refs.addAll(info.refs());
            }
        }
        if (refs.isEmpty()) {
            refs.add(QuestExportConstants.MISSING_ICON_REGISTRY_ID);
        }
        for (String ref : refs) {
            upsert(needs, ref, inner, outer, fluidIds);
        }
    }

    private static void collectTaskRewardItems(
            QuestNode quest, Map<String, SpriteNeed> needs, double gridScale, Set<String> fluidIds) {
        collectTaskRewardItemList(quest.tasks(), needs, gridScale, fluidIds);
        if (quest.rewards() != null) {
            for (QuestReward reward : quest.rewards()) {
                collectItemRefs(reward.items(), needs, gridScale, fluidIds);
            }
        }
    }

    private static void collectTaskRewardItemList(
            List<QuestTask> tasks,
            Map<String, SpriteNeed> needs,
            double gridScale,
            Set<String> fluidIds) {
        if (tasks == null) {
            return;
        }
        for (QuestTask task : tasks) {
            collectItemRefs(task.items(), needs, gridScale, fluidIds);
        }
    }

    private static void collectItemRefs(
            List<String> items, Map<String, SpriteNeed> needs, double gridScale, Set<String> fluidIds) {
        if (items == null) {
            return;
        }
        for (String ref : items) {
            if (ref != null && !ref.isBlank()) {
                upsert(needs, ref, 1.0, gridScale, fluidIds);
            }
        }
    }

    public static List<QuestRefs> collectRefsForQuest(QuestNode quest, double gridScale) {
        double size = quest.size() != null ? quest.size() : 1.0;
        int outer = QuestIconSizing.questIconPx(size, gridScale);
        int inner = QuestIconSizing.questIconInnerPx(outer);
        int tier = QuestIconSizing.ceilToTier(inner);

        List<String> refs = new ArrayList<>();
        if (quest.icon() != null && !quest.icon().isBlank()) {
            refs.add(quest.icon());
        }
        if (quest.iconItems() != null) {
            refs.addAll(quest.iconItems());
        }
        if (refs.isEmpty()) {
            refs.addAll(fallbackTaskItems(quest));
        }
        if (refs.isEmpty()) {
            refs.add(QuestExportConstants.MISSING_ICON_REGISTRY_ID);
        }
        return List.of(new QuestRefs(refs, tier, outer, inner));
    }

    private static List<String> fallbackTaskItems(QuestNode quest) {
        if (quest.tasks() == null) {
            return List.of();
        }
        for (QuestTask task : quest.tasks()) {
            if (task.items() == null) {
                continue;
            }
            for (String ref : task.items()) {
                if (ref != null && !ref.isBlank()) {
                    return List.of(ref);
                }
            }
        }
        return List.of();
    }

    private static void upsert(
            Map<String, SpriteNeed> needs, String ref, double size, double gridScale, Set<String> fluidIds) {
        int outer = QuestIconSizing.questIconPx(size, gridScale);
        int inner = QuestIconSizing.questIconInnerPx(outer);
        upsert(needs, ref, inner, outer, fluidIds);
    }

    private static void upsert(
            Map<String, SpriteNeed> needs, String ref, int displayInner, int outer, Set<String> fluidIds) {
        int packTier = QuestIconRefKind.packTier(ref, fluidIds, displayInner);
        String spriteId = ref + "@" + packTier;
        SpriteNeed existing = needs.get(spriteId);
        if (existing == null || packTier > existing.tier()) {
            needs.put(spriteId, new SpriteNeed(ref, packTier, outer, displayInner));
        }
    }

    public static Map<String, QuestNode> buildQuestCatalog(Map<String, ChapterData> chapters) {
        Map<String, QuestNode> catalog = new LinkedHashMap<>();
        for (ChapterData chapterData : chapters.values()) {
            if (chapterData.quests() == null) {
                continue;
            }
            for (QuestNode quest : chapterData.quests()) {
                if (quest.id() != null && !quest.id().isBlank()) {
                    catalog.put(quest.id(), quest);
                }
            }
        }
        return catalog;
    }
}
