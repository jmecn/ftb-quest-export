package io.github.jmecn.ftbquestexport.export.icons;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.PixelBuffer;

import java.util.ArrayList;
import java.util.List;

/** Frame layout for FTB chapter decoration icons (texture strips or {@link IconAnimation}). */
public final class ChapterImageFrames {

    public record Layout(int frameCount, int frameWidth, int frameHeight) {}

    private ChapterImageFrames() {}

    public static Layout analyze(Icon icon) {
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            PixelBuffer first = pixelBufferForIcon(animation.list.get(0));
            int frameW = first != null ? first.getWidth() : 0;
            int frameH = first != null ? first.getHeight() : 0;
            return new Layout(animation.list.size(), frameW, frameH);
        }

        Icon source = unwrapAnimation(icon);
        if (source == null || source.isEmpty()) {
            return new Layout(0, 0, 0);
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
        return new Layout(frameCount, frameWidth, frameHeight);
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

        Layout layout = analyze(icon);
        if (layout.frameCount() <= 1) {
            return List.of(buffer);
        }

        List<PixelBuffer> frames = new ArrayList<>(layout.frameCount());
        for (int i = 0; i < layout.frameCount(); i++) {
            frames.add(buffer.getSubimage(0, i * layout.frameHeight(), layout.frameWidth(), layout.frameHeight()));
        }
        return frames;
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
