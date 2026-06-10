package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.ftbquestexport.export.QuestExportLanguages;
import io.github.jmecn.minecraftwebexport.export.emi.LangMergerExporter;
import io.github.jmecn.minecraftwebexport.export.module.ExportHints;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class QuestLangExporter {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

    private QuestLangExporter() {}

    public record Result(
            int languagesWritten,
            long totalBytes,
            int duplicateKeyWarnings,
            int closureKeysRequested,
            int keysSkipped,
            int keysPerLanguage) {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean("quest.skipLangExport");
    }

    public static Result export(Path outputDir, Minecraft client) throws IOException {
        return export(outputDir, client, null, null);
    }

    public static Result exportHandbookLang(Path outputDir, Minecraft client) throws IOException {
        return exportHandbookLang(outputDir, client, null, null);
    }

    public static Result exportHandbookLang(
            Path outputDir,
            Minecraft client,
            Set<String> onlyNamespaces,
            Set<String> onlyKeys) throws IOException {
        return exportTo(outputDir.resolve("lang"), client, onlyNamespaces, onlyKeys);
    }

    public static Result export(Path outputDir, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
            throws IOException {
        return exportHandbookLang(outputDir, client, onlyNamespaces, onlyKeys);
    }

    public static Result exportTo(Path langRoot, Minecraft client, Set<String> onlyNamespaces, Set<String> onlyKeys)
            throws IOException {
        LangMergerExporter.Result merged = LangMergerExporter.exportTo(
                langRoot, client, onlyNamespaces, onlyKeys, questExportHints());
        return new Result(
                merged.languagesWritten(),
                merged.totalBytes(),
                merged.duplicateKeyWarnings(),
                merged.closureKeysRequested(),
                merged.keysSkipped(),
                merged.keysPerLanguage());
    }

    /** Full mod lang merge for {@link QuestItemsLangExporter} ({@code material.*}, {@code tagprefix.*}, …). */
    public static void exportComposeLang(Path outputDir, Minecraft client) throws IOException {
        Path composeRoot = outputDir.resolve(QuestItemsLangExporter.COMPOSE_LANG_DIR);
        LOGGER.info("[lang] writing compose-lang for items-lang -> {}", composeRoot);
        LangMergerExporter.exportTo(composeRoot, client, null, null, questExportHints());
    }

    static ExportHints questExportHints() {
        List<String> langs = QuestExportLanguages.asList();
        if (langs.isEmpty()) {
            return ExportHints.defaults();
        }
        return new ExportHints(java.util.Map.of(), java.util.Map.of(), List.of(), false, langs);
    }
}
