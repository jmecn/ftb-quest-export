package io.github.jmecn.ftbquestexport.export.scan;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Expands tag refs collected during scan into concrete item/block seeds. */
public final class QuestSeedExpander {


    private QuestSeedExpander() {}

    public static void expandTags(MinecraftServer server, QuestScanResult scan) {
        if (server == null || scan == null || scan.getTags().isEmpty()) {
            return;
        }
        int itemMembers = 0;
        int blockMembers = 0;
        for (String tagId : new ArrayList<>(scan.getTags())) {
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                continue;
            }
            List<String> items = expandItemTag(server, loc);
            for (String itemId : items) {
                scan.addItem(itemId);
            }
            itemMembers += items.size();

            List<String> blocks = expandBlockTag(server, loc);
            for (String blockId : blocks) {
                scan.addBlock(blockId);
            }
            blockMembers += blocks.size();
        }
        FtbQuestExportMod.LOGGER.info("[seeds] expanded {} tags → {} item + {} block member refs",
                scan.getTags().size(), itemMembers, blockMembers);
    }

    private static List<String> expandItemTag(MinecraftServer server, ResourceLocation tagId) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        return server.registryAccess().registryOrThrow(Registries.ITEM).getTag(key)
                .map(h -> {
                    List<String> ids = new ArrayList<>();
                    h.stream().forEach(item -> item.unwrapKey()
                            .ifPresent(k -> ids.add(k.location().toString())));
                    return ids;
                })
                .orElse(List.of());
    }

    private static List<String> expandBlockTag(MinecraftServer server, ResourceLocation tagId) {
        TagKey<Block> key = TagKey.create(Registries.BLOCK, tagId);
        return server.registryAccess().registryOrThrow(Registries.BLOCK).getTag(key)
                .map(h -> {
                    List<String> ids = new ArrayList<>();
                    h.stream().forEach(block -> block.unwrapKey()
                            .ifPresent(k -> ids.add(k.location().toString())));
                    return ids;
                })
                .orElse(List.of());
    }
}
