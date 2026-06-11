package io.github.jmecn.ftbquestexport.pojo;

import io.github.jmecn.ftbquestexport.model.SearchIndexManifestSection;

public final class ExportManifestBuilder {

    private String status;
    private String exportedAt;
    private String exporter;
    private String error;
    private QuestScanStats stats;
    private ChapterImageExportResult chapterImages;
    private String chapterImageExportError;
    private ChapterIconAtlasExportResult chapterIconAtlases;
    private String chapterIconAtlasExportError;
    private ShapeAtlasExportResult shapeAtlas;
    private String shapeAtlasExportError;
    private FluidExportResult fluids;
    private ItemsIndexExportResult itemsIndex;
    private LangExportResult lang;
    private ItemNameKeysExportResult itemNameKeys;
    private SearchIndexManifestSection searchIndex;
    private AssetExportManifestSection resources;
    private String resourceExportError;
    private ManifestExportSize exportSize;

    public ExportManifestBuilder status(String value) {
        this.status = value;
        return this;
    }

    public ExportManifestBuilder exportedAt(String value) {
        this.exportedAt = value;
        return this;
    }

    public ExportManifestBuilder exporter(String value) {
        this.exporter = value;
        return this;
    }

    public ExportManifestBuilder error(String value) {
        this.error = value;
        return this;
    }

    public ExportManifestBuilder stats(QuestScanStats value) {
        this.stats = value;
        return this;
    }

    public ExportManifestBuilder chapterImages(ChapterImageExportResult value) {
        this.chapterImages = value;
        return this;
    }

    public ExportManifestBuilder chapterImageExportError(String value) {
        this.chapterImageExportError = value;
        return this;
    }

    public ExportManifestBuilder chapterIconAtlases(ChapterIconAtlasExportResult value) {
        this.chapterIconAtlases = value;
        return this;
    }

    public ExportManifestBuilder chapterIconAtlasExportError(String value) {
        this.chapterIconAtlasExportError = value;
        return this;
    }

    public ExportManifestBuilder shapeAtlas(ShapeAtlasExportResult value) {
        this.shapeAtlas = value;
        return this;
    }

    public ExportManifestBuilder shapeAtlasExportError(String value) {
        this.shapeAtlasExportError = value;
        return this;
    }

    public ExportManifestBuilder fluids(FluidExportResult value) {
        this.fluids = value;
        return this;
    }

    public ExportManifestBuilder itemsIndex(ItemsIndexExportResult value) {
        this.itemsIndex = value;
        return this;
    }

    public ExportManifestBuilder lang(LangExportResult value) {
        this.lang = value;
        return this;
    }

    public ExportManifestBuilder itemNameKeys(ItemNameKeysExportResult value) {
        this.itemNameKeys = value;
        return this;
    }

    public ExportManifestBuilder searchIndex(SearchIndexManifestSection value) {
        this.searchIndex = value;
        return this;
    }

    public ExportManifestBuilder resources(AssetExportManifestSection value) {
        this.resources = value;
        return this;
    }

    public ExportManifestBuilder resourceExportError(String value) {
        this.resourceExportError = value;
        return this;
    }

    public ExportManifestBuilder exportSize(ManifestExportSize value) {
        this.exportSize = value;
        return this;
    }

    public ExportManifest build() {
        return new ExportManifest(
                status,
                exportedAt,
                exporter,
                error,
                stats,
                chapterImages,
                chapterImageExportError,
                chapterIconAtlases,
                chapterIconAtlasExportError,
                shapeAtlas,
                shapeAtlasExportError,
                fluids,
                itemsIndex,
                lang,
                itemNameKeys,
                searchIndex,
                resources,
                resourceExportError,
                exportSize);
    }
}
