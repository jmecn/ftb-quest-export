package io.github.jmecn.ftbquestexport.lang;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/** Minecraft lang keys for registry ids (aligned with emi-recipe-renderer lookup order). */
public final class RegistryLangKeys {

    private RegistryLangKeys() {
    }

    /** Mod namespace from a registry id, e.g. {@code gtceu:ingot} → {@code gtceu}. */
    public static String namespace(String registryId) {
        String bare = normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        return colon > 0 ? bare.substring(0, colon) : "";
    }

    public static String normalizeRegistryId(String registryId) {
        if (registryId == null) {
            return "";
        }
        String id = registryId.trim();
        if (id.startsWith("item:")) {
            id = id.substring(5);
        }
        int brace = id.indexOf('{');
        if (brace >= 0) {
            id = id.substring(0, brace);
        }
        int at = id.indexOf('@');
        if (at >= 0) {
            id = id.substring(0, at);
        }
        return id;
    }

    public static String dottedRegistryId(String registryId) {
        String bare = normalizeRegistryId(registryId);
        return bare.replace('/', '.').replace(':', '.');
    }

    public static String itemKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "item." + dotted;
    }

    public static String blockKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "block." + dotted;
    }

    public static String fluidKey(String registryId) {
        String dotted = dottedRegistryId(registryId);
        return dotted.isEmpty() ? "" : "fluid." + dotted;
    }

    /** Keys tried by the web renderer for items (item first, then block/fluid fallbacks). */
    public static List<String> itemLookupKeys(String registryId) {
        String dotted = dottedRegistryId(registryId);
        if (dotted.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(3);
        keys.add("item." + dotted);
        keys.add("block." + dotted);
        keys.add("fluid." + dotted);
        return keys;
    }

    public static List<String> fluidLookupKeys(String registryId) {
        String dotted = dottedRegistryId(registryId);
        if (dotted.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(3);
        keys.add("fluid." + dotted);
        keys.add("item." + dotted);
        keys.add("block." + dotted);
        return keys;
    }

    /**
     * Authoritative translation key for an item ({@link Item#getDescriptionId()} / {@link ItemStack#getDescriptionId()}).
     * Matches in-game display for mods that remap keys away from registry paths (e.g. TFC/AFC hanging signs).
     */
    public static String resolveItemDescriptionKey(Minecraft client, String registryId) {
        ResourceLocation id = ResourceLocation.tryParse(normalizeRegistryId(registryId));
        if (id == null || client == null || client.level == null) {
            return itemKey(registryId);
        }
        Registry<Item> items = client.level.registryAccess().registryOrThrow(Registries.ITEM);
        Item item = items.getHolder(ResourceKey.create(Registries.ITEM, id))
                .map(Holder::value)
                .orElse(null);
        if (item == null || item == Items.AIR) {
            return itemKey(registryId);
        }
        String descriptionId = new ItemStack(item).getDescriptionId();
        return descriptionId == null || descriptionId.isBlank() ? itemKey(registryId) : descriptionId;
    }

    /**
     * Authoritative translation key for a fluid (legacy block description when present).
     */
    public static String resolveFluidDescriptionKey(Minecraft client, String registryId) {
        ResourceLocation id = ResourceLocation.tryParse(normalizeRegistryId(registryId));
        if (id == null || client == null || client.level == null) {
            return fluidKey(registryId);
        }
        Registry<Fluid> fluids = client.level.registryAccess().registryOrThrow(Registries.FLUID);
        Fluid fluid = fluids.getHolder(ResourceKey.create(Registries.FLUID, id))
                .map(Holder::value)
                .orElse(null);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return fluidKey(registryId);
        }
        Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();
        if (block != null) {
            String descriptionId = block.getDescriptionId();
            if (descriptionId != null && !descriptionId.isBlank()) {
                return descriptionId;
            }
        }
        return fluidKey(registryId);
    }
}
