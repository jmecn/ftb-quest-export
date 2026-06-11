package io.github.jmecn.ftbquestexport.model;

import java.util.LinkedHashMap;

/** Global UI atlas: FTB node shapes, sidebar chapter icons, missing-icon placeholder. */
public record GlobalAtlas(
        String src,
        int width,
        int height,
        String missingIconId,
        LinkedHashMap<String, GlobalSpriteRect> sprites) {}
