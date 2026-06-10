package io.github.jmecn.ftbquestexport.export.pojo;

public record ChapterImageCacheEntry(
        String relative, int frameCount, int frameWidth, int frameHeight, long bytes) {}
