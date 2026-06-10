package io.github.jmecn.ftbquestexport.export.lang;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager;

import java.util.Map;
import java.util.Optional;

/**
 * Direct GTCEu API integration — loaded only when {@code gtceu} is present at runtime
 * (see {@link GtMaterialFacts}).
 */
final class GtMaterialFactsGtBackend implements GtMaterialFactsBackend {

    @Override
    public Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey) {
        Material material = resolveMaterial(namespace, materialPath);
        if (material == null) {
            return Optional.empty();
        }
        FluidStorageKey fluidKey = switch (storageKey) {
            case "molten" -> FluidStorageKeys.MOLTEN;
            case "plasma" -> FluidStorageKeys.PLASMA;
            case "liquid" -> FluidStorageKeys.LIQUID;
            case "gas" -> FluidStorageKeys.GAS;
            case "primary" -> primaryFluidStorageKey(material);
            default -> primaryFluidStorageKey(material);
        };
        String key = fluidKey.getTranslationKeyFor(material);
        return key == null || key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    @Override
    public String tagPrefixLangKey(
            String langSuffix, String namespace, String materialPath, Map<String, String> langTable) {
        String plain = "tagprefix." + langSuffix;
        String polymer = "tagprefix.polymer." + langSuffix;
        Material material = resolveMaterial(namespace, materialPath);
        if (material != null) {
            if (material.hasProperty(PropertyKey.POLYMER) && langKeyPresent(langTable, polymer)) {
                return polymer;
            }
            return plain;
        }
        return plain;
    }

    private static Material resolveMaterial(String namespace, String materialPath) {
        if (materialPath == null || materialPath.isEmpty()) {
            return null;
        }
        Material material = MaterialRegistryManager.getInstance().getMaterial(namespace + ":" + materialPath);
        if (material == null || material == GTMaterials.NULL) {
            return null;
        }
        return material;
    }

    private static FluidStorageKey primaryFluidStorageKey(Material material) {
        FluidProperty fluidProperty = material.getProperty(PropertyKey.FLUID);
        if (fluidProperty == null) {
            return FluidStorageKeys.LIQUID;
        }
        FluidStorageKey primary = fluidProperty.getPrimaryKey();
        return primary != null ? primary : FluidStorageKeys.LIQUID;
    }

    private static boolean langKeyPresent(Map<String, String> langTable, String key) {
        return langTable != null && langTable.containsKey(key);
    }
}
