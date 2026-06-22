package io.github.jmecn.ftbquestexport.pojo;

import io.github.jmecn.ftbquestexport.model.SearchIndexManifestSection;

public record ExportManifest(
        String status,
        String exportedAt,
        String exporter,
        String error,
        QuestScanStats stats,
        ChapterImageExportResult chapterImages,
        String chapterImageExportError,
        ChapterIconAtlasExportResult chapterIconAtlases,
        String chapterIconAtlasExportError,
        GlobalAtlasExportResult globalAtlas,
        String globalAtlasExportError,
        FluidExportResult fluids,
        ItemsIndexExportResult itemsIndex,
        LangExportResult lang,
        ItemNameKeysExportResult itemNameKeys,
        ItemsLangExportResult itemsLang,
        SearchIndexManifestSection searchIndex,
        AssetExportManifestSection resources,
        String resourceExportError,
        ManifestExportSize exportSize) {}
