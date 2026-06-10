package io.github.jmecn.ftbquestexport.export.assets;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import io.github.jmecn.ftbquestexport.export.QuestExportConstants;
import io.github.jmecn.ftbquestexport.export.icons.ChapterImages;
import io.github.jmecn.ftbquestexport.export.pojo.ChapterImageCacheEntry;
import io.github.jmecn.ftbquestexport.export.pojo.ChapterImageExportResult;
import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bakes chapter decoration images and writes {@code baked} paths into chapter JSON maps. */
public final class ChapterImageExporter {

    private ChapterImageExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_CHAPTER_IMAGE_EXPORT);
    }

    public static ChapterImageExportResult export(
            Path outputDir,
            List<Chapter> chapters,
            Map<String, Map<String, Object>> chapterJsonByFilename) throws IOException {
        Path bakeRoot = outputDir.resolve(QuestExportConstants.CHAPTER_IMAGES_REL_ROOT);
        Files.createDirectories(bakeRoot);

        Minecraft client = Minecraft.getInstance();
        var bufferSource = client.renderBuffers().bufferSource();
        var guiGraphics = new net.minecraft.client.gui.GuiGraphics(client, bufferSource);

        Map<String, ChapterImageCacheEntry> cache = new LinkedHashMap<>();
        int imagesSeen = 0;
        int uniqueBaked = 0;
        int failures = 0;
        long pngBytes = 0;

        for (Chapter chapter : chapters) {
            Map<String, Object> chapterJson = chapterJsonByFilename.get(chapter.getFilename());
            if (chapterJson == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> imageJsonList = (List<Map<String, Object>>) chapterJson.get("images");
            if (imageJsonList == null || imageJsonList.isEmpty()) {
                continue;
            }

            List<ChapterImage> sourceImages = chapter.getImages();
            int count = Math.min(sourceImages.size(), imageJsonList.size());
            for (int i = 0; i < count; i++) {
                imagesSeen++;
                ChapterImage source = sourceImages.get(i);
                Map<String, Object> imgJson = imageJsonList.get(i);

                try {
                    String cacheKey = bakeCacheKey(source);
                    ChapterImageCacheEntry entry = cache.get(cacheKey);
                    if (entry == null) {
                        String fileName = hashKey(cacheKey) + ".png";
                        Path out = bakeRoot.resolve(fileName);
                        var baked = ChapterImages.bake(source, out, client, guiGraphics);
                        if (baked == null) {
                            failures++;
                            continue;
                        }
                        entry = new ChapterImageCacheEntry(
                                QuestExportConstants.CHAPTER_IMAGES_REL_ROOT + "/" + fileName,
                                baked.frameCount(),
                                baked.frameWidth(),
                                baked.frameHeight(),
                                baked.bytes());
                        cache.put(cacheKey, entry);
                        uniqueBaked++;
                        pngBytes += entry.bytes();
                    }

                    if (entry.frameCount() > 1) {
                        imgJson.put("animated", true);
                        imgJson.put("frameCount", entry.frameCount());
                        imgJson.put("frameWidth", entry.frameWidth());
                        imgJson.put("frameHeight", entry.frameHeight());
                    }
                    imgJson.put("baked", entry.relative());
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn(
                            "[chapter-images] failed {} in chapter {}",
                            source.getImage(),
                            chapter.getFilename(),
                            ex);
                }
            }
        }

        FtbQuestExportMod.LOGGER.info(
                "[chapter-images] {} decoration(s), {} unique PNG(s), {} failure(s), {} bytes",
                imagesSeen,
                uniqueBaked,
                failures,
                pngBytes);
        return new ChapterImageExportResult(imagesSeen, uniqueBaked, failures, pngBytes);
    }

    static String bakeCacheKey(ChapterImage image) {
        var mod = ChapterImages.vertexColor(image);
        return image.getImage().toString() + "|rgba=" + mod.rgba();
    }

    private static String hashKey(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
