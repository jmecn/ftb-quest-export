package io.github.jmecn.ftbquestexport.lang;

import com.google.gson.JsonParseException;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.TextComponentParser;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Resolves FTB Quests rich text to plain strings for search indexing. */
public final class QuestPlainText {

    private static final Pattern COLLAPSE_WS = Pattern.compile("\\s+");

    private QuestPlainText() {}

    public static String textToPlain(Map<String, String> lang, String text) {
        return textToPlain(lang, null, text);
    }

    public static String textToPlain(Map<String, String> lang, Map<String, String> fallbackLang, String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && !isJsonText(trimmed)) {
            String key = trimmed.substring(1, trimmed.length() - 1);
            if (key.startsWith("image:") || Quest.PAGEBREAK_CODE.equals(key)) {
                return "";
            }
            String value = resolveLangValue(lang, fallbackLang, key);
            if (value != null) {
                return textToPlain(lang, fallbackLang, value);
            }
        }

        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String plain = lineToPlain(lang, fallbackLang, line);
            if (!plain.isEmpty()) {
                if (!out.isEmpty()) {
                    out.append(' ');
                }
                out.append(plain);
            }
        }
        return normalizeWhitespace(out.toString());
    }

    public static String lineToPlain(Map<String, String> lang, String line) {
        return lineToPlain(lang, null, line);
    }

    public static String lineToPlain(Map<String, String> lang, Map<String, String> fallbackLang, String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String trimmed = line.trim();
        if (Quest.PAGEBREAK_CODE.equals(trimmed) || trimmed.startsWith("{image:")) {
            return "";
        }

        if (isJsonText(trimmed)) {
            try {
                Component component = Component.Serializer.fromJson(trimmed);
                return normalizeWhitespace(component != null ? component.getString() : "");
            } catch (JsonParseException ignored) {
                return "";
            }
        }

        Component parsed = TextComponentParser.parse(line, inner -> substituteComponent(lang, fallbackLang, inner));
        return normalizeWhitespace(parsed.getString());
    }

    private static String resolveLangValue(
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            String key) {
        String value = lang.get(key);
        if (value != null) {
            return value;
        }
        if (fallbackLang != null) {
            return fallbackLang.get(key);
        }
        return null;
    }

    private static Component substituteComponent(
            Map<String, String> lang,
            Map<String, String> fallbackLang,
            String inner) {
        if (inner.isEmpty() || inner.startsWith("@")) {
            return Component.empty();
        }
        if (inner.indexOf(':') != -1) {
            Map<String, String> props = StringUtils.splitProperties(inner);
            if (props.containsKey("image")) {
                return Component.empty();
            }
            if (props.containsKey("open_url") && props.containsKey("text")) {
                return Component.literal(lineToPlain(lang, fallbackLang, props.get("text")));
            }
            return Component.empty();
        }
        String value = resolveLangValue(lang, fallbackLang, inner);
        if (value != null) {
            return Component.literal(textToPlain(lang, fallbackLang, value));
        }
        return Component.empty();
    }

    static boolean isJsonText(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        char first = trimmed.charAt(0);
        if (first != '{' && first != '[') {
            return false;
        }
        try {
            Component.Serializer.fromJson(trimmed);
            return true;
        } catch (JsonParseException e) {
            return false;
        }
    }

    public static String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return COLLAPSE_WS.matcher(text.trim()).replaceAll(" ");
    }

    public static String toSearchContent(String plain) {
        return normalizeWhitespace(plain).toLowerCase(Locale.ROOT);
    }
}
