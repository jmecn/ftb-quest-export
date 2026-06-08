package io.github.jmecn.ftbquestexport.export.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jmecn.ftbquestexport.export.scan.QuestScanResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Writes {@code extras/tag-members.json} using integrated server tag data. */
public final class QuestTagMembersExporter {

    private static final Logger LOGGER = LogManager.getLogger("ftb-quest-export");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestTagMembersExporter() {}

    public record Result(int tagsRequested, int totalMemberRefs, long bytes) {}

    public static boolean isEnabled() {
        return !Boolean.getBoolean("quest.skipTagMembersExport");
    }

    public static Result export(Path outputDir, MinecraftServer server, QuestScanResult scan) throws IOException {
        Set<String> tagIds = scan.getTags();
        if (tagIds.isEmpty() || server == null) {
            return new Result(0, 0, 0);
        }

        Map<String, List<String>> items = new LinkedHashMap<>();
        Map<String, List<String>> blocks = new LinkedHashMap<>();
        Map<String, List<String>> fluids = new LinkedHashMap<>();
        int memberRefs = 0;

        for (String tagId : tagIds) {
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                continue;
            }
            List<String> itemMembers = expandItemTag(server, loc);
            if (!itemMembers.isEmpty()) {
                items.put(tagId, itemMembers);
                memberRefs += itemMembers.size();
            }
            List<String> blockMembers = expandBlockTag(server, loc);
            if (!blockMembers.isEmpty()) {
                blocks.put(tagId, blockMembers);
                memberRefs += blockMembers.size();
            }
            List<String> fluidMembers = expandFluidTag(server, loc);
            if (!fluidMembers.isEmpty()) {
                fluids.put(tagId, fluidMembers);
                memberRefs += fluidMembers.size();
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        if (!items.isEmpty()) {
            root.put("items", items);
        }
        if (!blocks.isEmpty()) {
            root.put("blocks", blocks);
        }
        if (!fluids.isEmpty()) {
            root.put("fluids", fluids);
        }

        Path out = outputDir.resolve("extras/tag-members.json");
        Files.createDirectories(out.getParent());
        String json = GSON.toJson(root);
        Files.writeString(out, json);
        LOGGER.info("[tag-members] {} tags, {} member refs", tagIds.size(), memberRefs);
        return new Result(tagIds.size(), memberRefs, json.length());
    }

    private static List<String> expandItemTag(MinecraftServer server, ResourceLocation tagId) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        return server.registryAccess().registryOrThrow(Registries.ITEM).getTag(key)
                .map(h -> {
                    List<String> ids = new ArrayList<>();
                    h.stream().forEach(item -> ids.add(item.unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("unknown")));
                    return sorted(ids);
                })
                .orElse(List.of());
    }

    private static List<String> expandBlockTag(MinecraftServer server, ResourceLocation tagId) {
        TagKey<Block> key = TagKey.create(Registries.BLOCK, tagId);
        return server.registryAccess().registryOrThrow(Registries.BLOCK).getTag(key)
                .map(h -> {
                    List<String> ids = new ArrayList<>();
                    h.stream().forEach(block -> ids.add(block.unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("unknown")));
                    return sorted(ids);
                })
                .orElse(List.of());
    }

    private static List<String> expandFluidTag(MinecraftServer server, ResourceLocation tagId) {
        TagKey<Fluid> key = TagKey.create(Registries.FLUID, tagId);
        return server.registryAccess().registryOrThrow(Registries.FLUID).getTag(key)
                .map(h -> {
                    List<String> ids = new ArrayList<>();
                    h.stream().forEach(fluid -> ids.add(fluid.unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("unknown")));
                    return sorted(ids);
                })
                .orElse(List.of());
    }

    private static List<String> sorted(List<String> ids) {
        return new ArrayList<>(new TreeSet<>(ids));
    }
}
