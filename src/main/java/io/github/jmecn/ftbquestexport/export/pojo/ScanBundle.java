package io.github.jmecn.ftbquestexport.export.pojo;

import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;

import java.util.Map;

public record ScanBundle(
        QuestScanResult scan,
        Map<String, Object> index,
        Map<String, Map<String, Object>> chapters) {}
