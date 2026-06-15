package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.icon.Icon;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public final class QuestIconTileRenderer {

    private QuestIconTileRenderer() {}

    public static boolean captureTile(
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            OffScreenRenderer renderer,
            Set<String> fluidIds,
            String ref,
            int packTierPx) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        if (QuestExportConstants.MISSING_ICON_REGISTRY_ID.equals(ref)) {
            return false;
        }
        if (QuestIconRefKind.isItemOrFluid(ref, fluidIds)) {
            if (isFluidRef(ref, fluidIds)) {
                return captureFluid(client, guiGraphics, bufferSource, renderer, ref, packTierPx);
            }
            return captureItem(client, guiGraphics, bufferSource, renderer, ref, packTierPx);
        }
        return captureFtbIcon(guiGraphics, bufferSource, renderer, ref, packTierPx);
    }

    private static boolean isFluidRef(String ref, Set<String> fluidIds) {
        return fluidIds != null && fluidIds.contains(ref);
    }

    private static boolean captureFtbIcon(
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            OffScreenRenderer renderer,
            String ref,
            int packTierPx) {
        if (renderer.width() != packTierPx || renderer.height() != packTierPx) {
            return false;
        }
        Icon icon = Icon.getIcon(ref);
        if (icon == null || icon.isEmpty()) {
            return false;
        }
        try {
            renderer.setupFlatGuiRendering();
            renderer.capture(() -> {
                icon.draw(guiGraphics, 0, 0, packTierPx, packTierPx);
                finishDraw(guiGraphics, bufferSource);
            });
            return hasVisiblePixels(renderer);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean captureItem(
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            OffScreenRenderer renderer,
            String registryId,
            int packTierPx) {
        if (renderer.width() != packTierPx || renderer.height() != packTierPx) {
            return false;
        }
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) {
            return false;
        }

        ItemStack stack = new ItemStack(item);
        renderer.setupItemRendering();
        renderer.capture(() -> {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
            finishDraw(guiGraphics, bufferSource);
        });
        return hasVisiblePixels(renderer);
    }

    private static boolean captureFluid(
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            OffScreenRenderer renderer,
            String registryId,
            int packTierPx) {
        if (renderer.width() != packTierPx || renderer.height() != packTierPx) {
            return false;
        }
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return false;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return false;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, 1000);
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null) {
            return false;
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
        float fa = a;
        float fr = r;
        float fg = g;
        float fb = b;

        renderer.setupFlatGuiRendering();
        renderer.capture(() -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(fr, fg, fb, fa);
            guiGraphics.blit(0, 0, 0, packTierPx, packTierPx, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            finishDraw(guiGraphics, bufferSource);
        });
        return hasVisiblePixels(renderer);
    }

    private static void finishDraw(GuiGraphics guiGraphics, MultiBufferSource.BufferSource bufferSource) {
        guiGraphics.flush();
        bufferSource.endBatch();
    }

    private static boolean hasVisiblePixels(OffScreenRenderer renderer) {
        NativeImage image = renderer.copyPixels();
        try {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            image.close();
        }
    }
}
