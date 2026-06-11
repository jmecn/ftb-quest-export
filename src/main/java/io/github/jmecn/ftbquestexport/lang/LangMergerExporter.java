package io.github.jmecn.ftbquestexport.lang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.assets.QuestAssetExporter;
import io.github.jmecn.ftbquestexport.pojo.LangExportResult;
import io.github.jmecn.ftbquestexport.pojo.LangMergeStats;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
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

    private LangMergerExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_LANG_EXPORT);
    }

    public static LangExportResult exportTo(Path langRoot, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
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

        for (String langCode : languages) {
            String langFile = langCode + ".json";
            Map<String, String> merged = new TreeMap<>();
            Map<ResourceLocation, List<Resource>> stacks = collectLangStacks(client, langFile, onlyNamespaces);
            LangMergeStats stats = new LangMergeStats();

            if (stacks.isEmpty()) {
                FtbQuestExportMod.LOGGER.warn(
                        "{} {} - no mod lang files matched (namespaces={})",
                        QuestExportConstants.LOG_PREFIX_LANG,
                        langCode,
                        onlyNamespaces == null ? "all" : onlyNamespaces);
                logLangPathProbe(client, langFile);
            } else {
                mergeLangStacksInto(merged, stacks, onlyKeys, stats);
            }

            mergeFullNamespace(client, langFile, QuestExportConstants.FTBQUESTS_NAMESPACE, merged, stats);

            if (onlyKeys != null) {
                VanillaMinecraftLangSupplement.supplement(merged, client, langCode, onlyKeys);
            }

            if (merged.isEmpty()) {
                FtbQuestExportMod.LOGGER.warn(
                        "{} {} - 0 keys after merge ({} lang file stacks, {} pack layers)",
                        QuestExportConstants.LOG_PREFIX_LANG,
                        langCode,
                        stacks.size(),
                        stats.resourceLayersRead);
                continue;
            }

            Path out = langRoot.resolve(langFile);
            String json = QuestExportJson.GSON.toJson(merged);
            Files.writeString(out, json);
            languagesWritten++;
            totalBytes += json.length();
            keysPerLanguage = merged.size();
            keysSkipped += stats.keysSkipped;
            duplicateWarnings += stats.duplicateKeyWarnings;
            FtbQuestExportMod.LOGGER.info(
                    "{} {} - {} keys from {} lang file stacks ({} pack layers)",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    langCode,
                    merged.size(),
                    stacks.size(),
                    stats.resourceLayersRead);
        }

        if (onlyKeys != null) {
            FtbQuestExportMod.LOGGER.info(
                    "{} lang key filter: {} requested, ~{} keys per language file, {} entries skipped while scanning",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    onlyKeys.size(),
                    keysPerLanguage,
                    keysSkipped);
        }
        if (duplicateWarnings > QuestExportConstants.LOG_DETAIL_FAILURE_LIMIT) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} {} duplicate-key warnings while merging (first {} at DEBUG)",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    duplicateWarnings,
                    QuestExportConstants.LOG_DETAIL_FAILURE_LIMIT);
        }

        return new LangExportResult(
                languagesWritten,
                totalBytes,
                duplicateWarnings,
                onlyKeys != null ? onlyKeys.size() : 0,
                keysSkipped,
                keysPerLanguage);
    }

    public static boolean matchesLangPath(ResourceLocation location, String langFile) {
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
            LangMergeStats stats) {
        Map<ResourceLocation, List<Resource>> stacks =
                collectLangStacks(client, langFile, Set.of(namespace));
        if (stacks.isEmpty()) {
            FtbQuestExportMod.LOGGER.debug(
                    "{} {} - no lang stacks for namespace {}",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    langFile,
                    namespace);
            return;
        }
        int before = merged.size();
        mergeLangStacksInto(merged, stacks, null, stats);
        FtbQuestExportMod.LOGGER.debug(
                "{} {} - merged full {} lang ({} keys, {} stacks)",
                QuestExportConstants.LOG_PREFIX_LANG,
                langFile,
                namespace,
                merged.size() - before,
                stacks.size());
    }

    static void mergeLangStacksInto(
            Map<String, String> merged,
            Map<ResourceLocation, List<Resource>> stacks,
            Set<String> onlyKeys,
            LangMergeStats stats) {
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
                            logDetailFailure(
                                    stats.duplicateKeyWarnings,
                                    "{} duplicate key '{}' from {} (was {})",
                                    QuestExportConstants.LOG_PREFIX_LANG,
                                    key,
                                    location,
                                    previous);
                        }
                        merged.put(key, value);
                        keyOrigin.put(key, location);
                    }
                } catch (Exception e) {
                    FtbQuestExportMod.LOGGER.warn(
                            "{} failed to read {}: {}",
                            QuestExportConstants.LOG_PREFIX_LANG,
                            location,
                            e.getMessage());
                }
            }
        }
    }

    private static Map<ResourceLocation, List<Resource>> collectLangStacks(
            Minecraft client, String langFile, Set<String> onlyNamespaces) {
        Predicate<ResourceLocation> filter = location -> matchesLangPath(location, langFile)
                && !QuestAssetExporter.isExcluded(location)
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
                    QuestExportConstants.LOG_PREFIX_LANG,
                    shown,
                    langFile,
                    sample);
        } else {
            FtbQuestExportMod.LOGGER.warn(
                    "{} client ResourceManager has no resources under lang/ for {} (assets not loaded?)",
                    QuestExportConstants.LOG_PREFIX_LANG,
                    langFile);
        }
    }

    private static void logDetailFailure(int failureCount, String message, Object... args) {
        if (failureCount > QuestExportConstants.LOG_DETAIL_FAILURE_LIMIT) {
            return;
        }
        if (Boolean.getBoolean(QuestExportConstants.LOG_DETAIL_FAILURES)) {
            FtbQuestExportMod.LOGGER.warn(message, args);
        } else if (FtbQuestExportMod.LOGGER.isDebugEnabled()) {
            FtbQuestExportMod.LOGGER.debug(message, args);
        }
    }
}
