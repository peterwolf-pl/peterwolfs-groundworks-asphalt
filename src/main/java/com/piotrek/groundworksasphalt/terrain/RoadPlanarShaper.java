package com.piotrek.groundworksasphalt.terrain;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.block.entity.GranularBlockEntity;
import com.piotrek.groundworks.networking.GranularSyncHandler;
import com.piotrek.groundworks.terrain.cell.DirtyFlags;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.conversion.BlockConverter;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import com.piotrek.groundworksasphalt.block.AsphaltSlabBlock;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Precision 3D Planar Microvoxel Shaper for Road Subgrades and Asphalt Pavement.
 *
 * <p>Shapes the 64 individual micro-columns (8x8 resolution) of a {@link GranularCell}
 * to an exact continuous 3D mathematical plane equation Y(worldX, worldZ).
 * This eliminates horizontal staircases and discrete block steps, enabling
 * continuous smooth roadbed subgrades and smooth asphalt ramps at ANY arbitrary angle.</p>
 */
public final class RoadPlanarShaper {

    private RoadPlanarShaper() {}

    @FunctionalInterface
    public interface PlanarSurfaceFunction {
        double getY(double worldX, double worldZ);
    }

    public record ShapeResult(
            boolean changed,
            int unitsExcavated,
            int unitsDeposited,
            @Nullable GranularMaterial material
    ) {
        public static final ShapeResult NONE = new ShapeResult(false, 0, 0, null);
    }

    /**
     * Shapes a single world block position to the target 3D planar surface.
     *
     * @param level Server level
     * @param pos Block position
     * @param surface Continuous 3D plane elevation function
     * @param defaultFillMaterial Material to use for filling hollows
     * @param availableFillUnits Maximum units allowed to deposit
     * @param forceCompactedAsphalt True if shaping/compacting asphalt pavement
     * @return Result of the shaping operation
     */
    public static ShapeResult shapeCellToSurface(
            ServerLevel level,
            BlockPos pos,
            PlanarSurfaceFunction surface,
            @Nullable GranularMaterial defaultFillMaterial,
            int availableFillUnits,
            boolean forceCompactedAsphalt
    ) {
        return shapeCellToSurface(
                level, pos, surface, defaultFillMaterial, availableFillUnits,
                forceCompactedAsphalt, Integer.MAX_VALUE
        );
    }

    /**
     * Capacity-aware shaping variant. The excavation limit lets a machine preserve mass without
     * removing more terrain than its internal surge buffer can accept.
     */
    public static ShapeResult shapeCellToSurface(
            ServerLevel level,
            BlockPos pos,
            PlanarSurfaceFunction surface,
            @Nullable GranularMaterial defaultFillMaterial,
            int availableFillUnits,
            boolean forceCompactedAsphalt,
            int maxExcavationUnits
    ) {
        RoadbedCorridorStorage.RoadbedStage autoStage = forceCompactedAsphalt
                ? RoadbedCorridorStorage.RoadbedStage.FINISHED
                : null;
        return shapeCellToSurface(
                level, pos, surface, defaultFillMaterial, availableFillUnits,
                forceCompactedAsphalt, maxExcavationUnits, autoStage, 0.0D, 0.0D
        );
    }

    /**
     * Capacity- and corridor-stage aware shaping variant.
     * Records the cumulative progression of the roadbed mesh in {@link RoadbedCorridorStorage}.
     */
    public static ShapeResult shapeCellToSurface(
            ServerLevel level,
            BlockPos pos,
            PlanarSurfaceFunction surface,
            @Nullable GranularMaterial defaultFillMaterial,
            int availableFillUnits,
            boolean forceCompactedAsphalt,
            int maxExcavationUnits,
            @Nullable RoadbedCorridorStorage.RoadbedStage stageToRecord,
            double slopeX,
            double slopeZ
    ) {
        return shapeCellToSurface(
                level, pos, surface, defaultFillMaterial, availableFillUnits,
                forceCompactedAsphalt, maxExcavationUnits, stageToRecord, slopeX, slopeZ, false
        );
    }

    public static ShapeResult shapeCellToSurface(
            ServerLevel level,
            BlockPos pos,
            PlanarSurfaceFunction surface,
            @Nullable GranularMaterial defaultFillMaterial,
            int availableFillUnits,
            boolean forceCompactedAsphalt,
            int maxExcavationUnits,
            @Nullable RoadbedCorridorStorage.RoadbedStage stageToRecord,
            double slopeX,
            double slopeZ,
            boolean isVehicleRoller
    ) {
        if (level == null || pos == null || surface == null) {
            return ShapeResult.NONE;
        }

        GranularWorldStorage storage = GranularWorldStorage.get(level);
        if (storage == null) {
            return ShapeResult.NONE;
        }

        BlockState state = level.getBlockState(pos);
        GranularCell cell = storage.getCell(pos);
        if (cell == null || cell.isEmpty()) {
            if (state.is(GroundworksMod.GRANULAR_BLOCK)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof GranularBlockEntity gbe && gbe.getCell() != null && !gbe.getCell().isEmpty()) {
                    cell = gbe.getCell();
                    storage.putCell(pos, cell);
                }
            }
        }

        boolean isExistingAsphaltBlock = (state.getBlock() instanceof FreshAsphaltBlock)
                || (state.getBlock() instanceof CompactedAsphaltBlock)
                || (state.getBlock() instanceof AsphaltSlabBlock);

        boolean isExistingAsphaltCell = (cell != null && !cell.isEmpty()) && (
                (cell.surfaceCapMaterialId() == GranularMaterialRegistry.ASPHALT.id())
                || (cell.surfaceCapMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                || (cell.materialId() == GranularMaterialRegistry.ASPHALT.id())
                || (cell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                || (cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()) > 0)
                || (cell.unitsOfMaterial(GranularMaterialRegistry.COMPACTED_ASPHALT.id()) > 0)
        );

        boolean aboveHasAsphalt = RoadRollerEntity.hasAsphaltInCell(level, pos.above())
                || (level.getBlockState(pos.above()).getBlock() instanceof FreshAsphaltBlock)
                || (level.getBlockState(pos.above()).getBlock() instanceof CompactedAsphaltBlock);

        boolean isPavingAsphalt = (stageToRecord == RoadbedCorridorStorage.RoadbedStage.PAVED
                || defaultFillMaterial == GranularMaterialRegistry.ASPHALT);
        boolean isFinishingAsphalt = (stageToRecord == RoadbedCorridorStorage.RoadbedStage.FINISHED
                || forceCompactedAsphalt
                || defaultFillMaterial == GranularMaterialRegistry.COMPACTED_ASPHALT);
        boolean isAsphaltTarget = isPavingAsphalt || isFinishingAsphalt;

        GranularMaterial targetAsphaltMat = isFinishingAsphalt
                ? GranularMaterialRegistry.COMPACTED_ASPHALT
                : GranularMaterialRegistry.ASPHALT;

        boolean isExplicitPavingCommand = !isVehicleRoller && (stageToRecord == RoadbedCorridorStorage.RoadbedStage.PAVED
                || stageToRecord == RoadbedCorridorStorage.RoadbedStage.FINISHED);

        // 1. When rolling/smoothing asphalt: vehicle roller only compacts where asphalt already exists.
        // Explicit paving and road-finishing operations CAN lay asphalt onto subgrade!
        if (isAsphaltTarget) {
            if (!isExplicitPavingCommand && !aboveHasAsphalt && !isExistingAsphaltBlock && !isExistingAsphaltCell) {
                return ShapeResult.NONE;
            }
        }
        // 2. When shaping loose subgrade (compactor/grader): NEVER modify or overwrite existing asphalt!
        else if (isExistingAsphaltBlock || isExistingAsphaltCell) {
            return ShapeResult.NONE;
        }

        // Check valid surface range inside the corridor across microvoxels
        double validMinY = Double.MAX_VALUE;
        double validMaxY = -Double.MAX_VALUE;
        boolean hasValidMicrovoxel = false;
        for (int sz = 0; sz < GranularCell.RESOLUTION; sz++) {
            for (int sx = 0; sx < GranularCell.RESOLUTION; sx++) {
                double wy = surface.getY(pos.getX() + (sx + 0.5D) / 8.0D, pos.getZ() + (sz + 0.5D) / 8.0D);
                if (!Double.isNaN(wy)) {
                    hasValidMicrovoxel = true;
                    if (wy < validMinY) validMinY = wy;
                    if (wy > validMaxY) validMaxY = wy;
                }
            }
        }
        if (!hasValidMicrovoxel) {
            return ShapeResult.NONE;
        }

        // Convert or initialize cell if not already granular
        if (cell == null || cell.isEmpty()) {
            if (state.is(GroundworksMod.GRANULAR_BLOCK)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof GranularBlockEntity gbe && gbe.getCell() != null && !gbe.getCell().isEmpty()) {
                    cell = gbe.getCell();
                    storage.putCell(pos, cell);
                } else {
                    int fillId = isAsphaltTarget
                            ? targetAsphaltMat.id()
                            : ((defaultFillMaterial != null && defaultFillMaterial.id() > 0) ? defaultFillMaterial.id() : GranularMaterialRegistry.GRAVEL.id());
                    GranularMaterial fallbackMat = GranularMaterialRegistry.byId(fillId);
                    cell = isAsphaltTarget ? GranularCell.empty() : GranularCell.full(fallbackMat != null ? fallbackMat : GranularMaterialRegistry.GRAVEL);
                    cell.setMaterialId(fillId);
                    if (be instanceof GranularBlockEntity granularBe) {
                        granularBe.setMaterialId(fillId);
                        granularBe.setCell(cell);
                        granularBe.setChanged();
                        level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                    }
                    storage.putCell(pos, cell);
                }
            } else if (isExistingAsphaltBlock) {
                // Initialize full asphalt cell for existing asphalt block
                cell = GranularCell.full(GranularMaterialRegistry.COMPACTED_ASPHALT);
                level.setBlock(pos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof GranularBlockEntity granularBe) {
                    granularBe.setMaterialId(GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                    granularBe.setCell(cell);
                    granularBe.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                }
                storage.putCell(pos, cell);
            } else if (RoadRollerEntity.isLooseRoadbedBlock(state)) {
                // Convert full vanilla loose material block (gravel, sand, dirt, cobblestone)
                if (pos.getY() + 1.0D > validMinY - 0.05D) {
                    cell = BlockConverter.convert(level, pos, state);
                    if (cell == null) {
                        int fillId = isAsphaltTarget
                                ? targetAsphaltMat.id()
                                : ((defaultFillMaterial != null && defaultFillMaterial.id() > 0) ? defaultFillMaterial.id() : GranularMaterialRegistry.GRAVEL.id());
                        GranularMaterial fallbackMat = GranularMaterialRegistry.byId(fillId);
                        cell = GranularCell.full(fallbackMat != null ? fallbackMat : GranularMaterialRegistry.GRAVEL);
                        cell.setMaterialId(fillId);
                        level.setBlock(pos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof GranularBlockEntity granularBe) {
                            granularBe.setMaterialId(fillId);
                            granularBe.setCell(cell);
                            granularBe.setChanged();
                            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }
                        storage.putCell(pos, cell);
                    }
                }
            } else if (state.isAir() || state.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(state)) {
                // Ramp step transition / air hollow at grade: initialize empty cell if within plane elevation and supported below
                boolean allowAirInit = !isVehicleRoller && (isAsphaltTarget ? (isExplicitPavingCommand || forceCompactedAsphalt) : (availableFillUnits > 0));
                if (allowAirInit && validMaxY > pos.getY() + 0.01D) {
                    BlockState belowState = level.getBlockState(pos.below());
                    boolean belowSupported = !belowState.isAir() && !RoadRollerEntity.isVegetationOrReplaceable(belowState)
                            && (!belowState.canBeReplaced() || belowState.is(GroundworksMod.GRANULAR_BLOCK) || RoadRollerEntity.isLooseRoadbedBlock(belowState));
                    if (belowSupported) {
                        // Clear vegetation block at current position if present
                        if (RoadRollerEntity.isVegetationOrReplaceable(state)) {
                            level.destroyBlock(pos, false);
                        }
                        cell = GranularCell.empty();
                        int fillId = isAsphaltTarget
                                ? targetAsphaltMat.id()
                                : ((defaultFillMaterial != null && defaultFillMaterial.id() > 0) ? defaultFillMaterial.id() : GranularMaterialRegistry.GRAVEL.id());
                        cell.setMaterialId(fillId);
                        level.setBlock(pos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof GranularBlockEntity granularBe) {
                            granularBe.setMaterialId(fillId);
                            granularBe.setCell(cell);
                            granularBe.setChanged();
                            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }
                        storage.putCell(pos, cell);
                    }
                }
            }
        }

        if (cell == null) {
            return ShapeResult.NONE;
        }

        // Material management
        boolean converted = false;
        if (isAsphaltTarget) {
            if (isFinishingAsphalt) {
                if (cell.materialId() == GranularMaterialRegistry.ASPHALT.id()
                        || cell.surfaceCapMaterialId() == GranularMaterialRegistry.ASPHALT.id()
                        || cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()) > 0) {
                    cell.convertMaterial(GranularMaterialRegistry.ASPHALT.id(), GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                    converted = true;
                }
            }
            if (cell.materialId() != targetAsphaltMat.id()) {
                cell.setMaterialId(targetAsphaltMat.id());
                converted = true;
            }
            if (cell.surfaceCapMaterialId() != targetAsphaltMat.id() && cell.surfaceCapMask() != 0L) {
                cell.setSurfaceCap(cell.surfaceCapMask(), targetAsphaltMat.id());
                converted = true;
            }
        } else if (cell.materialId() <= 0) {
            int fillId = (defaultFillMaterial != null && defaultFillMaterial.id() > 0)
                    ? defaultFillMaterial.id()
                    : GranularMaterialRegistry.GRAVEL.id();
            cell.setMaterialId(fillId);
        }

        GranularMaterial activeMat = isAsphaltTarget
                ? targetAsphaltMat
                : (cell.material() != null && cell.material().id() > 0
                        ? cell.material()
                        : (defaultFillMaterial != null ? defaultFillMaterial : GranularMaterialRegistry.GRAVEL));

        int excavatedUnits = 0;
        int depositedUnits = 0;
        long surfaceCapMask = 0L;

        // Microvoxel shaping loop across all 64 columns (8x8)
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                double wx = pos.getX() + (x + 0.5D) / 8.0D;
                double wz = pos.getZ() + (z + 0.5D) / 8.0D;
                double targetWorldY = surface.getY(wx, wz);
                if (Double.isNaN(targetWorldY)) {
                    continue;
                }
                double localMicroY = (targetWorldY - pos.getY()) * 8.0D;

                // Target microvoxel index in range [-1, 7] (when leveling asphalt with a roller, never excavate below layer 0 to protect foundation)
                int minH = (isVehicleRoller && isAsphaltTarget) ? 0 : (!isExplicitPavingCommand && isAsphaltTarget) ? 0 : -1;
                int targetH = Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), minH, 7);
                int currentH = cell.getColumnHeight(x, z);

                // 1. Excavate voxels above target plane
                if (currentH > targetH && excavatedUnits < maxExcavationUnits) {
                    for (int y = currentH; y > targetH && excavatedUnits < maxExcavationUnits; y--) {
                        if (cell.isSet(x, y, z)) {
                            // When laying asphalt (pave/finish) on a subgrade block: NEVER excavate subgrade material
                            // (cell may be granular gravel/dirt below asphalt target elevation at grade breaks)
                            if (isAsphaltTarget) {
                                boolean cellIsSubgrade = (cell.materialId() != GranularMaterialRegistry.ASPHALT.id()
                                        && cell.materialId() != GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                                if (cellIsSubgrade) {
                                    break; // Protect subgrade - never excavate gravel/dirt during paving
                                }
                                if (!isExplicitPavingCommand || isVehicleRoller) {
                                    // Roller mode: only excavate asphalt voxels
                                    boolean isAsphaltVoxel = (cell.materialId() == targetAsphaltMat.id())
                                            || (cell.surfaceCapMaterialId() == targetAsphaltMat.id() && y == currentH)
                                            || (cell.unitsOfMaterial(targetAsphaltMat.id()) > 0);
                                    if (!isAsphaltVoxel) {
                                        break;
                                    }
                                }
                            }
                            boolean cleared = cell.clearMaterial(x, y, z, activeMat.id());
                            if (!cleared && isAsphaltTarget && targetAsphaltMat != null) {
                                cleared = cell.clearMaterial(x, y, z, targetAsphaltMat.id());
                            }
                            if (!cleared && !isAsphaltTarget) {
                                cleared = cell.clear(x, y, z);
                            }
                            if (cleared) {
                                excavatedUnits++;
                            }
                        }
                    }
                }
                // 2. Deposit voxels below target plane if fill is available
                else if ((!isAsphaltTarget || isExplicitPavingCommand || forceCompactedAsphalt) && currentH < targetH && depositedUnits < availableFillUnits) {
                    for (int y = Math.max(0, currentH + 1); y <= targetH; y++) {
                        if (depositedUnits >= availableFillUnits) {
                            break;
                        }
                        if (!cell.isSet(x, y, z)) {
                            if (cell.set(x, y, z)) {
                                depositedUnits++;
                            }
                        }
                    }
                }

                if (isAsphaltTarget && cell.getColumnHeight(x, z) >= 0) {
                    surfaceCapMask |= (1L << (x | (z << 3)));
                }
            }
        }

        if (isAsphaltTarget && surfaceCapMask != 0L) {
            cell.setSurfaceCap(surfaceCapMask, targetAsphaltMat.id());
            cell.markDirty(DirtyFlags.MATERIAL | DirtyFlags.SYNC | DirtyFlags.MESH);
        }

        boolean changed = (excavatedUnits > 0 || depositedUnits > 0 || forceCompactedAsphalt || converted);
        if (cell.isEmpty()) {
            if (level.getBlockState(pos).is(GroundworksMod.GRANULAR_BLOCK)) {
                storage.removeCell(pos);
                level.removeBlock(pos, false);
                GranularSyncHandler.sendCellRemoval(level, pos);
            }
            // Preserve the excavation accounting even when the last unit was removed. Returning
            // NONE here used to make a complete 512-unit bump vanish outside the machine buffer.
            return new ShapeResult(changed, excavatedUnits, depositedUnits, activeMat);
        }

        if (changed) {
            if (!level.getBlockState(pos).is(GroundworksMod.GRANULAR_BLOCK)) {
                level.setBlock(pos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            }
            cell.clearDirtyFlag(DirtyFlags.SIMULATE);
            cell.markDirty(DirtyFlags.OCCUPANCY | DirtyFlags.MATERIAL | DirtyFlags.SYNC | DirtyFlags.MESH);
            storage.enqueueDirty(pos);
            storage.setDirty();

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GranularBlockEntity granularBe) {
                granularBe.setMaterialId(cell.materialId());
                granularBe.setCell(cell);
                granularBe.setChanged();
                level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            }

            GranularSyncHandler.sendCellUpdate(level, pos, cell);
        }

        if (stageToRecord != null && (changed || cell.getColumnHeight(3, 3) >= 0)) {
            double targetCenterY = surface.getY(pos.getX() + 0.5D, pos.getZ() + 0.5D);
            RoadbedCorridorStorage.get(level).recordStage(
                    pos, stageToRecord, targetCenterY, slopeX, slopeZ, level.getGameTime()
            );
        }

        return new ShapeResult(changed, excavatedUnits, depositedUnits, activeMat);
    }
}
