package io.github.jmecn.ftbquestexport.export.pojo;

public record AssetExportResult(
        int assetFiles,
        int dataFiles,
        long assetBytes,
        long dataBytes,
        int failures,
        boolean serverSkipped,
        int seededLocations,
        int writtenLocations) {}
