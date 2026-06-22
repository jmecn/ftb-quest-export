package io.github.jmecn.ftbquestexport.pojo;

import java.util.List;

public record ItemsLangExportResult(int localesWritten, int itemCount, List<String> locales) {

    public static final ItemsLangExportResult EMPTY = new ItemsLangExportResult(0, 0, List.of());

    public ItemsLangExportResult {
        locales = List.copyOf(locales == null ? List.of() : locales);
    }
}
