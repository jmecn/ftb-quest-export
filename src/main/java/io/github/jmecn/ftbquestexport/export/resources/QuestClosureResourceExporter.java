package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Copies closure PNGs referenced by {@link QuestScanResult#getTextures()} into {@code assets/}.
 * Item icons use {@link QuestIconExporter} (off-screen render), not model JSON traversal.
 */
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
        ResourceManager rm = client.getResourceManager();
        Set<String> excluded = ResourceExportFilter.excludedNamespaces();

        int textureRefCount = scan.getTextures().size();
        Set<ResourceLocation> textures = TextureRefResolver.resolveAll(scan.getTextures());
        ExportCounters assets = writeTextures(rm, assetsRoot, textures, excluded);

        LOGGER.info(
                "[textures] {} scan refs → {} PNG locations, wrote {} files",
                textureRefCount,
                textures.size(),
                assets.files);

        return new Result(
                assets.files,
                0,
                assets.bytes,
                0,
                assets.failures,
                true,
                textureRefCount,
                assets.written);
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
            String path = id.getPath();
            if (!path.endsWith(".png") && !path.endsWith(".png.mcmeta")) {
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
                LOGGER.warn("[textures] failed to write {}: {}", id, e.getMessage());
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
