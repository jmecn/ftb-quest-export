package io.github.jmecn.ftbquestexport.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.lang.RegistryLangKeys;
import io.github.jmecn.ftbquestexport.pojo.ItemNameKeysExportResult;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class QuestItemNameKeysExporter {

    private QuestItemNameKeysExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_ITEM_NAME_KEYS_EXPORT);
    }

    public static ItemNameKeysExportResult export(Path outputDir, Minecraft client) throws IOException {
        if (client == null || client.level == null) {
            FtbQuestExportMod.LOGGER.warn("[name-keys] skipped: no client level");
            return ItemNameKeysExportResult.EMPTY;
        }

        Path indexPath = outputDir.resolve(QuestExportConstants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            FtbQuestExportMod.LOGGER.warn("[name-keys] skipped: missing {}", QuestExportConstants.ITEMS_INDEX_FILE);
            return ItemNameKeysExportResult.EMPTY;
        }

        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Map<String, String> items = new TreeMap<>();
        int fluidCount = 0;

        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String namespace = entry.getKey();
            if ("schema".equals(namespace)
                    || QuestExportConstants.FLUID_REGISTRY_IDS_KEY.equals(namespace)
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

        if (index.has(QuestExportConstants.FLUID_REGISTRY_IDS_KEY)
                && index.get(QuestExportConstants.FLUID_REGISTRY_IDS_KEY).isJsonArray()) {
            for (JsonElement fluidEl : index.getAsJsonArray(QuestExportConstants.FLUID_REGISTRY_IDS_KEY)) {
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
            return ItemNameKeysExportResult.EMPTY;
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("items", items);

        Path out = outputDir.resolve(QuestExportConstants.ITEM_NAME_KEYS_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, QuestExportJson.GSON.toJson(root), StandardCharsets.UTF_8);

        FtbQuestExportMod.LOGGER.info("[name-keys] {} registry ids ({} fluids) -> {}", items.size(), fluidCount, out);
        return new ItemNameKeysExportResult(items.size() - fluidCount, fluidCount);
    }
}
