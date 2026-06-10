package io.github.jmecn.ftbquestexport.export;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.ftbquestexport.export.resources.ExportDirectoryStats;
import io.github.jmecn.ftbquestexport.export.resources.QuestClosureResourceExporter;
import io.github.jmecn.ftbquestexport.export.resources.QuestFluidExporter;
import io.github.jmecn.ftbquestexport.export.icons.QuestItemIconExporter;
import io.github.jmecn.ftbquestexport.export.resources.QuestIconExporter;
import io.github.jmecn.ftbquestexport.export.resources.QuestItemNameKeysExporter;
import io.github.jmecn.ftbquestexport.export.resources.QuestItemsIndexExporter;
import io.github.jmecn.ftbquestexport.export.resources.QuestLangExporter;
import io.github.jmecn.ftbquestexport.export.lang.LangClosureKeys;
import io.github.jmecn.ftbquestexport.export.scan.QuestFileScanner;
import io.github.jmecn.ftbquestexport.export.scan.QuestRichTextScan;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.export.write.QuestJsonWriter;
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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        try {
            QuestFileScanner.ScanBundle bundle = QuestFileScanner.scan();
            scan = bundle.scan();
            QuestJsonWriter.write(outputDir, bundle.index(), bundle.chapters());
            writeFilters(outputDir, scan);
            manifest.put("stats", scan.toStatsMap());
            FtbQuestExportMod.LOGGER.info("[export] quests JSON written under {}", outputDir.resolve("quests").toAbsolutePath());
        } catch (Throwable t) {
            FtbQuestExportMod.LOGGER.error("quest scan failed", t);
            manifest.put("error", t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        QuestClosureResourceExporter.Result resources = null;
        if (scan != null) {
            try {
                QuestRichTextScan.enrichFromLangClosure(client, scan);
                resources = QuestClosureResourceExporter.export(outputDir, client, scan);
                manifest.put("resources", resourceStats(resources));
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("resource closure export failed", t);
                manifest.put("resourceExportError", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (scan != null && QuestIconExporter.isEnabled()) {
            try {
                QuestItemIconExporter.Result icons = QuestIconExporter.export(
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
                QuestFluidExporter.Result fluids = QuestFluidExporter.export(outputDir, scan);
                manifest.put("fluids", Map.of("entries", fluids.fluidsWritten(), "bytes", fluids.bytes()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("fluid export failed", e);
            }
        }

        if (scan != null) {
            try {
                QuestItemsIndexExporter.Result itemsIndex = QuestItemsIndexExporter.export(outputDir, scan);
                manifest.put("itemsIndex", Map.of(
                        "itemRefs", itemsIndex.itemRefs(),
                        "fluidRefs", itemsIndex.fluidRefs(),
                        "bytes", itemsIndex.bytes()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("items-index export failed", e);
            }
        }

        if (QuestLangExporter.isEnabled()) {
            try {
                Set<String> langKeys = scan != null ? scan.getLangKeys() : Set.of();
                if (scan != null) {
                    langKeys = LangClosureKeys.mergeClosureLangKeys(
                            langKeys, scan.getItems(), scan.getFluids());
                    langKeys = LangClosureKeys.mergeBlockLangKeys(langKeys, scan.getBlocks());
                    langKeys = LangClosureKeys.mergeEntityLangKeys(langKeys, scan.getEntities());
                    langKeys = LangClosureKeys.mergeTagLangKeys(langKeys, scan.getTags());
                }
                QuestLangExporter.Result lang = QuestLangExporter.export(
                        outputDir, client, null, langKeys.isEmpty() ? null : langKeys);
                manifest.put("lang", Map.of(
                        "files", lang.languagesWritten(),
                        "bytes", lang.totalBytes(),
                        "mode", langKeys.isEmpty() ? "full" : "closure",
                        "closureKeys", lang.closureKeysRequested()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("lang export failed", e);
            }
        }

        if (scan != null && QuestItemNameKeysExporter.isEnabled()) {
            try {
                QuestItemNameKeysExporter.Result nameKeys = QuestItemNameKeysExporter.export(outputDir, client);
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
            ExportDirectoryStats.Summary size = ExportDirectoryStats.summarize(outputDir);
            manifest.put("exportSize", ExportDirectoryStats.toMap(size));
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
        Files.writeString(out, GSON.toJson(scan.getExpandedFilters()));
    }

    private static void writeMeta(
            Path outputDir,
            QuestScanResult scan,
            QuestClosureResourceExporter.Result resources) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("refs", scan.toRefsMap());
        meta.put("taskTypeSupport", defaultTaskTypeSupport());
        meta.put("extras", Map.of(
                "filters", "extras/filters.json",
                "fluids", "extras/fluids.json"));
        meta.put("stats", scan.toStatsMap());
        if (resources != null) {
            meta.put("closure", Map.of(
                    "assetFiles", resources.assetFiles(),
                    "dataFiles", resources.dataFiles(),
                    "seeded", resources.seededLocations(),
                    "written", resources.writtenLocations()));
        }
        Files.writeString(outputDir.resolve("meta.json"), GSON.toJson(meta));
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

    private static Map<String, Object> resourceStats(QuestClosureResourceExporter.Result resources) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assetFiles", resources.assetFiles());
        stats.put("dataFiles", resources.dataFiles());
        stats.put("assetBytes", resources.assetBytes());
        stats.put("dataBytes", resources.dataBytes());
        stats.put("failures", resources.failures());
        stats.put("serverSkipped", resources.serverSkipped());
        stats.put("closureSeeded", resources.seededLocations());
        stats.put("closureWritten", resources.writtenLocations());
        return stats;
    }

    private static void writeManifest(Path outputDir, Map<String, Object> manifest) throws IOException {
        Files.writeString(outputDir.resolve("manifest.json"), GSON.toJson(manifest));
    }
}
