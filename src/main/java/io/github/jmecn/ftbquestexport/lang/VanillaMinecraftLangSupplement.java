package io.github.jmecn.ftbquestexport.lang;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.pojo.LangMergeStats;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class VanillaMinecraftLangSupplement {

    private VanillaMinecraftLangSupplement() {}

    static boolean isMinecraftRegistryLangKey(String key) {
        return key != null
                && (key.startsWith("item.minecraft.")
                        || key.startsWith("block.minecraft.")
                        || key.startsWith("fluid.minecraft."));
    }

    static void supplement(Map<String, String> merged, Minecraft client, String langCode, Set<String> onlyKeys) {
        if (onlyKeys == null || onlyKeys.isEmpty()) {
            return;
        }
        Set<String> missing = new TreeSet<>();
        for (String key : onlyKeys) {
            if (isMinecraftRegistryLangKey(key) && !merged.containsKey(key)) {
                missing.add(key);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        List<String> locales = new ArrayList<>();
        locales.add(langCode);
        if (!QuestExportConstants.FALLBACK_LOCALE.equals(langCode)) {
            locales.add(QuestExportConstants.FALLBACK_LOCALE);
        }

        int added = 0;
        for (String locale : locales) {
            if (missing.isEmpty()) {
                break;
            }
            added += copyMissingFromMinecraftPack(merged, client, locale + ".json", missing);
        }

        if (!missing.isEmpty()) {
            String sample = missing.stream().limit(5).reduce((a, b) -> a + ", " + b).orElse("");
            FtbQuestExportMod.LOGGER.info(
                    "{} {} - {} minecraft registry lang keys still missing after vanilla supplement (e.g. {})",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    langCode,
                    missing.size(),
                    sample);
        } else if (added > 0) {
            FtbQuestExportMod.LOGGER.info(
                    "{} {} - supplemented {} minecraft registry lang keys from vanilla pack",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    langCode,
                    added);
        }
    }

    private static int copyMissingFromMinecraftPack(
            Map<String, String> merged, Minecraft client, String langFile, Set<String> missing) {
        Map<ResourceLocation, List<Resource>> stacks =
                LangMergerExporter.collectLangStacksForNamespaces(client, langFile, Set.of("minecraft"));
        if (stacks.isEmpty()) {
            return 0;
        }
        Map<String, String> minecraftMerged = new TreeMap<>();
        LangMergerExporter.mergeLangStacksInto(minecraftMerged, stacks, null, new LangMergeStats());
        int added = 0;
        Set<String> found = new TreeSet<>();
        for (String key : missing) {
            if (merged.containsKey(key)) {
                continue;
            }
            String value = minecraftMerged.get(key);
            if (value == null) {
                continue;
            }
            merged.put(key, value);
            found.add(key);
            added++;
        }
        missing.removeAll(found);
        return added;
    }
}
