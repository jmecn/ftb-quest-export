package io.github.jmecn.ftbquestexport.export.lang;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

/**
 * Shared export log stage prefixes and bulk-export detail logging policy.
 */
final class LangExportLog {

    static final String LANG = "[lang]";
    static final String ITEMS_LANG = "[items-lang]";
    static final String ITEM_NAME_KEYS = "[item-name-keys]";

    static final int DETAIL_FAILURE_LIMIT = 20;

    private static final boolean DETAIL_FAILURES_ENABLED =
            Boolean.getBoolean("quest.export.logDetailFailures");

    private LangExportLog() {}

    static void detailFailure(int failureCount, String message, Object... args) {
        if (failureCount > DETAIL_FAILURE_LIMIT) {
            return;
        }
        if (DETAIL_FAILURES_ENABLED) {
            FtbQuestExportMod.LOGGER.warn(message, args);
        } else if (FtbQuestExportMod.LOGGER.isDebugEnabled()) {
            FtbQuestExportMod.LOGGER.debug(message, args);
        }
    }
}
