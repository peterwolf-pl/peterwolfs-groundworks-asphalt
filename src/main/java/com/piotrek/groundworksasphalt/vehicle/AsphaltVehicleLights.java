package com.piotrek.groundworksasphalt.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic lighting manager for Groundworks Asphalt machinery.
 *
 * <p>Projects realistic directional headlight and work light beams using vanilla
 * {@link LightBlock} instances (invisible, non-colliding light sources) ahead of
 * and behind the machine while its engine and lights are active.</p>
 */
public final class AsphaltVehicleLights {

    public static final int HEAD_LIGHT_LEVEL = 15;
    public static final int WORK_LIGHT_LEVEL = 13;

    private AsphaltVehicleLights() {}

    /**
     * Computes beam block positions projected from headlights or worklights.
     *
     * @param vehiclePos Position of the vehicle
     * @param forward Normalized forward vector
     * @param side Normalized lateral right vector
     * @param forwardOffset Distance forward along vehicle axis where lights are mounted
     * @param sideWidth Separation distance between left and right lamps
     * @param heightOffset Height of the lamps above ground
     * @param ranges Distance intervals in blocks for the light beam
     * @param rear True if shining backward (taillights / rear work lights)
     * @return List of block positions to illuminate
     */
    public static List<BlockPos> calculateBeams(
            Vec3 vehiclePos,
            Vec3 forward,
            Vec3 side,
            double forwardOffset,
            double sideWidth,
            double heightOffset,
            int[] ranges,
            boolean rear
    ) {
        Vec3 beamDir = rear ? forward.scale(-1.0D) : forward;
        Vec3 lampCenter = vehiclePos.add(forward.scale(forwardOffset)).add(0.0D, heightOffset, 0.0D);
        Vec3 leftLamp = lampCenter.add(side.scale(-sideWidth * 0.5D));
        Vec3 rightLamp = lampCenter.add(side.scale(sideWidth * 0.5D));

        List<BlockPos> positions = new ArrayList<>(ranges.length * 4);
        double spread = rear ? 0.35D : 0.45D;
        addBeam(positions, leftLamp, vehiclePos, beamDir, side.scale(-spread), ranges);
        addBeam(positions, rightLamp, vehiclePos, beamDir, side.scale(spread), ranges);
        return positions;
    }

    private static void addBeam(
            List<BlockPos> positions,
            Vec3 lamp,
            Vec3 vehiclePos,
            Vec3 beamDir,
            Vec3 sideDir,
            int[] ranges
    ) {
        for (int range : ranges) {
            double x = lamp.x + beamDir.x * range + sideDir.x * range;
            double z = lamp.z + beamDir.z * range + sideDir.z * range;
            add(positions, BlockPos.containing(x, vehiclePos.y + 1.0D, z));
            add(positions, BlockPos.containing(x, vehiclePos.y + 2.0D, z));
        }
    }

    private static void add(List<BlockPos> positions, BlockPos pos) {
        if (!positions.contains(pos)) {
            positions.add(pos);
        }
    }

    /**
     * Synchronizes Minecraft Light blocks with currently calculated beam positions.
     * Removes light blocks when the machine moves away, places new light blocks ahead.
     */
    public static void syncLightBlocks(
            ServerLevel level,
            List<BlockPos> held,
            List<BlockPos> next,
            int lightLevel
    ) {
        for (int i = held.size() - 1; i >= 0; i--) {
            BlockPos old = held.get(i);
            if (next.contains(old)) {
                continue;
            }
            held.remove(i);
            releaseLight(level, old, held);
        }

        for (BlockPos pos : next) {
            if (held.contains(pos) || !level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                level.setBlock(
                        pos,
                        Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel),
                        3
                );
                held.add(pos);
            } else if (state.is(Blocks.LIGHT)) {
                held.add(pos);
            }
        }
    }

    public static void releaseLight(ServerLevel level, BlockPos pos, List<BlockPos> held) {
        if (held.contains(pos)) {
            return;
        }
        if (level != null && level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void clearLights(ServerLevel level, List<BlockPos> held) {
        for (int i = held.size() - 1; i >= 0; i--) {
            BlockPos pos = held.remove(i);
            if (level != null && level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        held.clear();
    }
}
