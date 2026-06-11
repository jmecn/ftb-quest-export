package io.github.jmecn.ftbquestexport.pojo;

public record AssetExportManifestSection(
        int assetFiles,
        int dataFiles,
        long assetBytes,
        long dataBytes,
        int failures,
        boolean serverSkipped,
        int assetsSeeded,
        int assetsWritten) {

    public static AssetExportManifestSection from(AssetExportResult resources) {
        return new AssetExportManifestSection(
                resources.assetFiles(),
                resources.dataFiles(),
                resources.assetBytes(),
                resources.dataBytes(),
                resources.failures(),
                resources.serverSkipped(),
                resources.seededLocations(),
                resources.writtenLocations());
    }
}
