package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.model.IconDisplayFrame;
import net.minecraft.client.Minecraft;
import io.github.jmecn.ftbquestexport.model.IconDisplay;
import io.github.jmecn.ftbquestexport.model.QuestLink;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import io.github.jmecn.ftbquestexport.model.QuestReward;
import io.github.jmecn.ftbquestexport.model.QuestTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public final class QuestIconDisplayWriter {

    private QuestIconDisplayWriter() {}

    public static QuestNode attachQuestNodeIconDisplay(
            QuestNode quest, double gridScale, Set<String> fluidIds, Minecraft client) {
        List<ChapterSpriteCollector.QuestRefs> refsInfo =
                ChapterSpriteCollector.collectRefsForQuest(quest, gridScale);
        if (refsInfo.isEmpty()) {
            return quest;
        }
        ChapterSpriteCollector.QuestRefs first = refsInfo.get(0);
        return quest.withIconDisplay(
                buildIconDisplay(first.outerPx(), first.innerPx(), first.refs(), fluidIds, client));
    }

    public static QuestNode attachTaskRewardIconDisplays(
            QuestNode quest, Set<String> fluidIds, Minecraft client) {
        int outer = QuestExportConstants.DETAIL_ITEM_ICON_PX;
        int inner = QuestIconSizing.questIconInnerPx(outer);
        List<QuestTask> tasks = quest.tasks();
        if (tasks != null) {
            List<QuestTask> updatedTasks = new ArrayList<>(tasks.size());
            for (QuestTask task : tasks) {
                updatedTasks.add(withItemListIconDisplay(task, task.items(), outer, inner, fluidIds, client));
            }
            quest = quest.withTasks(updatedTasks);
        }
        List<QuestReward> rewards = quest.rewards();
        if (rewards != null) {
            List<QuestReward> updatedRewards = new ArrayList<>(rewards.size());
            for (QuestReward reward : rewards) {
                updatedRewards.add(withItemListIconDisplay(reward, reward.items(), outer, inner, fluidIds, client));
            }
            quest = quest.withRewards(updatedRewards);
        }
        return quest;
    }

    public static QuestLink attachLinkIconDisplay(
            QuestLink link,
            QuestNode linkedQuest,
            double gridScale,
            Set<String> fluidIds,
            Minecraft client) {
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
            List<ChapterSpriteCollector.QuestRefs> refsInfo =
                    ChapterSpriteCollector.collectRefsForQuest(linkedQuest, gridScale);
            if (!refsInfo.isEmpty()) {
                refs.addAll(refsInfo.get(0).refs());
            }
        }
        if (refs.isEmpty()) {
            refs.add(QuestExportConstants.MISSING_ICON_REGISTRY_ID);
        }
        return link.withIconDisplay(buildIconDisplay(outer, inner, refs, fluidIds, client));
    }

    public static IconDisplay buildIconDisplay(
            int outer, int inner, List<String> refs, Set<String> fluidIds, Minecraft client) {
        Map<String, Integer> nativeCache = new HashMap<>();
        List<IconDisplayFrame> frames = null;
        if (refs.size() > 1) {
            frames = new ArrayList<>(refs.size());
            for (String ref : refs) {
                frames.add(new IconDisplayFrame(spriteId(ref, inner, fluidIds, client, nativeCache)));
            }
        }
        return new IconDisplay(
                spriteId(refs.get(0), inner, fluidIds, client, nativeCache), outer, inner, frames);
    }

    private static QuestTask withItemListIconDisplay(
            QuestTask task,
            List<String> items,
            int outer,
            int inner,
            Set<String> fluidIds,
            Minecraft client) {
        List<String> refs = collectItemRefs(items);
        if (refs.isEmpty()) {
            return task;
        }
        return task.withIconDisplay(buildIconDisplay(outer, inner, refs, fluidIds, client));
    }

    private static QuestReward withItemListIconDisplay(
            QuestReward reward,
            List<String> items,
            int outer,
            int inner,
            Set<String> fluidIds,
            Minecraft client) {
        List<String> refs = collectItemRefs(items);
        if (refs.isEmpty()) {
            return reward;
        }
        return reward.withIconDisplay(buildIconDisplay(outer, inner, refs, fluidIds, client));
    }

    private static List<String> collectItemRefs(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> refs = new ArrayList<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                refs.add(item);
            }
        }
        return refs;
    }

    private static String spriteId(
            String ref,
            int displayInner,
            Set<String> fluidIds,
            Minecraft client,
            Map<String, Integer> nativeCache) {
        return ref + "@" + QuestIconRefKind.packTier(client, ref, fluidIds, displayInner, nativeCache);
    }
}
