package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;

import java.util.Set;

/**
 * Quest icon refs fall into two packing/render classes:
 * <ul>
 *   <li>Registry item / fluid — 16×16 GUI art; atlas cell {@link QuestExportConstants#ITEM_FLUID_ATLAS_PX} (no upscale).</li>
 *   <li>Texture / block-style FTB icon — off-screen at {@code ceilToTier(innerPx)} (16 / 32 / 64 / 128).</li>
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

    /** Atlas raster size for this ref (not the on-screen innerPx). */
    public static int packTier(String ref, Set<String> fluidIds, int displayInnerPx) {
        if (isItemOrFluid(ref, fluidIds)) {
            return QuestExportConstants.ITEM_FLUID_ATLAS_PX;
        }
        return QuestIconSizing.ceilToTier(displayInnerPx);
    }
}
