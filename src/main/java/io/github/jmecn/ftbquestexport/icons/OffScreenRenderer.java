package io.github.jmecn.ftbquestexport.icons;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/** Renders GUI/item icons into an off-screen buffer and writes PNG bytes or files. */
public final class OffScreenRenderer implements AutoCloseable {

    private final NativeImage nativeImage;
    private final TextureTarget frameBuffer;

    public OffScreenRenderer(int width, int height) {
        RenderSystem.viewport(0, 0, width, height);
        nativeImage = new NativeImage(width, height, true);
        frameBuffer = new TextureTarget(width, height, true, true);
        frameBuffer.setClearColor(0, 0, 0, 0);
        frameBuffer.clear(true);
    }

    @Override
    public void close() {
        nativeImage.close();
        frameBuffer.destroyBuffers();

        var minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
    }

    public void captureAsPng(Runnable runnable, Path path) throws IOException {
        renderToBuffer(runnable);
        nativeImage.writeToFile(path);
    }

    /** Renders into the buffer and returns a copy (safe to cache while the renderer is reused). */
    public NativeImage captureToNativeImage(Runnable runnable) {
        renderToBuffer(runnable);
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

    public boolean isAnimated(Collection<TextureAtlasSprite> sprites) {
        return false;
    }

    public void uploadAnimatedFirstFrame(Collection<TextureAtlasSprite> sprites) {
        // Static first frame is enough for quest icon export.
    }

    public void setupFlatGuiRendering() {
        var matrix4f = new Matrix4f().setOrtho(0.0f, 16, 16, 0.0f, 1000.0f, 21000.0f);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        var poseStack = RenderSystem.getModelViewStack();
        poseStack.setIdentity();
        poseStack.translate(0.0f, 0.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        FogRenderer.setupNoFog();
    }

    public void setupItemRendering() {
        var matrix4f = new Matrix4f().setOrtho(0.0f, 16, 16, 0.0f, 1000.0f, 21000.0f);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        var poseStack = RenderSystem.getModelViewStack();
        poseStack.setIdentity();
        poseStack.translate(0.0f, 0.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        FogRenderer.setupNoFog();
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
