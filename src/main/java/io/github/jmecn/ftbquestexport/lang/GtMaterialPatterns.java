package io.github.jmecn.ftbquestexport.lang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GtMaterialPatterns {

    public static final String GTCEU = "gtceu";
    public static final Set<String> COMPOSED_NAMESPACES = Set.of(GTCEU, "tfg", "greate");

    public static final Map<String, String> TAG_PREFIX_PATTERNS = Map.ofEntries(
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

    public static final String[] VOLTAGE_TIER_PREFIXES = {
            "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uev", "uhv", "max"
    };

    public static final Map<String, String> TOOL_ID_FORMAT_OVERRIDES = Map.ofEntries(
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

    public static final List<String> GTMUTILS_ELECTRIC_TOOL_NAMES = List.of(
            "mv_screwdriver", "ev_screwdriver", "luv_screwdriver", "zpm_screwdriver",
            "mv_chainsaw", "ev_chainsaw", "luv_chainsaw", "zpm_chainsaw",
            "luv_drill", "zpm_drill",
            "mv_wrench", "ev_wrench", "luv_wrench", "zpm_wrench",
            "mv_wirecutter", "ev_wirecutter", "luv_wirecutter", "zpm_wirecutter",
            "mv_buzzsaw", "hv_buzzsaw", "ev_buzzsaw", "iv_buzzsaw", "luv_buzzsaw", "zpm_buzzsaw");

    public static final List<String> GT_TOOL_NAMES = List.of(
            "sword", "pickaxe", "shovel", "axe", "hoe", "mining_hammer", "spade", "scythe", "saw", "hammer", "mallet",
            "wrench", "file", "crowbar", "screwdriver", "mortar", "wire_cutter", "knife", "butchery_knife", "plunger",
            "shears", "buzzsaw");

    private GtMaterialPatterns() {}

    public static String defaultGtToolIdPattern(String toolName) {
        String override = TOOL_ID_FORMAT_OVERRIDES.get(toolName);
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

    public static String extractMaterial(String path, String pattern) {
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

    public static List<Map.Entry<String, String>> orderedToolPatterns() {
        Set<String> toolNames = new LinkedHashSet<>(TOOL_ID_FORMAT_OVERRIDES.keySet());
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
        List<Map.Entry<String, String>> patterns = new ArrayList<>();
        for (String toolName : toolNames) {
            patterns.add(Map.entry(toolName, defaultGtToolIdPattern(toolName)));
        }
        patterns.sort((a, b) -> Integer.compare(b.getValue().length(), a.getValue().length()));
        return patterns;
    }
}
