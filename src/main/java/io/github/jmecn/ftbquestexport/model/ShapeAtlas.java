package io.github.jmecn.ftbquestexport.model;

import java.util.LinkedHashMap;

public record ShapeAtlas(
        String src,
        int width,
        int height,
        String missingIconId,
        LinkedHashMap<String, ShapeSpriteRect> sprites) {}
