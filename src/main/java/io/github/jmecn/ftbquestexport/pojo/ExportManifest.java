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
        ShapeAtlasExportResult shapeAtlas,
        String shapeAtlasExportError,
        FluidExportResult fluids,
        ItemsIndexExportResult itemsIndex,
        LangExportResult lang,
        ItemNameKeysExportResult itemNameKeys,
        SearchIndexManifestSection searchIndex,
        AssetExportManifestSection resources,
        String resourceExportError,
        ManifestExportSize exportSize) {}
