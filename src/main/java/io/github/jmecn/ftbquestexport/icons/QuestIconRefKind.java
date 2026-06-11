package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;

import java.util.Set;

/**
 * Quest icon refs fall into two render classes (pack tier is the same for both):
 * <ul>
 *   <li>Registry item / fluid — off-screen {@code renderItem} / fluid blit, scaled to {@code ceilToTier(innerPx)}.</li>
 *   <li>Texture / FTB {@link dev.ftb.mods.ftblibrary.icon.Icon} — {@code Icon.draw} at {@code ceilToTier(innerPx)}.</li>
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

    /** Atlas raster size from on-screen innerPx (16 / 32 / 64 / 128). */
    public static int packTier(String ref, Set<String> fluidIds, int displayInnerPx) {
        int tier = QuestIconSizing.ceilToTier(displayInnerPx);
        return Math.max(QuestExportConstants.ITEM_FLUID_ATLAS_PX, tier);
    }
}
