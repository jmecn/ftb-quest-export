package io.github.jmecn.ftbquestexport.export.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

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

/** Writes {@code items/index.json} from quest scan closure (registry ids referenced by tasks/rewards). */
public final class QuestItemsIndexExporter {

    public static final String ITEMS_INDEX_FILE = "items/index.json";
    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestItemsIndexExporter() {}

    public record Result(int itemRefs, int fluidRefs, long bytes) {}

    public static Result export(Path outputDir, QuestScanResult scan) throws IOException {
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
            root.put(FLUID_REGISTRY_IDS_KEY, fluids);
        }

        Path out = outputDir.resolve(ITEMS_INDEX_FILE);
        Files.createDirectories(out.getParent());
        String json = GSON.toJson(root) + "\n";
        Files.writeString(out, json, StandardCharsets.UTF_8);

        int itemRefs = scan.getItems().size();
        FtbQuestExportMod.LOGGER.info("[items-index] {} item refs, {} fluid refs -> {}", itemRefs, fluids.size(), out);
        return new Result(itemRefs, fluids.size(), json.length());
    }

    /** Registry ids listed in {@code items/index.json} (all namespaces). */
    public static Set<String> readIndexedItemIds(Path outputDir) throws IOException {
        Path indexPath = outputDir.resolve(ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            return Set.of();
        }
        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String key = entry.getKey();
            if ("schema".equals(key) || FLUID_REGISTRY_IDS_KEY.equals(key) || !entry.getValue().isJsonArray()) {
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
