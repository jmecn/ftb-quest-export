package io.github.jmecn.ftbquestexport.export.scan;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Classifies FTB observation {@code to_observe} refs into scan buckets. */
public final class ObserveTargetResolver {

    private ObserveTargetResolver() {}

    public static void collect(String raw, QuestScanResult scan) {
        if (raw == null || raw.isBlank() || scan == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("#")) {
            scan.addTag(trimmed.substring(1));
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(trimmed);
        if (loc == null) {
            return;
        }
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
            scan.addEntity(trimmed);
            return;
        }
        if (BuiltInRegistries.BLOCK.containsKey(loc)) {
            scan.addBlock(trimmed);
            return;
        }
        // FTB observation targets are usually blocks; keep unknown ids for texture/item closure attempts.
        scan.addBlock(trimmed);
    }
}
