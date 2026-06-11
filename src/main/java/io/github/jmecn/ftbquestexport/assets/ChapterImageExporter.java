package io.github.jmecn.ftbquestexport.assets;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.icons.ChapterImages;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageBakeResult;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageCacheEntry;
import io.github.jmecn.ftbquestexport.pojo.ChapterImageExportResult;
import io.github.jmecn.ftbquestexport.model.ChapterImage;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bakes chapter decoration images and writes {@code baked} paths into chapter JSON. */
public final class ChapterImageExporter {

    private ChapterImageExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_CHAPTER_IMAGE_EXPORT);
    }

    public static ChapterImageExportResult export(
            Path outputDir,
            List<dev.ftb.mods.ftbquests.quest.Chapter> chapters,
            Map<String, ChapterData> chapterJsonByFilename) throws IOException {
        Path bakeRoot = outputDir.resolve(QuestExportConstants.CHAPTER_IMAGES_REL_ROOT);
        Files.createDirectories(bakeRoot);

        Minecraft client = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);

        Map<String, ChapterImageCacheEntry> cache = new LinkedHashMap<>();
        int imagesSeen = 0;
        int uniqueBaked = 0;
        int failures = 0;
        long pngBytes = 0;

        for (dev.ftb.mods.ftbquests.quest.Chapter chapter : chapters) {
            ChapterData chapterData = chapterJsonByFilename.get(chapter.getFilename());
            if (chapterData == null) {
                continue;
            }

            List<ChapterImage> imageJsonList = chapterData.images();
            if (imageJsonList == null || imageJsonList.isEmpty()) {
                continue;
            }

            List<dev.ftb.mods.ftbquests.quest.ChapterImage> sourceImages = chapter.getImages();
            int count = Math.min(sourceImages.size(), imageJsonList.size());
            List<ChapterImage> updatedImages = new ArrayList<>(imageJsonList);
            for (int i = 0; i < count; i++) {
                imagesSeen++;
                dev.ftb.mods.ftbquests.quest.ChapterImage source = sourceImages.get(i);
                ChapterImage imgJson = imageJsonList.get(i);

                try {
                    String cacheKey = bakeCacheKey(source);
                    ChapterImageCacheEntry entry = cache.get(cacheKey);
                    if (entry == null) {
                        String fileName = hashKey(cacheKey) + ".png";
                        Path out = bakeRoot.resolve(fileName);
                        ChapterImageBakeResult baked = ChapterImages.bake(source, out, guiGraphics);
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

                    ChapterImage updated = imgJson.withBaked(entry.relative());
                    if (entry.frameCount() > 1) {
                        ChapterImages.AnimationMeta animation = ChapterImages.readAnimationMeta(
                                source.getImage(),
                                entry.frameCount());
                        updated = updated.withAnimation(
                                true,
                                entry.frameCount(),
                                animation.frameTime() > 0 ? animation.frameTime() : null,
                                animation.frameSequence().isEmpty() ? null : animation.frameSequence(),
                                entry.frameWidth(),
                                entry.frameHeight());
                    }
                    updatedImages.set(i, updated);
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn(
                            "[chapter-images] failed {} in chapter {}",
                            source.getImage(),
                            chapter.getFilename(),
                            ex);
                }
            }
            chapterJsonByFilename.put(chapter.getFilename(), chapterData.withImages(updatedImages));
        }

        FtbQuestExportMod.LOGGER.info(
                "[chapter-images] {} decoration(s), {} unique PNG(s), {} failure(s), {} bytes",
                imagesSeen,
                uniqueBaked,
                failures,
                pngBytes);
        return new ChapterImageExportResult(imagesSeen, uniqueBaked, failures, pngBytes);
    }

    static String bakeCacheKey(dev.ftb.mods.ftbquests.quest.ChapterImage image) {
        Color4I tint = ChapterImages.vertexTint(image);
        return image.getImage().toString() + "|rgb=" + tint.rgb();
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
