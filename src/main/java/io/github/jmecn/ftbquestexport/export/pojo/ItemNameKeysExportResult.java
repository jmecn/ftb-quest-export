package io.github.jmecn.ftbquestexport.export.pojo;

public record ItemNameKeysExportResult(int registryIds, int fluidIds) {

    public static final ItemNameKeysExportResult EMPTY = new ItemNameKeysExportResult(0, 0);
}
