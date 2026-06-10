package io.github.jmecn.ftbquestexport.export.scan;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** Classifies FTB observation / observe refs into scan seed buckets. */
public final class ObserveRefCollector {


    private ObserveRefCollector() {}

    public static void collect(String ref, QuestScanResult scan) {
        if (ref == null || ref.isBlank()) {
            return;
        }
        String trimmed = ref.trim();
        if (trimmed.startsWith("#")) {
            scan.addTag(trimmed.substring(1));
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(trimmed);
        if (loc == null) {
            FtbQuestExportMod.LOGGER.debug("[observe] skipping unparseable ref: {}", trimmed);
            return;
        }
        if (ForgeRegistries.BLOCKS.containsKey(loc)) {
            scan.addBlock(loc.toString());
            Item blockItem = ForgeRegistries.BLOCKS.getValue(loc).asItem();
            if (blockItem != null && blockItem != Items.AIR) {
                scan.addItem(ForgeRegistries.ITEMS.getKey(blockItem).toString());
            }
            return;
        }
        if (ForgeRegistries.ENTITY_TYPES.containsKey(loc)) {
            scan.addEntity(loc.toString());
            return;
        }
        FtbQuestExportMod.LOGGER.debug("[observe] ref not in block/entity registry, treating as block: {}", trimmed);
        scan.addBlock(trimmed);
    }
}
