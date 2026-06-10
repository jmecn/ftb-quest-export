package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.emi.RegistryLangKeys;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class QuestItemNameKeysExporter {

    public static final String ITEM_NAME_KEYS_FILE = "items/name-keys.json";

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestItemNameKeysExporter() {}

    public record Result(int registryIds, int fluidIds) {
        static final Result EMPTY = new Result(0, 0);
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("quest.skipItemNameKeysExport");
    }

    public static Result export(Path outputDir, Minecraft client) throws IOException {
        if (client == null || client.level == null) {
            LOGGER.warn("[name-keys] skipped: no client level");
            return Result.EMPTY;
        }

        Path indexPath = outputDir.resolve(QuestItemsIndexExporter.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            LOGGER.warn("[name-keys] skipped: missing {}", QuestItemsIndexExporter.ITEMS_INDEX_FILE);
            return Result.EMPTY;
        }

        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Map<String, String> items = new TreeMap<>();
        int fluidCount = 0;

        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String namespace = entry.getKey();
            if ("schema".equals(namespace)
                    || QuestItemsIndexExporter.FLUID_REGISTRY_IDS_KEY.equals(namespace)
                    || !entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement pathEl : entry.getValue().getAsJsonArray()) {
                if (!pathEl.isJsonPrimitive()) {
                    continue;
                }
                String path = pathEl.getAsString();
                if (path == null || path.isEmpty()) {
                    continue;
                }
                String registryId = path.contains(":") ? path : namespace + ":" + path;
                items.put(registryId, RegistryLangKeys.resolveItemDescriptionKey(client, registryId));
            }
        }

        if (index.has(QuestItemsIndexExporter.FLUID_REGISTRY_IDS_KEY)
                && index.get(QuestItemsIndexExporter.FLUID_REGISTRY_IDS_KEY).isJsonArray()) {
            for (JsonElement fluidEl : index.getAsJsonArray(QuestItemsIndexExporter.FLUID_REGISTRY_IDS_KEY)) {
                if (!fluidEl.isJsonPrimitive()) {
                    continue;
                }
                String registryId = fluidEl.getAsString();
                if (registryId == null || registryId.isEmpty()) {
                    continue;
                }
                items.put(registryId, RegistryLangKeys.resolveFluidDescriptionKey(client, registryId));
                fluidCount++;
            }
        }

        if (items.isEmpty()) {
            return Result.EMPTY;
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("items", items);

        Path out = outputDir.resolve(ITEM_NAME_KEYS_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);

        LOGGER.info("[name-keys] {} registry ids ({} fluids) -> {}", items.size(), fluidCount, out);
        return new Result(items.size() - fluidCount, fluidCount);
    }

    public static Map<String, String> readNameKeys(Path outputDir) throws IOException {
        Path file = outputDir.resolve(ITEM_NAME_KEYS_FILE);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonElement items = root.get("items");
        if (items == null || !items.isJsonObject()) {
            return Map.of();
        }
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : items.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return map;
    }
}
