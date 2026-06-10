package io.github.jmecn.ftbquestexport.export.lang;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads GregTech {@code Material} facts at export time when GTCEu is loaded.
 * Uses compile-time GTCEu types via {@link GtMaterialFactsGtBackend}; when GT is absent, falls back to lang-only rules.
 */
final class GtMaterialFacts {

    private static final String GT_BACKEND_CLASS =
            "io.github.jmecn.ftbquestexport.export.lang.GtMaterialFactsGtBackend";
    private static final String GT_MANAGER_CLASS =
            "com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager";

    private static final GtMaterialFactsBackend BACKEND = loadBackend();
    private static final boolean AVAILABLE = BACKEND != GtMaterialFactsBackend.NOOP;

    private GtMaterialFacts() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Lang key such as {@code gtceu.fluid.generic} (same rules as {@code FluidStorageKey#getTranslationKeyFor}).
     *
     * @param storageKey from {@link GtceuRegistryLabels} fluid path parse: {@code molten}, {@code gas}, {@code liquid},
     *                     {@code plasma}, {@code primary}
     */
    static Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey) {
        return BACKEND.fluidTranslationKey(namespace, materialPath, storageKey);
    }

    /**
     * {@code tagprefix.ingot} vs {@code tagprefix.polymer.ingot} — mirrors GT {@code TagPrefix#getUnlocalizedName(Material)}.
     */
    static String tagPrefixLangKey(
            String langSuffix, String namespace, String materialPath, Map<String, String> langTable) {
        return BACKEND.tagPrefixLangKey(langSuffix, namespace, materialPath, langTable);
    }

    static String normalizeStorageKey(String storageKey) {
        return storageKey == null ? "primary" : storageKey.toLowerCase(Locale.ROOT);
    }

    private static GtMaterialFactsBackend loadBackend() {
        if (!isGtceuPresent()) {
            FtbQuestExportMod.LOGGER.info(
                    "{} GregTech not loaded — label export uses lang-only fallbacks (expected without gtceu mod)",
                    LangExportLog.ITEMS_LANG);
            return GtMaterialFactsBackend.NOOP;
        }
        try {
            Class<?> backendClass = Class.forName(GT_BACKEND_CLASS, false, GtMaterialFacts.class.getClassLoader());
            return (GtMaterialFactsBackend) backendClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            FtbQuestExportMod.LOGGER.warn(
                    "{} GregTech detected but backend failed to load — using lang-only fallbacks: {}",
                    LangExportLog.ITEMS_LANG,
                    e.toString());
            return GtMaterialFactsBackend.NOOP;
        }
    }

    private static boolean isGtceuPresent() {
        try {
            if (net.minecraftforge.fml.ModList.get().isLoaded("gtceu")) {
                return true;
            }
        } catch (Throwable ignored) {
            // Not running under Forge (e.g. isolated tests).
        }
        try {
            Class.forName(GT_MANAGER_CLASS, false, GtMaterialFacts.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
