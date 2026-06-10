package io.github.jmecn.ftbquestexport.export.resources;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Maps FTB / quest scan texture refs to {@link ResourceLocation} PNG paths for direct copy. */
public final class TextureRefResolver {

    private TextureRefResolver() {}

    public static Set<ResourceLocation> resolveAll(Iterable<String> textureRefs) {
        Set<ResourceLocation> locations = new LinkedHashSet<>();
        for (String ref : textureRefs) {
            addTextureRef(ref, locations);
        }
        return locations;
    }

    static void addTextureRef(String textureRef, Set<ResourceLocation> pending) {
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
        pending.add(ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path + ".png"));
        pending.add(ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path + ".png.mcmeta"));
    }
}
