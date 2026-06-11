package io.github.jmecn.ftbquestexport.icons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.IResourceIcon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.PixelBuffer;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageBakeResult;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageLayout;
import io.github.jmecn.ftbquestexport.model.ChapterImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChapterImages {

    public record AnimationMeta(int frameTime, List<Integer> frameSequence) {}

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

    public static ChapterImage applyAnimationMeta(
            Icon icon, ChapterImage img) {
        ChapterImageLayout layout = analyze(icon);
        if (layout.frameCount() <= 1) {
            return img;
        }
        AnimationMeta animation = readAnimationMeta(icon, layout.frameCount());
        return img.withAnimation(
                true,
                layout.frameCount(),
                animation.frameTime() > 0 ? animation.frameTime() : null,
                animation.frameSequence().isEmpty() ? null : animation.frameSequence(),
                layout.frameWidth() > 0 ? layout.frameWidth() : null,
                layout.frameHeight() > 0 ? layout.frameHeight() : null);
    }

    public static ChapterImageBakeResult bake(
            dev.ftb.mods.ftbquests.quest.ChapterImage chapterImage,
            Path outputFile,
            GuiGraphics guiGraphics) throws IOException {
        Icon icon = chapterImage.getImage();
        if (icon == null || icon.isEmpty()) {
            return null;
        }

        Color4I tint = vertexTint(chapterImage);
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
                bakeRgbTint(scaled, tint);
                strip.setRGB(0, i * frameH, scaled);
            }
        } else {
            frameCount = 1;
            frameW = targetPx;
            frameH = targetPx;
            strip = renderFallback(icon, tint, frameW, frameH, guiGraphics);
        }

        Files.createDirectories(outputFile.getParent());
        ImageIO.write(strip.toImage(BufferedImage.TYPE_INT_ARGB), "png", outputFile.toFile());
        return new ChapterImageBakeResult(outputFile, frameCount, frameW, frameH, Files.size(outputFile));
    }

    public static Color4I vertexTint(dev.ftb.mods.ftbquests.quest.ChapterImage chapterImage) {
        if (!chapterImage.getColor().equals(Color4I.WHITE)) {
            return chapterImage.getColor();
        }
        return Color4I.WHITE;
    }

    public static int targetFramePixels(dev.ftb.mods.ftbquests.quest.ChapterImage chapterImage) {
        double grid = Math.max(chapterImage.getWidth(), chapterImage.getHeight());
        return Math.max(QuestExportConstants.CHAPTER_IMAGE_MIN_FRAME_PX, (int) Math.ceil(grid * 16D * 2D));
    }

    public static AnimationMeta readAnimationMeta(Icon icon, int textureFrameCount) {
        int frameTime = 1;
        List<Integer> sequence = new ArrayList<>();
        ResourceLocation texture = textureResourceLocation(icon);
        if (texture == null) {
            return defaultAnimationMeta(textureFrameCount);
        }
        ResourceLocation metaId = ResourceLocation.fromNamespaceAndPath(
                texture.getNamespace(),
                texture.getPath() + ".mcmeta");
        Minecraft client = Minecraft.getInstance();
        try {
            Resource resource = client.getResourceManager().getResource(metaId).orElse(null);
            if (resource == null) {
                return defaultAnimationMeta(textureFrameCount);
            }
            try (var reader = new java.io.InputStreamReader(resource.open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("animation")) {
                    return defaultAnimationMeta(textureFrameCount);
                }
                JsonObject animation = root.getAsJsonObject("animation");
                if (animation.has("frametime")) {
                    frameTime = Math.max(1, animation.get("frametime").getAsInt());
                }
                if (animation.has("frames")) {
                    JsonArray frames = animation.getAsJsonArray("frames");
                    for (JsonElement element : frames) {
                        if (element.isJsonPrimitive()) {
                            sequence.add(element.getAsInt());
                        } else if (element.isJsonObject()) {
                            JsonObject frame = element.getAsJsonObject();
                            if (frame.has("index")) {
                                sequence.add(frame.get("index").getAsInt());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return defaultAnimationMeta(textureFrameCount);
        }
        if (sequence.isEmpty()) {
            return defaultAnimationMeta(textureFrameCount, frameTime);
        }
        return new AnimationMeta(frameTime, List.copyOf(sequence));
    }

    private static AnimationMeta defaultAnimationMeta(int textureFrameCount) {
        return defaultAnimationMeta(textureFrameCount, 1);
    }

    private static AnimationMeta defaultAnimationMeta(int textureFrameCount, int frameTime) {
        int count = Math.max(1, textureFrameCount);
        List<Integer> sequence = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sequence.add(i);
        }
        return new AnimationMeta(frameTime, List.copyOf(sequence));
    }

    private static ResourceLocation textureResourceLocation(Icon icon) {
        Icon source = unwrapAnimation(icon);
        if (source instanceof IResourceIcon resourceIcon) {
            ResourceLocation id = resourceIcon.getResourceLocation();
            String path = id.getPath();
            if (path.startsWith("textures/")) {
                return id;
            }
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + path + ".png");
        }
        if (source instanceof ImageIcon imageIcon) {
            String s = imageIcon.toString();
            if (s.contains(":")) {
                String[] parts = s.split(":", 2);
                String path = parts[1];
                if (!path.endsWith(".png")) {
                    path = path + ".png";
                }
                if (!path.startsWith("textures/")) {
                    path = "textures/" + path;
                }
                return ResourceLocation.fromNamespaceAndPath(parts[0], path);
            }
        }
        return null;
    }

    private static PixelBuffer renderFallback(
            Icon icon,
            Color4I tint,
            int frameW,
            int frameH,
            GuiGraphics guiGraphics) throws IOException {
        Path temp = Files.createTempFile("chapter-image-bake", ".png");
        try (var renderer = new OffScreenRenderer(frameW, frameH)) {
            renderer.setupFlatGuiRendering();
            renderer.captureAsPng(() -> {
                icon.draw(guiGraphics, 0, 0, frameW, frameH);
                guiGraphics.flush();
            }, temp);
            PixelBuffer buffer = PixelBuffer.from(ImageIO.read(temp.toFile()));
            bakeRgbTint(buffer, tint);
            return buffer;
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

    static void bakeRgbTint(PixelBuffer buffer, Color4I tint) {
        int tintR = tint.redi();
        int tintG = tint.greeni();
        int tintB = tint.bluei();
        int[] pixels = buffer.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            int outR = r * tintR / 255;
            int outG = g * tintG / 255;
            int outB = b * tintB / 255;
            pixels[i] = (a << 24) | (outR << 16) | (outG << 8) | outB;
        }
        buffer.setPixels(pixels);
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
