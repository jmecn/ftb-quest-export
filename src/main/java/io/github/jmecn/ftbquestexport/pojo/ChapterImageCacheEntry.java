package io.github.jmecn.ftbquestexport.pojo;

public record ChapterImageCacheEntry(
        String relative, int frameCount, int frameWidth, int frameHeight, long bytes) {}
