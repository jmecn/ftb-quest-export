package io.github.jmecn.ftbquestexport.mod;

import io.github.jmecn.ftbquestexport.export.ci.QuestExportCiDriver;
import io.github.jmecn.ftbquestexport.export.QuestExportProperties;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Forge entrypoint for FTB Quests export ({@code /ftbquestexport run}, CI {@code quest.export.runAndExit}). */
@Mod(FtbQuestExportMod.MOD_ID)
public final class FtbQuestExportMod {

    public static final String MOD_ID = "ftb_quest_export";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public FtbQuestExportMod() {
        if (QuestExportProperties.runExportAndExit()) {
            QuestExportCiDriver.register();
        }
        LOGGER.info("FTB Quest Export initialized — /ftbquestexport run; CI: quest.export.runAndExit");
    }
}
