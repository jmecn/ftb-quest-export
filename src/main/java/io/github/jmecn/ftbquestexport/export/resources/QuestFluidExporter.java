package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes {@code extras/fluids.json} for quest-referenced fluids. */
public final class QuestFluidExporter {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");
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
            Fluid fluid = BuiltInRegistries.FLUID.get(loc);
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
        LOGGER.info("[fluids] wrote {} entries ({} bytes)", root.size(), json.length());
        return new Result(root.size(), json.length());
    }
}
