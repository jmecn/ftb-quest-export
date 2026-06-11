package io.github.jmecn.ftbquestexport.icons;

import java.nio.file.Files;
import java.nio.file.Path;

public final class IconPathResolver {

    private IconPathResolver() {}

    public static Path resolveExportedTexture(Path outputDir, String ref) {
        String rel = textureIconRel(ref);
        if (rel == null) {
            return null;
        }
        Path path = outputDir.resolve(rel);
        return Files.isRegularFile(path) ? path : null;
    }

    static String textureIconRel(String ref) {
        int colon = ref.indexOf(':');
        if (colon <= 0 || colon >= ref.length() - 1) {
            return null;
        }
        String namespace = ref.substring(0, colon);
        String path = ref.substring(colon + 1);
        if (path.startsWith("textures/")) {
            String base = path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
            return "assets/" + namespace + "/" + base + ".png";
        }
        if (path.startsWith("block/") || path.startsWith("item/") || path.startsWith("gui/") || path.startsWith("icons/")) {
            return "assets/" + namespace + "/textures/" + path + ".png";
        }
        return null;
    }
}
