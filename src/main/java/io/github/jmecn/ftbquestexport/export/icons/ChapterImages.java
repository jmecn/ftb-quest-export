package io.github.jmecn.ftbquestexport.export.icons;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.PixelBuffer;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import io.github.jmecn.ftbquestexport.export.pojo.ChapterImageBakeResult;
import io.github.jmecn.ftbquestexport.export.pojo.ChapterImageLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Frame layout, extraction, and PNG baking for FTB chapter decoration icons. */
public final class ChapterImages {

    private ChapterImages() {}

    public static ChapterImageLayout analyze(Icon icon) {
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            PixelBuffer first = pixelBufferForIcon(animation.list.get(0));
            int frameW = first != null ? first.getWidth() : 0;
            int frameH = first != null ? first.getHeight() : 0;
            return new ChapterImageLayout(animation.list.size(), frameW, frameH);
        }

        Icon source = unwrapAnimation(icon);
        if (source == null || source.isEmpty()) {
            return new ChapterImageLayout(0, 0, 0);
        }

        int frameCount = source.getPixelBufferFrameCount();
        PixelBuffer buffer = pixelBufferForIcon(source);
        int frameWidth = 0;
        int frameHeight = 0;

        if (buffer != null) {
            frameWidth = buffer.getWidth();
            frameHeight = buffer.getHeight();
            if (frameCount <= 1 && frameWidth > 0 && frameHeight > frameWidth && frameHeight % frameWidth == 0) {
                frameCount = frameHeight / frameWidth;
                frameHeight = frameWidth;
            } else if (frameCount > 1 && frameWidth > 0 && frameHeight > 0) {
                if (frameHeight > frameWidth && frameHeight % frameWidth == 0
                        && frameHeight / frameWidth == frameCount) {
                    frameHeight = frameWidth;
                } else {
                    frameHeight = frameHeight / frameCount;
                }
            }
        }

        if (frameCount <= 0) {
            frameCount = 1;
        }
        return new ChapterImageLayout(frameCount, frameWidth, frameHeight);
    }

    public static List<PixelBuffer> extractFrames(Icon icon) {
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            List<PixelBuffer> frames = new ArrayList<>(animation.list.size());
            for (Icon frame : animation.list) {
                PixelBuffer buffer = pixelBufferForIcon(frame);
                if (buffer != null) {
                    frames.add(buffer);
                }
            }
            return frames;
        }

        Icon source = unwrapAnimation(icon);
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        PixelBuffer buffer = pixelBufferForIcon(source);
        if (buffer == null) {
            return List.of();
        }

        ChapterImageLayout layout = analyze(icon);
        if (layout.frameCount() <= 1) {
            return List.of(buffer);
        }

        List<PixelBuffer> frames = new ArrayList<>(layout.frameCount());
        for (int i = 0; i < layout.frameCount(); i++) {
            frames.add(buffer.getSubimage(0, i * layout.frameHeight(), layout.frameWidth(), layout.frameHeight()));
        }
        return frames;
    }

    public static void applyAnimationMeta(Icon icon, Map<String, Object> img) {
        ChapterImageLayout layout = analyze(icon);
        if (layout.frameCount() <= 1) {
            return;
        }
        img.put("animated", true);
        img.put("frameCount", layout.frameCount());
        if (layout.frameWidth() > 0) {
            img.put("frameWidth", layout.frameWidth());
        }
        if (layout.frameHeight() > 0) {
            img.put("frameHeight", layout.frameHeight());
        }
    }

    public static ChapterImageBakeResult bake(
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

        List<PixelBuffer> frames = extractFrames(icon);
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
        return new ChapterImageBakeResult(outputFile, frameCount, frameW, frameH, Files.size(outputFile));
    }

    public static Color4I vertexColor(ChapterImage chapterImage) {
        if (!chapterImage.getColor().equals(Color4I.WHITE) || chapterImage.getAlpha() < 255) {
            return chapterImage.getColor().withAlpha(chapterImage.getAlpha());
        }
        return Color4I.WHITE;
    }

    public static int targetFramePixels(ChapterImage chapterImage) {
        double grid = Math.max(chapterImage.getWidth(), chapterImage.getHeight());
        return Math.max(QuestExportConstants.CHAPTER_IMAGE_MIN_FRAME_PX, (int) Math.ceil(grid * 16D * 2D));
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

    private static PixelBuffer pixelBufferForIcon(Icon icon) {
        if (icon == null || icon.isEmpty()) {
            return null;
        }
        PixelBuffer buffer = icon.createPixelBuffer();
        if (buffer == null) {
            return null;
        }
        if (icon instanceof ImageIcon imageIcon) {
            return cropImageIcon(imageIcon, buffer);
        }
        return buffer;
    }

    private static PixelBuffer cropImageIcon(ImageIcon icon, PixelBuffer full) {
        if (icon.minU == 0F && icon.minV == 0F && icon.maxU == 1F && icon.maxV == 1F) {
            return full;
        }
        int x = Math.round(icon.minU * full.getWidth());
        int y = Math.round(icon.minV * full.getHeight());
        int w = Math.max(1, Math.round((icon.maxU - icon.minU) * full.getWidth()));
        int h = Math.max(1, Math.round((icon.maxV - icon.minV) * full.getHeight()));
        return full.getSubimage(x, y, w, h);
    }

    private static Icon unwrapAnimation(Icon icon) {
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            return animation.list.get(0);
        }
        return icon;
    }
}
