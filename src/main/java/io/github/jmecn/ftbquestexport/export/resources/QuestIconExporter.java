package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.ftbquestexport.export.icons.QuestItemIconExporter;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Facade for quest closure icon export (per-item PNG, no atlas). */
public final class QuestIconExporter {

    private QuestIconExporter() {}

    public static boolean isEnabled() {
        return QuestItemIconExporter.isEnabled();
    }

    public static QuestItemIconExporter.Result export(
            Path iconsRoot,
            Minecraft client,
            Set<String> itemIds,
            Set<String> fluidIds) throws IOException {
        if (Files.exists(iconsRoot)) {
            deleteRecursive(iconsRoot);
        }
        Files.createDirectories(iconsRoot);
        return QuestItemIconExporter.export(iconsRoot, client, itemIds, fluidIds);
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            var paths = walk.sorted(java.util.Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
