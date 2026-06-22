package io.github.jmecn.ftbquestexport.lang;

import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import io.github.jmecn.ftbquestexport.QuestExportConstants;

import io.github.jmecn.ftbquestexport.lang.model.GtFluidPath;
import io.github.jmecn.ftbquestexport.lang.model.GtTagPrefixPattern;
import io.github.jmecn.ftbquestexport.lang.model.GtToolPattern;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class GtceuLabels {

    private GtceuLabels() {
    }

    static boolean isComposedNamespace(String namespace) {
        return GtMaterialPatterns.COMPOSED_NAMESPACES.contains(namespace);
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

    private static GtFluidPath parseFluidPath(String path) {
        if (path.startsWith("molten_")) {
            return new GtFluidPath("molten", path.substring("molten_".length()));
        }
        if (path.endsWith("_plasma")) {
            return new GtFluidPath("plasma", path.substring(0, path.length() - "_plasma".length()));
        }
        if (path.startsWith("liquid_")) {
            return new GtFluidPath("liquid", path.substring("liquid_".length()));
        }
        if (path.endsWith("_gas")) {
            return new GtFluidPath("gas", path.substring(0, path.length() - "_gas".length()));
        }
        return new GtFluidPath("primary", path);
    }

    private static String pickFluidTemplateKey(
            String storageKey, String materialPath, String namespace, Map<String, String> langTable) {
        String normalizedStorage = normalizeStorageKey(storageKey);
        Optional<String> gtKey =
                fluidTranslationKey(namespace, materialPath, normalizedStorage);
        return gtKey.orElseGet(() -> switch (normalizedStorage) {
            case "molten" -> "gtceu.fluid.molten";
            case "plasma" -> "gtceu.fluid.plasma";
            case "liquid" ->
                    firstPresent(langTable, "gtceu.fluid.liquid_generic", "gtceu.fluid.generic", "gtceu.fluid.liquid_generic");
            case "gas", "primary" -> pickGenericFluidTemplate(langTable);
            default -> pickGenericFluidTemplate(langTable);
        });

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
        GtFluidPath parsed = parseFluidPath(path);
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

    private static List<GtToolPattern> buildGtToolPatterns(Map<String, String> langTable) {
        List<GtToolPattern> patterns = new ArrayList<>();
        if (langTable == null) {
            return patterns;
        }
        String prefix = "item.gtceu.tool.";
        Set<String> seen = new LinkedHashSet<>();
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
            patterns.add(new GtToolPattern(toolName, GtMaterialPatterns.defaultGtToolIdPattern(toolName), key));
        }
        for (String toolName : GtMaterialPatterns.GTMUTILS_ELECTRIC_TOOL_NAMES) {
            if (!seen.add(toolName)) {
                continue;
            }
            String key = prefix + toolName;
            String template = langTable.get(key);
            if (template == null || !template.contains("%s")) {
                seen.remove(toolName);
                continue;
            }
            patterns.add(new GtToolPattern(toolName, GtMaterialPatterns.defaultGtToolIdPattern(toolName), key));
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
        if (!GtMaterialPatterns.GTCEU.equals(namespace) && langTable.containsKey(gtceu)) {
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
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
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

    private static String translateSurfaceIndicator(
            String namespace, String path, Function<String, String> translateKey) {
        if (!path.endsWith("_indicator") || path.endsWith("_bud_indicator")) {
            return null;
        }
        String materialPath = path.substring(0, path.length() - "_indicator".length());
        if (materialPath.isEmpty()) {
            return null;
        }
        String matLabel = resolveKey(translateKey, materialKey(namespace, materialPath));
        if (matLabel == null) {
            return null;
        }
        String composed = composeFromTemplate("block.surface_rock", matLabel, translateKey);
        if (composed != null) {
            return composed;
        }
        return composeFromTemplate("tagprefix.surface_rock", matLabel, translateKey);
    }

    private static List<GtTagPrefixPattern> buildGtTagPrefixPatterns(Map<String, String> langTable) {
        Set<String> suffixes = new LinkedHashSet<>(GtMaterialPatterns.TAG_PREFIX_PATTERNS.keySet());
        if (langTable != null) {
            for (String key : langTable.keySet()) {
                if (key.startsWith("tagprefix.") && !key.startsWith("tagprefix.polymer.")) {
                    suffixes.add(key.substring("tagprefix.".length()));
                }
            }
        }
        List<GtTagPrefixPattern> patterns = new ArrayList<>();
        for (String langSuffix : suffixes) {
            String pattern = GtMaterialPatterns.TAG_PREFIX_PATTERNS.getOrDefault(langSuffix, "%s_" + langSuffix);
            patterns.add(new GtTagPrefixPattern(langSuffix, pattern, "tagprefix." + langSuffix));
        }
        patterns.sort(Comparator.comparingInt((GtTagPrefixPattern p) -> p.pattern().length()).reversed());
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
        String prefixKey = tagPrefixLangKey(langSuffix, namespace, materialPath, langTable);
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
        for (GtTagPrefixPattern entry : buildGtTagPrefixPatterns(langTable)) {
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
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
        String surfaceLabel = translateSurfaceIndicator(namespace, path, translateKey);
        if (surfaceLabel != null) {
            return surfaceLabel;
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
        for (GtTagPrefixPattern entry : buildGtTagPrefixPatterns(langTable)) {
            String materialPath = GtMaterialPatterns.extractMaterial(path, entry.pattern());
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

    private static final String MANAGER_CLASS =
            "com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager";
    private static final String MATERIAL_CLASS = "com.gregtechceu.gtceu.api.data.chemical.material.Material";
    private static final String PROPERTY_KEY_CLASS =
            "com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey";
    private static final String FLUID_STORAGE_KEYS_CLASS =
            "com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys";

    private static final boolean GT_AVAILABLE = probeGt();
    private static final Method GT_GET_INSTANCE;
    private static final Method GT_GET_MATERIAL_STRING;
    private static final Method GT_HAS_PROPERTY;
    private static final Object GT_PROPERTY_KEY_POLYMER;
    private static final Object GT_PROPERTY_KEY_FLUID;
    private static final Method GT_GET_PROPERTY;
    private static final Method GT_FLUID_PROPERTY_GET_PRIMARY_KEY;
    private static final Method GT_FLUID_STORAGE_KEY_GET_TRANSLATION;
    private static final Object GT_FLUID_KEY_LIQUID;
    private static final Object GT_FLUID_KEY_GAS;
    private static final Object GT_FLUID_KEY_MOLTEN;
    private static final Object GT_FLUID_KEY_PLASMA;
    private static final Object GT_NULL_MATERIAL;

    private static final Class<?> GT_MATERIAL_TYPE = GT_AVAILABLE ? requireGtClass(MATERIAL_CLASS) : null;
    private static final Class<?> GT_PROPERTY_KEY_TYPE = GT_AVAILABLE ? requireGtClass(PROPERTY_KEY_CLASS) : null;

    static {
        if (!GT_AVAILABLE) {
            GT_GET_INSTANCE = null;
            GT_GET_MATERIAL_STRING = null;
            GT_HAS_PROPERTY = null;
            GT_PROPERTY_KEY_POLYMER = null;
            GT_PROPERTY_KEY_FLUID = null;
            GT_GET_PROPERTY = null;
            GT_FLUID_PROPERTY_GET_PRIMARY_KEY = null;
            GT_FLUID_STORAGE_KEY_GET_TRANSLATION = null;
            GT_FLUID_KEY_LIQUID = null;
            GT_FLUID_KEY_GAS = null;
            GT_FLUID_KEY_MOLTEN = null;
            GT_FLUID_KEY_PLASMA = null;
            GT_NULL_MATERIAL = null;
        } else {
            GT_GET_INSTANCE = requireGtMethod(MANAGER_CLASS, "getInstance");
            GT_GET_MATERIAL_STRING = requireGtMethod(MANAGER_CLASS, "getMaterial", String.class);
            GT_HAS_PROPERTY = requireGtMethod(GT_MATERIAL_TYPE, "hasProperty", GT_PROPERTY_KEY_TYPE);
            GT_PROPERTY_KEY_POLYMER = gtStaticField(PROPERTY_KEY_CLASS, "POLYMER");
            GT_PROPERTY_KEY_FLUID = gtStaticField(PROPERTY_KEY_CLASS, "FLUID");
            GT_GET_PROPERTY = requireGtMethod(GT_MATERIAL_TYPE, "getProperty", GT_PROPERTY_KEY_TYPE);
            Class<?> fluidPropertyClass = requireGtClass(
                    "com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty");
            GT_FLUID_PROPERTY_GET_PRIMARY_KEY = requireGtMethod(fluidPropertyClass, "getPrimaryKey");
            Class<?> fluidStorageKeyClass =
                    requireGtClass("com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey");
            GT_FLUID_STORAGE_KEY_GET_TRANSLATION =
                    requireGtMethod(fluidStorageKeyClass, "getTranslationKeyFor", GT_MATERIAL_TYPE);
            GT_FLUID_KEY_LIQUID = gtStaticField(FLUID_STORAGE_KEYS_CLASS, "LIQUID");
            GT_FLUID_KEY_GAS = gtStaticField(FLUID_STORAGE_KEYS_CLASS, "GAS");
            GT_FLUID_KEY_MOLTEN = gtStaticField(FLUID_STORAGE_KEYS_CLASS, "MOLTEN");
            GT_FLUID_KEY_PLASMA = gtStaticField(FLUID_STORAGE_KEYS_CLASS, "PLASMA");
            GT_NULL_MATERIAL = gtStaticField(
                    "com.gregtechceu.gtceu.common.data.GTMaterials", "NULL");
        }
    }

    private static Optional<Object> gtMaterial(String namespace, String materialPath) {
        if (!GT_AVAILABLE || materialPath == null || materialPath.isEmpty()) {
            return Optional.empty();
        }
        String key = namespace + ":" + materialPath;
        try {
            Object manager = GT_GET_INSTANCE.invoke(null);
            Object material = GT_GET_MATERIAL_STRING.invoke(manager, key);
            if (material == null || material == GT_NULL_MATERIAL) {
                return Optional.empty();
            }
            return Optional.of(material);
        } catch (ReflectiveOperationException e) {
            FtbQuestExportMod.LOGGER.debug("{} material lookup failed for {}: {}", QuestExportConstants.LOG_PREFIX_ITEMS_LANG, key, e.toString());
            return Optional.empty();
        }
    }

    private static boolean gtHasPolymer(Object material) {
        return gtHasProperty(material, GT_PROPERTY_KEY_POLYMER);
    }

    private static Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey) {
        Optional<Object> materialOpt = gtMaterial(namespace, materialPath);
        if (materialOpt.isEmpty()) {
            return Optional.empty();
        }
        Object material = materialOpt.get();
        Object fluidKey = switch (storageKey) {
            case "molten" -> GT_FLUID_KEY_MOLTEN;
            case "plasma" -> GT_FLUID_KEY_PLASMA;
            case "liquid" -> GT_FLUID_KEY_LIQUID;
            case "gas" -> GT_FLUID_KEY_GAS;
            case "primary" -> primaryFluidStorageKey(material);
            default -> primaryFluidStorageKey(material);
        };
        if (fluidKey == null) {
            return Optional.empty();
        }
        try {
            String key = (String) GT_FLUID_STORAGE_KEY_GET_TRANSLATION.invoke(fluidKey, material);
            return key == null || key.isEmpty() ? Optional.empty() : Optional.of(key);
        } catch (ReflectiveOperationException e) {
            FtbQuestExportMod.LOGGER.debug(
                    "{} fluid template for {} {}: {}",
                    QuestExportConstants.LOG_PREFIX_ITEMS_LANG,
                    namespace,
                    materialPath,
                    e.toString());
            return Optional.empty();
        }
    }

    private static Object primaryFluidStorageKey(Object material) {
        if (!GT_AVAILABLE || material == null) {
            return null;
        }
        try {
            Object fluidProperty = GT_GET_PROPERTY.invoke(material, GT_PROPERTY_KEY_FLUID);
            if (fluidProperty == null) {
                return GT_FLUID_KEY_LIQUID;
            }
            return GT_FLUID_PROPERTY_GET_PRIMARY_KEY.invoke(fluidProperty);
        } catch (ReflectiveOperationException e) {
            return GT_FLUID_KEY_LIQUID;
        }
    }

    private static boolean gtHasProperty(Object material, Object propertyKey) {
        if (!GT_AVAILABLE || material == null || propertyKey == null) {
            return false;
        }
        try {
            return (boolean) GT_HAS_PROPERTY.invoke(material, propertyKey);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean probeGt() {
        try {
            Class.forName(MANAGER_CLASS, false, GtceuLabels.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            FtbQuestExportMod.LOGGER.info(
                    "{} GregTech not on classpath — label export uses lang-only fallbacks (expected in unit tests)",
                    QuestExportConstants.LOG_PREFIX_ITEMS_LANG);
            return false;
        }
    }

    private static Class<?> requireGtClass(String name) {
        try {
            return Class.forName(name, false, GtceuLabels.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("GT class missing: " + name, e);
        }
    }

    private static Method requireGtMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method method = type.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(type.getName() + "#" + name, e);
        }
    }

    private static Method requireGtMethod(String className, String name, Class<?>... params) {
        return requireGtMethod(requireGtClass(className), name, params);
    }

    private static Object gtStaticField(String className, String fieldName) {
        try {
            Field field = requireGtClass(className).getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(className + "." + fieldName, e);
        }
    }

    private static String tagPrefixLangKey(
            String langSuffix, String namespace, String materialPath, Map<String, String> langTable) {
        String plain = "tagprefix." + langSuffix;
        String polymer = "tagprefix.polymer." + langSuffix;
        Optional<Object> materialOpt = gtMaterial(namespace, materialPath);
        if (materialOpt.isPresent()) {
            if (gtHasPolymer(materialOpt.get()) && langKeyPresent(langTable, polymer)) {
                return polymer;
            }
            return plain;
        }

        return plain;
    }

    private static String normalizeStorageKey(String storageKey) {
        return storageKey == null ? "primary" : storageKey.toLowerCase(Locale.ROOT);
    }
}
