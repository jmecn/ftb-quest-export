package io.github.jmecn.ftbquestexport.assets;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackItem;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackPage;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackRect;
import io.github.jmecn.ftbquestexport.icons.MissingIconRenderer;
import io.github.jmecn.ftbquestexport.icons.OffScreenRenderer;
import io.github.jmecn.ftbquestexport.icons.QuestIconDisplayWriter;
import io.github.jmecn.ftbquestexport.icons.QuestIconRefKind;
import io.github.jmecn.ftbquestexport.icons.QuestIconSizing;
import io.github.jmecn.ftbquestexport.icons.QuestIconTileRenderer;
import io.github.jmecn.ftbquestexport.model.ChapterSummary;
import io.github.jmecn.ftbquestexport.model.GlobalAtlas;
import io.github.jmecn.ftbquestexport.model.GlobalSpriteRect;
import io.github.jmecn.ftbquestexport.model.IconDisplay;
import io.github.jmecn.ftbquestexport.model.QuestIndex;
import io.github.jmecn.ftbquestexport.pojo.GlobalAtlasExportResult;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Packs global UI sprites into {@code quests/global-atlas.png}:
 * FTB node shape layers, sidebar chapter icons, and a 16×16 missing-icon row at the bottom.
 */
public final class GlobalAtlasExporter {

    public static final String GLOBAL_ATLAS_REL = "quests/global-atlas.png";
    private static final List<String> SHAPE_LAYERS = List.of("background", "outline", "shape");
    private static final int MISSING_ICON_PX = QuestExportConstants.ITEM_FLUID_ATLAS_PX;

    private GlobalAtlasExporter() {}

    public static GlobalAtlasExportResult export(
            Path outputDir,
            Minecraft client,
            QuestScanResult scan,
            QuestIndex index,
            Set<String> fluidIds) throws IOException {
        Set<String> shapeIds = scan.getQuestShapeIds();
        ResourceManager resourceManager = client.getResourceManager();
        List<AtlasTile> tiles = new ArrayList<>();
        int failures = 0;

        for (String shapeId : shapeIds) {
            for (String layer : SHAPE_LAYERS) {
                String spriteId = shapeSpriteId(shapeId, layer);
                ResourceLocation location = shapeResource(shapeId, layer);
                try {
                    NativeImage image = loadShapeImage(resourceManager, location);
                    if (image == null) {
                        failures++;
                        continue;
                    }
                    tiles.add(new AtlasTile(spriteId, image));
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn("[global-atlas] failed {}: {}", location, ex.toString());
                }
            }
        }

        int chapterIconCount = 0;
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);
        Map<Integer, OffScreenRenderer> renderersByTier = new HashMap<>();
        Map<String, Integer> nativeCache = new HashMap<>();
        int sidebarOuter = QuestExportConstants.DETAIL_ITEM_ICON_PX;
        int sidebarInner = QuestIconSizing.questIconInnerPx(sidebarOuter);

        try {
            for (ChapterSummary summary : index.chapters()) {
                String iconRef = summary.icon();
                if (iconRef == null || iconRef.isBlank()) {
                    continue;
                }
                String spriteId = chapterSpriteId(summary.filename());
                int tier = QuestIconRefKind.packTier(client, iconRef, fluidIds, sidebarInner, nativeCache);
                try {
                    NativeImage tile = rasterIconTile(
                            client, guiGraphics, bufferSource, fluidIds, renderersByTier, iconRef, tier);
                    tiles.add(new AtlasTile(spriteId, tile));
                    chapterIconCount++;
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn(
                            "[global-atlas] failed chapter icon {} ({})", summary.filename(), iconRef, ex);
                }
            }
        } finally {
            bufferSource.endBatch();
            for (OffScreenRenderer renderer : renderersByTier.values()) {
                renderer.close();
            }
        }

        LinkedHashMap<String, GlobalSpriteRect> spriteIndex = new LinkedHashMap<>();
        NativeImage packed = null;
        int contentW = 0;
        int contentH = 0;

        if (!tiles.isEmpty()) {
            List<PackItem> packItems = new ArrayList<>(tiles.size());
            for (AtlasTile tile : tiles) {
                packItems.add(new PackItem(tile.spriteId(), tile.image().getWidth(), tile.image().getHeight()));
            }

            MaxRectsPacker packer = MaxRectsPacker.defaults();
            packer.packAll(packItems);
            if (!packer.pages().isEmpty()) {
                PackPage page = packer.pages().get(0);
                int[] size = packer.pageContentSize(0);
                contentW = size[0];
                contentH = size[1];
                for (PackRect rect : page.rects()) {
                    spriteIndex.put(
                            rect.name(), new GlobalSpriteRect(rect.x(), rect.y(), rect.width(), rect.height()));
                }
                packed = new NativeImage(contentW, contentH, true);
                for (PackRect rect : page.rects()) {
                    NativeImage tile = findTile(tiles, rect.name());
                    if (tile != null) {
                        OffScreenRenderer.blit(packed, tile, rect.x(), rect.y());
                    }
                }
            }
            closeImages(tiles);
        }

        NativeImage atlas = appendMissingIcon(packed, contentW, contentH, spriteIndex);
        int atlasW = atlas.getWidth();
        int atlasH = atlas.getHeight();
        long pngBytes;
        try {
            Path out = outputDir.resolve(GLOBAL_ATLAS_REL);
            Files.createDirectories(out.getParent());
            atlas.writeToFile(out);
            pngBytes = Files.size(out);
        } finally {
            atlas.close();
        }

        String missingIconId = QuestExportConstants.MISSING_ICON_REGISTRY_ID;
        GlobalAtlas globalAtlas = new GlobalAtlas(GLOBAL_ATLAS_REL, atlasW, atlasH, missingIconId, spriteIndex);
        List<ChapterSummary> chapters = buildChapterSummaries(index.chapters(), spriteIndex);

        FtbQuestExportMod.LOGGER.info(
                "[global-atlas] {} shape(s), {} chapter icon(s), {} sprite(s) (+ missing icon), {} failure(s), {} bytes → {}",
                shapeIds.size(),
                chapterIconCount,
                spriteIndex.size(),
                failures,
                pngBytes,
                GLOBAL_ATLAS_REL);
        return new GlobalAtlasExportResult(
                shapeIds.size(), chapterIconCount, spriteIndex.size(), failures, pngBytes, globalAtlas, chapters);
    }

    private static List<ChapterSummary> buildChapterSummaries(
            List<ChapterSummary> chapters, LinkedHashMap<String, GlobalSpriteRect> spriteIndex) {
        List<ChapterSummary> updated = new ArrayList<>(chapters.size());
        for (ChapterSummary summary : chapters) {
            IconDisplay iconDisplay = null;
            String spriteId = chapterSpriteId(summary.filename());
            if (summary.icon() != null
                    && !summary.icon().isBlank()
                    && spriteIndex.containsKey(spriteId)) {
                iconDisplay = QuestIconDisplayWriter.buildChapterSummaryIconDisplay(spriteId);
            }
            updated.add(new ChapterSummary(
                    summary.id(),
                    summary.filename(),
                    summary.group(),
                    summary.orderIndex(),
                    summary.icon(),
                    summary.title(),
                    summary.subtitle(),
                    iconDisplay));
        }
        return updated;
    }

    private static NativeImage appendMissingIcon(
            NativeImage packed,
            int contentW,
            int contentH,
            LinkedHashMap<String, GlobalSpriteRect> spriteIndex) {
        String missingId = QuestExportConstants.MISSING_ICON_REGISTRY_ID;
        int newW = Math.max(contentW, MISSING_ICON_PX);
        int newH = contentH + MISSING_ICON_PX;
        NativeImage out = new NativeImage(newW, newH, true);
        if (packed != null) {
            OffScreenRenderer.blit(out, packed, 0, 0);
            packed.close();
        }
        NativeImage missing = MissingIconRenderer.create(MISSING_ICON_PX);
        try {
            OffScreenRenderer.blit(out, missing, 0, contentH);
        } finally {
            missing.close();
        }
        spriteIndex.put(missingId, new GlobalSpriteRect(0, contentH, MISSING_ICON_PX, MISSING_ICON_PX));
        return out;
    }

    public static String chapterSpriteId(String filename) {
        return "chapter:" + filename;
    }

    public static String shapeSpriteId(String shapeId, String layer) {
        return shapeId + ":" + layer;
    }

    public static ResourceLocation shapeResource(String shapeId, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                QuestExportConstants.FTBQUESTS_NAMESPACE, "textures/shapes/" + shapeId + "/" + layer + ".png");
    }

    public static boolean isQuestShapeTextureRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        return ref.startsWith(QuestExportConstants.FTBQUESTS_NAMESPACE + ":textures/shapes/");
    }

    private static NativeImage rasterIconTile(
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            Set<String> fluidIds,
            Map<Integer, OffScreenRenderer> renderersByTier,
            String ref,
            int tier) {
        OffScreenRenderer renderer = renderersByTier.computeIfAbsent(tier, t -> new OffScreenRenderer(t, t));
        if (QuestIconTileRenderer.captureTile(
                client, guiGraphics, bufferSource, renderer, fluidIds, ref, tier)) {
            return renderer.copyPixels();
        }
        return MissingIconRenderer.create(tier);
    }

    private static NativeImage loadShapeImage(ResourceManager resourceManager, ResourceLocation location)
            throws IOException {
        Resource resource = resourceManager.getResource(location).orElse(null);
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.open()) {
            return NativeImage.read(in);
        }
    }

    private static NativeImage findTile(List<AtlasTile> tiles, String spriteId) {
        for (AtlasTile tile : tiles) {
            if (tile.spriteId().equals(spriteId)) {
                return tile.image();
            }
        }
        return null;
    }

    private static void closeImages(List<AtlasTile> tiles) {
        for (AtlasTile tile : tiles) {
            tile.image().close();
        }
    }

    private record AtlasTile(String spriteId, NativeImage image) {}
}
