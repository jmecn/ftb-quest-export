package io.github.jmecn.ftbquestexport.scan;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import io.github.jmecn.ftbquestexport.pojo.QuestTitleFallback;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QuestDisplayExport {

    private QuestDisplayExport() {}

    public static void applyQuestDisplay(Quest quest, QuestNode.Builder q, QuestScanResult scan) {
        String rawTitle = quest.getRawTitle();
        if (rawTitle != null && !rawTitle.isBlank()) {
            q.title(rawTitle);
            scan.collectLangFromText(rawTitle);
        } else {
            QuestTitleFallback fallback = titleFallback(quest);
            if (fallback != null) {
                q.titleItem(fallback.itemId());
                scan.addItem(fallback.itemId());
                if (fallback.count() > 1L) {
                    q.titleCount(fallback.count());
                }
            }
        }

        String iconRef = resolveIconRef(quest.getIcon(), scan);
        if (iconRef == null) {
            iconRef = firstTaskIcon(quest, scan);
        }
        if (iconRef != null) {
            q.icon(iconRef);
        }

        List<String> iconItems = collectIconItemRefs(quest.getIcon(), scan);
        iconItems.forEach(scan::addItem);
        if (iconItems.size() > 1) {
            q.iconItems(iconItems);
        }
    }

    public static String resolveIconRef(Icon icon, QuestScanResult scan) {
        if (icon == null || icon.isEmpty()) {
            return null;
        }
        if (icon instanceof IconAnimation animation) {
            for (Icon frame : animation.list) {
                String ref = resolveIconRef(frame, scan);
                if (ref != null) {
                    return ref;
                }
            }
            return null;
        }
        if (icon instanceof ItemIcon itemIcon) {
            return itemStackRef(itemIcon.getStack(), scan);
        }
        Object ingredient = icon.getIngredient();
        if (ingredient instanceof ItemStack stack) {
            return itemStackRef(stack, scan);
        }
        String s = icon.toString();
        if (s.contains(":")) {
            scan.addTexture(s);
            return s;
        }
        return null;
    }

    private static List<String> collectIconItemRefs(Icon icon, QuestScanResult scan) {
        Set<String> refs = new LinkedHashSet<>();
        collectIconItemRefs(icon, scan, refs);
        return new ArrayList<>(refs);
    }

    private static void collectIconItemRefs(Icon icon, QuestScanResult scan, Set<String> refs) {
        if (icon == null || icon.isEmpty()) {
            return;
        }
        if (icon instanceof IconAnimation animation) {
            for (Icon frame : animation.list) {
                collectIconItemRefs(frame, scan, refs);
            }
            return;
        }
        if (icon instanceof ItemIcon itemIcon) {
            String ref = itemStackRef(itemIcon.getStack(), scan);
            if (ref != null) {
                refs.add(ref);
            }
            return;
        }
        Object ingredient = icon.getIngredient();
        if (ingredient instanceof ItemStack stack) {
            String ref = itemStackRef(stack, scan);
            if (ref != null) {
                refs.add(ref);
            }
        }
    }

    private static String firstTaskIcon(Quest quest, QuestScanResult scan) {
        for (Task task : quest.getTasks()) {
            String ref = resolveIconRef(task.getIcon(), scan);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    private static QuestTitleFallback titleFallback(Quest quest) {
        Task first = firstTask(quest);
        if (first == null) {
            return null;
        }
        if (!(first instanceof ItemTask itemTask)) {
            return null;
        }
        ItemStack stack = itemTask.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return new QuestTitleFallback(itemId, itemTask.getMaxProgress());
    }

    private static Task firstTask(Quest quest) {
        if (quest.getTasks().isEmpty()) {
            return null;
        }
        return quest.getTasks().iterator().next();
    }

    private static String itemStackRef(ItemStack stack, QuestScanResult scan) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        scan.addItem(id);
        return id;
    }
}
