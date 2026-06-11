package io.github.jmecn.ftbquestexport.assets;

import io.github.jmecn.ftbquestexport.QuestExportJson;
import io.github.jmecn.ftbquestexport.pojo.FluidExportResult;
import io.github.jmecn.ftbquestexport.scan.QuestScanResult;
import io.github.jmecn.ftbquestexport.FtbQuestExportMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestFluidExporter {

    private QuestFluidExporter() {}

    public static FluidExportResult export(Path outputDir, QuestScanResult scan) throws IOException {
        Path out = outputDir.resolve("extras/fluids.json");
        Map<String, Object> root = new LinkedHashMap<>();

        for (String fluidId : scan.getFluids()) {
            ResourceLocation loc = ResourceLocation.tryParse(fluidId);
            if (loc == null) {
                continue;
            }
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
            if (fluid == null || fluid.defaultFluidState().isEmpty()) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("displayNameKey", fluid.getFluidType().getDescriptionId());
            root.put(fluidId, entry);
        }

        Files.createDirectories(out.getParent());
        String json = QuestExportJson.PRETTY.toJson(root);
        Files.writeString(out, json);
        FtbQuestExportMod.LOGGER.info("[fluids] wrote {} entries ({} bytes)", root.size(), json.length());
        return new FluidExportResult(root.size(), json.length());
    }
}
