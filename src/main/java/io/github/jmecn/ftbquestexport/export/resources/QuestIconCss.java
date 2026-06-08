package io.github.jmecn.ftbquestexport.export.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Renames MWE {@code .icon-atlas} selectors for quest-book sprites. */
public final class QuestIconCss {

    public static final String QUEST_ICON_CSS_CLASS = "quest-icon-atlas";

    private QuestIconCss() {}

    public static void rewriteExportedCss(Path iconsRoot) throws IOException {
        Path css = iconsRoot.resolve("icons.css");
        if (!Files.isRegularFile(css)) {
            return;
        }
        String content = Files.readString(css);
        if (!content.contains(".icon-atlas")) {
            return;
        }
        content = content.replace(".icon-atlas {", "." + QUEST_ICON_CSS_CLASS + " {");
        content = content.replace(".icon-atlas[", "." + QUEST_ICON_CSS_CLASS + "[");
        Files.writeString(css, content);
    }
}
