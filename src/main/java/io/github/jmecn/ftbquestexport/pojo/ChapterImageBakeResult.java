package io.github.jmecn.ftbquestexport.pojo;

import java.nio.file.Path;

public record ChapterImageBakeResult(
        Path outputFile, int frameCount, int frameWidth, int frameHeight, long bytes) {}
