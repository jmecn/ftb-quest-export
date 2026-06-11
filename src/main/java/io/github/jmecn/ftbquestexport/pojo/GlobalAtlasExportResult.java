package io.github.jmecn.ftbquestexport.pojo;

import io.github.jmecn.ftbquestexport.model.ChapterSummary;
import io.github.jmecn.ftbquestexport.model.GlobalAtlas;

import java.util.List;

public record GlobalAtlasExportResult(
        int shapeCount,
        int chapterIconCount,
        int spriteCount,
        int failures,
        long pngBytes,
        GlobalAtlas globalAtlas,
        List<ChapterSummary> chapters) {}
