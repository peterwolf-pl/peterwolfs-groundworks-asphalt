package com.piotrek.groundworksasphalt.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Commands for the Cat Motor Grader (Równiarka Drogowa).
 */
public final class CatGraderCommand {

    private CatGraderCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("grader")
                        .then(Commands.literal("autolevel")
                                .executes(ctx -> toggleAutoLevel(ctx.getSource()))
                                .then(Commands.literal("on")
                                        .executes(ctx -> setAutoLevel(ctx.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> setAutoLevel(ctx.getSource(), false)))
                                .then(Commands.literal("toggle")
                                        .executes(ctx -> toggleAutoLevel(ctx.getSource()))))
                        .then(Commands.literal("level")
                                .executes(ctx -> toggleAutoLevel(ctx.getSource()))
                                .then(Commands.literal("on")
                                        .executes(ctx -> setAutoLevel(ctx.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> setAutoLevel(ctx.getSource(), false)))
                                .then(Commands.literal("toggle")
                                        .executes(ctx -> toggleAutoLevel(ctx.getSource()))))
        );

        // Alias /catgrader -> /grader
        dispatcher.register(Commands.literal("catgrader").redirect(root));
    }

    private static int toggleAutoLevel(CommandSourceStack source) {
        CatGraderEntity grader = findTargetGrader(source);
        if (grader == null) {
            source.sendFailure(Component.translatable("command.pw_groundworks_asphalt.grader_not_found"));
            return 0;
        }

        boolean next = !grader.isAutoLevel();
        grader.setAutoLevel(next);
        if (next) {
            source.sendSuccess(() -> Component.translatable("command.pw_groundworks_asphalt.grader_autolevel_on"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.pw_groundworks_asphalt.grader_autolevel_off"), true);
        }
        return 1;
    }

    private static int setAutoLevel(CommandSourceStack source, boolean enabled) {
        CatGraderEntity grader = findTargetGrader(source);
        if (grader == null) {
            source.sendFailure(Component.translatable("command.pw_groundworks_asphalt.grader_not_found"));
            return 0;
        }

        grader.setAutoLevel(enabled);
        if (enabled) {
            source.sendSuccess(() -> Component.translatable("command.pw_groundworks_asphalt.grader_autolevel_on"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.pw_groundworks_asphalt.grader_autolevel_off"), true);
        }
        return 1;
    }

    private static CatGraderEntity findTargetGrader(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return null;
        }

        // 1. If currently operating/riding the grader
        if (player.getVehicle() instanceof CatGraderEntity grader) {
            return grader;
        }

        // 2. Raycast in eye look direction up to 14 blocks
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 reach = eye.add(look.scale(14.0D));
        AABB box = player.getBoundingBox().expandTowards(look.scale(14.0D)).inflate(2.0D);

        for (CatGraderEntity grader : player.level().getEntitiesOfClass(CatGraderEntity.class, box)) {
            AABB graderBox = grader.getBoundingBox().inflate(0.6D);
            if (graderBox.clip(eye, reach).isPresent()) {
                return grader;
            }
        }

        // 3. Fallback: nearest grader within 8 blocks
        List<CatGraderEntity> nearby = player.level().getEntitiesOfClass(
                CatGraderEntity.class, player.getBoundingBox().inflate(8.0D));
        if (!nearby.isEmpty()) {
            nearby.sort(Comparator.comparingDouble(g -> g.distanceToSqr(player)));
            return nearby.get(0);
        }

        return null;
    }
}
