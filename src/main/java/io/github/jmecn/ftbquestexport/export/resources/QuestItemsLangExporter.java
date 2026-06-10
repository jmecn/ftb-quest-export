package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.export.lang.RegistryLabelResolver;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

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

/**
 * Precomputes {@code items-lang/<locale>.json} for quest bundle item labels
 * (schema version 2: {@code id}, {@code label}, {@code haystack} per item).
 */
public final class QuestItemsLangExporter {

    public static final String ITEMS_LANG_DIR = "items-lang";
    public static final String COMPOSE_LANG_DIR = "compose-lang";
    public static final String DEFAULT_LANGUAGE = "en_us";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PROGRESS_EVERY = 5000;

    private QuestItemsLangExporter() {}

    public record Result(int localeCount, int itemCount, List<String> locales) {
        static final Result EMPTY = new Result(0, 0, List.of());
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("quest.skipItemsLangExport");
    }

    public static Result export(Path outputDir) throws IOException {
        List<String> languages = QuestExportLanguages.asList();
        if (languages.isEmpty()) {
            Path langDir = outputDir.resolve("lang");
            if (Files.isDirectory(langDir)) {
                try (var stream = Files.list(langDir)) {
                    languages = stream
                            .filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .filter(name -> name.endsWith(".json"))
                            .map(name -> normalizeLocale(name.substring(0, name.length() - 5)))
                            .sorted()
                            .toList();
                }
            }
        }
        return export(outputDir, languages);
    }

    public static Result export(Path outputDir, List<String> languages) throws IOException {
        List<String> itemIds = List.copyOf(readItemIds(outputDir));
        Set<String> fluidRegistryIds = readFluidRegistryIds(outputDir);
        List<String> locales = resolveLocales(outputDir, languages);
        if (itemIds.isEmpty() || locales.isEmpty()) {
            FtbQuestExportMod.LOGGER.warn("[items-lang] skipped: {} items, {} locales", itemIds.size(), locales.size());
            return Result.EMPTY;
        }

        Path searchRoot = outputDir.resolve(ITEMS_LANG_DIR);
        Files.createDirectories(searchRoot);

        Map<String, String> enUs = readLangTable(outputDir, DEFAULT_LANGUAGE);
        Map<String, String> nameKeysByRegistryId = QuestItemNameKeysExporter.readNameKeys(outputDir);
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            String normalized = normalizeLocale(locale);
            FtbQuestExportMod.LOGGER.info("[items-lang] {}: building {} items ...", normalized, itemIds.size());
            long startedAt = System.currentTimeMillis();

            Map<String, String> current = readLangTable(outputDir, normalized);
            Map<String, String> fallback = DEFAULT_LANGUAGE.equals(normalized) ? Map.of() : enUs;
            RegistryLabelResolver currentResolver =
                    new RegistryLabelResolver(current, fallback, nameKeysByRegistryId);
            JsonArray items = new JsonArray();
            for (int i = 0; i < itemIds.size(); i++) {
                String id = itemIds.get(i);
                String kind = fluidRegistryIds.contains(id) ? "fluid" : "item";
                String label = currentResolver.translateRegistry(id, kind);
                JsonObject row = new JsonObject();
                row.addProperty("id", id);
                row.addProperty("label", label);
                row.addProperty("haystack", (id + " " + label).toLowerCase(Locale.ROOT));
                items.add(row);

                int n = i + 1;
                if (n % PROGRESS_EVERY == 0) {
                    FtbQuestExportMod.LOGGER.info(
                            "[items-lang] {}: {}/{} ({} ms)",
                            normalized,
                            n,
                            itemIds.size(),
                            System.currentTimeMillis() - startedAt);
                }
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("schema", 2);
            payload.addProperty("locale", normalized);
            payload.addProperty("itemCount", itemIds.size());
            payload.add("items", items);

            Path out = searchRoot.resolve(normalized + ".json");
            Files.writeString(out, GSON.toJson(payload) + "\n", StandardCharsets.UTF_8);
            writtenLocales.add(normalized);
            FtbQuestExportMod.LOGGER.info(
                    "[items-lang] {}: {} items ({} ms) -> {}",
                    normalized,
                    itemIds.size(),
                    System.currentTimeMillis() - startedAt,
                    out);
        }

        return new Result(writtenLocales.size(), itemIds.size(), List.copyOf(writtenLocales));
    }

    /** Removes temporary {@link #COMPOSE_LANG_DIR} after items-lang is written. */
    public static void deleteComposeLang(Path outputDir) {
        Path composeRoot = outputDir.resolve(COMPOSE_LANG_DIR);
        if (!java.nio.file.Files.isDirectory(composeRoot)) {
            return;
        }
        try (var walk = java.nio.file.Files.walk(composeRoot)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    java.nio.file.Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // non-fatal
                }
            });
        } catch (IOException ignored) {
            // non-fatal
        }
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static List<String> resolveLocales(Path outputDir, List<String> languages) throws IOException {
        if (languages != null && !languages.isEmpty()) {
            return languages.stream().map(QuestItemsLangExporter::normalizeLocale).distinct().sorted().toList();
        }
        Path langDir = outputDir.resolve("lang");
        if (!Files.isDirectory(langDir)) {
            return List.of(DEFAULT_LANGUAGE);
        }
        try (var stream = Files.list(langDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> normalizeLocale(name.substring(0, name.length() - 5)))
                    .sorted()
                    .toList();
        }
    }

    private static Map<String, String> readLangTable(Path outputDir, String locale) throws IOException {
        Path composePath = outputDir.resolve(COMPOSE_LANG_DIR).resolve(locale + ".json");
        if (Files.isRegularFile(composePath)) {
            return parseLangFile(composePath);
        }
        Path langPath = outputDir.resolve("lang").resolve(locale + ".json");
        if (!Files.isRegularFile(langPath)) {
            return Map.of();
        }
        return parseLangFile(langPath);
    }

    private static Map<String, String> parseLangFile(Path langPath) throws IOException {
        JsonObject object = JsonParser.parseString(Files.readString(langPath)).getAsJsonObject();
        Map<String, String> table = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                table.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return table;
    }

    private static Set<String> readItemIds(Path outputDir) throws IOException {
        return QuestItemsIndexExporter.readIndexedItemIds(outputDir);
    }

    private static Set<String> readFluidRegistryIds(Path outputDir) throws IOException {
        Path indexPath = outputDir.resolve(QuestItemsIndexExporter.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        String fluidKey = QuestItemsIndexExporter.FLUID_REGISTRY_IDS_KEY;
        if (!index.has(fluidKey) || !index.get(fluidKey).isJsonArray()) {
            return Set.of();
        }
        Set<String> ids = new TreeSet<>();
        for (JsonElement element : index.getAsJsonArray(fluidKey)) {
            if (element.isJsonPrimitive()) {
                String id = element.getAsString();
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return Set.copyOf(ids);
    }
}
