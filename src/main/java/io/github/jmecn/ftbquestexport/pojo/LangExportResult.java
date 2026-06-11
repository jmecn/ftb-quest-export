package io.github.jmecn.ftbquestexport.pojo;

public record LangExportResult(
        int languagesWritten,
        long totalBytes,
        int duplicateKeyWarnings,
        int langKeysRequested,
        int keysSkipped,
        int keysPerLanguage) {}
