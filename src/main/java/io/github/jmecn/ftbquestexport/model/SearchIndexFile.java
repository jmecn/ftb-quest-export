package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record SearchIndexFile(
        int schema,
        String locale,
        int questCount,
        List<SearchIndexQuestRow> quests) {

    public static SearchIndexFile of(String locale, List<SearchIndexQuestRow> quests) {
        return new SearchIndexFile(2, locale, quests.size(), quests);
    }
}
