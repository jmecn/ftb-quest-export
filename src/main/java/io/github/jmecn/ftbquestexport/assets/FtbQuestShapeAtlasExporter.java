package io.github.jmecn.ftbquestexport.assets;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackItem;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackPage;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackRect;
import io.github.jmecn.ftbquestexport.icons.MissingIconRenderer;
import io.github.jmecn.ftbquestexport.icons.OffScreenRenderer;
import io.github.jmecn.ftbquestexport.model.ShapeAtlas;
import io.github.jmecn.ftbquestexport.model.ShapeSpriteRect;
import io.github.jmecn.ftbquestexport.pojo.ShapeAtlasExportResult;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Packs FTB Quests node shape layers ({@code shape / background / outline}) into a single global atlas,
 * with a 16×16 {@linkplain QuestExportConstants#MISSING_ICON_REGISTRY_ID missing icon} row appended at the bottom.
 */
public final class FtbQuestShapeAtlasExporter {

    public static final String SHAPE_ATLAS_REL = "quests/shape-atlas.png";
    private static final List<String> LAYERS = List.of("background", "outline", "shape");
    private static final int MISSING_ICON_PX = QuestExportConstants.ITEM_FLUID_ATLAS_PX;

    private FtbQuestShapeAtlasExporter() {}

    public static ShapeAtlasExportResult export(
            Path outputDir, Minecraft client, QuestScanResult scan) throws IOException {
        Set<String> shapeIds = scan.getQuestShapeIds();
        ResourceManager resourceManager = client.getResourceManager();
        List<ShapeSprite> sprites = new ArrayList<>();
        int failures = 0;

        for (String shapeId : shapeIds) {
            for (String layer : LAYERS) {
                String spriteId = spriteId(shapeId, layer);
                ResourceLocation location = shapeResource(shapeId, layer);
                try {
                    NativeImage image = loadShapeImage(resourceManager, location);
                    if (image == null) {
                        failures++;
                        continue;
                    }
                    sprites.add(new ShapeSprite(spriteId, image));
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn("[shape-atlas] failed {}: {}", location, ex.toString());
                }
            }
        }

        LinkedHashMap<String, ShapeSpriteRect> spriteIndex = new LinkedHashMap<>();
        NativeImage packed = null;
        int contentW = 0;
        int contentH = 0;

        if (!sprites.isEmpty()) {
            List<PackItem> packItems = new ArrayList<>(sprites.size());
            for (ShapeSprite sprite : sprites) {
                packItems.add(new PackItem(sprite.spriteId(), sprite.image().getWidth(), sprite.image().getHeight()));
            }

            MaxRectsPacker packer = MaxRectsPacker.defaults();
            packer.packAll(packItems);
            if (!packer.pages().isEmpty()) {
                PackPage page = packer.pages().get(0);
                int[] size = packer.pageContentSize(0);
                contentW = size[0];
                contentH = size[1];
                for (PackRect rect : page.rects()) {
                    spriteIndex.put(rect.name(), new ShapeSpriteRect(rect.x(), rect.y(), rect.width(), rect.height()));
                }
                packed = new NativeImage(contentW, contentH, true);
                for (PackRect rect : page.rects()) {
                    NativeImage tile = findTile(sprites, rect.name());
                    if (tile != null) {
                        OffScreenRenderer.blit(packed, tile, rect.x(), rect.y());
                    }
                }
            }
            closeImages(sprites);
        }

        NativeImage atlas = appendMissingIcon(packed, contentW, contentH, spriteIndex);
        int atlasW = atlas.getWidth();
        int atlasH = atlas.getHeight();
        long pngBytes;
        try {
            Path out = outputDir.resolve(SHAPE_ATLAS_REL);
            Files.createDirectories(out.getParent());
            atlas.writeToFile(out);
            pngBytes = Files.size(out);
        } finally {
            atlas.close();
        }

        String missingIconId = QuestExportConstants.MISSING_ICON_REGISTRY_ID;
        ShapeAtlas shapeAtlas = new ShapeAtlas(SHAPE_ATLAS_REL, atlasW, atlasH, missingIconId, spriteIndex);
        FtbQuestExportMod.LOGGER.info(
                "[shape-atlas] {} shape(s), {} sprite(s) (+ missing icon), {} failure(s), {} bytes → {}",
                shapeIds.size(),
                spriteIndex.size(),
                failures,
                pngBytes,
                SHAPE_ATLAS_REL);
        return new ShapeAtlasExportResult(shapeIds.size(), spriteIndex.size(), failures, pngBytes, shapeAtlas);
    }

    private static NativeImage appendMissingIcon(
            NativeImage packed,
            int contentW,
            int contentH,
            LinkedHashMap<String, ShapeSpriteRect> spriteIndex) {
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
        spriteIndex.put(missingId, new ShapeSpriteRect(0, contentH, MISSING_ICON_PX, MISSING_ICON_PX));
        return out;
    }

    public static String spriteId(String shapeId, String layer) {
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

    private static NativeImage findTile(List<ShapeSprite> sprites, String spriteId) {
        for (ShapeSprite sprite : sprites) {
            if (sprite.spriteId().equals(spriteId)) {
                return sprite.image();
            }
        }
        return null;
    }

    private static void closeImages(List<ShapeSprite> sprites) {
        for (ShapeSprite sprite : sprites) {
            sprite.image().close();
        }
    }

    private record ShapeSprite(String spriteId, NativeImage image) {}
}
