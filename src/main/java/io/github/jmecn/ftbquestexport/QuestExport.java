package io.github.jmecn.ftbquestexport;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class QuestExport {

    private QuestExport() {}

    public static int run(CommandSourceStack source) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                source.sendFailure(Component.literal("[ftb-quest-export] client unavailable"));
                return 0;
            }
            Path questDir = QuestExportPaths.questDirectory(client.gameDirectory.toPath());
            Component message = QuestExportPipeline.run(questDir);
            source.sendSystemMessage(message);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("[ftb-quest-export] Export failed: " + e.getMessage()));
            return 0;
        }
    }
}
