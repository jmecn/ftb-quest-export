package io.github.jmecn.ftbquestexport;

import io.github.jmecn.ftbquestexport.pojo.ExportDirectorySummary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExportStats {

    private ExportStats() {}

    public static ExportDirectorySummary summarize(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new ExportDirectorySummary(0, 0);
        }
        var acc = new long[2];
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                acc[0]++;
                acc[1] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return new ExportDirectorySummary(acc[0], acc[1]);
    }

    public static Map<String, Object> toManifestMap(ExportDirectorySummary summary) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exportFileCount", summary.fileCount());
        m.put("exportTotalBytes", summary.totalBytes());
        return m;
    }
}
