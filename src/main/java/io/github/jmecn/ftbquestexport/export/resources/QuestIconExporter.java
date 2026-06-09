package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.minecraftwebexport.export.emi.ItemIconRendererExporter;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/** Writes quest item icons to {@code quest-export/assets/icons/items/} via MWE atlas + slice. */
public final class QuestIconExporter {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

    private QuestIconExporter() {}

    public record Result(
            ItemIconRendererExporter.Result atlas,
            QuestIconAtlasSlicer.Result slice) {}

    public static Result export(
            Path iconsRoot,
            Minecraft client,
            Set<String> onlyItemIds,
            Map<String, Integer> usageWeights) throws IOException {
        Files.createDirectories(iconsRoot);
        Path tempRoot = Files.createTempDirectory("ftb-quest-export-icons");
        try {
            ItemIconRendererExporter.Result atlas = ItemIconRendererExporter.export(
                    tempRoot,
                    client,
                    onlyItemIds,
                    Set.of(),
                    usageWeights,
                    Map.of());
            Path emiIcons = tempRoot.resolve("emi").resolve("icons");
            if (!Files.isDirectory(emiIcons)) {
                throw new IOException("Expected icon atlas at " + emiIcons);
            }
            if (Files.exists(iconsRoot)) {
                FileUtils.deleteDirectory(iconsRoot.toFile());
            }
            Files.createDirectories(iconsRoot.getParent());
            FileUtils.copyDirectory(emiIcons.toFile(), iconsRoot.toFile());

            QuestIconAtlasSlicer.Result slice = QuestIconAtlasSlicer.slice(iconsRoot);
            LOGGER.info(
                    "Quest item icons: {} rendered, {} sliced to per-item PNGs",
                    atlas.itemsWritten(),
                    slice.itemsSliced());
            return new Result(atlas, slice);
        } finally {
            deleteRecursive(tempRoot);
        }
    }

    private static void deleteRecursive(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOGGER.warn("Could not delete {}", path, e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not clean temp dir {}", root, e);
        }
    }
}
