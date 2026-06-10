package io.github.jmecn.ftbquestexport.export.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.export.assets.ResourceExportFilter;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Merges mod language files the same way {@link net.minecraft.client.resources.language.ClientLanguage}
 * does at runtime: {@link ResourceManager#listResourceStacks} per {@code assets/<ns>/lang/<locale>.json},
 * lower-priority packs first, later packs override individual keys (KubeJS partial overrides included).
 */
public final class LangMergerExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Always merged in full — small mod-specific UI set used by QuestBook (e.g. guide links). */
    private static final String FTBQUESTS_NAMESPACE = "ftbquests";

    private LangMergerExporter() {}

    public record Result(
            int languagesWritten,
            long totalBytes,
            int duplicateKeyWarnings,
            int closureKeysRequested,
            int keysSkipped,
            int keysPerLanguage) {}

    static final class MergeStats {
        int keysSkipped;
        int duplicateKeyWarnings;
        int resourceLayersRead;
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("quest.skipLangExport");
    }

    public static Result exportTo(Path langRoot, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
            throws IOException {
        Files.createDirectories(langRoot);

        List<String> configured = QuestExportLanguages.asList();
        Set<String> languages =
                configured.isEmpty() ? client.getLanguageManager().getLanguages().keySet() : Set.copyOf(configured);

        int languagesWritten = 0;
        long totalBytes = 0;
        int duplicateWarnings = 0;
        int keysSkipped = 0;
        int keysPerLanguage = 0;
        String mode = onlyKeys == null ? "full" : "closure";

        for (String langCode : languages) {
            String langFile = langCode + ".json";
            Map<String, String> merged = new TreeMap<>();
            Map<ResourceLocation, List<Resource>> stacks = collectLangStacks(client, langFile, onlyNamespaces);
            MergeStats stats = new MergeStats();

            if (stacks.isEmpty()) {
                FtbQuestExportMod.LOGGER.warn(
                        "{} {} - no mod lang files matched (namespaces={})",
                        LangExportLog.LANG,
                        langCode,
                        onlyNamespaces == null ? "all" : onlyNamespaces);
                logLangPathProbe(client, langFile);
            } else {
                mergeLangStacksInto(merged, stacks, onlyKeys, stats);
            }

            mergeFullNamespace(client, langFile, FTBQUESTS_NAMESPACE, merged, stats);

            if (onlyKeys != null) {
                VanillaMinecraftLangSupplement.supplement(merged, client, langCode, onlyKeys);
            }

            if (merged.isEmpty()) {
                FtbQuestExportMod.LOGGER.warn(
                        "{} {} - 0 keys after merge ({}, {} lang file stacks, {} pack layers)",
                        LangExportLog.LANG,
                        langCode,
                        mode,
                        stacks.size(),
                        stats.resourceLayersRead);
                continue;
            }

            Path out = langRoot.resolve(langFile);
            String json = GSON.toJson(merged);
            Files.writeString(out, json);
            languagesWritten++;
            totalBytes += json.length();
            keysPerLanguage = merged.size();
            keysSkipped += stats.keysSkipped;
            duplicateWarnings += stats.duplicateKeyWarnings;
            FtbQuestExportMod.LOGGER.info(
                    "{} {} - {} keys from {} lang file stacks ({} pack layers, {})",
                    LangExportLog.LANG,
                    langCode,
                    merged.size(),
                    stacks.size(),
                    stats.resourceLayersRead,
                    mode);
        }

        if (onlyKeys != null) {
            FtbQuestExportMod.LOGGER.info(
                    "{} closure key filter: {} requested, ~{} keys per language file, {} entries skipped while scanning",
                    LangExportLog.LANG,
                    onlyKeys.size(),
                    keysPerLanguage,
                    keysSkipped);
        }
        if (duplicateWarnings > LangExportLog.DETAIL_FAILURE_LIMIT) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} {} duplicate-key warnings while merging (first {} at DEBUG)",
                    LangExportLog.LANG,
                    duplicateWarnings,
                    LangExportLog.DETAIL_FAILURE_LIMIT);
        }

        return new Result(
                languagesWritten,
                totalBytes,
                duplicateWarnings,
                onlyKeys != null ? onlyKeys.size() : 0,
                keysSkipped,
                keysPerLanguage);
    }

    static boolean matchesLangPath(ResourceLocation location, String langFile) {
        String path = location.getPath();
        return path.equals(langFile) || path.equals("lang/" + langFile) || path.endsWith("/" + langFile);
    }

    static Map<ResourceLocation, List<Resource>> collectLangStacksForNamespaces(
            Minecraft client, String langFile, Set<String> onlyNamespaces) {
        return collectLangStacks(client, langFile, onlyNamespaces);
    }

    private static void mergeFullNamespace(
            Minecraft client,
            String langFile,
            String namespace,
            Map<String, String> merged,
            MergeStats stats) {
        Map<ResourceLocation, List<Resource>> stacks =
                collectLangStacks(client, langFile, Set.of(namespace));
        if (stacks.isEmpty()) {
            FtbQuestExportMod.LOGGER.debug(
                    "{} {} - no lang stacks for namespace {}", LangExportLog.LANG, langFile, namespace);
            return;
        }
        int before = merged.size();
        mergeLangStacksInto(merged, stacks, null, stats);
        FtbQuestExportMod.LOGGER.debug(
                "{} {} - merged full {} lang ({} keys, {} stacks)",
                LangExportLog.LANG,
                langFile,
                namespace,
                merged.size() - before,
                stacks.size());
    }

    static void mergeLangStacksInto(
            Map<String, String> merged,
            Map<ResourceLocation, List<Resource>> stacks,
            Set<String> onlyKeys,
            MergeStats stats) {
        Map<String, ResourceLocation> keyOrigin = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<Resource>> stackEntry : stacks.entrySet()) {
            ResourceLocation location = stackEntry.getKey();
            List<Resource> layers = stackEntry.getValue();
            if (layers == null || layers.isEmpty()) {
                continue;
            }
            for (Resource resource : layers) {
                stats.resourceLayersRead++;
                try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : object.entrySet()) {
                        String key = entry.getKey();
                        if (onlyKeys != null && !onlyKeys.contains(key)) {
                            stats.keysSkipped++;
                            continue;
                        }
                        String value = entry.getValue().getAsString();
                        ResourceLocation previous = keyOrigin.get(key);
                        if (previous != null && !previous.equals(location)) {
                            stats.duplicateKeyWarnings++;
                            LangExportLog.detailFailure(
                                    stats.duplicateKeyWarnings,
                                    "{} duplicate key '{}' from {} (was {})",
                                    LangExportLog.LANG,
                                    key,
                                    location,
                                    previous);
                        }
                        merged.put(key, value);
                        keyOrigin.put(key, location);
                    }
                } catch (Exception e) {
                    FtbQuestExportMod.LOGGER.warn(
                            "{} failed to read {}: {}", LangExportLog.LANG, location, e.getMessage());
                }
            }
        }
    }

    private static Map<ResourceLocation, List<Resource>> collectLangStacks(
            Minecraft client, String langFile, Set<String> onlyNamespaces) {
        Predicate<ResourceLocation> filter = location -> matchesLangPath(location, langFile)
                && !ResourceExportFilter.isExcluded(location)
                && (onlyNamespaces == null || onlyNamespaces.contains(location.getNamespace()));

        Map<ResourceLocation, List<Resource>> stacks = new LinkedHashMap<>();
        appendLangStacks(stacks, client.getResourceManager(), filter);
        var server = client.getSingleplayerServer();
        if (server != null) {
            appendLangStacks(stacks, server.getResourceManager(), filter);
        }
        return stacks;
    }

    private static void appendLangStacks(
            Map<ResourceLocation, List<Resource>> into,
            ResourceManager resourceManager,
            Predicate<ResourceLocation> filter) {
        for (var entry : resourceManager.listResourceStacks("lang", filter).entrySet()) {
            into.compute(
                    entry.getKey(),
                    (id, existing) -> {
                        if (existing == null || existing.isEmpty()) {
                            return new ArrayList<>(entry.getValue());
                        }
                        var combined = new ArrayList<>(existing);
                        combined.addAll(entry.getValue());
                        return combined;
                    });
        }
    }

    private static void logLangPathProbe(Minecraft client, String langFile) {
        int shown = 0;
        StringBuilder sample = new StringBuilder();
        for (ResourceLocation location :
                client.getResourceManager().listResourceStacks("lang", loc -> matchesLangPath(loc, langFile)).keySet()) {
            if (shown++ >= 5) {
                break;
            }
            if (shown > 1) {
                sample.append(", ");
            }
            sample.append(location);
        }
        if (shown > 0) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} client has {} lang file stack(s) for {} but none passed namespace filter; sample: {}",
                    LangExportLog.LANG,
                    shown,
                    langFile,
                    sample);
        } else {
            FtbQuestExportMod.LOGGER.warn(
                    "{} client ResourceManager has no resources under lang/ for {} (assets not loaded?)",
                    LangExportLog.LANG,
                    langFile);
        }
    }
}
