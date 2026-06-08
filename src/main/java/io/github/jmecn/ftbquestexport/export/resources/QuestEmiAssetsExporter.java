package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.minecraftwebexport.export.emi.IconPlaceholderRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes {@code assets/bundle.json} and mirrors {@code lang/} under {@code assets/lang/}
 * so {@code emi-recipe-renderer} can use {@code quest-export/assets/} as {@code baseUrl}.
 */
public final class QuestEmiAssetsExporter {

    private static final int BUNDLE_SCHEMA = 2;
    private static final Gson GSON = new GsonBuilder().create();

    private QuestEmiAssetsExporter() {}

    public static void finalizeIconBundle(Path outputDir) throws IOException {
        Path assetsDir = outputDir.resolve("assets");
        Path iconsDir = assetsDir.resolve("icons");
        if (!Files.isRegularFile(iconsDir.resolve("index.json"))) {
            return;
        }

        Path langDir = outputDir.resolve("lang");
        List<String> languages = listLangLocales(langDir);

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("schema", BUNDLE_SCHEMA);
        bundle.put("imageScale", 2);
        bundle.put("languages", languages.isEmpty() ? List.of("en_us") : languages);
        bundle.put("recipeCount", 0);
        bundle.put("recipeImageFormat", "png");
        bundle.put("missingIconId", IconPlaceholderRenderer.REGISTRY_ID);

        Files.createDirectories(assetsDir);
        Files.writeString(assetsDir.resolve("bundle.json"), GSON.toJson(bundle));

        if (!Files.isDirectory(langDir)) {
            return;
        }
        Path emiLangDir = assetsDir.resolve("lang");
        Files.createDirectories(emiLangDir);
        try (Stream<Path> stream = Files.list(langDir)) {
            for (Path src : stream.filter(QuestEmiAssetsExporter::isLangJson).toList()) {
                Files.copy(src, emiLangDir.resolve(src.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static List<String> listLangLocales(Path langDir) throws IOException {
        if (!Files.isDirectory(langDir)) {
            return List.of();
        }
        List<String> locales = new ArrayList<>();
        try (Stream<Path> stream = Files.list(langDir)) {
            stream.filter(QuestEmiAssetsExporter::isLangJson)
                    .map(path -> {
                        String name = path.getFileName().toString();
                        return name.substring(0, name.length() - ".json".length());
                    })
                    .sorted()
                    .forEach(locales::add);
        }
        return locales;
    }

    private static boolean isLangJson(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json");
    }
}
