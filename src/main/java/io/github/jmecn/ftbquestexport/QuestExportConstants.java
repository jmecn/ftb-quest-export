package io.github.jmecn.ftbquestexport;

import java.util.Set;

public final class QuestExportConstants {

    public static final String QUEST_SUBDIR = "quest-export";
    public static final String EXPORT_ROOT_PROPERTY = "quest.export.outputDir";
    public static final String EXPORT_FOLDER_PROPERTY = "quest.exportFolder";

    public static final String RUN_EXPORT_AND_EXIT = "quest.export.runAndExit";
    public static final String EXPORT_TIMEOUT_SECONDS = "quest.exportTimeoutSeconds";
    public static final String EXPORT_WORLD_NAME = "quest.exportWorldName";
    public static final String EXPORT_WORLD_DELAY_TICKS = "quest.exportWorldDelayTicks";
    public static final String EXPORT_WARMUP_TICKS = "quest.exportWarmupTicks";
    public static final int DEFAULT_WARMUP_TICKS = 100;
    public static final int DEFAULT_WORLD_DELAY_TICKS = 600;
    public static final int DEFAULT_TIMEOUT_SECONDS = 7200;
    public static final String DEFAULT_WORLD_NAME = "quest-export";

    public static final String EXPORT_LANGUAGES_PROPERTY = "quest.exportLanguages";
    public static final String FALLBACK_LOCALE = "en_us";

    // —— Asset export ——
    public static final String EXPORT_EXCLUDED_NAMESPACES_PROPERTY = "quest-export.exportExcludedNamespaces";
    public static final Set<String> DEFAULT_EXCLUDED_NAMESPACES = Set.of("additionalplacements");

    public static final String SKIP_LANG_EXPORT = "quest.skipLangExport";
    public static final String SKIP_CHAPTER_IMAGE_EXPORT = "quest.skipChapterImageExport";
    public static final String SKIP_ITEM_NAME_KEYS_EXPORT = "quest.skipItemNameKeysExport";
    public static final String SKIP_SEARCH_INDEX_EXPORT = "quest.skipSearchIndexExport";
    public static final String SKIP_CHAPTER_ICON_ATLAS_EXPORT = "quest.skipChapterIconAtlasExport";

    public static final String SEARCH_INDEX_DIR = "search-index";
    public static final String LOG_PREFIX_SEARCH_INDEX = "[search-index]";

    public static final String FTBQUESTS_NAMESPACE = "ftbquests";
    public static final String LOG_DETAIL_FAILURES = "quest.export.logDetailFailures";
    public static final int LOG_DETAIL_FAILURE_LIMIT = 20;
    public static final String LOG_PREFIX_LANG = "[lang]";

    public static final String CHAPTER_IMAGES_REL_ROOT = "assets/chapter-images";
    public static final int CHAPTER_IMAGE_MIN_FRAME_PX = 16;

    /** Atlas cell for registry item / fluid (MC GUI native 16×16; not upscaled). */
    public static final int ITEM_FLUID_ATLAS_PX = 16;
    /** Task/reward row icons in quest detail (QuestBook-React default). */
    public static final int DETAIL_ITEM_ICON_PX = 32;
    public static final int[] ICON_TIERS = {16, 32, 64, 128};
    public static final int ATLAS_MAX_WIDTH_PX = 2048;
    public static final int ATLAS_MAX_HEIGHT_PX = 4096;
    public static final int ATLAS_PADDING_PX = 0;
    public static final int GRID_STEP_BASE = 24;
    public static final double INNER_RATIO = 2.0 / 3.0;
    public static final double BUTTON_CELL_RATIO = 1.5 / (1.5 + 0.25);
    public static final String MISSING_ICON_REGISTRY_ID = "minecraft_web_export:missing_icon";
    public static final String LOG_PREFIX_CHAPTER_ICON_ATLAS = "[chapter-icon-atlas]";
    public static final int MISSING_ICON_MAGENTA = 0xFFFF00FF;
    public static final int MISSING_ICON_BLACK = 0xFF000000;

    public static final String ITEM_NAME_KEYS_FILE = "items/name-keys.json";
    public static final String ITEMS_INDEX_FILE = "items/index.json";
    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    private QuestExportConstants() {}
}
