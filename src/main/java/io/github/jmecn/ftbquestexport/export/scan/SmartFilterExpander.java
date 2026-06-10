package io.github.jmecn.ftbquestexport.export.scan;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands {@code ftbfiltersystem:smart_filter} strings to item id lists. */
public final class SmartFilterExpander {

    private static final Pattern ITEM_TOKEN = Pattern.compile("item\\(([^)]+)\\)");
    /** {@code item_tag(...)}, {@code block_tag(...)}, {@code fluid_tag(...)}, legacy {@code tag(...)}. */
    private static final Pattern TAG_TOKEN = Pattern.compile(
            "(?:item_tag|block_tag|fluid_tag|tag)\\(([^)]+)\\)");

    private SmartFilterExpander() {}

    public static List<String> extractTags(String filterRaw) {
        if (filterRaw == null || filterRaw.isBlank()) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = TAG_TOKEN.matcher(filterRaw);
        while (matcher.find()) {
            String id = matcher.group(1).trim();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }
            if (id.contains(":")) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    /** Collects item and tag refs from a smart-filter expression into the scan result. */
    public static void collectFilterRefs(String filterRaw, QuestScanResult scan) {
        if (filterRaw == null || filterRaw.isBlank() || scan == null) {
            return;
        }
        for (String tagId : extractTags(filterRaw)) {
            scan.addTag(tagId);
        }
        for (String itemId : expandFilterString(filterRaw)) {
            scan.addItem(itemId);
        }
    }

    public static List<String> expandFilterString(String filterRaw) {
        if (filterRaw == null || filterRaw.isBlank()) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        Matcher m = ITEM_TOKEN.matcher(filterRaw);
        while (m.find()) {
            String id = m.group(1).trim();
            if (id.contains(":")) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
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
                FtbQuestExportMod.LOGGER.warn("[filter] could not expand smart_filter: {}", raw);
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
}
