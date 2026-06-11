package io.github.jmecn.ftbquestexport.assets;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.pojo.AssetExportCounters;
import io.github.jmecn.ftbquestexport.pojo.AssetExportResult;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class QuestAssetExporter {

    private QuestAssetExporter() {}

    public static AssetExportResult export(Path outputDir, Minecraft client, QuestScanResult scan) throws IOException {
        Path assetsRoot = outputDir.resolve("assets");
        ResourceManager rm = client.getResourceManager();
        Set<String> excluded = excludedNamespaces();

        int textureRefCount = scan.getTextures().size();
        Set<ResourceLocation> textures = resolveTextureRefs(scan.getTextures());
        AssetExportCounters assets = writeTextures(rm, assetsRoot, textures, excluded);

        FtbQuestExportMod.LOGGER.info(
                "[textures] {} scan refs → {} PNG locations, wrote {} files",
                textureRefCount,
                textures.size(),
                assets.files);

        return new AssetExportResult(
                assets.files,
                0,
                assets.bytes,
                0,
                assets.failures,
                true,
                textureRefCount,
                assets.written);
    }

    public static Set<String> excludedNamespaces() {
        String extra = System.getProperty(QuestExportConstants.EXPORT_EXCLUDED_NAMESPACES_PROPERTY, "").trim();
        if (extra.isEmpty()) {
            return QuestExportConstants.DEFAULT_EXCLUDED_NAMESPACES;
        }
        var merged = new LinkedHashSet<>(QuestExportConstants.DEFAULT_EXCLUDED_NAMESPACES);
        Arrays.stream(extra.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(merged::add);
        return Set.copyOf(merged);
    }

    public static boolean isExcluded(ResourceLocation id) {
        return excludedNamespaces().contains(id.getNamespace());
    }

    private static Set<ResourceLocation> resolveTextureRefs(Iterable<String> textureRefs) {
        Set<ResourceLocation> locations = new LinkedHashSet<>();
        for (String ref : textureRefs) {
            addTextureRef(ref, locations);
        }
        return locations;
    }

    private static void addTextureRef(String textureRef, Set<ResourceLocation> pending) {
        if (textureRef == null || textureRef.isBlank()) {
            return;
        }
        if (GlobalAtlasExporter.isQuestShapeTextureRef(textureRef)) {
            return;
        }
        String ref = textureRef;
        if (ref.endsWith(".png") || ref.endsWith(".mcmeta")) {
            if (ref.endsWith(".mcmeta")) {
                ref = ref.substring(0, ref.length() - ".mcmeta".length());
            } else if (ref.endsWith(".png")) {
                ref = ref.substring(0, ref.length() - ".png".length());
            }
        }
        ResourceLocation loc = ResourceLocation.tryParse(ref);
        if (loc == null) {
            return;
        }
        String path = loc.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        pending.add(ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path + ".png"));
        pending.add(ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path + ".png.mcmeta"));
    }

    private static AssetExportCounters writeTextures(
            ResourceManager rm,
            Path assetsRoot,
            Set<ResourceLocation> textures,
            Set<String> excludedNamespaces) {
        AssetExportCounters counters = new AssetExportCounters();
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
                if (opt.isEmpty()) {
                    var stack = rm.getResourceStack(id);
                    if (!stack.isEmpty()) {
                        opt = java.util.Optional.of(stack.get(stack.size() - 1));
                    }
                }
                if (opt.isPresent()) {
                    counters.bytes += writeResource(assetsRoot, id, opt.get());
                    counters.files++;
                    counters.written++;
                } else {
                    counters.failures++;
                    FtbQuestExportMod.LOGGER.warn("[textures] missing resource for {}", id);
                }
            } catch (IOException e) {
                counters.failures++;
                FtbQuestExportMod.LOGGER.warn("[textures] failed to write {}: {}", id, e.getMessage());
            }
        }
        return counters;
    }

    private static long writeResource(Path typeRoot, ResourceLocation id, Resource resource) throws IOException {
        Path outFile = typeRoot.resolve(id.getNamespace()).resolve(id.getPath());
        Files.createDirectories(outFile.getParent());
        try (InputStream in = resource.open()) {
            long bytes = Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            return bytes > 0 ? bytes : Files.size(outFile);
        }
    }
}
