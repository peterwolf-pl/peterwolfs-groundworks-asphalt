package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.AsphaltSlabBlock;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.block.RoadLineBlock;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity.RoadSurface3D;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
import com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Heavy 14-Ton Tandem Vibratory Asphalt Roller (Bomag BW 184 AD / Hamm style).
 * <p>
 * Equipped with dual cylindrical steel drums (front and rear) and water sprinkler systems.
 * Exclusively designed for compacting, finishing, and smoothing existing asphalt pavement
 * (fresh asphalt -> compacted asphalt, continuous planar 3D transitions).
 * <p>
 * In accordance with strict operational rules:
 * - This roller ONLY smooths and compacts existing asphalt.
 * - It CANNOT build or lay new asphalt out of thin air, nor widen roads into dirt/air.
 */
public class AsphaltRollerEntity extends Entity {

    public static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> FRONT_DRUM_ROTATION =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> REAR_DRUM_ROTATION =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> IS_COMPACTING =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_VIBRATING =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_BRAKING =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> WATER_SPRAYING =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> LIGHTS_ON =
            SynchedEntityData.defineId(AsphaltRollerEntity.class, EntityDataSerializers.BOOLEAN);

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private boolean inputHorn = false;
    private int hornCooldown = 0;
    private int inputFreshTicks = 0;

    private float frontDrumRot = 0.0F;
    private float rearDrumRot = 0.0F;
    private boolean wasDriver = false;

    // Work lights placed in the world
    private final List<BlockPos> workLights = new ArrayList<>();

    public AsphaltRollerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STEER_ANGLE, 0.0F);
        builder.define(FORWARD_SPEED, 0.0F);
        builder.define(FRONT_DRUM_ROTATION, 0.0F);
        builder.define(REAR_DRUM_ROTATION, 0.0F);
        builder.define(VEHICLE_PITCH, 0.0F);
        builder.define(VEHICLE_ROLL, 0.0F);
        builder.define(IS_COMPACTING, true);
        builder.define(IS_VIBRATING, true);
        builder.define(ENGINE_RUNNING, false);
        builder.define(IS_BRAKING, false);
        builder.define(WATER_SPRAYING, true);
        builder.define(LIGHTS_ON, true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(STEER_ANGLE, input.getFloatOr("SteerAngle", 0.0F));
        this.entityData.set(IS_COMPACTING, input.getBooleanOr("Compacting", true));
        this.entityData.set(IS_VIBRATING, input.getBooleanOr("Vibrating", true));
        this.entityData.set(ENGINE_RUNNING, input.getBooleanOr("EngineRunning", false));
        this.entityData.set(WATER_SPRAYING, input.getBooleanOr("WaterSpraying", true));
        this.entityData.set(LIGHTS_ON, input.getBooleanOr("LightsOn", true));

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
        output.putFloat("SteerAngle", this.entityData.get(STEER_ANGLE));
        output.putBoolean("Compacting", this.entityData.get(IS_COMPACTING));
        output.putBoolean("Vibrating", this.entityData.get(IS_VIBRATING));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
        output.putBoolean("WaterSpraying", this.entityData.get(WATER_SPRAYING));
        output.putBoolean("LightsOn", this.entityData.get(LIGHTS_ON));

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

    public void setControlInputs(
            float throttle, float steer, float lift, float aux, boolean action1, boolean action2, boolean horn
    ) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputHorn = horn;
        this.inputFreshTicks = 5;

        if (action1) {
            boolean current = this.entityData.get(IS_COMPACTING);
            this.entityData.set(IS_COMPACTING, !current);
            this.entityData.set(IS_VIBRATING, !current);
            if (!this.level().isClientSide()) {
                float pitch = !current ? 1.1F : 0.8F;
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
                        0.6F, pitch
                );
            }
        }

        if (action2) {
            boolean spraying = this.entityData.get(WATER_SPRAYING);
            this.entityData.set(WATER_SPRAYING, !spraying);
            if (!this.level().isClientSide()) {
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS,
                        0.5F, spraying ? 0.8F : 1.2F
                );
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.inputFreshTicks > 0) {
            this.inputFreshTicks--;
        } else {
            this.inputThrottle = 0.0F;
            this.inputSteer = 0.0F;
            this.inputHorn = false;
        }

        if (this.hornCooldown > 0) {
            this.hornCooldown--;
        }

        boolean hasDriver = this.getFirstPassenger() instanceof LivingEntity;
        this.entityData.set(ENGINE_RUNNING, hasDriver);
        if (hasDriver) {
            if (!this.wasDriver) {
                this.entityData.set(IS_COMPACTING, true);
                this.entityData.set(IS_VIBRATING, true);
            }
        } else {
            this.entityData.set(IS_COMPACTING, false);
            this.entityData.set(IS_VIBRATING, false);
        }
        this.wasDriver = hasDriver;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.inputHorn && this.hornCooldown <= 0) {
                this.hornCooldown = 20;
                serverLevel.playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        GroundworksAsphaltMod.VEHICLE_HORN, SoundSource.NEUTRAL,
                        1.5F, 1.0F
                );
            }

            // Articulated steering kinematics
            float currentSteer = this.entityData.get(STEER_ANGLE);
            float targetSteer = this.inputSteer * 0.60F; // ~34.4 degrees max articulation
            float newSteer = Mth.rotLerp(0.20F, currentSteer, targetSteer);
            this.entityData.set(STEER_ANGLE, newSteer);

            if (Math.abs(inputThrottle) > 0.01F) {
                float yawChange = newSteer * inputThrottle * 2.8F;
                this.setYRot(Mth.wrapDegrees(this.getYRot() + yawChange));
                this.setYHeadRot(this.getYRot());
                this.setYBodyRot(this.getYRot());
            }

            double yawRad = Math.toRadians(this.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

            // Compute local asphalt surface plane
            RoadSurface3D asphaltSurface = computeLocalAsphaltSurface(serverLevel, forward, side);

            // Physical ground contact sampling under front drum and rear drum
            Vec3 frontCenter = this.position().add(forward.scale(1.6D));
            Vec3 rearCenter = this.position().subtract(forward.scale(1.6D));

            double maxAllowedY = this.getY() + 2.0D;
            double flY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.subtract(side.scale(0.9D)), maxAllowedY);
            double frY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.add(side.scale(0.9D)), maxAllowedY);
            double rlY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.subtract(side.scale(0.9D)), maxAllowedY);
            double rrY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.add(side.scale(0.9D)), maxAllowedY);

            double actualPhysicalGroundY = RoadRollerEntity.robustSupportHeight(flY, frY, rlY, rrY);
            if (!Double.isFinite(actualPhysicalGroundY)) {
                actualPhysicalGroundY = this.getY();
            }

            flY = RoadRollerEntity.clampSupportOutlier(flY, actualPhysicalGroundY);
            frY = RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
            rlY = RoadRollerEntity.clampSupportOutlier(rlY, actualPhysicalGroundY);
            rrY = RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

            // Edge-isolation: if machine drives near the edge and one side hangs over an embankment
            // or presses into a trench wall, isolate that outlier so it doesn't tilt chassis into the ravine.
            boolean flOff = Math.abs(flY - actualPhysicalGroundY) > 0.35D;
            boolean frOff = Math.abs(frY - actualPhysicalGroundY) > 0.35D;
            boolean rlOff = Math.abs(rlY - actualPhysicalGroundY) > 0.35D;
            boolean rrOff = Math.abs(rrY - actualPhysicalGroundY) > 0.35D;
            double effFlY = (flOff && !frOff) ? frY : flY;
            double effFrY = (frOff && !flOff) ? flY : frY;
            double effRlY = (rlOff && !rrOff) ? rrY : rlY;
            double effRrY = (rrOff && !rlOff) ? rlY : rrY;

            double frontAvg = (effFlY + effFrY) * 0.5D;
            double rearAvg = (effRlY + effRrY) * 0.5D;
            double leftAvg = (effFlY + effRlY) * 0.5D;
            double rightAvg = (effFrY + effRrY) * 0.5D;

            double targetPitch;
            double targetRoll;
            double targetGroundSupportY;

            if (asphaltSurface.isValid()) {
                double forwardSlope = asphaltSurface.slopeX() * forward.x + asphaltSurface.slopeZ() * forward.z;
                double sideSlope = asphaltSurface.slopeX() * side.x + asphaltSurface.slopeZ() * side.z;
                targetPitch = Math.toDegrees(Math.atan(-forwardSlope));
                targetRoll = Math.toDegrees(Math.atan(-sideSlope));
                targetPitch = Mth.clamp(targetPitch, -30.0D, 30.0D);
                targetRoll = Mth.clamp(targetRoll, -12.0D, 12.0D);
                double surfaceSupportY = asphaltSurface.getY(this.getX(), this.getZ());
                targetGroundSupportY = Mth.clamp(surfaceSupportY, actualPhysicalGroundY - 0.08D, actualPhysicalGroundY + 0.35D);
            } else {
                targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, 3.2D));
                targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 1.8D));
                targetPitch = Mth.clamp(targetPitch, -45.0D, 45.0D);
                targetRoll = Mth.clamp(targetRoll, -20.0D, 20.0D);
                targetGroundSupportY = actualPhysicalGroundY;
            }

            float currentPitch = this.entityData.get(VEHICLE_PITCH);
            float currentRoll = this.entityData.get(VEHICLE_ROLL);
            float newPitch = (float) Mth.lerp(0.2D, currentPitch, targetPitch);
            float newRoll = (float) Mth.lerp(0.2D, currentRoll, targetRoll);
            this.entityData.set(VEHICLE_PITCH, newPitch);
            this.entityData.set(VEHICLE_ROLL, newRoll);

            float targetSpeed = inputThrottle * 0.11F;
            double dy = RoadRollerEntity.calculateSmoothVerticalStep(targetGroundSupportY, this.getY(), targetSpeed, this.onGround());
            Vec3 motion = new Vec3(forward.x * targetSpeed, dy, forward.z * targetSpeed);

            Vec3 beforeMove = this.position();
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);

            Vec3 moved = this.position().subtract(beforeMove);
            float fwdSpeed = (float) Math.sqrt(moved.x * moved.x + moved.z * moved.z);
            this.entityData.set(FORWARD_SPEED, fwdSpeed);

            if (fwdSpeed > 0.001F || Math.abs(targetSpeed) > 0.001F) {
                float rollSpeed = fwdSpeed > 0.001F ? fwdSpeed : Math.abs(targetSpeed);
                float rotDelta = rollSpeed * 1.5F * (inputThrottle < 0 ? -1.0F : 1.0F);
                this.frontDrumRot += rotDelta;
                this.rearDrumRot += rotDelta;
                this.entityData.set(FRONT_DRUM_ROTATION, this.frontDrumRot);
                this.entityData.set(REAR_DRUM_ROTATION, this.rearDrumRot);
            }

            if (hasDriver || fwdSpeed > 0.001F || Math.abs(inputThrottle) > 0.01F || Math.abs(inputSteer) > 0.01F) {
                this.syncPosition = true;
                this.needsSync = true;
                this.syncVelocity = true;
            }

            // Dual drum asphalt compaction and planar smoothing
            if (isCompacting() && (hasDriver || fwdSpeed > 0.005F)) {
                compactUnderDualDrums(serverLevel, forward, newPitch, newRoll, asphaltSurface, fwdSpeed);
            }

            // Update worklights
            if (this.isEngineRunning() && this.isLightsOn()) {
                // Front drum lights
                List<BlockPos> beams = AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, 1.8D, 1.7D, 1.4D, new int[]{2, 4, 7, 10}, false
                );
                // Rear drum lights
                beams.addAll(AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, -1.8D, 1.7D, 1.4D, new int[]{2, 4, 7}, true
                ));
                AsphaltVehicleLights.syncLightBlocks(serverLevel, this.workLights, beams, AsphaltVehicleLights.HEAD_LIGHT_LEVEL);
            } else if (!this.workLights.isEmpty()) {
                AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
            }
        }
    }

    private void compactUnderDualDrums(
            ServerLevel level, Vec3 forward, float pitchDeg, float rollDeg, RoadSurface3D surface, float fwdSpeed
    ) {
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 frontCenter = this.position().add(forward.scale(1.6D));
        Vec3 rearCenter = this.position().subtract(forward.scale(1.6D));

        // Front drum pass
        compactDrumAtAxle(level, frontCenter, side, pitchDeg, rollDeg, surface, true);
        // Rear drum pass (double compaction on every run)
        compactDrumAtAxle(level, rearCenter, side, pitchDeg, rollDeg, surface, false);
    }

    private void compactDrumAtAxle(
            ServerLevel level, Vec3 axleCenter, Vec3 side, float pitchDeg, float rollDeg,
            RoadSurface3D surface, boolean isFront
    ) {
        double rollTangent = Math.tan(Math.toRadians(rollDeg));
        double pitchSine = -Math.sin(Math.toRadians(pitchDeg));
        double axleBaseY = this.getY() + pitchSine * (isFront ? 1.6D : -1.6D);

        final RoadPlanarShaper.PlanarSurfaceFunction rawSurfaceFunc;
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget axleSurvey =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, axleCenter.x, axleCenter.z);
        if (axleSurvey.inCorridor()) {
            rawSurfaceFunc = (wx, wz) -> {
                var s = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, wx, wz);
                return s.inCorridor() ? s.targetY() + 0.10D : (axleSurvey.targetY() + 0.10D);
            };
        } else if (surface != null && surface.isValid()) {
            rawSurfaceFunc = surface::getY;
        } else {
            rawSurfaceFunc = (wx, wz) -> {
                double offU = (wx - axleCenter.x) * side.x + (wz - axleCenter.z) * side.z;
                return axleBaseY + offU * rollTangent;
            };
        }

        // Physical drum floor clamp: the steel drum is rigidly mounted to the axle and can NEVER cut
        // deeper than 0.08m below the physical axle baseline, preventing sinkholes or tunneling.
        final double physicalDrumFloorY = axleBaseY - 0.50D;
        RoadPlanarShaper.PlanarSurfaceFunction surfaceFunc = (wx, wz) -> {
            double raw = rawSurfaceFunc.getY(wx, wz);
            return Math.max(raw, physicalDrumFloorY);
        };

        // 11 points across drum width from -1.0m to +1.0m (every 0.2m)
        double[] drumOffsets = {-1.0D, -0.8D, -0.6D, -0.4D, -0.2D, 0.0D, 0.2D, 0.4D, 0.6D, 0.8D, 1.0D};
        Set<Long> targetColumns = new java.util.LinkedHashSet<>();

        for (double off : drumOffsets) {
            Vec3 checkPt = axleCenter.add(side.scale(off));
            int bx = Mth.floor(checkPt.x);
            int bz = Mth.floor(checkPt.z);
            targetColumns.add((((long) bx) << 32) | (bz & 0xFFFFFFFFL));
        }

        boolean asphaltSmoothed = false;
        BlockPos effectPos = null;

        for (long colKey : targetColumns) {
            int bx = (int) (colKey >> 32);
            int bz = (int) colKey;

            double targetY = surfaceFunc.getY(bx + 0.5D, bz + 0.5D);
            int baseBlockY = Mth.floor(targetY - 0.0625D);

            double minAsphaltY = surfaceFunc.getY(bx + 0.125D, bz + 0.125D);
            double maxAsphaltY = minAsphaltY;
            for (double ox : new double[]{0.125D, 0.875D}) {
                for (double oz : new double[]{0.125D, 0.875D}) {
                    double ay = surfaceFunc.getY(bx + ox, bz + oz);
                    if (ay < minAsphaltY) minAsphaltY = ay;
                    if (ay > maxAsphaltY) maxAsphaltY = ay;
                }
            }
            int minAsphaltBlockY = Mth.floor(minAsphaltY - 0.0625D);
            int maxAsphaltBlockY = Mth.floor(maxAsphaltY - 0.0625D);

            int minY = minAsphaltBlockY - 1;
            int maxY = maxAsphaltBlockY + 2;

            // Check if ANY block in this column under the drum has asphalt
            boolean columnHasAsphalt = false;
            for (int by = minY; by <= maxY; by++) {
                BlockPos checkPos = new BlockPos(bx, by, bz);
                if (!level.isInWorldBounds(checkPos)) continue;
                BlockState checkState = level.getBlockState(checkPos);
                if (checkState.getBlock() instanceof FreshAsphaltBlock
                        || isAsphaltBlock(checkState)
                        || hasAsphaltInCell(level, checkPos)) {
                    columnHasAsphalt = true;
                    break;
                }
            }

            if (!columnHasAsphalt) {
                continue;
            }

            for (int by = maxY; by >= minY; by--) {
                BlockPos targetPos = new BlockPos(bx, by, bz);
                if (!level.isInWorldBounds(targetPos)) continue;
                BlockState state = level.getBlockState(targetPos);

                // Convert vanilla fresh asphalt block directly into compacted asphalt
                if (state.getBlock() instanceof FreshAsphaltBlock) {
                    if (axleSurvey.inCorridor()) {
                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);
                        double slopeX = ptSurvey.inCorridor() ? ptSurvey.slopeX() : 0.0D;
                        double slopeZ = ptSurvey.inCorridor() ? ptSurvey.slopeZ() : 0.0D;
                        RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                                level,
                                targetPos,
                                surfaceFunc,
                                GranularMaterialRegistry.COMPACTED_ASPHALT,
                                128,
                                true,
                                Integer.MAX_VALUE,
                                com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.FINISHED,
                                slopeX,
                                slopeZ,
                                true
                        );
                        if (result.changed()) {
                            asphaltSmoothed = true;
                            effectPos = targetPos;
                        }
                        continue;
                    } else {
                        int layers = state.hasProperty(FreshAsphaltBlock.LAYERS) ? state.getValue(FreshAsphaltBlock.LAYERS) : 1;
                        level.setBlock(targetPos, GroundworksAsphaltMod.COMPACTED_ASPHALT.defaultBlockState().setValue(CompactedAsphaltBlock.LAYERS, layers), Block.UPDATE_ALL_IMMEDIATE);
                        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).recordStage(
                                targetPos,
                                com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.FINISHED,
                                targetPos.getY() + layers * 0.125D,
                                0.0D, 0.0D,
                                level.getGameTime()
                        );
                        asphaltSmoothed = true;
                        effectPos = targetPos;
                        continue;
                    }
                }

                boolean hasAsphalt = isAsphaltBlock(state) || hasAsphaltInCell(level, targetPos);

                // CRITICAL: An asphalt finishing roller ONLY touches blocks containing asphalt!
                // It must NEVER touch, convert, or carve underlying subgrade/dirt/gravel,
                // and must NEVER create asphalt out of thin air.
                if (hasAsphalt) {
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry pavedEntry =
                            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(targetPos);
                    if (pavedEntry == null) {
                        pavedEntry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(targetPos.below());
                    }
                    final com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry finalPavedEntry = pavedEntry;

                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);

                    RoadPlanarShaper.PlanarSurfaceFunction finalSurfaceFunc = (wx, wz) -> {
                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget pt =
                                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, wx, wz);
                        if (pt.inCorridor()) {
                            return pt.targetY() + 0.10D;
                        }
                        if (finalPavedEntry != null && finalPavedEntry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.PAVED)) {
                            return Math.max(finalPavedEntry.getY(wx, wz) - 0.02D, physicalDrumFloorY);
                        }
                        return surfaceFunc.getY(wx, wz);
                    };

                    double slopeX = ptSurvey.inCorridor() ? ptSurvey.slopeX() : (finalPavedEntry != null ? finalPavedEntry.slopeX() : 0.0D);
                    double slopeZ = ptSurvey.inCorridor() ? ptSurvey.slopeZ() : (finalPavedEntry != null ? finalPavedEntry.slopeZ() : 0.0D);

                    RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                            level,
                            targetPos,
                            finalSurfaceFunc,
                            GranularMaterialRegistry.COMPACTED_ASPHALT,
                            128, // Allow up to 128 units of fill deposit to bridge seams/depressions smoothly!
                            true,
                            Integer.MAX_VALUE,
                            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.FINISHED,
                            slopeX,
                            slopeZ,
                            true
                    );

                    if (result.changed()) {
                        asphaltSmoothed = true;
                        effectPos = targetPos;
                    }
                }
            }
        }

        if (asphaltSmoothed && effectPos != null && isFront) {
            BlockState effectState = level.getBlockState(effectPos);
            if (!effectState.isAir()) {
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, effectState),
                        checkPosCenter(effectPos).x, effectPos.getY() + 0.15D, checkPosCenter(effectPos).z,
                        4,
                        0.25D, 0.05D, 0.25D,
                        0.02D
                );
            }

            if (isWaterSpraying()) {
                level.sendParticles(
                        ParticleTypes.SPLASH,
                        checkPosCenter(effectPos).x, effectPos.getY() + 0.15D, checkPosCenter(effectPos).z,
                        4,
                        0.2D, 0.02D, 0.2D,
                        0.01D
                );
            }

            if (this.tickCount % 6 == 0) {
                level.playSound(
                        null, effectPos,
                        GroundworksAsphaltMod.ROLLER_VIBRATE, SoundSource.BLOCKS,
                        0.75F, 0.85F
                );
            }
        }
    }

    public record AsphaltSample(double y, double conf) {}

    /**
     * Samples the surface height and confidence of asphalt at a single column (bx, bz).
     * Returns null if no valid asphalt is detected.
     */
    public static AsphaltSample sampleAsphaltColumn(
            ServerLevel level, int bx, int bz, int hintY, double rollerPosY
    ) {
        double px = bx + 0.5D;
        double pz = bz + 0.5D;

        if (level != null) {
            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget survey =
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, px, pz);
            if (survey.inCorridor()) {
                double targetAsphaltY = survey.targetY() + 0.125D;
                if (Math.abs(targetAsphaltY - rollerPosY) <= 1.85D) {
                    return new AsphaltSample(targetAsphaltY, 2.0D);
                }
            }

            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry entry =
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level)
                            .getEntry(new BlockPos(bx, hintY, bz));
            if (entry == null) {
                entry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level)
                        .getEntry(new BlockPos(bx, hintY - 1, bz));
            }
            if (entry != null && entry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.PAVED)) {
                double roadY = entry.getY(px, pz);
                if (Math.abs(roadY - rollerPosY) <= 1.85D) {
                    return new AsphaltSample(roadY, 1.5D);
                }
            }
        }

        for (int dy = 2; dy >= -1; dy--) {
            int by = hintY + dy;
            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof FreshAsphaltBlock) {
                int layers = state.hasProperty(FreshAsphaltBlock.LAYERS) ? state.getValue(FreshAsphaltBlock.LAYERS) : 1;
                double topY = by + layers * 0.125D;
                if (Math.abs(topY - rollerPosY) <= 1.85D) {
                    return new AsphaltSample(topY, 1.0D);
                }
            }
            if (state.getBlock() instanceof CompactedAsphaltBlock) {
                int layers = state.hasProperty(CompactedAsphaltBlock.LAYERS) ? state.getValue(CompactedAsphaltBlock.LAYERS) : 1;
                double topY = by + layers * 0.125D;
                if (Math.abs(topY - rollerPosY) <= 1.85D) {
                    return new AsphaltSample(topY, 1.25D);
                }
            }
            if (state.getBlock() instanceof AsphaltSlabBlock) {
                double topY = by + 0.5D;
                if (Math.abs(topY - rollerPosY) <= 1.85D) {
                    return new AsphaltSample(topY, 1.25D);
                }
            }
            if (state.getBlock() instanceof RoadLineBlock) {
                double topY = by + 1.0D;
                if (Math.abs(topY - rollerPosY) <= 0.85D) {
                    return new AsphaltSample(topY, 1.25D);
                }
            }

            GranularCell cell = GroundworksApi.queryCell(level, pos);
            if (cell != null && !cell.isEmpty()) {
                double surfaceY = GroundworksApi.getSurfaceWorldY(level, pos, px, pz);
                if (Double.isFinite(surfaceY) && Math.abs(surfaceY - rollerPosY) <= 0.85D) {
                    boolean isAsphalt = (cell.surfaceCapMaterialId() == GranularMaterialRegistry.ASPHALT.id())
                            || (cell.surfaceCapMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                            || (cell.materialId() == GranularMaterialRegistry.ASPHALT.id())
                            || (cell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                            || (cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()) > 0)
                            || (cell.unitsOfMaterial(GranularMaterialRegistry.COMPACTED_ASPHALT.id()) > 0);

                    if (isAsphalt) {
                        double conf = (cell.surfaceCapMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id()
                                || cell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id()) ? 1.25D : 1.0D;
                        return new AsphaltSample(surfaceY, conf);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Detects existing asphalt pavement surrounding the roller (within ~5m ahead and behind).
     * Uses outward center-first scanning: when driving near the edge of an embankment or trench,
     * terrain beyond the road edge is excluded from the mesh calculation.
     * Calculates the best-fit 3D planar surface to smooth out all inclines, transitions, and crowns.
     */
    public RoadSurface3D computeLocalAsphaltSurface(ServerLevel level, Vec3 forward, Vec3 side) {
        Vec3 rollerPos = this.position();
        int hintY = Mth.floor(rollerPos.y);

        List<Vec3> asphaltPoints = new ArrayList<>();
        List<Double> asphaltWeights = new ArrayList<>();

        double[] uStations = {-3.8D, -3.0D, -2.2D, -1.5D, -0.7D, 0.0D, 0.7D, 1.5D, 2.2D, 3.0D, 3.8D, 4.8D};
        double[] lateralRight = {0.8D, 1.6D, 2.4D};
        double[] lateralLeft = {-0.8D, -1.6D, -2.4D};

        Set<BlockPos> visitedColumns = new HashSet<>();

        for (double u : uStations) {
            Vec3 alongU = rollerPos.add(forward.scale(u));

            // Center of asphalt roadbed (v = 0.0D)
            int bx0 = Mth.floor(alongU.x);
            int bz0 = Mth.floor(alongU.z);
            BlockPos centerColKey = new BlockPos(bx0, 0, bz0);
            Double centerY = null;
            if (visitedColumns.add(centerColKey)) {
                AsphaltSample s0 = sampleAsphaltColumn(level, bx0, bz0, hintY, rollerPos.y);
                if (s0 != null) {
                    centerY = s0.y();
                    asphaltPoints.add(new Vec3(bx0 + 0.5D, s0.y(), bz0 + 0.5D));
                    asphaltWeights.add(1.0D * s0.conf());
                }
            }

            double refY = (centerY != null) ? centerY : rollerPos.y;

            // Scan outwards to the RIGHT: stop immediately upon encountering the edge of the asphalt mat
            double lastRightY = refY;
            for (double v : lateralRight) {
                Vec3 pt = alongU.add(side.scale(v));
                int bx = Mth.floor(pt.x);
                int bz = Mth.floor(pt.z);
                BlockPos colKey = new BlockPos(bx, 0, bz);
                if (!visitedColumns.add(colKey)) {
                    continue;
                }
                AsphaltSample sample = sampleAsphaltColumn(level, bx, bz, hintY, rollerPos.y);
                if (sample == null) {
                    break;
                }
                if (Math.abs(sample.y() - lastRightY) > 0.45D || Math.abs(sample.y() - refY) > 0.95D) {
                    break;
                }
                double latWeight = 1.0D / (1.0D + 0.15D * v * v);
                asphaltPoints.add(new Vec3(bx + 0.5D, sample.y(), bz + 0.5D));
                asphaltWeights.add(latWeight * sample.conf());
                lastRightY = sample.y();
            }

            // Scan outwards to the LEFT: stop immediately upon encountering the edge of the asphalt mat
            double lastLeftY = refY;
            for (double v : lateralLeft) {
                Vec3 pt = alongU.add(side.scale(v));
                int bx = Mth.floor(pt.x);
                int bz = Mth.floor(pt.z);
                BlockPos colKey = new BlockPos(bx, 0, bz);
                if (!visitedColumns.add(colKey)) {
                    continue;
                }
                AsphaltSample sample = sampleAsphaltColumn(level, bx, bz, hintY, rollerPos.y);
                if (sample == null) {
                    break;
                }
                if (Math.abs(sample.y() - lastLeftY) > 0.45D || Math.abs(sample.y() - refY) > 0.95D) {
                    break;
                }
                double latWeight = 1.0D / (1.0D + 0.15D * v * v);
                asphaltPoints.add(new Vec3(bx + 0.5D, sample.y(), bz + 0.5D));
                asphaltWeights.add(latWeight * sample.conf());
                lastLeftY = sample.y();
            }
        }

        if (asphaltPoints.size() >= 4) {
            return RoadRollerEntity.fitPlane(asphaltPoints, asphaltWeights, true, forward);
        }

        double fallbackY = this.getY();
        return new RoadSurface3D(false, 0, this.getX(), fallbackY, this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    public static boolean isAsphaltBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return state.getBlock() instanceof FreshAsphaltBlock
                || state.getBlock() instanceof CompactedAsphaltBlock
                || state.getBlock() instanceof AsphaltSlabBlock
                || state.getBlock() instanceof RoadLineBlock;
    }

    public static boolean hasAsphaltInCell(ServerLevel level, BlockPos pos) {
        var entry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(pos);
        if (entry == null) {
            entry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(pos.above());
        }
        if (entry == null) {
            entry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(pos.below());
        }
        if (entry != null && entry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.PAVED)) {
            return true;
        }
        GranularCell cell = GroundworksApi.queryCell(level, pos);
        if (cell == null || cell.isEmpty()) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof com.piotrek.groundworks.block.entity.GranularBlockEntity gbe) {
                cell = gbe.getCell();
                if (gbe.getMaterialId() == GranularMaterialRegistry.ASPHALT.id()
                        || gbe.getMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id()) {
                    return true;
                }
            }
        }
        if (cell == null || cell.isEmpty()) {
            return false;
        }
        return (cell.surfaceCapMaterialId() == GranularMaterialRegistry.ASPHALT.id())
                || (cell.surfaceCapMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                || (cell.materialId() == GranularMaterialRegistry.ASPHALT.id())
                || (cell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                || (cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()) > 0)
                || (cell.unitsOfMaterial(GranularMaterialRegistry.COMPACTED_ASPHALT.id()) > 0);
    }

    private static Vec3 checkPosCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public Item getDropItem() {
        return GroundworksAsphaltMod.ASPHALT_ROLLER_ITEM;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
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
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (this.getPassengers().isEmpty()) {
            this.entityData.set(IS_COMPACTING, false);
            this.entityData.set(IS_VIBRATING, false);
            this.entityData.set(ENGINE_RUNNING, false);
        }
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
        // High central panoramic cab seat: centered laterally (X=0.0m), attachment height 2.41m (player butt sits firmly on 2.59m cushion),
        // longitudinal position -0.03m (-0.5 px in model), oriented dynamically with vehicle 3D pitch and roll
        return computeSeatOffset(0.0D, 2.41D, -0.03D, getVehiclePitch(), getVehicleRoll());
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
        // Dismount at the base of the left 3-step boarding ladder
        return this.position().add(left.scale(1.8D)).add(forward.scale(-0.25D)).add(0.0D, 0.25D, 0.0D);
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

    // ── Getters for Renderers & Client State ──────────────────────────

    public float getSteerAngle() {
        return this.entityData.get(STEER_ANGLE);
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getFrontDrumRotation() {
        return this.entityData.get(FRONT_DRUM_ROTATION);
    }

    public float getRearDrumRotation() {
        return this.entityData.get(REAR_DRUM_ROTATION);
    }

    public float getVehiclePitch() {
        return this.entityData.get(VEHICLE_PITCH);
    }

    public float getVehicleRoll() {
        return this.entityData.get(VEHICLE_ROLL);
    }

    public boolean isCompacting() {
        return this.entityData.get(IS_COMPACTING);
    }

    public boolean isVibrating() {
        return this.entityData.get(IS_VIBRATING);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public boolean isBraking() {
        return this.entityData.get(IS_BRAKING);
    }

    public boolean isWaterSpraying() {
        return this.entityData.get(WATER_SPRAYING);
    }

    public boolean isLightsOn() {
        return this.entityData.get(LIGHTS_ON);
    }

    public void setLightsOn(boolean lightsOn) {
        this.entityData.set(LIGHTS_ON, lightsOn);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
        }
        super.remove(reason);
    }
}
