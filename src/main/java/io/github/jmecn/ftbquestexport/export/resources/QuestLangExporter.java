package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.ftbquestexport.export.lang.LangMergerExporter;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public final class QuestLangExporter {

    private QuestLangExporter() {}

    public record Result(
            int languagesWritten,
            long totalBytes,
            int duplicateKeyWarnings,
            int closureKeysRequested,
            int keysSkipped,
            int keysPerLanguage) {}

    public static boolean isEnabled() {
        return LangMergerExporter.isEnabled();
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
        LangMergerExporter.Result merged = LangMergerExporter.exportTo(langRoot, client, onlyNamespaces, onlyKeys);
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
        FtbQuestExportMod.LOGGER.info("[lang] writing compose-lang for items-lang -> {}", composeRoot);
        LangMergerExporter.exportTo(composeRoot, client, null, null);
    }
}
