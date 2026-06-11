package io.github.jmecn.ftbquestexport.pojo;

import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.model.QuestIndex;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;

import java.util.Map;

public record ScanBundle(
        QuestScanResult scan,
        QuestIndex index,
        Map<String, ChapterData> chapters) {}
