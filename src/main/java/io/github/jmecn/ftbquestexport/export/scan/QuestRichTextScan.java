package io.github.jmecn.ftbquestexport.export.scan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.ftbquestexport.export.resources.ResourceExportFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects texture refs from FTB Quests rich text ({@code {image:mod:textures/... width:100 ...}}),
 * mirroring {@code ClientTextComponentUtils} / {@code ImageComponent}.
 */
public final class QuestRichTextScan {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

    private static final Pattern IMAGE_BLOCK = Pattern.compile("\\{image:([^}]+)\\}", Pattern.CASE_INSENSITIVE);

    private QuestRichTextScan() {}

    public static void collectFromText(QuestScanResult scan, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = IMAGE_BLOCK.matcher(text);
        while (matcher.find()) {
            String ref = imageRefFromProperties(matcher.group(1));
            if (ref != null) {
                scan.addTexture(ref);
            }
        }
    }

    public static void collectFromLines(QuestScanResult scan, java.util.List<String> lines) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            collectFromText(scan, line);
        }
    }

    /** Scan resolved lang strings for embedded {@code {image:...}} before texture closure. */
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
            LOGGER.info(
                    "[scan] rich-text lang closure added {} texture ref(s) from {} lang values ({} locales)",
                    added,
                    values.size(),
                    QuestExportLanguages.closureLanguages(client).size());
        }
    }

    static String imageRefFromProperties(String props) {
        if (props == null || props.isBlank()) {
            return null;
        }
        String image = splitProperties(props).get("image");
        if (image == null || image.isBlank()) {
            return null;
        }
        return normalizeTextureRef(image);
    }

    static Map<String, String> splitProperties(String input) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String token : input.split(" ")) {
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon < 0) {
                map.put(token, "");
            } else {
                map.put(token.substring(0, colon), token.substring(colon + 1).replace("%20", " "));
            }
        }
        return map;
    }

    static String normalizeTextureRef(String ref) {
        String trimmed = ref.trim();
        if (!trimmed.contains(":")) {
            return null;
        }
        return trimmed.endsWith(".png") ? trimmed : trimmed + ".png";
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
