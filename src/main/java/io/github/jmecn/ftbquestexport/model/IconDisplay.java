package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record IconDisplay(
        String spriteId,
        int nodeOuterPx,
        int innerPx,
        List<IconDisplayFrame> frames) {
}