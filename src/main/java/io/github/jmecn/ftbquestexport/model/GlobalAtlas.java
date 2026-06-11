package io.github.jmecn.ftbquestexport.model;

import java.util.LinkedHashMap;

public record GlobalAtlas(
        String src,
        int width,
        int height,
        String missingIconId,
        LinkedHashMap<String, GlobalSpriteRect> sprites) {}
