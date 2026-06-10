package io.github.jmecn.ftbquestexport.export.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves registry ids to display labels (aligned with emi-recipe-renderer/registry-label.mjs). */
public final class RegistryLabelResolver {

    /** Namespaces that use GTCEu-style composed labels before flat / exported keys. */
    static final Set<String> COMPOSED_FIRST_NAMESPACES = Set.of("gtceu", "tfg", "greate");

    private final Map<String, String> current;
    private final Map<String, String> fallback;
    private final Map<String, String> merged;
    private final Map<String, String> nameKeysByRegistryId;

    public RegistryLabelResolver(Map<String, String> current, Map<String, String> fallback) {
        this(current, fallback, Map.of());
    }

    public RegistryLabelResolver(
            Map<String, String> current,
            Map<String, String> fallback,
            Map<String, String> nameKeysByRegistryId) {
        this.current = current == null ? Map.of() : current;
        this.fallback = fallback == null ? Map.of() : fallback;
        this.nameKeysByRegistryId = nameKeysByRegistryId == null ? Map.of() : nameKeysByRegistryId;
        this.merged = new HashMap<>(this.fallback);
        this.merged.putAll(this.current);
    }

    public String translate(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (current.containsKey(key)) {
            return current.get(key);
        }
        if (fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return key;
    }

    public String translateRegistry(String registryId) {
        return translateRegistry(registryId, "item");
    }

    public String translateRegistry(String registryId, String kind) {
        String bare = RegistryLangKeys.normalizeRegistryId(registryId);
        String namespace = registryNamespace(bare);

        if (COMPOSED_FIRST_NAMESPACES.contains(namespace) && isRegistryKind(kind)) {
            String composed = GtceuRegistryLabels.translateComposedRegistry(bare, kind, this::translate, merged);
            if (composed != null) {
                return composed;
            }
            return translateDefaultRules(bare, registryId, kind);
        }

        return translateDefaultRules(bare, registryId, kind);
    }

    /** Namespace segment before {@code :}, or {@code minecraft} when absent. */
    static String registryNamespace(String bareRegistryId) {
        String bare = RegistryLangKeys.normalizeRegistryId(bareRegistryId);
        String namespace = RegistryLangKeys.namespace(bare);
        return namespace.isEmpty() ? "minecraft" : namespace;
    }

    private static boolean isRegistryKind(String kind) {
        return "item".equals(kind) || "block".equals(kind) || "fluid".equals(kind);
    }

    private String translateDefaultRules(String bare, String registryId, String kind) {
        String exportedKey = nameKeysByRegistryId.get(bare);
        if (exportedKey != null && !exportedKey.isBlank()) {
            String label = translate(exportedKey);
            if (!exportedKey.equals(label)) {
                return label;
            }
        }

        List<String> candidates = switch (kind) {
            case "fluid" -> RegistryLangKeys.fluidLookupKeys(registryId);
            case "block" -> List.of(
                    RegistryLangKeys.blockKey(registryId),
                    RegistryLangKeys.itemKey(registryId),
                    RegistryLangKeys.fluidKey(registryId));
            default -> RegistryLangKeys.itemLookupKeys(registryId);
        };
        for (String candidate : candidates) {
            String label = translate(candidate);
            if (!candidate.equals(label)) {
                return label;
            }
        }

        return bare.isEmpty() ? String.valueOf(registryId) : bare;
    }
}
