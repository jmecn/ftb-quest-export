package io.github.jmecn.ftbquestexport.export.icons;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Renders a fluid still texture (tinted) into an {@link OffScreenRenderer} buffer. */
public final class FluidStillIconRenderer {

    private FluidStillIconRenderer() {}

    public static boolean render(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            Fluid fluid,
            Path output) throws IOException {
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

        if (renderer.isAnimated(List.of(sprite))) {
            renderer.uploadAnimatedFirstFrame(List.of(sprite));
        }

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
        Runnable draw = () -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(fr, fg, fb, fa);
            guiGraphics.blit(0, 0, 0, QuestExportConstants.FLUID_ICON_GUI_SIZE, QuestExportConstants.FLUID_ICON_GUI_SIZE, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        };
        renderer.captureAsPng(draw, output);
        return true;
    }
}
