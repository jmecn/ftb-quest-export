package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.icon.Icon;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Renders one quest icon atlas cell. Item/fluid tiles are {@link QuestExportConstants#ITEM_FLUID_ATLAS_PX};
 * texture/block FTB icons are drawn off-screen at {@code packTierPx}.
 */
public final class QuestIconTileRenderer {

    private QuestIconTileRenderer() {}

    public static NativeImage renderTile(
            Minecraft client,
            GuiGraphics guiGraphics,
            Path outputDir,
            Set<String> fluidIds,
            String ref,
            int packTierPx) throws IOException {
        if (ref == null || ref.isBlank()) {
            return renderMissing(QuestExportConstants.ITEM_FLUID_ATLAS_PX);
        }
        if (QuestIconRefKind.isItemOrFluid(ref, fluidIds)) {
            if (fluidIds != null && fluidIds.contains(ref)) {
                NativeImage fluid = renderFluid(client, guiGraphics, ref);
                if (fluid != null) {
                    return fluid;
                }
            }
            NativeImage item = renderItem(client, guiGraphics, ref);
            if (item != null) {
                return item;
            }
            return renderMissing(QuestExportConstants.ITEM_FLUID_ATLAS_PX);
        }
        NativeImage texture = renderTextureIcon(client, guiGraphics, outputDir, ref, packTierPx);
        if (texture != null) {
            return texture;
        }
        return renderMissing(packTierPx);
    }

    private static NativeImage renderItem(Minecraft client, GuiGraphics guiGraphics, String registryId)
            throws IOException {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) {
            return null;
        }

        int cell = QuestExportConstants.ITEM_FLUID_ATLAS_PX;
        ItemStack stack = new ItemStack(item);
        try (OffScreenRenderer renderer = new OffScreenRenderer(cell, cell)) {
            renderer.setupItemRendering();
            var sprites = collectSprites(client, stack);
            if (renderer.isAnimated(sprites)) {
                renderer.uploadAnimatedFirstFrame(sprites);
            }
            Runnable draw = () -> {
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
            };
            return renderer.captureToNativeImage(draw);
        }
    }

    private static NativeImage renderFluid(Minecraft client, GuiGraphics guiGraphics, String registryId)
            throws IOException {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return null;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return null;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, 1000);
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null) {
            return null;
        }

        TextureAtlas atlas = client.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(still);
        int tint = extensions.getTintColor(stack);
        float a = ((tint >> 24) & 0xFF) / 255.0F;
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        if (a <= 0.0F) {
            a = 1.0F;
        }
        float fr = r;
        float fg = g;
        float fb = b;
        float fa = a;

        int cell = QuestExportConstants.ITEM_FLUID_ATLAS_PX;
        try (OffScreenRenderer renderer = new OffScreenRenderer(cell, cell)) {
            renderer.setupFlatGuiRendering();
            Runnable draw = () -> {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(fr, fg, fb, fa);
                guiGraphics.blit(
                        0,
                        0,
                        0,
                        cell,
                        cell,
                        sprite);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            };
            return renderer.captureToNativeImage(draw);
        }
    }

    private static NativeImage renderTextureIcon(
            Minecraft client,
            GuiGraphics guiGraphics,
            Path outputDir,
            String ref,
            int tierPx) throws IOException {
        Icon icon = Icon.getIcon(ref);
        if (icon != null && !icon.isEmpty()) {
            try (OffScreenRenderer renderer = new OffScreenRenderer(tierPx, tierPx)) {
                renderer.setupFlatGuiRendering();
                return renderer.captureToNativeImage(() -> icon.draw(guiGraphics, 0, 0, tierPx, tierPx));
            } catch (Exception ignored) {
                // Fall through to exported asset.
            }
        }
        return loadTextureAsset(outputDir, ref, tierPx);
    }

    private static NativeImage renderMissing(int tierPx) {
        NativeImage tile = new NativeImage(tierPx, tierPx, true);
        int cell = Math.max(1, tierPx / 2);
        for (int y = 0; y < tierPx; y++) {
            for (int x = 0; x < tierPx; x++) {
                boolean magenta = ((x / cell) + (y / cell)) % 2 == 0;
                int color = magenta ? QuestExportConstants.MISSING_ICON_MAGENTA
                        : QuestExportConstants.MISSING_ICON_BLACK;
                tile.setPixelRGBA(x, y, color);
            }
        }
        return tile;
    }

    private static NativeImage loadTextureAsset(Path outputDir, String ref, int tierPx) throws IOException {
        Path source = IconPathResolver.resolveExportedTexture(outputDir, ref);
        if (source == null || !Files.isRegularFile(source)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(source)) {
            NativeImage image = NativeImage.read(in);
            if (image.getWidth() == tierPx && image.getHeight() == tierPx) {
                return image;
            }
            NativeImage resized = resizeNearest(image, tierPx, tierPx);
            image.close();
            return resized;
        }
    }

    private static NativeImage resizeNearest(NativeImage source, int width, int height) {
        NativeImage out = new NativeImage(width, height, true);
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        for (int y = 0; y < height; y++) {
            int sy = y * srcH / height;
            for (int x = 0; x < width; x++) {
                int sx = x * srcW / width;
                out.setPixelRGBA(x, y, source.getPixelRGBA(sx, sy));
            }
        }
        return out;
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
}
