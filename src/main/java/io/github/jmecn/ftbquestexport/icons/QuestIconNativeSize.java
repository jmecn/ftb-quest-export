package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.platform.NativeImage;
import dev.ftb.mods.ftblibrary.icon.CombinedIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.LazyIcon;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
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

public final class QuestIconNativeSize {

    private QuestIconNativeSize() {}

    public static int nativeFlatIconPx(Minecraft client, String ref, Set<String> fluidIds) {
        if (ref == null || ref.isBlank()) {
            return fallbackPx();
        }
        if (QuestExportConstants.MISSING_ICON_REGISTRY_ID.equals(ref)) {
            return fallbackPx();
        }
        if (fluidIds != null && fluidIds.contains(ref)) {
            return nativeFluidPx(client, ref);
        }
        if (QuestIconRefKind.isRegistryItemRef(ref)) {
            return nativeRegistryItemPx(client, ref);
        }
        return nativeTextureIconPx(client, ref);
    }

    private static int nativeRegistryItemPx(Minecraft client, String registryId) {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return fallbackPx();
        }
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) {
            return fallbackPx();
        }
        return nativeItemStackPx(client, new ItemStack(item));
    }

    private static int nativeFluidPx(Minecraft client, String registryId) {
        ResourceLocation loc = ResourceLocation.tryParse(registryId);
        if (loc == null) {
            return fallbackPx();
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return fallbackPx();
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation still = extensions.getStillTexture(new FluidStack(fluid, 1000));
        if (still == null) {
            return fallbackPx();
        }
        TextureAtlasSprite sprite =
                client.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(still);
        return spriteSize(sprite);
    }

    private static int nativeTextureIconPx(Minecraft client, String ref) {
        ResourceLocation loc = ResourceLocation.tryParse(ref);
        if (loc != null) {
            if (ref.endsWith(".png") || ref.endsWith(".jpg")) {
                int fromFile = readImageMaxEdge(client, loc);
                if (fromFile > 0) {
                    return fromFile;
                }
            } else {
                TextureAtlasSprite sprite =
                        client.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(loc);
                int fromAtlas = spriteSize(sprite);
                if (fromAtlas > 0) {
                    return fromAtlas;
                }
            }
        }
        return nativeIconPx(client, Icon.getIcon(ref));
    }

    private static int nativeIconPx(Minecraft client, Icon icon) {
        if (icon == null || icon.isEmpty()) {
            return fallbackPx();
        }
        if (icon instanceof IconAnimation animation) {
            int max = fallbackPx();
            for (Icon frame : animation.list) {
                max = Math.max(max, nativeIconPx(client, frame));
            }
            return max;
        }
        if (icon instanceof CombinedIcon combined) {
            int max = fallbackPx();
            for (Icon part : combined.list) {
                max = Math.max(max, nativeIconPx(client, part));
            }
            return max;
        }
        if (icon instanceof LazyIcon lazy) {
            return nativeIconPx(client, lazy.getIcon());
        }
        return fallbackPx();
    }

    private static int nativeItemStackPx(Minecraft client, ItemStack stack) {
        if (stack.isEmpty()) {
            return fallbackPx();
        }
        BakedModel model = client.getItemRenderer().getModel(stack, null, client.player, 0);
        RandomSource random = RandomSource.create(0);
        int max = fallbackPx();
        for (BakedQuad quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
            max = Math.max(max, spriteSize(quad.getSprite()));
        }
        return max;
    }

    private static int spriteSize(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return 0;
        }
        return Math.max(sprite.contents().width(), sprite.contents().height());
    }

    private static int readImageMaxEdge(Minecraft client, ResourceLocation loc) {
        try {
            var resource = client.getResourceManager().getResource(loc);
            if (resource.isEmpty()) {
                return 0;
            }
            try (var stream = resource.get().open();
                    NativeImage image = NativeImage.read(stream)) {
                return Math.max(image.getWidth(), image.getHeight());
            }
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int fallbackPx() {
        return QuestExportConstants.ITEM_FLUID_ATLAS_PX;
    }
}
