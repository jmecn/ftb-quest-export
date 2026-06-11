package io.github.jmecn.ftbquestexport.pojo;

import io.github.jmecn.ftbquestexport.model.ShapeAtlas;

public record ShapeAtlasExportResult(
        int shapes,
        int spritesPacked,
        int failures,
        long pngBytes,
        ShapeAtlas shapeAtlas) {}
