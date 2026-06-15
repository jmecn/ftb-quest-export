package io.github.jmecn.ftbquestexport;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import io.github.jmecn.ftbquestexport.assets.ChapterIconAtlasExporter;
import io.github.jmecn.ftbquestexport.assets.ChapterImageExporter;
import io.github.jmecn.ftbquestexport.assets.GlobalAtlasExporter;
import io.github.jmecn.ftbquestexport.assets.QuestAssetExporter;
import io.github.jmecn.ftbquestexport.assets.QuestFluidExporter;
import io.github.jmecn.ftbquestexport.assets.QuestItemNameKeysExporter;
import io.github.jmecn.ftbquestexport.assets.QuestItemsIndexExporter;
import io.github.jmecn.ftbquestexport.lang.QuestLangKeys;
import io.github.jmecn.ftbquestexport.lang.LangMergerExporter;
import io.github.jmecn.ftbquestexport.lang.QuestSearchIndexExporter;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.pojo.AssetExportManifestSection;
import io.github.jmecn.ftbquestexport.pojo.AssetExportResult;
import io.github.jmecn.ftbquestexport.pojo.ChapterIconAtlasExportResult;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageExportResult;
import io.github.jmecn.ftbquestexport.pojo.ExportManifest;
import io.github.jmecn.ftbquestexport.pojo.ExportManifestBuilder;
import io.github.jmecn.ftbquestexport.pojo.ExportMeta;
import io.github.jmecn.ftbquestexport.pojo.ExportMetaAssets;
import io.github.jmecn.ftbquestexport.pojo.ExportMetaExtras;
import io.github.jmecn.ftbquestexport.pojo.FluidExportResult;
import io.github.jmecn.ftbquestexport.pojo.ItemNameKeysExportResult;
import io.github.jmecn.ftbquestexport.pojo.ItemsIndexExportResult;
import io.github.jmecn.ftbquestexport.pojo.LangExportResult;
import io.github.jmecn.ftbquestexport.pojo.ManifestExportSize;
import io.github.jmecn.ftbquestexport.pojo.QuestSearchIndexExportResult;
import io.github.jmecn.ftbquestexport.pojo.ScanBundle;
import io.github.jmecn.ftbquestexport.pojo.GlobalAtlasExportResult;
import io.github.jmecn.ftbquestexport.model.QuestIndex;
import io.github.jmecn.ftbquestexport.model.SearchIndexManifestSection;
import io.github.jmecn.ftbquestexport.scan.QuestFileScanner;
import io.github.jmecn.ftbquestexport.scan.QuestRichTextScan;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class QuestExportPipeline {

    private QuestExportPipeline() {}

    public static Component run(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        ExportManifestBuilder manifest = new ExportManifestBuilder()
                .status("export")
                .exportedAt(Instant.now().toString())
                .exporter("ftb-quest-export");

        Minecraft client = Minecraft.getInstance();

        QuestScanResult scan = null;
        ScanBundle bundle = null;
        try {
            bundle = QuestFileScanner.scan();
            scan = bundle.scan();

            if (ChapterImageExporter.isEnabled()) {
                try {
                    ChapterImageExportResult chapterImages = ChapterImageExporter.export(
                            outputDir,
                            FTBQuestsAPI.api().getQuestFile(false).getAllChapters(),
                            bundle.chapters());
                    manifest.chapterImages(chapterImages);
                } catch (Throwable t) {
                    FtbQuestExportMod.LOGGER.error("chapter image export failed", t);
                    manifest.chapterImageExportError(t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }

            write(outputDir, bundle.index(), bundle.chapters());
            writeFilters(outputDir, scan);
            manifest.stats(scan.toStats());
            FtbQuestExportMod.LOGGER.info("[export] quests JSON written under {}", outputDir.resolve("quests").toAbsolutePath());
        } catch (Throwable t) {
            FtbQuestExportMod.LOGGER.error("quest scan failed", t);
            manifest.error(t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        AssetExportResult resources = null;
        if (scan != null) {
            try {
                QuestRichTextScan.enrichTexturesFromLang(client, scan);
                resources = QuestAssetExporter.export(outputDir, client, scan);
                manifest.resources(AssetExportManifestSection.from(resources));
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("resource export failed", t);
                manifest.resourceExportError(t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (bundle != null && scan != null) {
            try {
                GlobalAtlasExportResult globalAtlas = GlobalAtlasExporter.export(
                        outputDir, client, scan, bundle.index(), scan.getFluids());
                manifest.globalAtlas(globalAtlas);
                if (globalAtlas.globalAtlas() != null) {
                    QuestIndex indexWithGlobal = bundle.index().withGlobalAtlas(
                            globalAtlas.globalAtlas(), globalAtlas.chapters());
                    writeIndex(outputDir, indexWithGlobal);
                }
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("global atlas export failed", t);
                manifest.globalAtlasExportError(t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (bundle != null && scan != null && ChapterIconAtlasExporter.isEnabled()) {
            try {
                ChapterIconAtlasExportResult chapterAtlases = ChapterIconAtlasExporter.export(
                        outputDir,
                        bundle.index(),
                        bundle.chapters(),
                        client,
                        scan.getFluids());
                writeChapters(outputDir, bundle.chapters());
                manifest.chapterIconAtlases(chapterAtlases);
            } catch (Throwable t) {
                FtbQuestExportMod.LOGGER.error("chapter icon atlas export failed", t);
                manifest.chapterIconAtlasExportError(t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        if (scan != null) {
            try {
                FluidExportResult fluids = QuestFluidExporter.export(outputDir, scan);
                manifest.fluids(fluids);
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("fluid export failed", e);
            }
        }

        if (scan != null) {
            try {
                ItemsIndexExportResult itemsIndex = QuestItemsIndexExporter.export(outputDir, scan);
                manifest.itemsIndex(itemsIndex);
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
                LangExportResult lang = LangMergerExporter.exportTo(
                        outputDir.resolve("lang"), client, null, langKeys.isEmpty() ? null : langKeys);
                manifest.lang(lang);
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("lang export failed", e);
            }
        }

        if (scan != null && QuestItemNameKeysExporter.isEnabled()) {
            try {
                ItemNameKeysExportResult nameKeys = QuestItemNameKeysExporter.export(outputDir, client);
                manifest.itemNameKeys(nameKeys);
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("item name-keys export failed", e);
            }
        }

        if (bundle != null && QuestSearchIndexExporter.isEnabled()) {
            try {
                QuestSearchIndexExportResult searchIndex = QuestSearchIndexExporter.export(outputDir, bundle.chapters());
                manifest.searchIndex(new SearchIndexManifestSection(
                        searchIndex.localesWritten(),
                        searchIndex.questCount(),
                        searchIndex.locales()));
            } catch (IOException e) {
                FtbQuestExportMod.LOGGER.error("search-index export failed", e);
            }
        }

        if (scan != null) {
            writeMeta(outputDir, scan, resources);
        }

        try {
            ManifestExportSize size = ManifestExportSize.from(ExportStats.summarize(outputDir));
            manifest.exportSize(size);
        } catch (IOException e) {
            FtbQuestExportMod.LOGGER.warn("[export] could not summarize export directory size", e);
        }

        writeManifest(outputDir, manifest.build());
        return Component.literal("[ftb-quest-export] → " + outputDir.toAbsolutePath());
    }

    public static void write(Path outputDir, QuestIndex index, Map<String, ChapterData> chapters)
            throws IOException {
        Path questsRoot = outputDir.resolve("quests");
        Files.createDirectories(questsRoot.resolve("chapters"));
        Files.writeString(questsRoot.resolve("index.json"), QuestExportJson.GSON.toJson(index));
        writeChapters(outputDir, chapters);
    }

    public static void writeIndex(Path outputDir, QuestIndex index) throws IOException {
        Path questsRoot = outputDir.resolve("quests");
        Files.createDirectories(questsRoot);
        Files.writeString(questsRoot.resolve("index.json"), QuestExportJson.GSON.toJson(index));
    }

    public static void writeChapters(Path outputDir, Map<String, ChapterData> chapters) throws IOException {
        Path chaptersRoot = outputDir.resolve("quests/chapters");
        Files.createDirectories(chaptersRoot);
        for (Map.Entry<String, ChapterData> entry : chapters.entrySet()) {
            Path chapterFile = chaptersRoot.resolve(entry.getKey() + ".json");
            Files.writeString(chapterFile, QuestExportJson.GSON.toJson(entry.getValue()));
        }
    }

    private static void writeFilters(Path outputDir, QuestScanResult scan) throws IOException {
        if (scan.getExpandedFilters().isEmpty()) {
            return;
        }
        Path out = outputDir.resolve("extras/filters.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, QuestExportJson.GSON.toJson(scan.getExpandedFilters()));
    }

    private static void writeMeta(
            Path outputDir,
            QuestScanResult scan,
            AssetExportResult resources) throws IOException {
        ExportMeta meta = new ExportMeta(
                scan.toRefs(),
                defaultTaskTypeSupport(),
                new ExportMetaExtras("extras/filters.json", "extras/fluids.json"),
                scan.toStats(),
                resources != null ? ExportMetaAssets.from(resources) : null);
        Files.writeString(outputDir.resolve("meta.json"), QuestExportJson.GSON.toJson(meta));
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

    private static void writeManifest(Path outputDir, ExportManifest manifest) throws IOException {
        Files.writeString(outputDir.resolve("manifest.json"), QuestExportJson.GSON.toJson(manifest));
    }
}
