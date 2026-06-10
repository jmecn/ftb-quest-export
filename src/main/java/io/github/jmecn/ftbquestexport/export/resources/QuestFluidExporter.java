package io.github.jmecn.ftbquestexport.export.resources;

import io.github.jmecn.ftbquestexport.mod.FtbQuestExportMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestFluidExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestFluidExporter() {}

    public record Result(int fluidsWritten, long bytes) {}

    public static Result export(Path outputDir, QuestScanResult scan) throws IOException {
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
        String json = GSON.toJson(root);
        Files.writeString(out, json);
        FtbQuestExportMod.LOGGER.info("[fluids] wrote {} entries ({} bytes)", root.size(), json.length());
        return new Result(root.size(), json.length());
    }
}
