package io.github.jmecn.ftbquestexport.pojo;

public record ChapterIconAtlasExportResult(
        int chaptersProcessed,
        int atlasFilesWritten,
        int spritesPacked,
        int failures,
        long pngBytes) {}
