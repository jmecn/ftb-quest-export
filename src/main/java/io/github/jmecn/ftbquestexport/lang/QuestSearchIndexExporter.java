package io.github.jmecn.ftbquestexport.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.pojo.QuestSearchIndexExportResult;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import io.github.jmecn.ftbquestexport.model.SearchIndexFile;
import io.github.jmecn.ftbquestexport.model.SearchIndexQuestRow;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class QuestSearchIndexExporter {

    private QuestSearchIndexExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_SEARCH_INDEX_EXPORT);
    }

    public static QuestSearchIndexExportResult export(
            Path outputDir,
            Map<String, ChapterData> chapters) throws IOException {
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
            List<SearchIndexQuestRow> rows = buildRows(chapters, lang, fallbackLang, nameKeys);
            questsWritten = Math.max(questsWritten, rows.size());

            SearchIndexFile payload = SearchIndexFile.of(locale, rows);

            Path out = searchRoot.resolve(locale + ".json");
            Files.writeString(out, QuestExportJson.GSON.toJson(payload), StandardCharsets.UTF_8);
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

    private static List<SearchIndexQuestRow> buildRows(
            Map<String, ChapterData> chapters,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Map<String, String> nameKeys) {
        List<SearchIndexQuestRow> rows = new ArrayList<>();
        for (Map.Entry<String, ChapterData> chapterEntry : chapters.entrySet()) {
            String chapterFilename = chapterEntry.getKey();
            ChapterData chapterData = chapterEntry.getValue();
            String chapterTitle = resolveChapterTitle(chapterData, chapterFilename, lang, fallbackLang);
            if (chapterData.quests() == null) {
                continue;
            }
            for (QuestNode quest : chapterData.quests()) {
                if (quest.id() == null || quest.id().isBlank()) {
                    continue;
                }

                String title = resolveQuestTitle(quest, lang, fallbackLang, nameKeys);

                StringBuilder plain = new StringBuilder();
                appendField(plain, lang, fallbackLang, quest.title());
                appendField(plain, lang, fallbackLang, quest.subtitle());
                appendDescription(plain, lang, fallbackLang, quest.description());

                if (quest.title() == null && quest.titleItem() != null) {
                    appendField(plain, lang, fallbackLang, resolveItemLabel(lang, fallbackLang, nameKeys, quest.titleItem()));
                }

                String content = QuestPlainText.toSearchContent(plain.toString());
                if (content.isEmpty()) {
                    continue;
                }

                SearchIndexQuestRow row = new SearchIndexQuestRow(
                        quest.id(),
                        chapterFilename,
                        chapterTitle,
                        title.isEmpty() ? null : title,
                        content);
                rows.add(row);
            }
        }
        return rows;
    }

    private static String resolveChapterTitle(
            ChapterData chapterData,
            String chapterFilename,
            Map<String, String> lang,
            Map<String, String> fallbackLang) {
        String raw = chapterData.title();
        if (raw == null || raw.isBlank()) {
            return chapterFilename;
        }
        String plain = QuestPlainText.textToPlain(lang, fallbackLang, raw);
        return plain.isEmpty() ? chapterFilename : plain;
    }

    private static String resolveQuestTitle(
            QuestNode quest,
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            Map<String, String> nameKeys) {
        String raw = quest.title();
        if (raw != null && !raw.isBlank()) {
            return QuestPlainText.textToPlain(lang, fallbackLang, raw);
        }
        if (quest.titleItem() != null) {
            return resolveItemLabel(lang, fallbackLang, nameKeys, quest.titleItem());
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
            List<String> description) {
        if (description == null) {
            return;
        }
        for (String text : description) {
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

    private static List<String> resolveLocales(Path outputDir) throws IOException {
        List<String> configured = QuestExportLanguages.asList();
        if (!configured.isEmpty()) {
            return configured.stream().map(QuestSearchIndexExporter::normalizeLocale).distinct().sorted().toList();
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
