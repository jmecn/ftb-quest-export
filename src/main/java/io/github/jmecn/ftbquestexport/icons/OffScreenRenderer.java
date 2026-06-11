package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Off-screen GL capture, aligned with {@code minecraft-web-export} {@code OffScreenRenderer}:
 * draw into a fixed framebuffer, then {@link #copyPixelsTo} or {@link #copyPixels}.
 */
public final class OffScreenRenderer implements AutoCloseable {

    static final int ITEM_LOGICAL_PX = 16;

    private final int width;
    private final int height;
    private final NativeImage nativeImage;
    private final TextureTarget frameBuffer;

    public OffScreenRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        RenderSystem.viewport(0, 0, width, height);
        nativeImage = new NativeImage(width, height, true);
        frameBuffer = new TextureTarget(width, height, true, true);
        frameBuffer.setClearColor(0, 0, 0, 0);
        frameBuffer.clear(true);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void close() {
        nativeImage.close();
        frameBuffer.destroyBuffers();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            var window = minecraft.getWindow();
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
        }
    }

    public void capture(Runnable runnable) {
        renderToBuffer(runnable);
    }

    public void captureAsPng(Runnable runnable, Path path) throws IOException {
        renderToBuffer(runnable);
        nativeImage.writeToFile(path);
    }

    /** Returns a copy of the last {@link #capture} result. */
    public NativeImage copyPixels() {
        return copyImage(nativeImage);
    }

    public static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    public void copyPixelsTo(NativeImage target, int destX, int destY) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tx = destX + x;
                int ty = destY + y;
                if (tx >= 0 && ty >= 0 && tx < target.getWidth() && ty < target.getHeight()) {
                    target.setPixelRGBA(tx, ty, nativeImage.getPixelRGBA(x, y));
                }
            }
        }
    }

    public static void blit(NativeImage target, NativeImage tile, int x, int y) {
        int w = tile.getWidth();
        int h = tile.getHeight();
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int tx = x + dx;
                int ty = y + dy;
                if (tx >= 0 && ty >= 0 && tx < target.getWidth() && ty < target.getHeight()) {
                    target.setPixelRGBA(tx, ty, tile.getPixelRGBA(dx, dy));
                }
            }
        }
    }

    /** Flat GUI / fluid / FTB {@code Icon.draw} — ortho matches framebuffer size. */
    public void setupFlatGuiRendering() {
        setupOrtho(width, height);
        Lighting.setupForFlatItems();
        FogRenderer.setupNoFog();
    }

    /** Item stack rendering — 16×16 logical slot, scaled via pose in the caller. */
    public void setupItemRendering() {
        setupOrtho(ITEM_LOGICAL_PX, ITEM_LOGICAL_PX);
        Lighting.setupFor3DItems();
        FogRenderer.setupNoFog();
    }

    private void setupOrtho(float logicalW, float logicalH) {
        var matrix4f = new Matrix4f().setOrtho(0.0f, logicalW, logicalH, 0.0f, 1000.0f, 21000.0f);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        var poseStack = RenderSystem.getModelViewStack();
        poseStack.setIdentity();
        poseStack.translate(0.0f, 0.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
    }

    private void renderToBuffer(Runnable runnable) {
        frameBuffer.bindWrite(true);
        GlStateManager._clear(GL12.GL_COLOR_BUFFER_BIT | GL12.GL_DEPTH_BUFFER_BIT, false);
        runnable.run();
        frameBuffer.unbindWrite();

        frameBuffer.bindRead();
        nativeImage.downloadTexture(0, false);
        nativeImage.flipY();
        frameBuffer.unbindRead();
    }
}
