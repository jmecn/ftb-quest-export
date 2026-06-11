package io.github.jmecn.ftbquestexport.pojo;

public record ExportMetaAssets(int assetFiles, int dataFiles, int seeded, int written) {

    public static ExportMetaAssets from(AssetExportResult resources) {
        return new ExportMetaAssets(
                resources.assetFiles(),
                resources.dataFiles(),
                resources.seededLocations(),
                resources.writtenLocations());
    }
}
