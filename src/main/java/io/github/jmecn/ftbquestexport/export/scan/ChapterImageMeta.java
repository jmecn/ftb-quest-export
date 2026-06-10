package io.github.jmecn.ftbquestexport.export.scan;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.math.PixelBuffer;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Chapter decoration image fields for web export (tint, anchor, animation, optional SNBT). */
public final class ChapterImageMeta {

    private ChapterImageMeta() {}

    public static void exportDisplayFields(ChapterImage image, Map<String, Object> img) {
        img.put("alpha", image.getAlpha());
        if (!image.getColor().equals(Color4I.WHITE)) {
            img.put("color", image.getColor().rgb());
        }
        if (image.isAlignToCorner()) {
            img.put("alignToCorner", true);
        }
        exportOptionalSnbtFields(image, img);
        applyAnimationMeta(image.getImage(), img);
    }

    private static void exportOptionalSnbtFields(ChapterImage image, Map<String, Object> img) {
        CompoundTag tag = new CompoundTag();
        image.writeData(tag);
        if (tag.contains("dependency")) {
            img.put("dependency", tag.getString("dependency"));
        }
        if (tag.getBoolean("dev")) {
            img.put("editorsOnly", true);
        }
        ListTag hoverTag = tag.getList("hover", Tag.TAG_STRING);
        if (!hoverTag.isEmpty()) {
            List<String> hover = new ArrayList<>(hoverTag.size());
            for (int i = 0; i < hoverTag.size(); i++) {
                hover.add(hoverTag.getString(i));
            }
            img.put("hover", hover);
        }
    }

    static void applyAnimationMeta(Icon icon, Map<String, Object> img) {
        Icon source = unwrapAnimation(icon);
        if (source == null || source.isEmpty()) {
            return;
        }

        int frameCount = source.getPixelBufferFrameCount();
        PixelBuffer buffer = source.createPixelBuffer();
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

        if (frameCount <= 1) {
            return;
        }

        img.put("animated", true);
        img.put("frameCount", frameCount);
        if (frameWidth > 0) {
            img.put("frameWidth", frameWidth);
        }
        if (frameHeight > 0) {
            img.put("frameHeight", frameHeight);
        }
    }

    private static Icon unwrapAnimation(Icon icon) {
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            return animation.list.get(0);
        }
        return icon;
    }
}
