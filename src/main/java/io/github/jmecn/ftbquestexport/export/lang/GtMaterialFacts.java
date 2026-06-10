package io.github.jmecn.ftbquestexport.export.lang;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads GregTech {@code Material} facts at export time (game classpath).
 * When GT is not loaded (unit tests), methods return empty / false and label code uses safe fallbacks.
 */
final class GtMaterialFacts {

    private static final String MANAGER_CLASS =
            "com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager";
    private static final String MATERIAL_CLASS = "com.gregtechceu.gtceu.api.data.chemical.material.Material";
    private static final String PROPERTY_KEY_CLASS =
            "com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey";
    private static final String FLUID_STORAGE_KEYS_CLASS =
            "com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys";

    private static final boolean AVAILABLE = probe();
    private static final Method GET_INSTANCE;
    private static final Method GET_MATERIAL_STRING;
    private static final Method HAS_PROPERTY;
    private static final Method IS_ELEMENT;
    private static final Object PROPERTY_KEY_POLYMER;
    private static final Object PROPERTY_KEY_FLUID;
    private static final Method GET_PROPERTY;
    private static final Method FLUID_PROPERTY_GET_PRIMARY_KEY;
    private static final Method FLUID_STORAGE_KEY_GET_TRANSLATION;
    private static final Object FLUID_KEY_LIQUID;
    private static final Object FLUID_KEY_GAS;
    private static final Object FLUID_KEY_MOLTEN;
    private static final Object FLUID_KEY_PLASMA;
    private static final Object NULL_MATERIAL;

    private static final Class<?> MATERIAL_TYPE = AVAILABLE ? requireClass(MATERIAL_CLASS) : null;
    private static final Class<?> PROPERTY_KEY_TYPE = AVAILABLE ? requireClass(PROPERTY_KEY_CLASS) : null;

    static {
        if (!AVAILABLE) {
            GET_INSTANCE = null;
            GET_MATERIAL_STRING = null;
            HAS_PROPERTY = null;
            IS_ELEMENT = null;
            PROPERTY_KEY_POLYMER = null;
            PROPERTY_KEY_FLUID = null;
            GET_PROPERTY = null;
            FLUID_PROPERTY_GET_PRIMARY_KEY = null;
            FLUID_STORAGE_KEY_GET_TRANSLATION = null;
            FLUID_KEY_LIQUID = null;
            FLUID_KEY_GAS = null;
            FLUID_KEY_MOLTEN = null;
            FLUID_KEY_PLASMA = null;
            NULL_MATERIAL = null;
        } else {
            GET_INSTANCE = requireMethod(MANAGER_CLASS, "getInstance");
            GET_MATERIAL_STRING = requireMethod(MANAGER_CLASS, "getMaterial", String.class);
            HAS_PROPERTY = requireMethod(MATERIAL_TYPE, "hasProperty", PROPERTY_KEY_TYPE);
            IS_ELEMENT = requireMethod(MATERIAL_TYPE, "isElement");
            PROPERTY_KEY_POLYMER = staticField(PROPERTY_KEY_CLASS, "POLYMER");
            PROPERTY_KEY_FLUID = staticField(PROPERTY_KEY_CLASS, "FLUID");
            GET_PROPERTY = requireMethod(MATERIAL_TYPE, "getProperty", PROPERTY_KEY_TYPE);
            Class<?> fluidPropertyClass = requireClass(
                    "com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty");
            FLUID_PROPERTY_GET_PRIMARY_KEY = requireMethod(fluidPropertyClass, "getPrimaryKey");
            Class<?> fluidStorageKeyClass =
                    requireClass("com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey");
            FLUID_STORAGE_KEY_GET_TRANSLATION =
                    requireMethod(fluidStorageKeyClass, "getTranslationKeyFor", MATERIAL_TYPE);
            FLUID_KEY_LIQUID = staticField(FLUID_STORAGE_KEYS_CLASS, "LIQUID");
            FLUID_KEY_GAS = staticField(FLUID_STORAGE_KEYS_CLASS, "GAS");
            FLUID_KEY_MOLTEN = staticField(FLUID_STORAGE_KEYS_CLASS, "MOLTEN");
            FLUID_KEY_PLASMA = staticField(FLUID_STORAGE_KEYS_CLASS, "PLASMA");
            NULL_MATERIAL = staticField(
                    "com.gregtechceu.gtceu.common.data.GTMaterials", "NULL");
        }
    }

    private GtMaterialFacts() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * @param namespace mod id ({@code gtceu}, {@code tfg}, …)
     * @param materialPath material registry name without tag prefix ({@code stainless_steel})
     */
    static Optional<Object> material(String namespace, String materialPath) {
        if (!AVAILABLE || materialPath == null || materialPath.isEmpty()) {
            return Optional.empty();
        }
        String key = namespace + ":" + materialPath;
        try {
            Object manager = GET_INSTANCE.invoke(null);
            Object material = GET_MATERIAL_STRING.invoke(manager, key);
            if (material == null || material == NULL_MATERIAL) {
                return Optional.empty();
            }
            return Optional.of(material);
        } catch (ReflectiveOperationException e) {
            FtbQuestExportMod.LOGGER.debug("{} material lookup failed for {}: {}", LangExportLog.ITEMS_LANG, key, e.toString());
            return Optional.empty();
        }
    }

    static boolean hasPolymer(Object material) {
        return hasProperty(material, PROPERTY_KEY_POLYMER);
    }

    static boolean isElement(Object material) {
        if (!AVAILABLE || material == null) {
            return false;
        }
        try {
            return (boolean) IS_ELEMENT.invoke(material);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Lang key such as {@code gtceu.fluid.generic} (same rules as {@code FluidStorageKey#getTranslationKeyFor}).
     *
     * @param storageKey from {@link GtceuRegistryLabels} fluid path parse: {@code molten}, {@code gas}, {@code liquid},
     *                     {@code plasma}, {@code primary}
     */
    static Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey) {
        Optional<Object> materialOpt = material(namespace, materialPath);
        if (materialOpt.isEmpty()) {
            return Optional.empty();
        }
        Object material = materialOpt.get();
        Object fluidKey = switch (storageKey) {
            case "molten" -> FLUID_KEY_MOLTEN;
            case "plasma" -> FLUID_KEY_PLASMA;
            case "liquid" -> FLUID_KEY_LIQUID;
            case "gas" -> FLUID_KEY_GAS;
            case "primary" -> primaryFluidStorageKey(material);
            default -> primaryFluidStorageKey(material);
        };
        if (fluidKey == null) {
            return Optional.empty();
        }
        try {
            String key = (String) FLUID_STORAGE_KEY_GET_TRANSLATION.invoke(fluidKey, material);
            return key == null || key.isEmpty() ? Optional.empty() : Optional.of(key);
        } catch (ReflectiveOperationException e) {
            FtbQuestExportMod.LOGGER.debug(
                    "{} fluid template for {} {}: {}",
                    LangExportLog.ITEMS_LANG,
                    namespace,
                    materialPath,
                    e.toString());
            return Optional.empty();
        }
    }

    private static Object primaryFluidStorageKey(Object material) {
        if (!AVAILABLE || material == null) {
            return null;
        }
        try {
            Object fluidProperty = GET_PROPERTY.invoke(material, PROPERTY_KEY_FLUID);
            if (fluidProperty == null) {
                return FLUID_KEY_LIQUID;
            }
            return FLUID_PROPERTY_GET_PRIMARY_KEY.invoke(fluidProperty);
        } catch (ReflectiveOperationException e) {
            return FLUID_KEY_LIQUID;
        }
    }

    private static boolean hasProperty(Object material, Object propertyKey) {
        if (!AVAILABLE || material == null || propertyKey == null) {
            return false;
        }
        try {
            return (boolean) HAS_PROPERTY.invoke(material, propertyKey);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean probe() {
        try {
            Class.forName(MANAGER_CLASS, false, GtMaterialFacts.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            FtbQuestExportMod.LOGGER.info(
                    "{} GregTech not on classpath — label export uses lang-only fallbacks (expected in unit tests)",
                    LangExportLog.ITEMS_LANG);
            return false;
        }
    }

    private static Class<?> requireClass(String name) {
        try {
            return Class.forName(name, false, GtMaterialFacts.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("GT class missing: " + name, e);
        }
    }

    private static Method requireMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method method = type.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(type.getName() + "#" + name, e);
        }
    }

    private static Method requireMethod(String className, String name, Class<?>... params) {
        return requireMethod(requireClass(className), name, params);
    }

    private static Object staticField(String className, String fieldName) {
        try {
            Field field = requireClass(className).getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(className + "." + fieldName, e);
        }
    }

    /**
     * {@code tagprefix.ingot} vs {@code tagprefix.polymer.ingot} — mirrors GT {@code TagPrefix#getUnlocalizedName(Material)}.
     */
    static String tagPrefixLangKey(
            String langSuffix, String namespace, String materialPath, java.util.Map<String, String> langTable) {
        String plain = "tagprefix." + langSuffix;
        String polymer = "tagprefix.polymer." + langSuffix;
        Optional<Object> materialOpt = material(namespace, materialPath);
        if (materialOpt.isPresent()) {
            if (hasPolymer(materialOpt.get()) && langKeyPresent(langTable, polymer)) {
                return polymer;
            }
            return plain;
        }
        // No GT: never pick polymer only because the key exists in lang (fixes metal ingot → 「条」).
        return plain;
    }

    private static boolean langKeyPresent(java.util.Map<String, String> langTable, String key) {
        return langTable != null && langTable.containsKey(key);
    }

    static String normalizeStorageKey(String storageKey) {
        return storageKey == null ? "primary" : storageKey.toLowerCase(Locale.ROOT);
    }
}
