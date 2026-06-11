package io.github.jmecn.ftbquestexport.pojo;

import java.util.Map;

public record ExportMeta(
        QuestRefsSnapshot refs,
        Map<String, String> taskTypeSupport,
        ExportMetaExtras extras,
        QuestScanStats stats,
        ExportMetaAssets assets) {}
