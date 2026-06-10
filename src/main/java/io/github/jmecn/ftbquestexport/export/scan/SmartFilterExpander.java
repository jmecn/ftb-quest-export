package io.github.jmecn.ftbquestexport.export.scan;

import dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI;
import dev.ftb.mods.ftbfiltersystem.api.FilterException;
import dev.ftb.mods.ftbfiltersystem.api.filter.DumpedFilter;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbfiltersystem.filter.ExpressionFilter;
import dev.ftb.mods.ftbfiltersystem.filter.ItemFilter;
import dev.ftb.mods.ftbfiltersystem.filter.ItemTagFilter;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Collects explicit {@code item(...)} and tag refs from {@code ftbfiltersystem:smart_filter} strings. */
public final class SmartFilterExpander {

    private SmartFilterExpander() {}

    public static List<String> extractTags(String filterRaw) {
        if (filterRaw == null || filterRaw.isBlank()) {
            return List.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        collectRefs(filterRaw, new LinkedHashSet<>(), tags);
        return new ArrayList<>(tags);
    }

    /** Collects item and tag refs from a smart-filter expression into the scan result. */
    public static void collectFilterRefs(String filterRaw, QuestScanResult scan) {
        if (filterRaw == null || filterRaw.isBlank() || scan == null) {
            return;
        }
        Set<String> items = new LinkedHashSet<>();
        Set<String> tags = new LinkedHashSet<>();
        collectRefs(filterRaw, items, tags);
        tags.forEach(scan::addTag);
        items.forEach(scan::addItem);
    }

    public static List<String> expandFilterString(String filterRaw) {
        if (filterRaw == null || filterRaw.isBlank()) {
            return List.of();
        }
        Set<String> items = new LinkedHashSet<>();
        collectRefs(filterRaw, items, new LinkedHashSet<>());
        return new ArrayList<>(items);
    }

    public static List<String> expandFromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        if ("ftbfiltersystem:smart_filter".equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString())) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("ftbfiltersystem:filter")) {
                String raw = tag.getString("ftbfiltersystem:filter");
                List<String> expanded = expandFilterString(raw);
                if (!expanded.isEmpty()) {
                    return expanded;
                }
                if (extractTags(raw).isEmpty()) {
                    FtbQuestExportMod.LOGGER.warn("[filter] no explicit item or tag refs in smart_filter: {}", raw);
                }
                return List.of();
            }
        }
        return List.of(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
    }

    public static String filterRawFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("ftbfiltersystem:filter")) {
            return tag.getString("ftbfiltersystem:filter");
        }
        return null;
    }

    private static void collectRefs(String filterRaw, Set<String> items, Set<String> tags) {
        try {
            SmartFilter root = FTBFilterSystemAPI.api().parseFilter(filterRaw);
            for (DumpedFilter entry : FTBFilterSystemAPI.api().dump(root)) {
                collectFromFilter(entry.filter(), items, tags);
            }
        } catch (FilterException e) {
            FtbQuestExportMod.LOGGER.warn("[filter] parse failed: {} ({})", filterRaw, e.getMessage());
        }
    }

    private static void collectFromFilter(SmartFilter filter, Set<String> items, Set<String> tags) {
        if (filter instanceof ItemFilter itemFilter) {
            var itemId = ForgeRegistries.ITEMS.getKey(itemFilter.getMatchItem());
            if (itemId != null) {
                items.add(itemId.toString());
            }
        } else if (filter instanceof ItemTagFilter tagFilter) {
            tags.add(tagFilter.getTagKey().location().toString());
        } else if (filter instanceof ExpressionFilter expressionFilter) {
            collectRefs(expressionFilter.getExpression(), items, tags);
        }
    }
}
