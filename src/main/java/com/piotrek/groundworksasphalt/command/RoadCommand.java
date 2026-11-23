package com.piotrek.groundworksasphalt.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Commands for building and shaping engineered roads, subgrades, and asphalt pavement.
 */
public final class RoadCommand {

    private static final String[] MATERIAL_SUGGESTIONS = new String[]{"gravel", "dirt", "sand", "cobblestone"};

    private RoadCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("road")
                        // 1. Subgrade building (/road subgrade [material], /road build_subgrade, /road build)
                        .then(Commands.literal("subgrade")
                                .executes(ctx -> executeBuildSubgrade(ctx.getSource(), "gravel"))
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildSubgrade(ctx.getSource(), StringArgumentType.getString(ctx, "material")))))
                        .then(Commands.literal("build_subgrade")
                                .executes(ctx -> executeBuildSubgrade(ctx.getSource(), "gravel"))
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildSubgrade(ctx.getSource(), StringArgumentType.getString(ctx, "material")))))
                        .then(Commands.literal("build")
                                .executes(ctx -> executeBuildSubgrade(ctx.getSource(), "gravel"))
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildSubgrade(ctx.getSource(), StringArgumentType.getString(ctx, "material")))))

                        // 2. Pave fresh asphalt mat (/road pave [material], /road lay_asphalt, /road fresh_asphalt, /road mat)
                        .then(Commands.literal("pave")
                                .executes(ctx -> executePaveAsphalt(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executePaveAsphalt(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("lay_asphalt")
                                .executes(ctx -> executePaveAsphalt(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executePaveAsphalt(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("fresh_asphalt")
                                .executes(ctx -> executePaveAsphalt(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executePaveAsphalt(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("mat")
                                .executes(ctx -> executePaveAsphalt(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executePaveAsphalt(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))

                        // 3. Complete finished rolled road (/road finish [material], /road asphalt, /road complete, /road full)
                        .then(Commands.literal("finish")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("asphalt")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("complete")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("full")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("compact")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
                        .then(Commands.literal("roll")
                                .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), "gravel"))
                                .then(Commands.argument("foundation_material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MATERIAL_SUGGESTIONS, builder))
                                        .executes(ctx -> executeBuildFinishedRoad(ctx.getSource(), StringArgumentType.getString(ctx, "foundation_material")))))
        );

        // Alias /roadbed -> /road
        dispatcher.register(Commands.literal("roadbed").redirect(root));
    }

    private static GranularMaterial parseMaterial(String materialName) {
        return switch (materialName.toLowerCase()) {
            case "dirt" -> GranularMaterialRegistry.DIRT;
            case "sand" -> GranularMaterialRegistry.SAND;
            case "cobblestone", "stone" -> GranularMaterialRegistry.COBBLESTONE;
            default -> GranularMaterialRegistry.GRAVEL;
        };
    }

    private static int executeBuildSubgrade(CommandSourceStack source, String materialName) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        GranularMaterial material = parseMaterial(materialName);

        RoadSurveyCorridor.BuildResult result = RoadSurveyCorridor.buildSubgradeBetweenStakes(
                level, origin, 256.0D, material
        );

        if (!result.success()) {
            source.sendFailure(result.message());
            return 0;
        }

        source.sendSuccess(() -> result.message(), true);
        return result.columnsShaped();
    }

    private static int executePaveAsphalt(CommandSourceStack source, String materialName) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        GranularMaterial material = parseMaterial(materialName);

        RoadSurveyCorridor.BuildResult result = RoadSurveyCorridor.paveAsphaltBetweenStakes(
                level, origin, 256.0D, material
        );

        if (!result.success()) {
            source.sendFailure(result.message());
            return 0;
        }

        source.sendSuccess(() -> result.message(), true);
        return result.columnsShaped();
    }

    private static int executeBuildFinishedRoad(CommandSourceStack source, String materialName) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        GranularMaterial material = parseMaterial(materialName);

        RoadSurveyCorridor.BuildResult result = RoadSurveyCorridor.buildFinishedRoadBetweenStakes(
                level, origin, 256.0D, material
        );

        if (!result.success()) {
            source.sendFailure(result.message());
            return 0;
        }

        source.sendSuccess(() -> result.message(), true);
        return result.columnsShaped();
    }
}
