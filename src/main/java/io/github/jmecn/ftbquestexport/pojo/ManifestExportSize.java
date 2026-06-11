package io.github.jmecn.ftbquestexport.pojo;

public record ManifestExportSize(long exportFileCount, long exportTotalBytes) {

    public static ManifestExportSize from(ExportDirectorySummary summary) {
        return new ManifestExportSize(summary.fileCount(), summary.totalBytes());
    }
}
