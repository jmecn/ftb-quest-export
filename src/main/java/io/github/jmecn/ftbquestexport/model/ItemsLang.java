package io.github.jmecn.ftbquestexport.model;

import io.github.jmecn.ftbquestexport.QuestExportConstants;
import java.util.List;

public record ItemsLang(int schema, String locale, int itemCount, List<ItemsLangEntry> items) {

    public ItemsLang {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public static ItemsLang of(String locale, List<ItemsLangEntry> items) {
        return new ItemsLang(QuestExportConstants.ITEMS_LANG_SCHEMA, locale, items.size(), items);
    }
}
