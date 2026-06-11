package io.github.jmecn.ftbquestexport.pojo;

import java.util.List;

public record QuestRefsSnapshot(
        List<String> items,
        List<String> tags,
        List<String> fluids,
        List<String> blocks,
        List<String> entities,
        List<String> textures,
        List<String> langKeys) {}
