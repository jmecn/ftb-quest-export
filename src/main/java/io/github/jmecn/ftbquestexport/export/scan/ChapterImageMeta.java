package io.github.jmecn.ftbquestexport.export.scan;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import io.github.jmecn.ftbquestexport.export.icons.ChapterImageFrames;
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
        ChapterImageFrames.Layout layout = ChapterImageFrames.analyze(icon);
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
}
