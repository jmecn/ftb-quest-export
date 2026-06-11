package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record SearchIndexManifestSection(
        int locales,
        int quests,
        List<String> files) {
}
