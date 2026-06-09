package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits MWE item icon atlas pages into per-item PNGs under {@code icons/items/<ns>/<path>.png},
 * then removes atlas pages, CSS, and {@code index.json}.
 */
public final class QuestIconAtlasSlicer {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type INDEX_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private QuestIconAtlasSlicer() {}

    public record Result(int itemsSliced, int failures, long pngBytes) {}

    public static Result slice(Path iconsRoot) throws IOException {
        Path indexPath = iconsRoot.resolve("index.json");
        if (!Files.isRegularFile(indexPath)) {
            LOGGER.warn("Icon atlas index missing at {}", indexPath.toAbsolutePath());
            return new Result(0, 0, 0);
        }

        Map<String, Object> index = GSON.fromJson(Files.readString(indexPath), INDEX_TYPE);
        if (index == null) {
            throw new IOException("Invalid icon index: " + indexPath);
        }

        int cellSize = intField(index.get("cellSize"), 32);
        List<Map<String, Object>> pages = castList(index.get("pages"));
        Map<String, Map<String, Object>> items = castItemMap(index.get("items"));
        if (items.isEmpty()) {
            LOGGER.warn("Icon atlas index has no items");
            cleanupAtlas(iconsRoot, pages);
            return new Result(0, 0, 0);
        }

        Path itemsRoot = iconsRoot.resolve("items");
        Files.createDirectories(itemsRoot);

        Map<Integer, NativeImage> pageCache = new HashMap<>();
        int sliced = 0;
        int failures = 0;
        long pngBytes = 0;
        List<String> manifestItems = new ArrayList<>();

        try {
            for (var entry : items.entrySet()) {
                String itemId = entry.getKey();
                Map<String, Object> sprite = entry.getValue();
                if (sprite == null) {
                    failures++;
                    continue;
                }

                String[] parts = splitItemId(itemId);
                if (parts == null) {
                    LOGGER.warn("Skipping invalid item id '{}'", itemId);
                    failures++;
                    continue;
                }

                int page = intField(sprite.get("page"), -1);
                int x = intField(sprite.get("x"), 0);
                int y = intField(sprite.get("y"), 0);
                if (page < 0 || page >= pages.size()) {
                    LOGGER.warn("Skipping '{}' — page {} out of range", itemId, page);
                    failures++;
                    continue;
                }

                String pageFile = stringField(pages.get(page).get("file"));
                if (pageFile.isEmpty()) {
                    failures++;
                    continue;
                }

                NativeImage pageImage = pageCache.computeIfAbsent(page, key -> {
                    Path pagePath = iconsRoot.resolve(pageFile);
                    try (InputStream in = Files.newInputStream(pagePath)) {
                        return NativeImage.read(in);
                    } catch (IOException e) {
                        throw new RuntimeException("failed to read atlas page " + pageFile, e);
                    }
                });

                Path out = itemsRoot.resolve(parts[0]).resolve(parts[1] + ".png");
                Files.createDirectories(out.getParent());
                try (NativeImage tile = crop(pageImage, x, y, cellSize, cellSize)) {
                    tile.writeToFile(out);
                }
                pngBytes += Files.size(out);
                manifestItems.add(itemId);
                sliced++;
            }
        } finally {
            for (NativeImage image : pageCache.values()) {
                image.close();
            }
        }

        writeManifest(itemsRoot, cellSize, manifestItems);
        cleanupAtlas(iconsRoot, pages);

        LOGGER.info(
                "Sliced {} item icons ({} failures, {} bytes) under {}",
                sliced,
                failures,
                pngBytes,
                itemsRoot.toAbsolutePath());

        return new Result(sliced, failures, pngBytes);
    }

    private static void writeManifest(Path itemsRoot, int cellSize, List<String> itemIds) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", 1);
        manifest.put("cellSize", cellSize);
        manifest.put("count", itemIds.size());
        manifest.put("items", itemIds);
        Files.writeString(itemsRoot.resolve("manifest.json"), GSON.toJson(manifest));
    }

    private static void cleanupAtlas(Path iconsRoot, List<Map<String, Object>> pages) throws IOException {
        for (Map<String, Object> page : pages) {
            String file = stringField(page.get("file"));
            if (!file.isEmpty()) {
                Files.deleteIfExists(iconsRoot.resolve(file));
            }
        }
        Files.deleteIfExists(iconsRoot.resolve("icons.css"));
        Files.deleteIfExists(iconsRoot.resolve("index.json"));
    }

    private static NativeImage crop(NativeImage source, int x, int y, int width, int height) {
        NativeImage cropped = new NativeImage(width, height, true);
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                cropped.setPixelRGBA(dx, dy, source.getPixelRGBA(x + dx, y + dy));
            }
        }
        return cropped;
    }

    private static String[] splitItemId(String itemId) {
        int colon = itemId.indexOf(':');
        if (colon <= 0 || colon >= itemId.length() - 1) {
            return null;
        }
        return new String[] {itemId.substring(0, colon), itemId.substring(colon + 1)};
    }

    private static int intField(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static String stringField(Object value) {
        return value != null ? value.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> castItemMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Map<String, Object>>) map;
        }
        return Map.of();
    }
}
