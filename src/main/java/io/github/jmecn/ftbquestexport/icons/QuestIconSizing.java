package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;

public final class QuestIconSizing {

    private QuestIconSizing() {}

    public static int questIconPx(double size, double gridScale) {
        double questSize = size > 0 ? size : 1.0;
        double gridStep = QuestExportConstants.GRID_STEP_BASE / gridScale;
        return (int) Math.round(gridStep * QuestExportConstants.BUTTON_CELL_RATIO * questSize);
    }

    public static int questIconInnerPx(int outerPx) {
        return (int) Math.round(outerPx * QuestExportConstants.INNER_RATIO);
    }

    public static int ceilToTier(int needPx) {
        for (int tier : QuestExportConstants.ICON_TIERS) {
            if (tier >= needPx) {
                return tier;
            }
        }
        return QuestExportConstants.ICON_TIERS[QuestExportConstants.ICON_TIERS.length - 1];
    }
}
