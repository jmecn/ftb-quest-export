package io.github.jmecn.ftbquestexport.export.icons;

import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import io.github.jmecn.ftbquestexport.export.pojo.ItemIconExportResult;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders quest-referenced item/fluid icons to {@code icons/items/<ns>/<path>.png}
 */
public final class QuestItemIconExporter {

    private QuestItemIconExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_ICON_EXPORT);
    }

    public static ItemIconExportResult export(Path iconsRoot, Minecraft client, Set<String> itemIds, Set<String> fluidIds)
            throws IOException {
        Files.createDirectories(iconsRoot.resolve("items"));

        Set<String> ordered = new TreeSet<>();
        if (itemIds != null) {
            ordered.addAll(itemIds);
        }
        if (fluidIds != null) {
            ordered.addAll(fluidIds);
        }

        Set<String> fluidSet = fluidIds == null ? Set.of() : Set.copyOf(fluidIds);
        FtbQuestExportMod.LOGGER.info(
                "[icons] rendering {} id(s) ({} items, {} fluids) at {}px -> {}",
                ordered.size(),
                itemIds == null ? 0 : itemIds.size(),
                fluidIds == null ? 0 : fluidIds.size(),
                QuestExportConstants.ICON_CELL_PX,
                iconsRoot);

        int itemsRendered = 0;
        int fluidsRendered = 0;
        int fluidsSkipped = 0;
        int failures = 0;
        long pngBytes = 0;

        try (var renderer = new OffScreenRenderer(QuestExportConstants.ICON_CELL_PX, QuestExportConstants.ICON_CELL_PX)) {
            var bufferSource = client.renderBuffers().bufferSource();
            var guiGraphics = new GuiGraphics(client, bufferSource);

            Path missingPath = iconOutputPath(iconsRoot, MissingIconRenderer.REGISTRY_ID);
            Files.createDirectories(missingPath.getParent());
            MissingIconRenderer.render(client, guiGraphics, renderer, missingPath);
            pngBytes += Files.size(missingPath);

            renderer.setupItemRendering();

            int index = 0;
            int total = ordered.size();
            for (String registryId : ordered) {
                index++;
                Path out = iconOutputPath(iconsRoot, registryId);
                Files.createDirectories(out.getParent());

                try {
                    if (fluidSet.contains(registryId)) {
                        int fluidResult = renderFluid(client, guiGraphics, renderer, registryId, out);
                        if (fluidResult == 1) {
                            fluidsRendered++;
                            pngBytes += Files.size(out);
                        } else if (fluidResult == 0) {
                            fluidsSkipped++;
                        }
                    } else {
                        renderItem(client, guiGraphics, renderer, registryId, out);
                        itemsRendered++;
                        pngBytes += Files.size(out);
                    }

                    if (index % QuestExportConstants.ICON_FLUSH_RENDER_EVERY == 0) {
                        bufferSource.endBatch();
                    }
                    if (index % QuestExportConstants.ICON_LOG_STRIDE == 0 || index == total) {
                        FtbQuestExportMod.LOGGER.info(
                                "[icons] {}% {}/{} ({} items, {} fluids, {} skipped, {} fail)",
                                (index * 100) / total,
                                index,
                                total,
                                itemsRendered,
                                fluidsRendered,
                                fluidsSkipped,
                                failures);
                    }
                } catch (Exception e) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn("[icons] failed {}: {}", registryId, failureSummary(e));
                }
            }

            bufferSource.endBatch();
        }

        FtbQuestExportMod.LOGGER.info(
                "[icons] done: {} items, {} fluids ({} skipped no still), {} failures, {} bytes",
                itemsRendered,
                fluidsRendered,
                fluidsSkipped,
                failures,
                pngBytes);

        return new ItemIconExportResult(itemsRendered, fluidsRendered, fluidsSkipped, failures, pngBytes);
    }

    /** @return 1 rendered, 0 skipped (no still), -1 not a fluid */
    private static int renderFluid(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            String registryId,
            Path output) throws IOException {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return -1;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return 0;
        }
        renderer.setupFlatGuiRendering();
        if (!FluidStillIconRenderer.render(client, guiGraphics, renderer, fluid, output)) {
            return 0;
        }
        renderer.setupItemRendering();
        return 1;
    }

    private static void renderItem(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            String registryId,
            Path output) throws IOException {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            throw new IllegalArgumentException("invalid registry id");
        }
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("unknown item");
        }

        ItemStack stack = new ItemStack(item);
        var sprites = collectSprites(client, stack);
        if (renderer.isAnimated(sprites)) {
            renderer.uploadAnimatedFirstFrame(sprites);
        }
        Runnable draw = () -> {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
        };
        renderer.captureAsPng(draw, output);
    }

    static Path iconOutputPath(Path iconsRoot, String registryId) {
        int colon = registryId.indexOf(':');
        if (colon <= 0 || colon >= registryId.length() - 1) {
            throw new IllegalArgumentException("invalid registry id: " + registryId);
        }
        String namespace = registryId.substring(0, colon);
        String path = registryId.substring(colon + 1);
        return iconsRoot.resolve("items").resolve(namespace).resolve(path + ".png");
    }

    private static Set<TextureAtlasSprite> collectSprites(Minecraft client, ItemStack stack) {
        BakedModel model = client.getItemRenderer().getModel(stack, null, null, 0);
        return guessSprites(Set.of(model));
    }

    private static Set<TextureAtlasSprite> guessSprites(Collection<BakedModel> models) {
        var result = Collections.newSetFromMap(new IdentityHashMap<TextureAtlasSprite, Boolean>());
        var random = RandomSource.create(0);
        for (var model : models) {
            for (var quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
                result.add(quad.getSprite());
            }
        }
        return result;
    }

    private static String failureSummary(Throwable error) {
        String msg = error.getMessage();
        if (msg != null && !msg.isBlank()) {
            return error.getClass().getSimpleName() + ": " + msg;
        }
        return error.getClass().getSimpleName();
    }
}
