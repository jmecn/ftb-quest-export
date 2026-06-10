package io.github.jmecn.ftbquestexport.export;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import io.github.jmecn.ftbquestexport.export.assets.ChapterImageExporter;
import io.github.jmecn.ftbquestexport.export.assets.QuestAssetExporter;
import io.github.jmecn.ftbquestexport.export.assets.QuestFluidExporter;
import io.github.jmecn.ftbquestexport.export.assets.QuestIconExporter;
import io.github.jmecn.ftbquestexport.export.assets.QuestItemNameKeysExporter;
import io.github.jmecn.ftbquestexport.export.assets.QuestItemsIndexExporter;
import io.github.jmecn.ftbquestexport.export.lang.QuestLangKeys;
import io.github.jmecn.ftbquestexport.export.lang.LangMergerExporter;
import io.github.jmecn.ftbquestexport.export.pojo.AssetExportResult;
import io.github.jmecn.ftbquestexport.export.pojo.FluidExportResult;
import io.github.jmecn.ftbquestexport.export.pojo.ScanBundle;
import io.github.jmecn.ftbquestexport.export.scan.QuestFileScanner;
import io.github.jmecn.ftbquestexport.export.scan.QuestRichTextScan;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.export.write.QuestJsonWriter;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Main quest-export pipeline. */
public final class QuestExportPipeline {

    private QuestExportPipeline() {}

    public static Component run(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Files.createDirectories(outputDir.resolve("assets/icons"));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("status", "export");
        manifest.put("exportedAt", Instant.now().toString());
        manifest.put("exporter", "ftb-quest-export");

        Minecraft client = Minecraft.getInstance();

        QuestScanResult scan = null;
        ScanBundle bundle;
        try {
            bundle = QuestFileScanner.scan();
            scan = bundle.scan();

            if (ChapterImageExporter.isEnabled()) {
                try {
                    var chapterImages = ChapterImageExporter.export(
                            outputDir,
                            FTBQuestsAPI.api().getQuestFile(false).getAllChapters(),
                            bundle.chapters());
                    manifest.put("chapterImages", Map.of(
                            "imagesSeen", chapterImages.imagesSeen(),
                            "uniqueBaked", chapterImages.uniqueBaked(),
                            "failures", chapterImages.failures(),
                            "pngBytes", chapterImages.pngBytes()));
                } catch (Throwable t) {
                    FtbQuestExportMod.LOGGER.error("chapter image export failed", t);
                    manifest.put("chapterImageExportError", t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }

            QuestJsonWriter.write(outputDir, bundle.index(), bundle.chapters());
            writeFilters(outputDir, scan);
            manifest.put("stats", scan.toStatsMap());
            FtbQuestExportMod.LOGGER.info("[export] quests JSON written under {}", outputDir.resolve("quests").toAbsolutePath());
        } catch (Throwable t) {
            FtbQuestExportMod.LOGGER.error("quest scan failed", t);
            manifest.put("error", t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        AssetExportResult resources = null;
        if (scan != null) {
            try {
                QuestRichTextScan.enrichTexturesFromLang(client, scan);
                resources = QuestAssetExporter.export(outputDir, client, scan);
                manifest.put("resources", resourceStats(resources));
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("resource export failed", t);
                manifest.put("resourceExportError", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (scan != null && QuestIconExporter.isEnabled()) {
            try {
                var icons = QuestIconExporter.export(
                        outputDir.resolve("assets/icons"),
                        client,
                        scan.getItems(),
                        scan.getFluids());
                manifest.put("icons", Map.of(
                        "itemsRendered", icons.itemsRendered(),
                        "fluidsRendered", icons.fluidsRendered(),
                        "fluidsSkipped", icons.fluidsSkipped(),
                        "failures", icons.failures(),
                        "pngBytes", icons.pngBytes()));
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("icon export failed", t);
                manifest.put("iconExportError", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (scan != null) {
            try {
                FluidExportResult fluids = QuestFluidExporter.export(outputDir, scan);
                manifest.put("fluids", Map.of("entries", fluids.fluidsWritten(), "bytes", fluids.bytes()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("fluid export failed", e);
            }
        }

        if (scan != null) {
            try {
                var itemsIndex = QuestItemsIndexExporter.export(outputDir, scan);
                manifest.put("itemsIndex", Map.of(
                        "itemRefs", itemsIndex.itemRefs(),
                        "fluidRefs", itemsIndex.fluidRefs(),
                        "bytes", itemsIndex.bytes()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("items-index export failed", e);
            }
        }

        if (LangMergerExporter.isEnabled()) {
            try {
                Set<String> langKeys = scan != null ? scan.getLangKeys() : Set.of();
                if (scan != null) {
                    langKeys = QuestLangKeys.mergeItemFluidLangKeys(
                            langKeys, scan.getItems(), scan.getFluids());
                    langKeys = QuestLangKeys.mergeBlockLangKeys(langKeys, scan.getBlocks());
                    langKeys = QuestLangKeys.mergeEntityLangKeys(langKeys, scan.getEntities());
                    langKeys = QuestLangKeys.mergeTagLangKeys(langKeys, scan.getTags());
                }
                var lang = LangMergerExporter.exportTo(
                        outputDir.resolve("lang"), client, null, langKeys.isEmpty() ? null : langKeys);
                manifest.put("lang", Map.of(
                        "files", lang.languagesWritten(),
                        "bytes", lang.totalBytes(),
                        "keys", lang.langKeysRequested()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("lang export failed", e);
            }
        }

        if (scan != null && QuestItemNameKeysExporter.isEnabled()) {
            try {
                var nameKeys = QuestItemNameKeysExporter.export(outputDir, client);
                manifest.put("itemNameKeys", Map.of(
                        "registryIds", nameKeys.registryIds(),
                        "fluids", nameKeys.fluidIds()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("item name-keys export failed", e);
            }
        }

        if (scan != null) {
            writeMeta(outputDir, scan, resources);
        }

        try {
            var size = ExportStats.summarize(outputDir);
            manifest.put("exportSize", ExportStats.toManifestMap(size));
        } catch (IOException e) {
            FtbQuestExportMod.LOGGER.warn("[export] could not summarize export directory size", e);
        }

        writeManifest(outputDir, manifest);
        return Component.literal("[ftb-quest-export] → " + outputDir.toAbsolutePath());
    }

    private static void writeFilters(Path outputDir, QuestScanResult scan) throws IOException {
        if (scan.getExpandedFilters().isEmpty()) {
            return;
        }
        Path out = outputDir.resolve("extras/filters.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, QuestExportJson.PRETTY.toJson(scan.getExpandedFilters()));
    }

    private static void writeMeta(
            Path outputDir,
            QuestScanResult scan,
            AssetExportResult resources) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("refs", scan.toRefsMap());
        meta.put("taskTypeSupport", defaultTaskTypeSupport());
        meta.put("extras", Map.of(
                "filters", "extras/filters.json",
                "fluids", "extras/fluids.json"));
        meta.put("stats", scan.toStatsMap());
        if (resources != null) {
            meta.put("assets", Map.of(
                    "assetFiles", resources.assetFiles(),
                    "dataFiles", resources.dataFiles(),
                    "seeded", resources.seededLocations(),
                    "written", resources.writtenLocations()));
        }
        Files.writeString(outputDir.resolve("meta.json"), QuestExportJson.PRETTY.toJson(meta));
    }

    private static Map<String, String> defaultTaskTypeSupport() {
        Map<String, String> support = new LinkedHashMap<>();
        support.put("item", "full");
        support.put("fluid", "full");
        support.put("checkmark", "full");
        support.put("observation", "display-only");
        support.put("dimension", "display-only");
        support.put("biome", "display-only");
        support.put("kill", "display-only");
        support.put("gamestage", "display-only");
        return support;
    }

    private static Map<String, Object> resourceStats(AssetExportResult resources) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assetFiles", resources.assetFiles());
        stats.put("dataFiles", resources.dataFiles());
        stats.put("assetBytes", resources.assetBytes());
        stats.put("dataBytes", resources.dataBytes());
        stats.put("failures", resources.failures());
        stats.put("serverSkipped", resources.serverSkipped());
        stats.put("assetsSeeded", resources.seededLocations());
        stats.put("assetsWritten", resources.writtenLocations());
        return stats;
    }

    private static void writeManifest(Path outputDir, Map<String, Object> manifest) throws IOException {
        Files.writeString(outputDir.resolve("manifest.json"), QuestExportJson.PRETTY.toJson(manifest));
    }
}
