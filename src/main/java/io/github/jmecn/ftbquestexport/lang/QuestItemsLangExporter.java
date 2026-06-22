package io.github.jmecn.ftbquestexport.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.model.ItemsLang;
import io.github.jmecn.ftbquestexport.model.ItemsLangEntry;
import io.github.jmecn.ftbquestexport.pojo.ItemsLangExportResult;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class QuestItemsLangExporter {

    private static final int PROGRESS_EVERY = 2_000;

    private QuestItemsLangExporter() {}

    public static ItemsLangExportResult export(Path outputDir, QuestScanResult scan) throws IOException {
        if (scan == null) {
            return ItemsLangExportResult.EMPTY;
        }

        List<String> locales = resolveLocales(outputDir);
        Map<String, String> nameKeys = readNameKeys(outputDir);
        Set<String> fluidIds = new TreeSet<>(scan.getFluids());
        List<String> itemIds = new ArrayList<>(new TreeSet<>(scan.getItems()));
        itemIds.addAll(fluidIds.stream().sorted().toList());

        if (itemIds.isEmpty() || locales.isEmpty()) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} skipped: {} items, {} locales",
                    QuestExportConstants.LOG_PREFIX_ITEMS_LANG,
                    itemIds.size(),
                    locales.size());
            return ItemsLangExportResult.EMPTY;
        }

        Path searchRoot = outputDir.resolve(QuestExportConstants.ITEMS_LANG_DIR);
        Files.createDirectories(searchRoot);

        Map<String, String> enUs = readLangTable(outputDir, QuestExportConstants.FALLBACK_LOCALE);
        int localesWritten = 0;

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            Map<String, String> current = readLangTable(outputDir, normalized);
            Map<String, String> fallback =
                    QuestExportConstants.FALLBACK_LOCALE.equals(normalized) ? Map.of() : enUs;
            RegistryResolver currentResolver = new RegistryResolver(current, fallback, nameKeys);

            long startedAt = System.currentTimeMillis();
            List<ItemsLangEntry> entries = new ArrayList<>(itemIds.size());
            for (int i = 0; i < itemIds.size(); i++) {
                String id = itemIds.get(i);
                entries.add(buildEntry(id, fluidIds, currentResolver));
                int n = i + 1;
                if (n % PROGRESS_EVERY == 0) {
                    FtbQuestExportMod.LOGGER.info(
                            "{} {}: {}/{} ({} ms)",
                            QuestExportConstants.LOG_PREFIX_ITEMS_LANG,
                            normalized,
                            n,
                            itemIds.size(),
                            System.currentTimeMillis() - startedAt);
                }
            }

            ItemsLang payload = ItemsLang.of(normalized, entries);
            Path out = searchRoot.resolve(normalized + ".json");
            Files.writeString(out, QuestExportJson.GSON.toJson(payload), StandardCharsets.UTF_8);
            localesWritten++;
            FtbQuestExportMod.LOGGER.info(
                    "{} {}: {} items -> {} ({} ms)",
                    QuestExportConstants.LOG_PREFIX_ITEMS_LANG,
                    normalized,
                    itemIds.size(),
                    out,
                    System.currentTimeMillis() - startedAt);
        }

        return new ItemsLangExportResult(localesWritten, itemIds.size(), locales);
    }

    private static ItemsLangEntry buildEntry(
            String id, Set<String> fluidIds, RegistryResolver currentResolver) {
        String kind = resolveRegistryKind(id, fluidIds, currentResolver);
        String label = currentResolver.translateRegistry(id, kind);
        return new ItemsLangEntry(id, label);
    }

    private static String resolveRegistryKind(
            String registryId, Set<String> fluidRegistryIds, RegistryResolver resolver) {
        if (fluidRegistryIds != null && fluidRegistryIds.contains(registryId)) {
            return "fluid";
        }
        if (fluidRegistryIds == null || fluidRegistryIds.isEmpty()) {
            return inferFluidKindWhenIndexMissing(registryId, resolver);
        }
        return "item";
    }

    private static String inferFluidKindWhenIndexMissing(String registryId, RegistryResolver resolver) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        String namespace = RegistryResolver.registryNamespace(bare);
        if (!RegistryResolver.COMPOSED_FIRST_NAMESPACES.contains(namespace)) {
            return "item";
        }
        int colon = bare.indexOf(':');
        if (colon <= 0 || colon >= bare.length() - 1) {
            return "item";
        }
        String path = bare.substring(colon + 1);
        if (path.endsWith("_bucket")) {
            return "item";
        }
        String asItem = resolver.translateRegistry(registryId, "item");
        if (!asItem.equals(bare)) {
            return "item";
        }
        String asFluid = resolver.translateRegistry(registryId, "fluid");
        return asFluid.equals(bare) ? "item" : "fluid";
    }

    private static List<String> resolveLocales(Path outputDir) throws IOException {
        List<String> configured = QuestExportLanguages.asList();
        if (!configured.isEmpty()) {
            return configured.stream().map(QuestItemsLangExporter::normalizeLocale).distinct().sorted().toList();
        }
        Path langDir = outputDir.resolve("lang");
        if (!Files.isDirectory(langDir)) {
            return List.of(QuestExportConstants.FALLBACK_LOCALE);
        }
        try (Stream<Path> stream = Files.list(langDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> normalizeLocale(name.substring(0, name.length() - 5)))
                    .sorted()
                    .toList();
        }
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return QuestExportConstants.FALLBACK_LOCALE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Map<String, String> readLangTable(Path outputDir, String locale) throws IOException {
        Path langPath = outputDir.resolve("lang").resolve(locale + ".json");
        if (!Files.isRegularFile(langPath)) {
            return Map.of();
        }
        JsonObject root = JsonParser.parseString(Files.readString(langPath)).getAsJsonObject();
        Map<String, String> table = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                table.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return table;
    }

    private static Map<String, String> readNameKeys(Path outputDir) throws IOException {
        Path path = outputDir.resolve(QuestExportConstants.ITEM_NAME_KEYS_FILE);
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        if (!root.has("items") || !root.get("items").isJsonObject()) {
            return Map.of();
        }
        Map<String, String> keys = new TreeMap<>();
        JsonObject items = root.getAsJsonObject("items");
        for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                keys.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return keys;
    }
}
