package io.github.jmecn.ftbquestexport.export;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads export locale codes from {@code -Dquest.exportLanguages=...}.
 * Language list is owned by QuestBook-Modern ({@code Language} enum); CI sets this property.
 */
public final class QuestExportLanguages {

    private static final Logger LOGGER = LogManager.getLogger(QuestExportLanguages.class);
    private static final String PROPERTY = "quest.exportLanguages";
    private static final String FALLBACK = "en_us";

    private QuestExportLanguages() {}

    /** @return configured locales, or {@code null} when property is {@code *} (all MC languages) */
    public static Set<String> resolve() {
        String raw = System.getProperty(PROPERTY, "").trim();
        if (raw.isEmpty()) {
            LOGGER.warn(
                    "{} unset — exporting {} only; QuestBook-Modern CI should set this from Language enum",
                    PROPERTY,
                    FALLBACK);
            return Set.of(FALLBACK);
        }
        if ("*".equals(raw)) {
            return null;
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public static List<String> asList() {
        Set<String> languages = resolve();
        if (languages == null) {
            return List.of();
        }
        return languages.stream().sorted().toList();
    }
}
