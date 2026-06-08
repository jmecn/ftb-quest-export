package io.github.jmecn.ftbquestexport.export.scan;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads FTB Quest task/reward SNBT fields via {@code writeData}. */
final class QuestNbtExport {

    private QuestNbtExport() {}

    static CompoundTag writeTask(dev.ftb.mods.ftbquests.quest.task.Task task) {
        CompoundTag tag = new CompoundTag();
        task.writeData(tag);
        return tag;
    }

    static boolean optionalTask(CompoundTag tag) {
        return tag.getBoolean("optional_task");
    }

    static String string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : null;
    }

    static long longVal(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : 0L;
    }

    static net.minecraft.resources.ResourceLocation resource(CompoundTag tag, String key) {
        String s = string(tag, key);
        return s != null ? net.minecraft.resources.ResourceLocation.tryParse(s) : null;
    }
}
