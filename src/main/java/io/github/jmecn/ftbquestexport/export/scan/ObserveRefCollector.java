package io.github.jmecn.ftbquestexport.export.scan;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Classifies FTB observation / observe refs into scan seed buckets. */
public final class ObserveRefCollector {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");

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
            LOGGER.debug("[observe] skipping unparseable ref: {}", trimmed);
            return;
        }
        if (BuiltInRegistries.BLOCK.containsKey(loc)) {
            scan.addBlock(loc.toString());
            Item blockItem = BuiltInRegistries.BLOCK.get(loc).asItem();
            if (blockItem != null && blockItem != Items.AIR) {
                scan.addItem(BuiltInRegistries.ITEM.getKey(blockItem).toString());
            }
            return;
        }
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
            scan.addEntity(loc.toString());
            return;
        }
        LOGGER.debug("[observe] ref not in block/entity registry, treating as block: {}", trimmed);
        scan.addBlock(trimmed);
    }
}
