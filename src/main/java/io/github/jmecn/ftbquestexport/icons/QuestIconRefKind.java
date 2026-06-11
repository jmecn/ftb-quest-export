package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Atlas raster resolution:
 * <ul>
 *   <li>{@link BlockItem} — {@code ceilToTier(innerPx)} off-screen {@code renderItem}; 3D GUI model benefits from
 *       extra pixels when displayed large.</li>
 *   <li>Other registry items, fluids, FTB texture / block-face {@link dev.ftb.mods.ftblibrary.icon.Icon} refs —
 *       fixed {@value io.github.jmecn.ftbquestexport.QuestExportConstants#ITEM_FLUID_ATLAS_PX}×
 *       px (square atlas pixels; upscaling has no benefit).</li>
 * </ul>
 */
public final class QuestIconRefKind {

    private QuestIconRefKind() {}

    public static boolean isItemOrFluid(String ref, Set<String> fluidIds) {
        if (ref == null || ref.isBlank()) {
            return true;
        }
        if (QuestExportConstants.MISSING_ICON_REGISTRY_ID.equals(ref)) {
            return true;
        }
        if (fluidIds != null && fluidIds.contains(ref)) {
            return true;
        }
        return isRegistryItemRef(ref);
    }

    /** {@code namespace:path} registry id (not an FTB texture ref). */
    public static boolean isRegistryItemRef(String ref) {
        int colon = ref.indexOf(':');
        if (colon <= 0 || colon >= ref.length() - 1) {
            return false;
        }
        String path = ref.substring(colon + 1);
        return !(path.startsWith("textures/")
                || path.startsWith("block/")
                || path.startsWith("item/")
                || path.startsWith("gui/")
                || path.startsWith("icons/")
                || path.endsWith(".png"));
    }

    /** Registry {@link BlockItem} id ({@code mod:block_name}), not an FTB {@code mod:block/texture} ref. */
    public static boolean isBlockItemRef(String ref) {
        if (!isRegistryItemRef(ref)) {
            return false;
        }
        ResourceLocation loc = ResourceLocation.tryParse(ref);
        if (loc == null) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        return item instanceof BlockItem;
    }

    /** Atlas cell edge length: tiered only for {@link BlockItem}; otherwise 16. */
    public static int packTier(String ref, Set<String> fluidIds, int displayInnerPx) {
        if (isBlockItemRef(ref)) {
            return QuestIconSizing.ceilToTier(displayInnerPx);
        }
        return QuestExportConstants.ITEM_FLUID_ATLAS_PX;
    }

    /** FTB texture / block-face / gui icon ref (not a registry item id). */
    public static boolean isTextureIconRef(String ref) {
        return ref != null && !ref.isBlank() && !isRegistryItemRef(ref);
    }
}
