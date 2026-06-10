package io.github.jmecn.ftbquestexport.export;

import java.util.Set;

/** Centralized system properties, path segments, defaults, and export tuning knobs. */
public final class QuestExportConstants {

    // —— Output layout ——
    public static final String QUEST_SUBDIR = "quest-export";
    public static final String EXPORT_ROOT_PROPERTY = "quest.export.outputDir";
    public static final String EXPORT_FOLDER_PROPERTY = "quest.exportFolder";

    // —— CI / headless export ——
    public static final String RUN_EXPORT_AND_EXIT = "quest.export.runAndExit";
    public static final String EXPORT_TIMEOUT_SECONDS = "quest.exportTimeoutSeconds";
    public static final String EXPORT_WORLD_NAME = "quest.exportWorldName";
    public static final String EXPORT_WORLD_DELAY_TICKS = "quest.exportWorldDelayTicks";
    public static final String EXPORT_WARMUP_TICKS = "quest.exportWarmupTicks";
    public static final int DEFAULT_WARMUP_TICKS = 100;
    public static final int DEFAULT_WORLD_DELAY_TICKS = 600;
    public static final int DEFAULT_TIMEOUT_SECONDS = 7200;
    public static final String DEFAULT_WORLD_NAME = "quest-export";

    // —— Locales ——
    public static final String EXPORT_LANGUAGES_PROPERTY = "quest.exportLanguages";
    public static final String FALLBACK_LOCALE = "en_us";

    // —— Asset export ——
    public static final String EXPORT_EXCLUDED_NAMESPACES_PROPERTY = "quest-export.exportExcludedNamespaces";
    public static final Set<String> DEFAULT_EXCLUDED_NAMESPACES = Set.of("additionalplacements");

    // —— Feature toggles (skip flags) ——
    public static final String SKIP_LANG_EXPORT = "quest.skipLangExport";
    public static final String SKIP_ICON_EXPORT = "quest.skipIconExport";
    public static final String SKIP_CHAPTER_IMAGE_EXPORT = "quest.skipChapterImageExport";
    public static final String SKIP_ITEM_NAME_KEYS_EXPORT = "quest.skipItemNameKeysExport";
    public static final String SKIP_SEARCH_INDEX_EXPORT = "quest.skipSearchIndexExport";

    // —— Quest search index ——
    public static final String SEARCH_INDEX_DIR = "search-index";
    public static final String LOG_PREFIX_SEARCH_INDEX = "[search-index]";

    // —— Lang merge ——
    public static final String FTBQUESTS_NAMESPACE = "ftbquests";
    public static final String LOG_DETAIL_FAILURES = "quest.export.logDetailFailures";
    public static final int LOG_DETAIL_FAILURE_LIMIT = 20;
    public static final String LOG_PREFIX_LANG = "[lang]";

    // —— Chapter decoration images ——
    public static final String CHAPTER_IMAGES_REL_ROOT = "assets/chapter-images";
    public static final int CHAPTER_IMAGE_MIN_FRAME_PX = 16;

    // —— Item / fluid icon rendering ——
    public static final int ICON_CELL_PX = 32;
    public static final int ICON_FLUSH_RENDER_EVERY = 256;
    public static final int ICON_LOG_STRIDE = 200;
    public static final int FLUID_ICON_GUI_SIZE = 16;
    public static final int MISSING_ICON_MAGENTA = 0xFFFF00FF;
    public static final int MISSING_ICON_BLACK = 0xFF000000;

    // —— Index / sidecar JSON paths ——
    public static final String ITEM_NAME_KEYS_FILE = "items/name-keys.json";
    public static final String ITEMS_INDEX_FILE = "items/index.json";
    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    private QuestExportConstants() {}
}
