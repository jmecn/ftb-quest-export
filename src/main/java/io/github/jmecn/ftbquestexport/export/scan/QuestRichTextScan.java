package io.github.jmecn.ftbquestexport.export.scan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.TextComponentParser;
import dev.ftb.mods.ftblibrary.util.client.ImageComponent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.util.TextUtils;
import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.export.assets.ResourceExportFilter;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Collects lang keys and texture refs from FTB Quests rich text.
 */
public final class QuestRichTextScan {

    private QuestRichTextScan() {}

    public static void collectFromText(QuestScanResult scan, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\n", -1)) {
            collectFromLine(scan, line);
        }
    }

    /** Scan resolved lang strings for embedded rich text before texture closure. */
    public static void enrichFromLangClosure(Minecraft client, QuestScanResult scan) {
        Set<String> wanted = scan.getLangKeys();
        if (wanted.isEmpty()) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String lang : QuestExportLanguages.closureLanguages(client)) {
            String langFile = lang + ".json";
            readLangValues(client.getResourceManager(), langFile, wanted).forEach(values::putIfAbsent);
            var server = client.getSingleplayerServer();
            if (server != null) {
                readLangValues(server.getResourceManager(), langFile, wanted).forEach(values::putIfAbsent);
            }
        }
        int before = scan.getTextures().size();
        for (String value : values.values()) {
            collectFromText(scan, value);
        }
        int added = scan.getTextures().size() - before;
        if (added > 0) {
            FtbQuestExportMod.LOGGER.info(
                    "[scan] rich-text lang closure added {} texture ref(s) from {} lang values ({} locales)",
                    added,
                    values.size(),
                    QuestExportLanguages.closureLanguages(client).size());
        }
    }

    private static void collectFromLine(QuestScanResult scan, String line) {
        if (line == null || line.isBlank() || Quest.PAGEBREAK_CODE.equals(line.trim())) {
            return;
        }
        walkComponentTree(TextUtils.parseRawText(line), scan);
        if (!isJsonText(line)) {
            TextComponentParser.parse(line, inner -> {
                recordLangSubstitute(scan, inner);
                return Component.empty();
            });
        }
    }

    /**
     * {@link TextUtils} has no public JSON probe; it uses a private regex then
     * {@link Component.Serializer#fromJson}. We probe with the same deserializer so
     * {@link TextComponentParser} is skipped only on lines {@link TextUtils#parseRawText} treats as JSON.
     */
    private static boolean isJsonText(String line) {
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

    private static void recordLangSubstitute(QuestScanResult scan, String inner) {
        if (inner.isEmpty() || inner.startsWith("@")) {
            return;
        }
        if (inner.indexOf(':') != -1) {
            Map<String, String> props = StringUtils.splitProperties(inner);
            if (props.containsKey("image") || props.containsKey("open_url")) {
                return;
            }
            return;
        }
        scan.addLangKey(inner);
    }

    private static void walkComponentTree(Component root, QuestScanResult scan) {
        walkComponentNode(root, scan);
    }

    private static void walkComponentNode(Component component, QuestScanResult scan) {
        ComponentContents contents = component.getContents();
        if (contents instanceof ImageComponent image) {
            addImageTexture(scan, image);
        } else if (contents instanceof TranslatableContents translatable) {
            scan.addLangKey(translatable.getKey());
        }
        for (Component sibling : component.getSiblings()) {
            walkComponentNode(sibling, scan);
        }
    }

    private static void addImageTexture(QuestScanResult scan, ImageComponent image) {
        if (image.image == null || image.image.isEmpty()) {
            return;
        }
        String ref = image.image.toString();
        if (ref.isBlank() || !ref.contains(":")) {
            return;
        }
        scan.addTexture(ref.endsWith(".png") ? ref : ref + ".png");
    }

    private static Map<String, String> readLangValues(
            ResourceManager rm,
            String langFile,
            Set<String> wanted) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> hits = rm.listResources(
                "lang",
                loc -> matchesLangPath(loc, langFile) && !ResourceExportFilter.isExcluded(loc));
        for (var entry : hits.entrySet()) {
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : wanted) {
                    if (values.containsKey(key) || !root.has(key) || !root.get(key).isJsonPrimitive()) {
                        continue;
                    }
                    values.put(key, root.get(key).getAsString());
                }
            } catch (Exception ignored) {
                // non-fatal
            }
        }
        return values;
    }

    private static boolean matchesLangPath(ResourceLocation loc, String langFile) {
        String path = loc.getPath();
        return path.equals(langFile) || path.equals("lang/" + langFile) || path.endsWith("/" + langFile);
    }
}
