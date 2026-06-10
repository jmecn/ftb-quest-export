package io.github.jmecn.ftbquestexport.export.icons;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftblibrary.math.PixelBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Bakes FTB chapter decoration icons into tinted PNG strips (full animation frames). */
public final class ChapterImageBaker {

    private static final int MIN_FRAME_PX = 16;

    public record BakeResult(Path outputFile, int frameCount, int frameWidth, int frameHeight, long bytes) {}

    private ChapterImageBaker() {}

    public static BakeResult bake(
            ChapterImage chapterImage,
            Path outputFile,
            Minecraft client,
            GuiGraphics guiGraphics) throws IOException {
        Icon icon = chapterImage.getImage();
        if (icon == null || icon.isEmpty()) {
            return null;
        }

        Color4I mod = vertexColor(chapterImage);
        int targetPx = targetFramePixels(chapterImage);

        List<PixelBuffer> frames = ChapterImageFrames.extractFrames(icon);
        PixelBuffer strip;
        int frameW;
        int frameH;
        int frameCount;

        if (!frames.isEmpty()) {
            frameCount = frames.size();
            frameW = targetPx;
            frameH = targetPx;
            strip = new PixelBuffer(frameW, frameH * frameCount);
            for (int i = 0; i < frameCount; i++) {
                PixelBuffer scaled = scaleFrame(frames.get(i), frameW, frameH);
                modulate(scaled, mod);
                strip.setRGB(0, i * frameH, scaled);
            }
        } else {
            frameCount = 1;
            frameW = targetPx;
            frameH = targetPx;
            strip = renderFallback(icon, mod, frameW, frameH, client, guiGraphics);
            if (strip == null) {
                return null;
            }
        }

        Files.createDirectories(outputFile.getParent());
        ImageIO.write(strip.toImage(BufferedImage.TYPE_INT_ARGB), "png", outputFile.toFile());
        return new BakeResult(outputFile, frameCount, frameW, frameH, Files.size(outputFile));
    }

    public static Color4I vertexColor(ChapterImage chapterImage) {
        if (!chapterImage.getColor().equals(Color4I.WHITE) || chapterImage.getAlpha() < 255) {
            return chapterImage.getColor().withAlpha(chapterImage.getAlpha());
        }
        return Color4I.WHITE;
    }

    public static int targetFramePixels(ChapterImage chapterImage) {
        double grid = Math.max(chapterImage.getWidth(), chapterImage.getHeight());
        return Math.max(MIN_FRAME_PX, (int) Math.ceil(grid * 16D * 2D));
    }

    private static PixelBuffer renderFallback(
            Icon icon,
            Color4I mod,
            int frameW,
            int frameH,
            Minecraft client,
            GuiGraphics guiGraphics) throws IOException {
        Icon drawIcon = !mod.equals(Color4I.WHITE) ? icon.withColor(mod) : icon;
        Path temp = Files.createTempFile("chapter-image-bake", ".png");
        try (var renderer = new OffScreenRenderer(frameW, frameH)) {
            renderer.setupFlatGuiRendering();
            Icon finalDrawIcon = drawIcon;
            renderer.captureAsPng(() -> finalDrawIcon.draw(guiGraphics, 0, 0, frameW, frameH), temp);
            return PixelBuffer.from(ImageIO.read(temp.toFile()));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static PixelBuffer scaleFrame(PixelBuffer source, int targetW, int targetH) {
        if (source.getWidth() == targetW && source.getHeight() == targetH) {
            return source.copy();
        }
        BufferedImage img = source.toImage(BufferedImage.TYPE_INT_ARGB);
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(img, 0, 0, targetW, targetH, null);
        graphics.dispose();
        return PixelBuffer.from(scaled);
    }

    private static void modulate(PixelBuffer buffer, Color4I mod) {
        if (mod.equals(Color4I.WHITE)) {
            return;
        }
        int modR = mod.redi();
        int modG = mod.greeni();
        int modB = mod.bluei();
        int modA = mod.alphai();
        int[] pixels = buffer.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = modulatePixel(pixels[i], modR, modG, modB, modA);
        }
        buffer.setPixels(pixels);
    }

    private static int modulatePixel(int argb, int modR, int modG, int modB, int modA) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int outA = a * modA / 255;
        int outR = r * modR / 255;
        int outG = g * modG / 255;
        int outB = b * modB / 255;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }
}
