package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Writes {@code items/index.json} from quest scan closure (registry ids referenced by tasks/rewards). */
public final class QuestItemsIndexExporter {

    public static final String ITEMS_INDEX_FILE = "items/index.json";
    /** Same key as {@code minecraft-web-export} {@link io.github.jmecn.minecraftwebexport.export.emi.ItemsSearchIndexExporter}. */
    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");
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
        LOGGER.info("[items-index] {} item refs, {} fluid refs -> {}", itemRefs, fluids.size(), out);
        return new Result(itemRefs, fluids.size(), json.length());
    }
}
