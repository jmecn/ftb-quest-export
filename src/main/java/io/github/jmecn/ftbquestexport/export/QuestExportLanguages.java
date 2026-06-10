package io.github.jmecn.ftbquestexport.export;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Reads export locale codes from {@code -Dquest.exportLanguages=...}. */
public final class QuestExportLanguages {

    private QuestExportLanguages() {}

    public static Set<String> resolve() {
        String raw = System.getProperty(QuestExportConstants.EXPORT_LANGUAGES_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} unset — exporting {} only; QuestBook-Modern CI should set this from Language enum",
                    QuestExportConstants.EXPORT_LANGUAGES_PROPERTY,
                    QuestExportConstants.FALLBACK_LOCALE);
            return Set.of(QuestExportConstants.FALLBACK_LOCALE);
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

    public static Collection<String> exportLocales(Minecraft client) {
        Set<String> configured = resolve();
        if (configured != null) {
            return configured;
        }
        return client.getLanguageManager().getLanguages().keySet();
    }
}
