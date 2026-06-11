package io.github.jmecn.ftbquestexport.lang;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lang keys to merge into {@code lang/<locale>.json} for quest-referenced items/fluids.
 * GT material / tagprefix resolution for display names is done in QuestBook
 * ({@code gtceu-composed-registry.ts}), not at export time.
 */
public final class QuestLangKeys {

    private static final String GTCEU = "gtceu";
    private static final java.util.Set<String> COMPOSED_MATERIAL_NAMESPACES = java.util.Set.of(GTCEU, "tfg", "greate");

    private static final Map<String, String> GTCEU_TAG_PREFIX_PATTERNS = Map.ofEntries(
            Map.entry("raw", "raw_%s"),
            Map.entry("raw_ore_block", "raw_%s_block"),
            Map.entry("refined_ore", "refined_%s_ore"),
            Map.entry("purified_ore", "purified_%s_ore"),
            Map.entry("crushed_ore", "crushed_%s_ore"),
            Map.entry("hot_ingot", "hot_%s_ingot"),
            Map.entry("chipped_gem", "chipped_%s_gem"),
            Map.entry("flawed_gem", "flawed_%s_gem"),
            Map.entry("flawless_gem", "flawless_%s_gem"),
            Map.entry("exquisite_gem", "exquisite_%s_gem"),
            Map.entry("small_dust", "small_%s_dust"),
            Map.entry("tiny_dust", "tiny_%s_dust"),
            Map.entry("impure_dust", "impure_%s_dust"),
            Map.entry("pure_dust", "pure_%s_dust"),
            Map.entry("dense_plate", "dense_%s_plate"),
            Map.entry("double_plate", "double_%s_plate"),
            Map.entry("long_rod", "long_%s_rod"),
            Map.entry("small_spring", "small_%s_spring"),
            Map.entry("fine_wire", "fine_%s_wire"),
            Map.entry("wire_gt_single", "%s_single_wire"),
            Map.entry("wire_gt_double", "%s_double_wire"),
            Map.entry("wire_gt_quadruple", "%s_quadruple_wire"),
            Map.entry("wire_gt_octal", "%s_octal_wire"),
            Map.entry("wire_gt_hex", "%s_hex_wire"),
            Map.entry("cable_gt_single", "%s_single_cable"),
            Map.entry("cable_gt_double", "%s_double_cable"),
            Map.entry("cable_gt_quadruple", "%s_quadruple_cable"),
            Map.entry("cable_gt_octal", "%s_octal_cable"),
            Map.entry("cable_gt_hex", "%s_hex_cable"),
            Map.entry("small_gear", "small_%s_gear"),
            Map.entry("pipe_tiny_fluid", "%s_tiny_fluid_pipe"),
            Map.entry("pipe_small_fluid", "%s_small_fluid_pipe"),
            Map.entry("pipe_normal_fluid", "%s_normal_fluid_pipe"),
            Map.entry("pipe_large_fluid", "%s_large_fluid_pipe"),
            Map.entry("pipe_huge_fluid", "%s_huge_fluid_pipe"),
            Map.entry("pipe_quadruple_fluid", "%s_quadruple_fluid_pipe"),
            Map.entry("pipe_nonuple_fluid", "%s_nonuple_fluid_pipe"),
            Map.entry("pipe_small_item", "%s_small_item_pipe"),
            Map.entry("pipe_normal_item", "%s_normal_item_pipe"),
            Map.entry("pipe_large_item", "%s_large_item_pipe"),
            Map.entry("pipe_huge_item", "%s_huge_item_pipe"),
            Map.entry("pipe_small_restrictive", "%s_small_restrictive_item_pipe"),
            Map.entry("pipe_normal_restrictive", "%s_normal_restrictive_item_pipe"),
            Map.entry("pipe_large_restrictive", "%s_large_restrictive_item_pipe"),
            Map.entry("pipe_huge_restrictive", "%s_huge_restrictive_item_pipe"),
            Map.entry("poor_raw", "poor_raw_%s"),
            Map.entry("rich_raw", "rich_raw_%s"),
            Map.entry("dusty_raw", "dusty_raw_%s"),
            Map.entry("repair_kit", "repair_kit_%s"),
            Map.entry("unfired_repair_kit", "unfired_repair_kit_%s"));

    private static final String[] VOLTAGE_TIER_PREFIXES = {
            "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uev", "uhv", "max"
    };

    private static final Map<String, String> GT_TOOL_ID_FORMAT_OVERRIDES = Map.ofEntries(
            Map.entry("lv_drill", "lv_%s_drill"),
            Map.entry("mv_drill", "mv_%s_drill"),
            Map.entry("hv_drill", "hv_%s_drill"),
            Map.entry("ev_drill", "ev_%s_drill"),
            Map.entry("iv_drill", "iv_%s_drill"),
            Map.entry("lv_chainsaw", "lv_%s_chainsaw"),
            Map.entry("hv_chainsaw", "hv_%s_chainsaw"),
            Map.entry("iv_chainsaw", "iv_%s_chainsaw"),
            Map.entry("lv_wrench", "lv_%s_wrench"),
            Map.entry("hv_wrench", "hv_%s_wrench"),
            Map.entry("iv_wrench", "iv_%s_wrench"),
            Map.entry("lv_wirecutter", "lv_%s_wire_cutter"),
            Map.entry("hv_wirecutter", "hv_%s_wire_cutter"),
            Map.entry("iv_wirecutter", "iv_%s_wire_cutter"),
            Map.entry("lv_screwdriver", "lv_%s_screwdriver"),
            Map.entry("hv_screwdriver", "hv_%s_screwdriver"),
            Map.entry("iv_screwdriver", "iv_%s_screwdriver"));

    /** gtmutils UtilToolType — lang keys are {@code item.gtceu.tool.<name>} (see assets/gtmutils/lang). */
    private static final java.util.List<String> GTMUTILS_ELECTRIC_TOOL_NAMES = java.util.List.of(
            "mv_screwdriver", "ev_screwdriver", "luv_screwdriver", "zpm_screwdriver",
            "mv_chainsaw", "ev_chainsaw", "luv_chainsaw", "zpm_chainsaw",
            "luv_drill", "zpm_drill",
            "mv_wrench", "ev_wrench", "luv_wrench", "zpm_wrench",
            "mv_wirecutter", "ev_wirecutter", "luv_wirecutter", "zpm_wirecutter",
            "mv_buzzsaw", "hv_buzzsaw", "ev_buzzsaw", "iv_buzzsaw", "luv_buzzsaw", "zpm_buzzsaw");

    private static final java.util.List<String> GT_TOOL_NAMES = java.util.List.of(
            "sword", "pickaxe", "shovel", "axe", "hoe", "mining_hammer", "spade", "scythe", "saw", "hammer", "mallet",
            "wrench", "file", "crowbar", "screwdriver", "mortar", "wire_cutter", "knife", "butchery_knife", "plunger",
            "shears", "buzzsaw");

    private static String defaultGtToolIdPattern(String toolName) {
        String override = GT_TOOL_ID_FORMAT_OVERRIDES.get(toolName);
        if (override != null) {
            return override;
        }
        for (String tier : VOLTAGE_TIER_PREFIXES) {
            String wirecutter = tier + "_wirecutter";
            if (wirecutter.equals(toolName)) {
                return tier + "_%s_wire_cutter";
            }
            String prefix = tier + "_";
            if (toolName.startsWith(prefix)) {
                return prefix + "%s_" + toolName.substring(prefix.length());
            }
        }
        return "%s_" + toolName;
    }

    private QuestLangKeys() {
    }

    public static void addForItem(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryLangKeys.itemLookupKeys(registryId));
        addComposedMaterialItemKeys(into, registryId);
    }

    public static void addForFluid(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryLangKeys.fluidLookupKeys(registryId));
        addComposedMaterialFluidKeys(into, registryId);
    }

    /** Observation / chapter-image block refs — {@code block.*} plus item/fluid fallbacks. */
    public static void addForBlock(Set<String> into, String registryId) {
        addLookupKeys(into, RegistryLangKeys.itemLookupKeys(registryId));
    }

    /** Observation entity refs — {@code entity.*}. */
    public static void addForEntity(Set<String> into, String registryId) {
        String dotted = RegistryLangKeys.dottedRegistryId(registryId);
        if (!dotted.isEmpty()) {
            into.add("entity." + dotted);
        }
    }

    private static void addLookupKeys(Set<String> into, java.util.List<String> keys) {
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                into.add(key);
            }
        }
    }

    private static void addComposedMaterialItemKeys(Set<String> into, String registryId) {
        String namespace = RegistryLangKeys.namespace(registryId);
        if (!COMPOSED_MATERIAL_NAMESPACES.contains(namespace)) {
            return;
        }
        String path = path(registryId);
        if (path.isEmpty()) {
            return;
        }
        if (path.endsWith("_bucket")) {
            into.add("item." + namespace + ".bucket");
            if ("tfg".equals(namespace)) {
                into.add("item.gtceu.bucket");
            }
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_bucket".length()));
            return;
        }
        if (path.endsWith("_bud_indicator")) {
            into.add("block.bud_indicator");
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_bud_indicator".length()));
            return;
        }
        if (addGtToolLangKeys(into, namespace, path)) {
            return;
        }
        for (var entry : GTCEU_TAG_PREFIX_PATTERNS.entrySet()) {
            String material = extractMaterial(path, entry.getValue());
            if (material == null) {
                continue;
            }
            into.add("tagprefix." + entry.getKey());
            into.add("tagprefix.polymer." + entry.getKey());
            into.add("material." + namespace + "." + material);
            return;
        }
        int lastUnderscore = path.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String suffix = path.substring(lastUnderscore + 1);
            String material = extractMaterial(path, "%s_" + suffix);
            if (material != null && !material.isEmpty()) {
                into.add("tagprefix." + suffix);
                into.add("tagprefix.polymer." + suffix);
                into.add("material." + namespace + "." + material);
            }
        }
    }

    private static void addComposedMaterialFluidKeys(Set<String> into, String registryId) {
        String namespace = RegistryLangKeys.namespace(registryId);
        if (!COMPOSED_MATERIAL_NAMESPACES.contains(namespace)) {
            return;
        }
        String path = path(registryId);
        if (path.isEmpty()) {
            return;
        }
        into.add("gtceu.fluid.liquid_generic");
        into.add("gtceu.fluid.generic");
        into.add("gtceu.fluid.molten");
        into.add("gtceu.fluid.plasma");
        into.add("gtceu.fluid.gas_vapor");
        into.add("gtceu.fluid.gas_generic");
        if (path.startsWith("molten_")) {
            into.add("material." + namespace + "." + path.substring("molten_".length()));
        } else if (path.endsWith("_plasma")) {
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_plasma".length()));
        } else if (path.startsWith("liquid_")) {
            into.add("material." + namespace + "." + path.substring("liquid_".length()));
        } else if (path.endsWith("_gas")) {
            into.add("material." + namespace + "." + path.substring(0, path.length() - "_gas".length()));
        } else {
            into.add("material." + namespace + "." + path);
        }
    }

    private static String path(String registryId) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        return colon >= 0 ? bare.substring(colon + 1) : bare;
    }

    private static boolean addGtToolLangKeys(Set<String> into, String namespace, String path) {
        java.util.Set<String> toolNames = new java.util.LinkedHashSet<>(GT_TOOL_ID_FORMAT_OVERRIDES.keySet());
        toolNames.addAll(GT_TOOL_NAMES);
        toolNames.addAll(GTMUTILS_ELECTRIC_TOOL_NAMES);
        for (String tier : VOLTAGE_TIER_PREFIXES) {
            toolNames.add(tier + "_buzzsaw");
            toolNames.add(tier + "_chainsaw");
            toolNames.add(tier + "_drill");
            toolNames.add(tier + "_screwdriver");
            toolNames.add(tier + "_wrench");
            toolNames.add(tier + "_wirecutter");
        }
        toolNames.add("buzzsaw");
        toolNames.add("chainsaw");
        toolNames.add("screwdriver");
        toolNames.add("wrench");
        toolNames.add("wirecutter");
        toolNames.add("wire_cutter");
        java.util.List<java.util.Map.Entry<String, String>> patterns = new java.util.ArrayList<>();
        for (String toolName : toolNames) {
            String pattern = defaultGtToolIdPattern(toolName);
            patterns.add(Map.entry(toolName, pattern));
        }
        patterns.sort((a, b) -> Integer.compare(b.getValue().length(), a.getValue().length()));
        for (var entry : patterns) {
            String material = extractMaterial(path, entry.getValue());
            if (material == null || material.isEmpty()) {
                continue;
            }
            into.add("item.gtceu.tool." + entry.getKey());
            into.add("material." + namespace + "." + material);
            return true;
        }
        return false;
    }

    static String extractMaterial(String path, String pattern) {
        if (path == null || pattern == null || !pattern.contains("%s")) {
            return null;
        }
        if (pattern.startsWith("%s_")) {
            String suffix = pattern.substring(2);
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
            return null;
        }
        if (pattern.endsWith("_%s")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
            return null;
        }
        int idx = pattern.indexOf("%s");
        String before = pattern.substring(0, idx);
        String after = pattern.substring(idx + 2);
        if (path.startsWith(before) && path.endsWith(after)) {
            return path.substring(before.length(), path.length() - after.length());
        }
        return null;
    }

    /** Merge seed lang keys with item/fluid registry lookups. */
    public static Set<String> mergeItemFluidLangKeys(
            Set<String> seedLangKeys, Set<String> itemIds, Set<String> fluidIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String itemId : itemIds == null ? Set.<String>of() : itemIds) {
            addForItem(merged, itemId);
        }
        for (String fluidId : fluidIds == null ? Set.<String>of() : fluidIds) {
            addForFluid(merged, fluidId);
        }
        return Set.copyOf(merged);
    }

    public static Set<String> mergeBlockLangKeys(Set<String> seedLangKeys, Set<String> blockIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String blockId : blockIds == null ? Set.<String>of() : blockIds) {
            addForBlock(merged, blockId);
        }
        return Set.copyOf(merged);
    }

    public static Set<String> mergeEntityLangKeys(Set<String> seedLangKeys, Set<String> entityIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String entityId : entityIds == null ? Set.<String>of() : entityIds) {
            addForEntity(merged, entityId);
        }
        return Set.copyOf(merged);
    }

    /** {@code tag.item.*} / {@code tag.block.*} / {@code tag.fluid.*} lang keys. */
    public static void addForTag(Set<String> into, String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return;
        }
        String dotted = RegistryLangKeys.dottedRegistryId(tagId);
        if (dotted.isEmpty()) {
            return;
        }
        into.add("tag.item." + dotted);
        into.add("tag.block." + dotted);
        into.add("tag.fluid." + dotted);
    }

    public static Set<String> mergeTagLangKeys(Set<String> seedLangKeys, Set<String> tagIds) {
        Set<String> merged = new TreeSet<>(seedLangKeys == null ? Set.of() : seedLangKeys);
        for (String tagId : tagIds == null ? Set.<String>of() : tagIds) {
            addForTag(merged, tagId);
        }
        return Set.copyOf(merged);
    }
}
