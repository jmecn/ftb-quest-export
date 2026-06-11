package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.ftbquestexport.QuestExportConstants;

public final class MissingIconRenderer {

    private MissingIconRenderer() {}

    public static NativeImage create(int tierPx) {
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
}
