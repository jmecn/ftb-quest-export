package io.github.jmecn.ftbquestexport.export.lang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * GregTech-style composed registry labels (aligned with {@code emi-recipe-renderer/gtceu-translate.js}).
 */
public final class GtceuRegistryLabels {

    static final String GTCEU = "gtceu";
    private static final java.util.Set<String> COMPOSED_NAMESPACES = java.util.Set.of(GTCEU, "tfg", "greate");

    private static final Map<String, String> TAG_PREFIX_PATTERN_OVERRIDES = Map.ofEntries(
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

    private static final Map<String, String> TOOL_ID_FORMAT_OVERRIDES = Map.ofEntries(
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

    /** gtmutils {@code UtilToolType} — lang under {@code assets/gtmutils/lang} as {@code item.gtceu.tool.*}. */
    private static final java.util.List<String> GTMUTILS_ELECTRIC_TOOL_NAMES = java.util.List.of(
            "mv_screwdriver", "ev_screwdriver", "luv_screwdriver", "zpm_screwdriver",
            "mv_chainsaw", "ev_chainsaw", "luv_chainsaw", "zpm_chainsaw",
            "luv_drill", "zpm_drill",
            "mv_wrench", "ev_wrench", "luv_wrench", "zpm_wrench",
            "mv_wirecutter", "ev_wirecutter", "luv_wirecutter", "zpm_wirecutter",
            "mv_buzzsaw", "hv_buzzsaw", "ev_buzzsaw", "iv_buzzsaw", "luv_buzzsaw", "zpm_buzzsaw");

    private record GtToolPattern(String toolName, String pattern, String templateKey) {
    }

    static String defaultGtToolIdPattern(String toolName) {
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

    private GtceuRegistryLabels() {
    }

    static boolean isComposedNamespace(String namespace) {
        return COMPOSED_NAMESPACES.contains(namespace);
    }

    static String formatTemplate(String template, String... args) {
        if (template == null) {
            return "";
        }
        int i = 0;
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (true) {
            int next = template.indexOf("%s", idx);
            if (next < 0) {
                out.append(template, idx, template.length());
                break;
            }
            out.append(template, idx, next);
            out.append(i < args.length ? args[i++] : "%s");
            idx = next + 2;
        }
        return out.toString();
    }

    private static String resolveKey(Function<String, String> translateKey, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String value = translateKey.apply(key);
        return value != null && !value.equals(key) ? value : null;
    }

    private static String materialKey(String namespace, String materialPath) {
        return "material." + namespace + "." + materialPath;
    }

    private static boolean langKeyPresent(Map<String, String> langTable, String key) {
        return langTable != null && langTable.containsKey(key);
    }

    static String resolveBucketTemplateKey(String namespace, Map<String, String> langTable) {
        String own = "item." + namespace + ".bucket";
        if (langKeyPresent(langTable, own)) {
            return own;
        }
        if ("tfg".equals(namespace) && langKeyPresent(langTable, "item.gtceu.bucket")) {
            return "item.gtceu.bucket";
        }
        return null;
    }

    private record FluidPath(String storageKey, String materialPath) {
    }

    private static FluidPath parseFluidPath(String path) {
        if (path.startsWith("molten_")) {
            return new FluidPath("molten", path.substring("molten_".length()));
        }
        if (path.endsWith("_plasma")) {
            return new FluidPath("plasma", path.substring(0, path.length() - "_plasma".length()));
        }
        if (path.startsWith("liquid_")) {
            return new FluidPath("liquid", path.substring("liquid_".length()));
        }
        if (path.endsWith("_gas")) {
            return new FluidPath("gas", path.substring(0, path.length() - "_gas".length()));
        }
        return new FluidPath("primary", path);
    }

    private static String pickFluidTemplateKey(
            String storageKey, String materialPath, String namespace, Map<String, String> langTable) {
        String normalizedStorage = GtMaterialFacts.normalizeStorageKey(storageKey);
        Optional<String> gtKey =
                GtMaterialFacts.fluidTranslationKey(namespace, materialPath, normalizedStorage);
        if (gtKey.isPresent()) {
            return gtKey.get();
        }

        boolean modpackFluid = !GTCEU.equals(namespace);
        return switch (normalizedStorage) {
            case "molten" -> "gtceu.fluid.molten";
            case "plasma" -> "gtceu.fluid.plasma";
            case "liquid" -> firstPresent(langTable, "gtceu.fluid.liquid_generic", "gtceu.fluid.generic", "gtceu.fluid.liquid_generic");
            case "gas", "primary" -> pickGenericFluidTemplate(langTable);
            default -> pickGenericFluidTemplate(langTable);
        };
    }

    private static String firstPresent(Map<String, String> langTable, String... keys) {
        for (String key : keys) {
            if (langKeyPresent(langTable, key)) {
                return key;
            }
        }
        return keys.length > 0 ? keys[keys.length - 1] : "";
    }

    private static String resolveMaterialLabel(
            String namespace,
            String materialPath,
            String fullPath,
            Function<String, String> translateKey) {
        String label = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (label != null) {
            return label;
        }
        return resolveKey(translateKey, materialKey(namespace, fullPath));
    }

    private static String composeFromTemplate(String templateKey, String matLabel, Function<String, String> translateKey) {
        String template = resolveKey(translateKey, templateKey);
        if (template == null || matLabel == null) {
            return null;
        }
        if (!template.contains("%s")) {
            return template;
        }
        return formatTemplate(template, matLabel);
    }

    static String translateComposedFluid(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (!isComposedNamespace(namespace) || path.isEmpty()) {
            return null;
        }
        String flatFluid = resolveKey(translateKey, "fluid." + namespace + "." + path);
        if (flatFluid != null) {
            return flatFluid;
        }
        FluidPath parsed = parseFluidPath(path);
        String matLabel = resolveMaterialLabel(namespace, parsed.materialPath(), path, translateKey);
        if (matLabel == null) {
            return resolveKey(translateKey, materialKey(namespace, path));
        }
        String templateKey = pickFluidTemplateKey(parsed.storageKey(), parsed.materialPath(), namespace, langTable);
        String composed = composeFromTemplate(templateKey, matLabel, translateKey);
        if (composed != null) {
            return composed;
        }
        String fallback = resolveKey(translateKey, materialKey(namespace, path));
        return fallback != null ? fallback : resolveKey(translateKey, materialKey(namespace, parsed.materialPath()));
    }

    private record TagPrefixPattern(String langSuffix, String pattern, String langKey) {
    }

    private static List<GtToolPattern> buildGtToolPatterns(Map<String, String> langTable) {
        List<GtToolPattern> patterns = new ArrayList<>();
        if (langTable == null) {
            return patterns;
        }
        String prefix = "item.gtceu.tool.";
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (String key : langTable.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String toolName = key.substring(prefix.length());
            if (toolName.isEmpty() || toolName.contains(".") || !seen.add(toolName)) {
                continue;
            }
            String template = langTable.get(key);
            if (template == null || !template.contains("%s")) {
                seen.remove(toolName);
                continue;
            }
            patterns.add(new GtToolPattern(toolName, defaultGtToolIdPattern(toolName), key));
        }
        for (String toolName : GTMUTILS_ELECTRIC_TOOL_NAMES) {
            if (!seen.add(toolName)) {
                continue;
            }
            String key = prefix + toolName;
            String template = langTable.get(key);
            if (template == null || !template.contains("%s")) {
                seen.remove(toolName);
                continue;
            }
            patterns.add(new GtToolPattern(toolName, defaultGtToolIdPattern(toolName), key));
        }
        patterns.sort(Comparator.comparingInt((GtToolPattern p) -> p.pattern().length()).reversed());
        return patterns;
    }

    private static String resolveGtToolTemplateKey(
            String namespace, String toolName, Map<String, String> langTable) {
        if (langTable == null) {
            return null;
        }
        String own = "item." + namespace + ".tool." + toolName;
        if (langTable.containsKey(own)) {
            return own;
        }
        String gtceu = "item.gtceu.tool." + toolName;
        if (!GTCEU.equals(namespace) && langTable.containsKey(gtceu)) {
            return gtceu;
        }
        return null;
    }

    private static String translateGtToolItem(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        for (GtToolPattern entry : buildGtToolPatterns(langTable)) {
            String materialPath = LangClosureKeys.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String templateKey = resolveGtToolTemplateKey(namespace, entry.toolName(), langTable);
            if (templateKey == null) {
                templateKey = entry.templateKey();
            }
            String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
            if (matLabel == null) {
                continue;
            }
            String composed = composeFromTemplate(templateKey, matLabel, translateKey);
            if (composed != null) {
                return composed;
            }
        }
        return null;
    }

    private static String translateBudIndicator(
            String namespace, String path, Function<String, String> translateKey) {
        if (!path.endsWith("_bud_indicator")) {
            return null;
        }
        String materialPath = path.substring(0, path.length() - "_bud_indicator".length());
        if (materialPath.isEmpty()) {
            return null;
        }
        String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (matLabel == null) {
            return null;
        }
        return composeFromTemplate("block.bud_indicator", matLabel, translateKey);
    }

    private static List<TagPrefixPattern> buildTagPrefixPatterns(Map<String, String> langTable) {
        Set<String> suffixes = new LinkedHashSet<>(TAG_PREFIX_PATTERN_OVERRIDES.keySet());
        if (langTable != null) {
            for (String key : langTable.keySet()) {
                if (key.startsWith("tagprefix.") && !key.startsWith("tagprefix.polymer.")) {
                    suffixes.add(key.substring("tagprefix.".length()));
                }
            }
        }
        List<TagPrefixPattern> patterns = new ArrayList<>();
        for (String langSuffix : suffixes) {
            String pattern = TAG_PREFIX_PATTERN_OVERRIDES.getOrDefault(langSuffix, "%s_" + langSuffix);
            patterns.add(new TagPrefixPattern(langSuffix, pattern, "tagprefix." + langSuffix));
        }
        patterns.sort(Comparator.comparingInt((TagPrefixPattern p) -> p.pattern().length()).reversed());
        return patterns;
    }

    private static String pickGenericFluidTemplate(Map<String, String> langTable) {
        return firstPresent(langTable, "gtceu.fluid.generic", "gtceu.fluid.liquid_generic", "gtceu.fluid.generic");
    }

    private static String composeTagPrefixLabel(
            String namespace,
            String materialPath,
            String langSuffix,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (matLabel == null) {
            return null;
        }
        String prefixKey = GtMaterialFacts.tagPrefixLangKey(langSuffix, namespace, materialPath, langTable);
        String prefixTemplate = resolveKey(translateKey, prefixKey);
        if (prefixTemplate == null) {
            return null;
        }
        return formatTemplate(prefixTemplate, matLabel);
    }

    static String tryItemSpecificLang(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (path.isEmpty()) {
            return null;
        }
        String itemKey = "item." + namespace + "." + path;
        String itemTemplate = resolveKey(translateKey, itemKey);
        if (itemTemplate == null) {
            return null;
        }
        if (!itemTemplate.contains("%s")) {
            return itemTemplate;
        }
        for (TagPrefixPattern entry : buildTagPrefixPatterns(langTable)) {
            String materialPath = LangClosureKeys.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
            if (matLabel != null) {
                return formatTemplate(itemTemplate, matLabel);
            }
        }
        String matLabel = resolveMaterialLabelForItemPath(namespace, path, translateKey, langTable);
        return matLabel != null ? formatTemplate(itemTemplate, matLabel) : null;
    }

    private static String resolveMaterialLabelForItemPath(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String direct = resolveKey(translateKey, materialKey(namespace, path));
        if (direct != null) {
            return direct;
        }
        String prefix = "material." + namespace + ".";
        String bestMatPath = null;
        if (langTable == null) {
            return null;
        }
        for (String key : langTable.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String matPath = key.substring(prefix.length());
            if (matPath.isEmpty()) {
                continue;
            }
            if (path.equals(matPath) || path.startsWith(matPath + "_")) {
                if (bestMatPath == null || matPath.length() > bestMatPath.length()) {
                    bestMatPath = matPath;
                }
            }
        }
        if (bestMatPath == null) {
            return null;
        }
        return resolveKey(translateKey, materialKey(namespace, bestMatPath));
    }

    static String translateComposedItem(
            String namespace,
            String path,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        if (!isComposedNamespace(namespace) || path.isEmpty()) {
            return null;
        }
        String budLabel = translateBudIndicator(namespace, path, translateKey);
        if (budLabel != null) {
            return budLabel;
        }
        String toolLabel = translateGtToolItem(namespace, path, translateKey, langTable);
        if (toolLabel != null) {
            return toolLabel;
        }
        String bucketTemplateKey = resolveBucketTemplateKey(namespace, langTable);
        if (path.endsWith("_bucket") && bucketTemplateKey != null) {
            String fluidPath = path.substring(0, path.length() - "_bucket".length());
            String bucketTemplate = resolveKey(translateKey, bucketTemplateKey);
            String fluidLabel = translateComposedFluid(namespace, fluidPath, translateKey, langTable);
            if (bucketTemplate != null && fluidLabel != null) {
                return formatTemplate(bucketTemplate, fluidLabel);
            }
        }
        for (TagPrefixPattern entry : buildTagPrefixPatterns(langTable)) {
            String materialPath = LangClosureKeys.extractMaterial(path, entry.pattern());
            if (materialPath == null) {
                continue;
            }
            String label = composeTagPrefixLabel(namespace, materialPath, entry.langSuffix(), translateKey, langTable);
            if (label != null) {
                return label;
            }
        }
        String itemOverride = tryItemSpecificLang(namespace, path, translateKey, langTable);
        return itemOverride;
    }

    static String translateComposedRegistry(
            String registryId,
            String kind,
            Function<String, String> translateKey,
            Map<String, String> langTable) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        int colon = bare.indexOf(':');
        if (colon <= 0 || colon >= bare.length() - 1) {
            return null;
        }
        String namespace = bare.substring(0, colon);
        String path = bare.substring(colon + 1);
        if ("fluid".equals(kind)) {
            return translateComposedFluid(namespace, path, translateKey, langTable);
        }
        if ("item".equals(kind) || "block".equals(kind)) {
            return translateComposedItem(namespace, path, translateKey, langTable);
        }
        return null;
    }
}
