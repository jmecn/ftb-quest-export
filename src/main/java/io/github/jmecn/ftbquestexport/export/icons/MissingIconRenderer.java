package io.github.jmecn.ftbquestexport.export.icons;

import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Path;

/** Checkerboard placeholder when item/fluid icon render fails (QuestBook fallback). */
public final class MissingIconRenderer {

    /** Registry id used by {@code QuestBook-React} {@code quest-item-icon.ts}. */
    public static final String REGISTRY_ID = "minecraft_web_export:missing_icon";

    private MissingIconRenderer() {}

    public static void render(Minecraft client, GuiGraphics guiGraphics, OffScreenRenderer renderer, Path output)
            throws IOException {
        Runnable draw = () -> {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int color = ((x / 8) + (y / 8)) % 2 == 0
                            ? QuestExportConstants.MISSING_ICON_MAGENTA
                            : QuestExportConstants.MISSING_ICON_BLACK;
                    guiGraphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        };
        renderer.setupFlatGuiRendering();
        renderer.captureAsPng(draw, output);
    }
}
