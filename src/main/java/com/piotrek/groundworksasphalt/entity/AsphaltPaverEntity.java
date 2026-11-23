package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.container.IGranularContainer;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.block.entity.GranularBlockEntity;
import com.piotrek.groundworks.terrain.cell.DirtyFlags;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
import com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor;
import com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage;
import com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Heavy Tracked Asphalt Paver (Rozściełacz Asfaltu) Entity.
 *
 * <p>Equipped with a 14-tonne capacity receiving hopper, dual hydraulic folding wings,
 * twin feeder conveyors, spreading augers, and a heavy 5-block wide floating screed.
 *
 * <p>Implements {@link IGranularContainer} so Groundworks dump trucks, loaders, and conveyors
 * can discharge hot asphalt mix directly into the paver's hopper.
 */
public class AsphaltPaverEntity extends Entity implements IGranularContainer {

    public static final int MAX_HOPPER_CAPACITY_UNITS = 16384; // ~32 cubic meters of hot mix
    public static final int UNITS_PER_BLOCK = 512;
    public static final int UNITS_PER_LAYER = 64; // 1/8 block layer = 64 units

    // ── Synched Entity Data ───────────────────────────────────────────
    private static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LEFT_TRACK_SPEED =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RIGHT_TRACK_SPEED =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HOPPER_FOLD =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCREED_HEIGHT =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ASPHALT_FILL_RATIO =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_PAVING =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WORK_LIGHTS_ON =
            SynchedEntityData.defineId(AsphaltPaverEntity.class, EntityDataSerializers.BOOLEAN);

    // Work lights placed in the world
    private final List<BlockPos> workLights = new ArrayList<>();

    // Stored asphalt mix material units
    private int storedAsphaltUnits = 8192; // starts half full

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private float inputLift = 0.0F;
    private float inputAux = 0.0F;
    private boolean inputHorn = false;
    private int hornCooldown = 0;
    private int inputFreshTicks = 0;

    public AsphaltPaverEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VEHICLE_PITCH, 0.0F);
        builder.define(VEHICLE_ROLL, 0.0F);
        builder.define(LEFT_TRACK_SPEED, 0.0F);
        builder.define(RIGHT_TRACK_SPEED, 0.0F);
        builder.define(HOPPER_FOLD, 0.0F);
        builder.define(SCREED_HEIGHT, 0.0F);
        builder.define(ASPHALT_FILL_RATIO, 0.5F);
        builder.define(IS_PAVING, false);
        builder.define(ENGINE_RUNNING, false);
        builder.define(WORK_LIGHTS_ON, true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.storedAsphaltUnits = input.getIntOr("StoredAsphaltUnits", 8192);
        this.entityData.set(HOPPER_FOLD, input.getFloatOr("HopperFold", 0.0F));
        this.entityData.set(SCREED_HEIGHT, input.getFloatOr("ScreedHeight", 0.0F));
        this.entityData.set(IS_PAVING, input.getBooleanOr("Paving", false));
        this.entityData.set(ENGINE_RUNNING, input.getBooleanOr("EngineRunning", false));
        this.entityData.set(WORK_LIGHTS_ON, input.getBooleanOr("WorkLightsOn", true));
        updateFillRatio();

        this.workLights.clear();
        int lightCount = input.getIntOr("WorkLightCount", 0);
        for (int i = 0; i < lightCount && i < 32; i++) {
            this.workLights.add(new BlockPos(
                    input.getIntOr("WorkLightX" + i, 0),
                    input.getIntOr("WorkLightY" + i, 0),
                    input.getIntOr("WorkLightZ" + i, 0)
            ));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("StoredAsphaltUnits", this.storedAsphaltUnits);
        output.putFloat("HopperFold", this.entityData.get(HOPPER_FOLD));
        output.putFloat("ScreedHeight", this.entityData.get(SCREED_HEIGHT));
        output.putBoolean("Paving", this.entityData.get(IS_PAVING));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
        output.putBoolean("WorkLightsOn", this.entityData.get(WORK_LIGHTS_ON));

        output.putInt("WorkLightCount", this.workLights.size());
        for (int i = 0; i < this.workLights.size(); i++) {
            BlockPos pos = this.workLights.get(i);
            output.putInt("WorkLightX" + i, pos.getX());
            output.putInt("WorkLightY" + i, pos.getY());
            output.putInt("WorkLightZ" + i, pos.getZ());
        }
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2) {
        setControlInputs(throttle, steer, lift, aux, action1, action2, false);
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2, boolean horn) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputLift = Mth.clamp(lift, -1.0F, 1.0F);
        this.inputAux = Mth.clamp(aux, -1.0F, 1.0F);
        if (action1) {
            setPaving(!isPaving());
        }
        if (action2) {
            setWorkLightsOn(!isWorkLightsOn());
        }
        this.inputHorn = horn;
        this.inputFreshTicks = 10;
    }

    private void updateFillRatio() {
        float ratio = (float) this.storedAsphaltUnits / (float) MAX_HOPPER_CAPACITY_UNITS;
        this.entityData.set(ASPHALT_FILL_RATIO, Mth.clamp(ratio, 0.0F, 1.0F));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            LivingEntity driver = this.getControllingPassenger();
            boolean hasDriver = driver != null;

            if (this.inputHorn && this.hornCooldown <= 0) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        GroundworksAsphaltMod.VEHICLE_HORN, SoundSource.PLAYERS, 1.4F, 0.85F);
                this.hornCooldown = 12;
            }
            if (this.hornCooldown > 0) {
                this.hornCooldown--;
            }

            if (driver instanceof ServerPlayer player) {
                var vanilla = player.getLastClientInput();
                if (inputFreshTicks > 0) {
                    inputFreshTicks--;
                } else {
                    inputThrottle = vanilla.forward() ? 1.0F : vanilla.backward() ? -1.0F : 0.0F;
                    inputSteer = vanilla.left() ? -1.0F : vanilla.right() ? 1.0F : 0.0F;
                }
                if (Math.abs(inputThrottle) < 0.01F) {
                    if (vanilla.forward()) inputThrottle = 1.0F;
                    else if (vanilla.backward()) inputThrottle = -1.0F;
                }
                if (Math.abs(inputSteer) < 0.01F) {
                    if (vanilla.left()) inputSteer = -1.0F;
                    else if (vanilla.right()) inputSteer = 1.0F;
                }
            } else {
                inputThrottle = 0.0F;
                inputSteer = 0.0F;
                inputFreshTicks = 0;
            }

            this.entityData.set(ENGINE_RUNNING, hasDriver);

            // Hydraulic screed lift and hopper fold adjustment
            if (Math.abs(inputLift) > 0.01F) {
                float currentScreed = this.entityData.get(SCREED_HEIGHT);
                float newScreed = Mth.clamp(currentScreed + inputLift * 0.04F, 0.0F, 1.0F);
                this.entityData.set(SCREED_HEIGHT, newScreed);
            }

            if (Math.abs(inputAux) > 0.01F) {
                float currentFold = this.entityData.get(HOPPER_FOLD);
                float newFold = Mth.clamp(currentFold + inputAux * 0.04F, 0.0F, 1.0F);
                this.entityData.set(HOPPER_FOLD, newFold);
            }

            // Differential crawler track steering
            float forwardDrive = inputThrottle * 0.09F;
            float turnDrive = inputSteer * 2.0F;

            if (Math.abs(inputSteer) > 0.01F) {
                this.setYRot(Mth.wrapDegrees(this.getYRot() + turnDrive));
                this.setYHeadRot(this.getYRot());
                this.setYBodyRot(this.getYRot());
            }

            double yawRad = Math.toRadians(this.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

            // Track contact sampling (chassis center ~0, tracks ±1.4m ahead/behind, ±1.1m gauge)
            Vec3 frontCenter = this.position().add(forward.scale(1.4D));
            Vec3 rearCenter = this.position().subtract(forward.scale(1.4D));
            double maxAllowedY = this.getY() + 2.0D;
            double flY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.subtract(side.scale(1.1D)), maxAllowedY);
            double frY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.add(side.scale(1.1D)), maxAllowedY);
            double rlY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.subtract(side.scale(1.1D)), maxAllowedY);
            double rrY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.add(side.scale(1.1D)), maxAllowedY);

            double frontAvg = (flY + frY) * 0.5D;
            double rearAvg = (rlY + rrY) * 0.5D;
            double leftAvg = (flY + rlY) * 0.5D;
            double rightAvg = (frY + rrY) * 0.5D;
            double targetGroundY = (frontAvg + rearAvg) * 0.5D;

            double targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, 2.8D));
            double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 2.2D));
            targetPitch = Mth.clamp(targetPitch, -30.0D, 30.0D);
            targetRoll = Mth.clamp(targetRoll, -25.0D, 25.0D);

            float curPitch = this.entityData.get(VEHICLE_PITCH);
            float curRoll = this.entityData.get(VEHICLE_ROLL);
            this.entityData.set(VEHICLE_PITCH, (float) Mth.lerp(0.2D, curPitch, targetPitch));
            this.entityData.set(VEHICLE_ROLL, (float) Mth.lerp(0.2D, curRoll, targetRoll));

            double dy = RoadRollerEntity.calculateSmoothVerticalStep(targetGroundY, this.getY(), forwardDrive, this.onGround());
            Vec3 motion = new Vec3(forward.x * forwardDrive, dy, forward.z * forwardDrive);

            Vec3 beforeMove = this.position();
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);

            Vec3 moved = this.position().subtract(beforeMove);
            float fwdSpeed = (float) Math.sqrt(moved.x * moved.x + moved.z * moved.z);

            float baseTrackSpeed = fwdSpeed > 0.001F ? fwdSpeed : Math.abs(forwardDrive);
            float leftTrack = (inputThrottle < 0 ? -baseTrackSpeed : baseTrackSpeed) + (inputSteer > 0 ? 0.04F : 0.0F);
            float rightTrack = (inputThrottle < 0 ? -baseTrackSpeed : baseTrackSpeed) + (inputSteer < 0 ? 0.04F : 0.0F);
            this.entityData.set(LEFT_TRACK_SPEED, leftTrack);
            this.entityData.set(RIGHT_TRACK_SPEED, rightTrack);

            if (hasDriver || fwdSpeed > 0.001F || Math.abs(inputThrottle) > 0.01F || Math.abs(inputSteer) > 0.01F) {
                this.syncPosition = true;
                this.needsSync = true;
                this.syncVelocity = true;
            }

            // Automatic paving behind screed
            boolean shouldPave = (isPaving() || (hasDriver && inputThrottle > 0.0F))
                    && getScreedHeight() < 0.25F
                    && this.storedAsphaltUnits >= UNITS_PER_LAYER;

            if (shouldPave) {
                paveAsphaltCourse(serverLevel, forward);
            }

            // Update worklights
            if (this.isEngineRunning() && this.isWorkLightsOn()) {
                // Front hopper floodlights
                List<BlockPos> beams = AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, 2.8D, 2.2D, 1.5D, new int[]{2, 5, 9}, false
                );
                // Rear screed floodlights illuminating the freshly paved mat
                beams.addAll(AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, -3.2D, 2.8D, 1.2D, new int[]{2, 4, 7}, true
                ));
                AsphaltVehicleLights.syncLightBlocks(serverLevel, this.workLights, beams, AsphaltVehicleLights.HEAD_LIGHT_LEVEL);
            } else if (!this.workLights.isEmpty()) {
                AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
            }
        }
    }

    /**
     * Lays a 5-block wide course behind the floating screed, adhering directly to subgrade microvoxels.
     */
    private void paveAsphaltCourse(ServerLevel level, Vec3 forward) {
        // Screed trailing location: ~2.4m behind machine pivot
        Vec3 screedCenter = this.position().subtract(forward.scale(2.4D));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        boolean placedAny = false;

        // 5-block wide screed (-2, -1, 0, 1, 2)
        double[] laneOffsets = {-2.0D, -1.0D, 0.0D, 1.0D, 2.0D};

        for (double off : laneOffsets) {
            if (this.storedAsphaltUnits < UNITS_PER_LAYER) {
                break;
            }

            Vec3 pt = screedCenter.add(side.scale(off));
            int bx = Mth.floor(pt.x);
            int bz = Mth.floor(pt.z);

            RoadSurveyCorridor.SurveyTarget survey = RoadSurveyCorridor.sampleTarget(level, pt.x, pt.z);
            RoadbedCorridorStorage corridor = RoadbedCorridorStorage.get(level);
            BlockPos pavedPos = null;

            if (survey.inCorridor()) {
                // Paving inside survey corridor: shape continuous 3D microvoxel asphalt mat adhering to subgrade
                double targetPavedY = survey.targetY() + 0.125D;
                int targetBlockY = Mth.floor(targetPavedY - 0.0625D);
                BlockPos targetCellPos = new BlockPos(bx, targetBlockY, bz);

                // If target block already has asphalt at proper level, skip to avoid wasting asphalt
                if (AsphaltRollerEntity.hasAsphaltInCell(level, targetCellPos)
                        || level.getBlockState(targetCellPos).getBlock() instanceof FreshAsphaltBlock
                        || level.getBlockState(targetCellPos).getBlock() instanceof CompactedAsphaltBlock) {
                    continue;
                }

                // 1. Clear overhead clearance strictly above targetBlockY (up to 3 blocks)
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos clearPos = targetCellPos.above(dy);
                    BlockState clearState = level.getBlockState(clearPos);
                    if (!clearState.isAir() && (clearState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(clearState))) {
                        level.destroyBlock(clearPos, false);
                    }
                }

                // 2. Ensure solid support foundation below targetBlockY
                BlockPos supportPos = targetCellPos.below();
                BlockState supportState = level.getBlockState(supportPos);
                if (supportState.isAir() || supportState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(supportState)) {
                    level.setBlock(supportPos, Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                }

                // 3. Shape targetCellPos to the engineered paved surface
                RoadPlanarShaper.shapeCellToSurface(
                        level,
                        targetCellPos,
                        (wx, wz) -> {
                            var s = RoadSurveyCorridor.sampleTarget(level, wx, wz);
                            return s.inCorridor() ? s.targetY() + 0.125D : targetPavedY;
                        },
                        GranularMaterialRegistry.ASPHALT,
                        UNITS_PER_LAYER,
                        false,
                        UNITS_PER_LAYER,
                        RoadbedCorridorStorage.RoadbedStage.PAVED,
                        survey.slopeX(),
                        survey.slopeZ()
                );

                pavedPos = targetCellPos;
            } else {
                // Manual free-form paving outside corridor:
                // Find highest solid/granular foundation beneath this screed point
                int hintY = Mth.floor(this.getY());
                BlockPos screedGround = null;

                for (int dy = 1; dy >= -3; dy--) {
                    BlockPos checkPos = new BlockPos(bx, hintY + dy, bz);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (AsphaltRollerEntity.hasAsphaltInCell(level, checkPos)
                            || checkState.getBlock() instanceof FreshAsphaltBlock
                            || checkState.getBlock() instanceof CompactedAsphaltBlock) {
                        screedGround = null; // Already paved
                        break;
                    }

                    GranularCell cell = GroundworksApi.queryCell(level, checkPos);
                    if (cell != null && !cell.isEmpty()) {
                        screedGround = checkPos;
                        break;
                    }

                    if (!checkState.isAir() && !checkState.canBeReplaced() && !RoadRollerEntity.isVegetationOrReplaceable(checkState)) {
                        screedGround = checkPos;
                        break;
                    }
                }

                if (screedGround != null) {
                    GranularCell cell = GroundworksApi.queryCell(level, screedGround);
                    if (cell != null && !cell.isEmpty() && !cell.isFull()) {
                        // Cap the existing granular subgrade directly
                        int added = GroundworksApi.laySurfaceCap(level, screedGround, GranularMaterialRegistry.ASPHALT, 1);
                        if (added > 0) {
                            corridor.recordStage(
                                    screedGround,
                                    RoadbedCorridorStorage.RoadbedStage.PAVED,
                                    screedGround.getY() + 0.5D,
                                    0.0D, 0.0D,
                                    level.getGameTime()
                            );
                            pavedPos = screedGround;
                        }
                    } else {
                        // Place conforming microvoxel asphalt mat in the block resting directly on screedGround
                        BlockPos targetPos = screedGround.above();
                        BlockState targetState = level.getBlockState(targetPos);
                        if (targetState.isAir() || targetState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(targetState)) {
                            if (RoadRollerEntity.isVegetationOrReplaceable(targetState)) {
                                level.destroyBlock(targetPos, false);
                            }
                            GranularWorldStorage storage = GranularWorldStorage.get(level);
                            GranularCell above = GranularCell.empty();
                            above.setMaterialId(GranularMaterialRegistry.ASPHALT.id());
                            for (int cx = 0; cx < GranularCell.RESOLUTION; cx++) {
                                for (int cz = 0; cz < GranularCell.RESOLUTION; cz++) {
                                    above.seedSurfaceCap(cx, cz, GranularMaterialRegistry.ASPHALT.id());
                                }
                            }
                            storage.putCell(targetPos, above);
                            level.setBlock(targetPos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            above.markDirty(DirtyFlags.OCCUPANCY | DirtyFlags.MATERIAL | DirtyFlags.SYNC | DirtyFlags.MESH);
                            storage.enqueueDirty(targetPos);
                            storage.setDirty();

                            BlockEntity be = level.getBlockEntity(targetPos);
                            if (be instanceof GranularBlockEntity gbe) {
                                gbe.setMaterialId(above.materialId());
                                gbe.setCell(above);
                                gbe.setChanged();
                                level.sendBlockUpdated(targetPos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            }

                            corridor.recordStage(
                                    targetPos,
                                    RoadbedCorridorStorage.RoadbedStage.PAVED,
                                    targetPos.getY() + 0.125D,
                                    0.0D, 0.0D,
                                    level.getGameTime()
                            );
                            pavedPos = targetPos;
                        }
                    }
                }
            }

            if (pavedPos != null && this.storedAsphaltUnits >= UNITS_PER_LAYER) {
                this.storedAsphaltUnits -= UNITS_PER_LAYER;
                updateFillRatio();
                placedAny = true;

                // Hot asphalt steam & smoke emission at screed
                level.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pt.x, pavedPos.getY() + 0.15D, pt.z,
                        2,
                        0.25D, 0.05D, 0.25D,
                        0.01D
                );
            }
        }

        if (placedAny && this.tickCount % 10 == 0) {
            level.playSound(
                    null, screedCenter.x, this.getY(), screedCenter.z,
                    GroundworksAsphaltMod.PAVER_SCREED, SoundSource.BLOCKS,
                    0.8F, 1.0F
            );
        }
    }

    public Item getDropItem() {
        return GroundworksAsphaltMod.ASPHALT_PAVER_ITEM;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);

        // Player loading asphalt mix by hand into receiving hopper
        if (held.is(GroundworksAsphaltMod.ASPHALT_MIX)) {
            if (this.storedAsphaltUnits + UNITS_PER_BLOCK <= MAX_HOPPER_CAPACITY_UNITS) {
                if (!this.level().isClientSide()) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    this.storedAsphaltUnits = Math.min(MAX_HOPPER_CAPACITY_UNITS, this.storedAsphaltUnits + UNITS_PER_BLOCK);
                    updateFillRatio();
                    this.level().playSound(
                            null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F
                    );
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }

        // Shift + empty hand pickup
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (!this.level().isClientSide()) {
                this.ejectPassengers();
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(new ItemStack(getDropItem()));
                }
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F
                );
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide()) {
            if (this.getPassengers().isEmpty()) {
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && this.getPassengers().isEmpty();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), 1.0F));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // Left swivel operator seat on platform: lateral offset +0.40625m (6.5 px to driver's left, in front of ErgoPlus desk),
        // attachment height 1.45m (player butt sits firmly on 1.63m cushion, well clear of canopy roof),
        // longitudinal position -0.375m (-6 px in model), oriented dynamically with paver 3D pitch and roll
        return computeSeatOffset(0.40625D, 1.45D, -0.375D, getVehiclePitch(), getVehicleRoll());
    }

    private Vec3 computeSeatOffset(double lx, double ly, double lz, float pitchDeg, float rollDeg) {
        double radPitch = Math.toRadians(pitchDeg);
        double radRoll = Math.toRadians(rollDeg);

        // 1. Pitch rotation around lateral axis X
        double y1 = ly * Math.cos(radPitch) - lz * Math.sin(radPitch);
        double z1 = ly * Math.sin(radPitch) + lz * Math.cos(radPitch);
        double x1 = lx;

        // 2. Roll rotation around longitudinal axis Z
        double x2 = x1 * Math.cos(radRoll) - y1 * Math.sin(radRoll);
        double y2 = x1 * Math.sin(radRoll) + y1 * Math.cos(radRoll);
        double z2 = z1;

        // 3. World yaw rotation
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        return forward.scale(z2).add(right.scale(x2)).add(up.scale(y2));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad)).scale(-1.0D);
        // Dismount at the base of the left platform access ladder
        return this.position().add(left.scale(1.8D)).add(forward.scale(-0.75D)).add(0.0D, 0.25D, 0.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, getDropItem());
            }
            this.ejectPassengers();
            this.discard();
            return true;
        }
        this.ejectPassengers();
        this.discard();
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return other != null && !this.hasPassenger(other);
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 0.85F;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ── IGranularContainer Implementation ─────────────────────────────

    @Override
    public int capacity() {
        return MAX_HOPPER_CAPACITY_UNITS;
    }

    @Override
    public int storedUnits() {
        return this.storedAsphaltUnits;
    }

    @Override
    public GranularMaterial storedMaterial() {
        return GranularMaterialRegistry.GRAVEL != null ? GranularMaterialRegistry.GRAVEL : GranularMaterial.EMPTY;
    }

    @Override
    public int acceptMaterial(GranularMaterial material, int units) {
        if (material == null || units <= 0) return 0;
        int space = Math.max(0, capacity() - this.storedAsphaltUnits);
        int accepted = Math.min(units, space);
        if (accepted > 0) {
            this.storedAsphaltUnits += accepted;
            updateFillRatio();
        }
        return accepted;
    }

    @Override
    public int extractMaterial(int maxUnits) {
        // Paver discharges forward via screed only, does not dispense back out
        return 0;
    }

    // ── Getters for Renderers & Client State ──────────────────────────

    public float getVehiclePitch() {
        return this.entityData.get(VEHICLE_PITCH);
    }

    public void setVehiclePitch(float vehiclePitch) {
        this.entityData.set(VEHICLE_PITCH, vehiclePitch);
    }

    public float getVehicleRoll() {
        return this.entityData.get(VEHICLE_ROLL);
    }

    public void setVehicleRoll(float vehicleRoll) {
        this.entityData.set(VEHICLE_ROLL, vehicleRoll);
    }

    public float getTrackLeftSpeed() {
        return this.entityData.get(LEFT_TRACK_SPEED);
    }

    public float getTrackRightSpeed() {
        return this.entityData.get(RIGHT_TRACK_SPEED);
    }

    public float getHopperFold() {
        return this.entityData.get(HOPPER_FOLD);
    }

    public void setHopperFold(float hopperFold) {
        this.entityData.set(HOPPER_FOLD, hopperFold);
    }

    public float getScreedHeight() {
        return this.entityData.get(SCREED_HEIGHT);
    }

    public void setScreedHeight(float screedHeight) {
        this.entityData.set(SCREED_HEIGHT, screedHeight);
    }

    public float getAsphaltFillRatio() {
        return this.entityData.get(ASPHALT_FILL_RATIO);
    }

    public void setAsphaltFillRatio(float asphaltFillRatio) {
        this.entityData.set(ASPHALT_FILL_RATIO, asphaltFillRatio);
    }

    public boolean isPaving() {
        return this.entityData.get(IS_PAVING);
    }

    public void setPaving(boolean paving) {
        this.entityData.set(IS_PAVING, paving);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public void setEngineRunning(boolean engineRunning) {
        this.entityData.set(ENGINE_RUNNING, engineRunning);
    }

    public boolean isWorkLightsOn() {
        return this.entityData.get(WORK_LIGHTS_ON);
    }

    public void setWorkLightsOn(boolean workLightsOn) {
        this.entityData.set(WORK_LIGHTS_ON, workLightsOn);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
        }
        super.remove(reason);
    }
}
