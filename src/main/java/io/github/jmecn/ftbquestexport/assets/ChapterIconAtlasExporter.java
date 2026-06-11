package io.github.jmecn.ftbquestexport.assets;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jmecn.ftbquestexport.QuestExportConstants;
import io.github.jmecn.ftbquestexport.icons.ChapterSpriteCollector;
import io.github.jmecn.ftbquestexport.icons.ChapterSpriteCollector.SpriteNeed;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackItem;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackPage;
import io.github.jmecn.ftbquestexport.icons.MaxRectsPacker.PackRect;
import io.github.jmecn.ftbquestexport.icons.MissingIconRenderer;
import io.github.jmecn.ftbquestexport.icons.OffScreenRenderer;
import io.github.jmecn.ftbquestexport.icons.QuestIconDisplayWriter;
import io.github.jmecn.ftbquestexport.icons.QuestIconTileRenderer;
import io.github.jmecn.ftbquestexport.model.ChapterData;
import io.github.jmecn.ftbquestexport.pojo.ChapterIconAtlasExportResult;
import io.github.jmecn.ftbquestexport.model.IconAtlasPage;
import io.github.jmecn.ftbquestexport.model.IconSpriteRect;
import io.github.jmecn.ftbquestexport.model.QuestIndex;
import io.github.jmecn.ftbquestexport.model.QuestLink;
import io.github.jmecn.ftbquestexport.model.QuestNode;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Packs chapter quest icons into {@code quests/chapters/{filename}.png} and writes
 * {@code iconAtlases} / {@code iconSprites} / {@code iconDisplay} into chapter JSON.
 */
public final class ChapterIconAtlasExporter {

    private ChapterIconAtlasExporter() {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean(QuestExportConstants.SKIP_CHAPTER_ICON_ATLAS_EXPORT);
    }

    public static ChapterIconAtlasExportResult export(
            Path outputDir,
            QuestIndex index,
            Map<String, ChapterData> chapters,
            Minecraft client,
            Set<String> fluidIds) throws IOException {
        double gridScale = index.gridScale() > 0 ? index.gridScale() : 0.5;
        Map<String, QuestNode> questById = ChapterSpriteCollector.buildQuestCatalog(chapters);

        int chaptersProcessed = 0;
        int atlasFilesWritten = 0;
        int spritesPacked = 0;
        int failures = 0;
        long pngBytes = 0;

        Path chaptersDir = outputDir.resolve("quests/chapters");
        Files.createDirectories(chaptersDir);

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);
        Map<String, NativeImage> tileCache = new HashMap<>();
        Map<Integer, OffScreenRenderer> renderersByTier = new HashMap<>();

        try {
            for (Map.Entry<String, ChapterData> entry : chapters.entrySet()) {
                String filename = entry.getKey();
                ChapterData chapterData = entry.getValue();
                try {
                    Map<String, SpriteNeed> needs = ChapterSpriteCollector.collectChapterNeeds(
                            chapterData, questById, gridScale, fluidIds, client);
                    if (needs.isEmpty()) {
                        continue;
                    }

                    List<AtlasPagePlan> plans = buildPlans(needs);
                    ChapterData updated =
                            applyJsonFields(chapterData, filename, plans, questById, gridScale, fluidIds, client);
                    chapters.put(filename, updated);

                    for (AtlasPagePlan plan : plans) {
                        long bytes = renderAtlasPage(
                                outputDir,
                                client,
                                guiGraphics,
                                bufferSource,
                                fluidIds,
                                tileCache,
                                renderersByTier,
                                plan);
                        pngBytes += bytes;
                        atlasFilesWritten++;
                    }

                    chaptersProcessed++;
                    spritesPacked += needs.size();
                } catch (Exception ex) {
                    failures++;
                    FtbQuestExportMod.LOGGER.warn("[chapterData-icon-atlas] failed chapterData {}", filename, ex);
                }
            }
        } finally {
            bufferSource.endBatch();
            for (OffScreenRenderer renderer : renderersByTier.values()) {
                renderer.close();
            }
            for (NativeImage image : tileCache.values()) {
                image.close();
            }
        }

        FtbQuestExportMod.LOGGER.info(
                "[chapter-icon-atlas] {} chapter(s), {} atlas file(s), {} sprite(s), {} failure(s), {} bytes",
                chaptersProcessed,
                atlasFilesWritten,
                spritesPacked,
                failures,
                pngBytes);
        return new ChapterIconAtlasExportResult(
                chaptersProcessed, atlasFilesWritten, spritesPacked, failures, pngBytes);
    }

    private static List<AtlasPagePlan> buildPlans(Map<String, SpriteNeed> needs) {
        List<PackItem> items = new ArrayList<>();
        for (SpriteNeed need : needs.values()) {
            items.add(new PackItem(need.spriteId(), need.tier(), need.tier()));
        }
        MaxRectsPacker packer = MaxRectsPacker.defaults();
        packer.packAll(items);

        List<AtlasPagePlan> plans = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < packer.pages().size(); pageIndex++) {
            PackPage page = packer.pages().get(pageIndex);
            int[] size = packer.pageContentSize(pageIndex);
            LinkedHashMap<String, IconSpriteRect> sprites = new LinkedHashMap<>();
            for (PackRect rect : page.rects()) {
                sprites.put(
                        rect.name(),
                        new IconSpriteRect(
                                String.valueOf(pageIndex),
                                rect.x(),
                                rect.y(),
                                rect.width(),
                                rect.height()));
            }
            plans.add(new AtlasPagePlan(pageIndex, size[0], size[1], sprites));
        }
        return plans;
    }

    private static ChapterData applyJsonFields(
            ChapterData chapterData,
            String filename,
            List<AtlasPagePlan> plans,
            Map<String, QuestNode> questById,
            double gridScale,
            Set<String> fluidIds,
            Minecraft client) {
        int pageCount = plans.size();
        LinkedHashMap<String, IconAtlasPage> iconAtlases = new LinkedHashMap<>();
        LinkedHashMap<String, IconSpriteRect> iconSprites = new LinkedHashMap<>();

        for (AtlasPagePlan plan : plans) {
            String rel = atlasRelativePath(filename, plan.pageIndex(), pageCount);
            plan.relativeSrc = rel;
            iconAtlases.put(
                    String.valueOf(plan.pageIndex()),
                    new IconAtlasPage(rel, plan.width(), plan.height()));
            iconSprites.putAll(plan.sprites());
        }

        ChapterData updated = chapterData.withIconAtlases(iconAtlases).withIconSprites(iconSprites);

        if (updated.quests() != null) {
            List<QuestNode> quests = new ArrayList<>(updated.quests().size());
            for (QuestNode quest : updated.quests()) {
                QuestNode withDisplay =
                        QuestIconDisplayWriter.attachQuestNodeIconDisplay(quest, gridScale, fluidIds, client);
                quests.add(QuestIconDisplayWriter.attachTaskRewardIconDisplays(withDisplay, fluidIds, client));
            }
            updated = updated.withQuests(quests);
        }

        if (updated.questLinks() != null) {
            List<QuestLink> links = new ArrayList<>(updated.questLinks().size());
            for (QuestLink link : updated.questLinks()) {
                if (link.linkedQuest() == null || link.linkedQuest().isBlank()) {
                    links.add(link);
                    continue;
                }
                QuestNode linkedQuest = questById.get(link.linkedQuest());
                if (linkedQuest == null) {
                    links.add(link);
                    continue;
                }
                links.add(
                        QuestIconDisplayWriter.attachLinkIconDisplay(link, linkedQuest, gridScale, fluidIds, client));
            }
            updated = updated.withQuestLinks(links);
        }

        return updated;
    }

    private static long renderAtlasPage(
            Path outputDir,
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            Set<String> fluidIds,
            Map<String, NativeImage> tileCache,
            Map<Integer, OffScreenRenderer> renderersByTier,
            AtlasPagePlan plan) throws IOException {
        NativeImage atlas = new NativeImage(plan.width(), plan.height(), true);
        try {
            for (Map.Entry<String, IconSpriteRect> entry : plan.sprites().entrySet()) {
                String spriteId = entry.getKey();
                IconSpriteRect rect = entry.getValue();
                String ref = spriteId.substring(0, spriteId.lastIndexOf('@'));
                int tier = rect.w();
                NativeImage tile = tileCache.computeIfAbsent(
                        spriteId,
                        id -> rasterTile(
                                client,
                                guiGraphics,
                                bufferSource,
                                fluidIds,
                                renderersByTier,
                                ref,
                                tier));
                OffScreenRenderer.blit(atlas, tile, rect.x(), rect.y());
            }
            Path out = outputDir.resolve(plan.relativeSrc());
            Files.createDirectories(out.getParent());
            atlas.writeToFile(out);
            return Files.size(out);
        } finally {
            atlas.close();
        }
    }

    private static NativeImage rasterTile(
            Minecraft client,
            GuiGraphics guiGraphics,
            MultiBufferSource.BufferSource bufferSource,
            Set<String> fluidIds,
            Map<Integer, OffScreenRenderer> renderersByTier,
            String ref,
            int tier) {
        OffScreenRenderer renderer = renderersByTier.computeIfAbsent(tier, t -> new OffScreenRenderer(t, t));
        if (QuestIconTileRenderer.captureTile(
                client, guiGraphics, bufferSource, renderer, fluidIds, ref, tier)) {
            return renderer.copyPixels();
        }
        return MissingIconRenderer.create(tier);
    }

    private static String atlasRelativePath(String filename, int pageIndex, int pageCount) {
        if (pageCount == 1 || pageIndex == 0) {
            return "quests/chapters/" + filename + ".png";
        }
        return "quests/chapters/" + filename + "-" + pageIndex + ".png";
    }

    private static final class AtlasPagePlan {
        private final int pageIndex;
        private final int width;
        private final int height;
        private final LinkedHashMap<String, IconSpriteRect> sprites;
        private String relativeSrc;

        AtlasPagePlan(int pageIndex, int width, int height, LinkedHashMap<String, IconSpriteRect> sprites) {
            this.pageIndex = pageIndex;
            this.width = width;
            this.height = height;
            this.sprites = sprites;
        }

        int pageIndex() {
            return pageIndex;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        LinkedHashMap<String, IconSpriteRect> sprites() {
            return sprites;
        }

        String relativeSrc() {
            return relativeSrc;
        }
    }
}
