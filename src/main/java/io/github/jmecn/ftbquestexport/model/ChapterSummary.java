package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record ChapterSummary(
        String id,
        String filename,
        String group,
        int orderIndex,
        String icon,
        String title,
        List<String> subtitle) {
}
