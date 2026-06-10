package io.github.jmecn.ftbquestexport.export.lang;

import java.util.Map;
import java.util.Optional;

interface GtMaterialFactsBackend {

    GtMaterialFactsBackend NOOP = new GtMaterialFactsBackend() {
        @Override
        public Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey) {
            return Optional.empty();
        }

        @Override
        public String tagPrefixLangKey(
                String langSuffix, String namespace, String materialPath, Map<String, String> langTable) {
            return "tagprefix." + langSuffix;
        }
    };

    Optional<String> fluidTranslationKey(String namespace, String materialPath, String storageKey);

    String tagPrefixLangKey(
            String langSuffix, String namespace, String materialPath, Map<String, String> langTable);
}
