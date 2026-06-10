package io.github.jmecn.ftbquestexport.export.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import io.github.jmecn.ftbquestexport.export.QuestExportJson;
import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.export.pojo.QuestSearchIndexExportResult;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Precomputes {@code search-index/<locale>.json} for quest substring search in QuestBook-React.
 */
public final class QuestSearchIndexExporter {

    private QuestSearchIndexExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_SEARCH_INDEX_EXPORT);
    }

    public static QuestSearchIndexExportResult export(
            Path outputDir,
            Map<String, Map<String, Object>> chapters) throws IOException {
        List<String> locales = resolveLocales(outputDir);
        if (locales.isEmpty()) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} skipped: no locale files under lang/",
                    QuestExportConstants.LOG_PREFIX_SEARCH_INDEX);
            return QuestSearchIndexExportResult.EMPTY;
        }

        Map<String, String> nameKeys = readNameKeys(outputDir);
        Map<String, String> enUsLang = readLangTable(outputDir, QuestExportConstants.FALLBACK_LOCALE);
        Path searchRoot = outputDir.resolve(QuestExportConstants.SEARCH_INDEX_DIR);
        Files.createDirectories(searchRoot);

        int questsWritten = 0;
        List<String> writtenLocales = new ArrayList<>();

        for (String locale : locales) {
            Map<String, String> lang = readLangTable(outputDir, locale);
            Map<String, String> fallbackLang = QuestExportConstants.FALLBACK_LOCALE.equals(locale)
                    ? null
                    : enUsLang;
            List<Map<String, String>> rows = buildRows(chapters, lang, fallbackLang, nameKeys);
            questsWritten = Math.max(questsWritten, rows.size());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("schema", 2);
            payload.put("locale", locale);
            payload.put("questCount", rows.size());
            payload.put("quests", rows);

            Path out = searchRoot.resolve(locale + ".json");
            Files.writeString(out, QuestExportJson.PRETTY.toJson(payload) + "\n", StandardCharsets.UTF_8);
            writtenLocales.add(locale);
            FtbQuestExportMod.LOGGER.info(
                    "{} {}: {} quests -> {}",
                    QuestExportConstants.LOG_PREFIX_SEARCH_INDEX,
                    locale,
                    rows.size(),
                    out);
        }

        return new QuestSearchIndexExportResult(writtenLocales.size(), questsWritten, writtenLocales);
    }

    private static List<Map<String, String>> buildRows(
            Map<String, Map<String, Object>> chapters,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Map<String, String> nameKeys) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> chapterEntry : chapters.entrySet()) {
            String chapterFilename = chapterEntry.getKey();
            Map<String, Object> chapter = chapterEntry.getValue();
            String chapterTitle = resolveChapterTitle(chapter, chapterFilename, lang, fallbackLang);
            Object questsRaw = chapter.get("quests");
            if (!(questsRaw instanceof List<?> questList)) {
                continue;
            }
            for (Object questObj : questList) {
                if (!(questObj instanceof Map<?, ?> quest)) {
                    continue;
                }
                String id = stringValue(quest.get("id"));
                if (id == null || id.isBlank()) {
                    continue;
                }

                String title = resolveQuestTitle(quest, lang, fallbackLang, nameKeys);

                StringBuilder plain = new StringBuilder();
                appendField(plain, lang, fallbackLang, stringValue(quest.get("title")));
                appendField(plain, lang, fallbackLang, stringValue(quest.get("subtitle")));
                appendDescription(plain, lang, fallbackLang, quest.get("description"));

                if (!quest.containsKey("title") && quest.get("titleItem") instanceof String titleItem) {
                    appendField(plain, lang, fallbackLang, resolveItemLabel(lang, fallbackLang, nameKeys, titleItem));
                }

                String content = QuestPlainText.toSearchContent(plain.toString());
                if (content.isEmpty()) {
                    continue;
                }

                Map<String, String> row = new LinkedHashMap<>();
                row.put("id", id);
                row.put("chapter", chapterFilename);
                row.put("chapterTitle", chapterTitle);
                if (!title.isEmpty()) {
                    row.put("title", title);
                }
                row.put("content", content);
                rows.add(row);
            }
        }
        return rows;
    }

    private static String resolveChapterTitle(
            Map<String, Object> chapter,
            String chapterFilename,
            Map<String, String> lang,
            Map<String, String> fallbackLang) {
        String raw = stringValue(chapter.get("title"));
        if (raw == null || raw.isBlank()) {
            return chapterFilename;
        }
        String plain = QuestPlainText.textToPlain(lang, fallbackLang, raw);
        return plain.isEmpty() ? chapterFilename : plain;
    }

    private static String resolveQuestTitle(
            Map<?, ?> quest,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Map<String, String> nameKeys) {
        String raw = stringValue(quest.get("title"));
        if (raw != null && !raw.isBlank()) {
            return QuestPlainText.textToPlain(lang, fallbackLang, raw);
        }
        if (quest.get("titleItem") instanceof String titleItem) {
            return resolveItemLabel(lang, fallbackLang, nameKeys, titleItem);
        }
        return "";
    }

    private static void appendField(
            StringBuilder target,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String plain = QuestPlainText.textToPlain(lang, fallbackLang, raw);
        if (plain.isEmpty()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(' ');
        }
        target.append(plain);
    }

    private static void appendDescription(
            StringBuilder target,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Object description) {
        if (description == null) {
            return;
        }
        if (description instanceof String text) {
            appendField(target, lang, fallbackLang, text);
            return;
        }
        if (description instanceof List<?> lines) {
            for (Object line : lines) {
                if (line instanceof String text) {
                    String plain = QuestPlainText.lineToPlain(lang, fallbackLang, text);
                    if (plain.isEmpty()) {
                        continue;
                    }
                    if (!target.isEmpty()) {
                        target.append(' ');
                    }
                    target.append(plain);
                }
            }
        }
    }

    private static String resolveItemLabel(
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Map<String, String> nameKeys,
            String itemId) {
        String bare = RegistryLangKeys.normalizeRegistryId(itemId);
        String descKey = nameKeys.get(bare);
        if (descKey != null) {
            String fromDesc = lang.get(descKey);
            if (fromDesc == null || fromDesc.isBlank()) {
                fromDesc = fallbackLang != null ? fallbackLang.get(descKey) : null;
            }
            if (fromDesc != null && !fromDesc.isBlank()) {
                return QuestPlainText.textToPlain(lang, fallbackLang, fromDesc);
            }
        }
        String itemKey = RegistryLangKeys.itemKey(bare);
        String fromItem = lang.get(itemKey);
        if (fromItem == null || fromItem.isBlank()) {
            fromItem = fallbackLang != null ? fallbackLang.get(itemKey) : null;
        }
        if (fromItem != null && !fromItem.isBlank()) {
            return QuestPlainText.textToPlain(lang, fallbackLang, fromItem);
        }
        String segment = bare.contains(":") ? bare.substring(bare.indexOf(':') + 1) : bare;
        return segment.replace('_', ' ');
    }

    private static String stringValue(Object value) {
        return value instanceof String s ? s : null;
    }

    private static List<String> resolveLocales(Path outputDir) throws IOException {
        List<String> configured = QuestExportLanguages.asList();
        if (!configured.isEmpty()) {
            return configured.stream().map(QuestSearchIndexExporter::normalizeLocale).distinct().sorted().toList();
        }
        Path langDir = outputDir.resolve("lang");
        if (!Files.isDirectory(langDir)) {
            return List.of(QuestExportConstants.FALLBACK_LOCALE);
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

    private static String normalizeLocale(String locale) {
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
