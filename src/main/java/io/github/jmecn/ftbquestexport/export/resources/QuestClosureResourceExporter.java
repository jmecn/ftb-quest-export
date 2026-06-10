package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class QuestClosureResourceExporter {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

    private QuestClosureResourceExporter() {}

    public record Result(
            int assetFiles,
            int dataFiles,
            long assetBytes,
            long dataBytes,
            int failures,
            boolean serverSkipped,
            int seededLocations,
            int writtenLocations) {}

    public static Result export(Path outputDir, Minecraft client, QuestScanResult scan) throws IOException {
        Path assetsRoot = outputDir.resolve("assets");
        ResourceManager clientRm = client.getResourceManager();
        Set<String> excluded = ResourceExportFilter.excludedNamespaces();

        Set<ResourceLocation> modelQueue = new LinkedHashSet<>();
        for (String texture : scan.getTextures()) {
            ModelDependencyCollector.seedTextureRef(texture, modelQueue);
        }
        for (String item : scan.getItems()) {
            ModelDependencyCollector.seedItem(clientRm, item, modelQueue);
        }
        for (String block : scan.getBlocks()) {
            ModelDependencyCollector.seedBlockId(clientRm, block, modelQueue);
        }

        int seeded = modelQueue.size();
        Set<ResourceLocation> textures = collectTextureRefs(clientRm, modelQueue, excluded);
        ExportCounters assets = writeTextures(clientRm, assetsRoot, textures, excluded);

        LOGGER.info("[closure] texture-only: seeded {} model/blockstate refs, {} textures, wrote {} files",
                seeded, textures.size(), assets.files);

        return new Result(
                assets.files, 0,
                assets.bytes, 0,
                assets.failures,
                true,
                seeded,
                assets.written);
    }

    private static Set<ResourceLocation> collectTextureRefs(
            ResourceManager rm,
            Set<ResourceLocation> seeds,
            Set<String> excludedNamespaces) {
        Set<ResourceLocation> textures = new LinkedHashSet<>();
        Set<ResourceLocation> visited = new HashSet<>();
        Deque<ResourceLocation> pending = new ArrayDeque<>(seeds);

        while (!pending.isEmpty()) {
            ResourceLocation id = pending.removeFirst();
            if (!visited.add(id)) {
                continue;
            }
            if (excludedNamespaces.contains(id.getNamespace())) {
                continue;
            }
            String path = id.getPath();
            if (path.endsWith(".png") || path.endsWith(".png.mcmeta")) {
                textures.add(id);
                continue;
            }
            if (path.endsWith(".json") && path.startsWith("blockstates/")) {
                rm.getResource(id).ifPresent(resource ->
                        ModelDependencyWalker.enqueueBlockstateDependencies(rm, id, resource, pending, visited));
                continue;
            }
            if (path.endsWith(".json") && path.startsWith("models/")) {
                walkModelJson(rm, id, pending, visited);
            }
        }
        return textures;
    }

    private static void walkModelJson(
            ResourceManager rm,
            ResourceLocation modelId,
            Deque<ResourceLocation> pending,
            Set<ResourceLocation> visited) {
        var opt = rm.getResource(modelId);
        if (opt.isPresent()) {
            try (var reader = new InputStreamReader(opt.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                ModelDependencyWalker.enqueueModelDependenciesFromJson(rm, modelId, root, pending, visited);
            } catch (Exception ignored) {
                // non-fatal
            }
            return;
        }
        var synthetic = SyntheticModelCatalog.content(modelId);
        if (synthetic.isEmpty()) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(synthetic.get()).getAsJsonObject();
            ModelDependencyWalker.enqueueModelDependenciesFromJson(rm, modelId, root, pending, visited);
        } catch (Exception ignored) {
            // non-fatal
        }
    }

    private static ExportCounters writeTextures(
            ResourceManager rm,
            Path assetsRoot,
            Set<ResourceLocation> textures,
            Set<String> excludedNamespaces) {
        ExportCounters counters = new ExportCounters();
        Set<ResourceLocation> written = new HashSet<>();
        for (ResourceLocation id : textures) {
            if (!written.add(id)) {
                continue;
            }
            if (excludedNamespaces.contains(id.getNamespace())) {
                continue;
            }
            try {
                var opt = rm.getResource(id);
                if (opt.isPresent()) {
                    counters.bytes += ResourceFileWriter.write(assetsRoot, id, opt.get());
                    counters.files++;
                    counters.written++;
                }
            } catch (IOException e) {
                counters.failures++;
                LOGGER.warn("[closure] failed to write {}: {}", id, e.getMessage());
            }
        }
        return counters;
    }

    private static final class ExportCounters {
        int files;
        long bytes;
        int failures;
        int written;
    }
}
