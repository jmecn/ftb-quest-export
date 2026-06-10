package io.github.jmecn.ftbquestexport.export.resources;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Set;

@SuppressWarnings("removal")
final class ModelDependencyCollector {

    private ModelDependencyCollector() {}

    static void seedItem(ResourceManager rm, String itemId, Set<ResourceLocation> pending) {
        if (itemId == null || itemId.isBlank() || itemId.startsWith("#")) {
            return;
        }
        ResourceLocation itemLoc = ResourceLocation.tryParse(itemId);
        if (itemLoc == null) {
            return;
        }
        enqueueModel(rm, new ResourceLocation(itemLoc.getNamespace(), "models/item/" + itemLoc.getPath() + ".json"), pending);
        enqueueModel(rm, new ResourceLocation(itemLoc.getNamespace(), "models/block/" + itemLoc.getPath() + ".json"), pending);
        enqueueBlockstate(rm, itemLoc.getNamespace(), itemLoc.getPath(), pending);
    }

    static void seedBlockId(ResourceManager rm, String blockId, Set<ResourceLocation> pending) {
        if (blockId == null || blockId.isBlank() || blockId.startsWith("#")) {
            return;
        }
        int bracket = blockId.indexOf('[');
        if (bracket > 0) {
            blockId = blockId.substring(0, bracket);
        }
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) {
            return;
        }
        enqueueBlockstate(rm, loc.getNamespace(), loc.getPath(), pending);
        enqueueModel(rm, new ResourceLocation(loc.getNamespace(), "models/block/" + loc.getPath() + ".json"), pending);
    }

    static void seedTextureRef(String textureRef, Set<ResourceLocation> pending) {
        if (textureRef == null || textureRef.isBlank()) {
            return;
        }
        String ref = textureRef;
        if (ref.endsWith(".png") || ref.endsWith(".mcmeta")) {
            if (ref.endsWith(".mcmeta")) {
                ref = ref.substring(0, ref.length() - ".mcmeta".length());
            } else if (ref.endsWith(".png")) {
                ref = ref.substring(0, ref.length() - ".png".length());
            }
        }
        ResourceLocation loc = ResourceLocation.tryParse(ref);
        if (loc == null) {
            return;
        }
        String path = loc.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        pending.add(new ResourceLocation(loc.getNamespace(), path + ".png"));
        pending.add(new ResourceLocation(loc.getNamespace(), path + ".png.mcmeta"));
    }

    private static void enqueueBlockstate(
            ResourceManager rm,
            String namespace,
            String path,
            Set<ResourceLocation> pending) {
        ResourceLocation bs = new ResourceLocation(namespace, "blockstates/" + path + ".json");
        if (rm.getResource(bs).isPresent()) {
            pending.add(bs);
        }
    }

    private static void enqueueModel(ResourceManager rm, ResourceLocation modelId, Set<ResourceLocation> pending) {
        if (SyntheticModelCatalog.isAvailable(rm, modelId)) {
            pending.add(modelId);
        }
    }
}
