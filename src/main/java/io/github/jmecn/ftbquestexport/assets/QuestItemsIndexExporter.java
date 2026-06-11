package io.github.jmecn.ftbquestexport.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.pojo.ItemsIndexExportResult;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Writes {@code items/index.json} from quest scan (registry ids referenced by tasks/rewards). */
public final class QuestItemsIndexExporter {

    private QuestItemsIndexExporter() {}

    public static ItemsIndexExportResult export(Path outputDir, QuestScanResult scan) throws IOException {
        Map<String, TreeSet<String>> buckets = new TreeMap<>();
        for (String id : scan.getItems()) {
            int colon = id.indexOf(':');
            if (colon <= 0 || colon >= id.length() - 1) {
                continue;
            }
            buckets.computeIfAbsent(id.substring(0, colon), ignored -> new TreeSet<>())
                    .add(id.substring(colon + 1));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.putAll(buckets);

        List<String> fluids = new ArrayList<>(scan.getFluids());
        if (!fluids.isEmpty()) {
            root.put(QuestExportConstants.FLUID_REGISTRY_IDS_KEY, fluids);
        }

        Path out = outputDir.resolve(QuestExportConstants.ITEMS_INDEX_FILE);
        Files.createDirectories(out.getParent());
        String json = QuestExportJson.GSON.toJson(root);
        Files.writeString(out, json, StandardCharsets.UTF_8);

        int itemRefs = scan.getItems().size();
        FtbQuestExportMod.LOGGER.info("[items-index] {} item refs, {} fluid refs -> {}", itemRefs, fluids.size(), out);
        return new ItemsIndexExportResult(itemRefs, fluids.size(), json.length());
    }

    /** Registry ids listed in {@code items/index.json} (all namespaces). */
    public static Set<String> readIndexedItemIds(Path outputDir) throws IOException {
        Path indexPath = outputDir.resolve(QuestExportConstants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String key = entry.getKey();
            if ("schema".equals(key)
                    || QuestExportConstants.FLUID_REGISTRY_IDS_KEY.equals(key)
                    || !entry.getValue().isJsonArray()) {
                continue;
            }
            String namespace = entry.getKey();
            for (JsonElement pathEl : entry.getValue().getAsJsonArray()) {
                if (!pathEl.isJsonPrimitive()) {
                    continue;
                }
                String path = pathEl.getAsString();
                if (path == null || path.isEmpty()) {
                    continue;
                }
                ids.add(path.contains(":") ? path : namespace + ":" + path);
            }
        }
        return Set.copyOf(ids);
    }
}
