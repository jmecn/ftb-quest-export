package io.github.jmecn.ftbquestexport.export.pojo;

public record ItemIconExportResult(
        int itemsRendered, int fluidsRendered, int fluidsSkipped, int failures, long pngBytes) {}
