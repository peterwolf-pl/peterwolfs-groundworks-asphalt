package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularComposition;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.AsphaltSlabBlock;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.block.RoadLineBlock;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
import com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 16-Ton Heavy Road Roller / Asphalt Compactor Entity.
 *
 * <p>Equipped with a high-amplitude vibratory steel compaction drum at the front
 * and smooth pneumatic compactor tires at the rear. Automatically converts
 * {@link FreshAsphaltBlock} into {@link com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock}
 * when rolling over fresh mat.
 */
public class RoadRollerEntity extends Entity {

    // ── Synched Entity Data ───────────────────────────────────────────
    private static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_ROTATION =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DRUM_ROTATION =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_COMPACTING =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_VIBRATING =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_BRAKING =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WATER_SPRAYING =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTS_ON =
            SynchedEntityData.defineId(RoadRollerEntity.class, EntityDataSerializers.BOOLEAN);

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private boolean inputHorn = false;
    private int hornCooldown = 0;
    private int inputFreshTicks = 0;

    private float wheelRot = 0.0F;
    private float drumRot = 0.0F;
    private boolean wasDriver = false;

    // Work lights placed in the world
    private final List<BlockPos> workLights = new ArrayList<>();

    // Ephemeral compaction surge buffer pushed in front of the vibratory drum
    private final GranularComposition carried = new GranularComposition();
    private static final int MAX_CARRIED_UNITS = 2048;

    public RoadRollerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STEER_ANGLE, 0.0F);
        builder.define(FORWARD_SPEED, 0.0F);
        builder.define(WHEEL_ROTATION, 0.0F);
        builder.define(DRUM_ROTATION, 0.0F);
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

        this.carried.clear();
        int carriedLength = Mth.clamp(input.getIntOr("CarriedLength", 0), 0, 256);
        int[] carriedCounts = new int[carriedLength];
        for (int i = 0; i < carriedLength; i++) {
            carriedCounts[i] = Math.max(0, input.getIntOr("CarriedMaterial" + i, 0));
        }
        this.carried.replaceWith(carriedCounts);

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

        int[] carriedCounts = this.carried.toArray();
        output.putInt("CarriedLength", carriedCounts.length);
        for (int i = 0; i < carriedCounts.length; i++) {
            output.putInt("CarriedMaterial" + i, carriedCounts[i]);
        }

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
        if (action1) {
            boolean current = isCompacting();
            setCompacting(!current);
            setVibrating(!current);
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
            setWaterSpraying(!isWaterSpraying());
            if (!this.level().isClientSide()) {
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS,
                        0.5F, isWaterSpraying() ? 1.2F : 0.8F
                );
            }
        }
        this.inputHorn = horn;
        this.inputFreshTicks = 10;
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

            if (hasDriver) {
                if (!this.wasDriver) {
                    setCompacting(true);
                    setVibrating(true);
                }
            } else {
                setVibrating(false);
                setCompacting(false);
            }
            this.wasDriver = hasDriver;

            this.entityData.set(ENGINE_RUNNING, hasDriver);

            // Steering articulation
            float targetSteer = inputSteer * 0.45F;
            float currentSteer = this.entityData.get(STEER_ANGLE);
            float newSteer = Mth.approach(currentSteer, targetSteer, 0.05F);
            this.entityData.set(STEER_ANGLE, newSteer);

            // Vehicle heading and drive speed
            if (Math.abs(inputThrottle) > 0.01F && Math.abs(newSteer) > 0.001F) {
                float yawChange = newSteer * inputThrottle * 2.8F;
                this.setYRot(Mth.wrapDegrees(this.getYRot() + yawChange));
                this.setYHeadRot(this.getYRot());
                this.setYBodyRot(this.getYRot());
            }

            double yawRad = Math.toRadians(this.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

            // 3D Road Surface Calculation (detects surrounding road mat and computes best-fit 3D plane)
            RoadSurface3D roadSurface = computeLocalRoadSurface(serverLevel, forward, side);

            // Physical ground contact sampling under the 4 wheels/drum
            Vec3 frontCenter = this.position().add(forward.scale(1.8D));
            Vec3 rearCenter = this.position().subtract(forward.scale(1.5D));

            double maxAllowedY = this.getY() + 2.0D;
            double flY = sampleGroundHeight(serverLevel, frontCenter.subtract(side.scale(0.9D)), maxAllowedY);
            double frY = sampleGroundHeight(serverLevel, frontCenter.add(side.scale(0.9D)), maxAllowedY);
            double rlY = sampleGroundHeight(serverLevel, rearCenter.subtract(side.scale(0.9D)), maxAllowedY);
            double rrY = sampleGroundHeight(serverLevel, rearCenter.add(side.scale(0.9D)), maxAllowedY);

            // A single wheel can be above an empty micro-column or a sharp ledge. Never let that
            // outlier drag the whole 16-ton chassis several blocks underground.
            double actualPhysicalGroundY = robustSupportHeight(flY, frY, rlY, rrY);
            boolean hasPhysicalSupport = Double.isFinite(actualPhysicalGroundY);
            if (!hasPhysicalSupport) {
                actualPhysicalGroundY = this.getY();
            }

            flY = clampSupportOutlier(flY, actualPhysicalGroundY);
            frY = clampSupportOutlier(frY, actualPhysicalGroundY);
            rlY = clampSupportOutlier(rlY, actualPhysicalGroundY);
            rrY = clampSupportOutlier(rrY, actualPhysicalGroundY);

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

            if (roadSurface.isValid()) {
                double forwardSlope = roadSurface.slopeX * forward.x + roadSurface.slopeZ * forward.z;
                double sideSlope = roadSurface.slopeX * side.x + roadSurface.slopeZ * side.z;
                targetPitch = Math.toDegrees(Math.atan(-forwardSlope));
                targetRoll = Math.toDegrees(Math.atan(-sideSlope));
                targetPitch = Mth.clamp(targetPitch, -30.0D, 30.0D);
                targetRoll = Mth.clamp(targetRoll, -12.0D, 12.0D);
            } else {
                targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, 3.3D));
                targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 1.8D));
                targetPitch = Mth.clamp(targetPitch, -45.0D, 45.0D);
                targetRoll = Mth.clamp(targetRoll, -20.0D, 20.0D);
            }

            float currentPitch = this.entityData.get(VEHICLE_PITCH);
            float currentRoll = this.entityData.get(VEHICLE_ROLL);
            float newPitch = (float) Mth.lerp(0.2D, currentPitch, targetPitch);
            float newRoll = (float) Mth.lerp(0.2D, currentRoll, targetRoll);
            this.entityData.set(VEHICLE_PITCH, newPitch);
            this.entityData.set(VEHICLE_ROLL, newRoll);

            float targetSpeed = inputThrottle * 0.12F;
            double targetGroundY;
            if (roadSurface.isValid()) {
                double surfaceY = roadSurface.getY(this.getX(), this.getZ());
                targetGroundY = Mth.clamp(surfaceY, actualPhysicalGroundY - 0.15D, actualPhysicalGroundY + 0.35D);
            } else {
                targetGroundY = rearAvg + (frontAvg - rearAvg) * (1.5D / 3.3D);
            }

            double dy = calculateSmoothVerticalStep(targetGroundY, this.getY(), targetSpeed, this.onGround());
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
                this.wheelRot += rotDelta;
                this.drumRot += rotDelta;
                this.entityData.set(WHEEL_ROTATION, this.wheelRot);
                this.entityData.set(DRUM_ROTATION, this.drumRot);
            }

            if (hasDriver || fwdSpeed > 0.001F || Math.abs(inputThrottle) > 0.01F || Math.abs(inputSteer) > 0.01F) {
                this.syncPosition = true;
                this.needsSync = true;
                this.syncVelocity = true;
            }

            // Compaction logic using the calculated 3D surface
            if (isCompacting() && (hasDriver || fwdSpeed > 0.005F)) {
                compactUnderDrum(serverLevel, forward, newPitch, newRoll, roadSurface, fwdSpeed);
            }

            // Update worklights
            if (this.isEngineRunning() && this.isLightsOn()) {
                List<BlockPos> beams = AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, 1.8D, 1.8D, 1.4D, new int[]{2, 4, 7, 10}, false
                );
                beams.addAll(AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, -1.8D, 1.6D, 1.2D, new int[]{2, 5}, true
                ));
                AsphaltVehicleLights.syncLightBlocks(serverLevel, this.workLights, beams, AsphaltVehicleLights.HEAD_LIGHT_LEVEL);
            } else if (!this.workLights.isEmpty()) {
                AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
            }
        }
    }

    private void compactUnderDrum(
            ServerLevel level, Vec3 forward, float pitchDeg, float rollDeg, RoadSurface3D surface, float fwdSpeed
    ) {
        Vec3 drumCenter = this.position().add(forward.scale(1.8D));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

        // Primary compaction pass under the front vibratory steel drum
        compactAtAxle(level, drumCenter, forward, side, pitchDeg, rollDeg, surface, true);

        // When reversing, pneumatic rear compactor tires also knead and seal the asphalt mat
        if (inputThrottle < -0.01F && fwdSpeed > 0.005F) {
            Vec3 rearCenter = this.position().subtract(forward.scale(1.5D));
            compactAtAxle(level, rearCenter, forward, side, pitchDeg, rollDeg, surface, false);
        }
    }

    private void compactAtAxle(
            ServerLevel level, Vec3 axleCenter, Vec3 forward, Vec3 side, float pitchDeg, float rollDeg,
            RoadSurface3D surface, boolean isFrontDrum
    ) {
        double rollTangent = Math.tan(Math.toRadians(rollDeg));
        double pitchSine = -Math.sin(Math.toRadians(pitchDeg));
        double axleBaseY = this.getY() + pitchSine * (isFrontDrum ? 1.8D : -1.5D);

        // Continuous planar surface function with subtractive and additive leveling:
        // When a multi-point regression surface is valid, follow the continuous 3D plane across the roadbed!
        // When invalid, follow the vehicle's dynamic attitude with pitch and roll.
        // Clamp to prevent tunneling deeper than 0.60m below axle baseline.
        final double physicalDrumFloorY = axleBaseY - 0.60D;
        RoadPlanarShaper.PlanarSurfaceFunction surfaceFunc = (wx, wz) -> {
            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget survey =
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, wx, wz);
            if (survey.inCorridor()) {
                return survey.targetY();
            }
            BlockPos cellPos = BlockPos.containing(wx, axleBaseY, wz);
            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry entry =
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(cellPos);
            double targetY;
            if (entry != null && entry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED)) {
                // Follow the established roadbed mesh (camber, crown, grade) established by grader
                // rather than wiping it out with an arbitrary single flat regression plane!
                targetY = entry.getY(wx, wz);
            } else if (surface != null && surface.isValid()) {
                targetY = surface.getY(wx, wz);
            } else {
                double offU = (wx - axleCenter.x) * side.x + (wz - axleCenter.z) * side.z;
                double offV = (wx - axleCenter.x) * forward.x + (wz - axleCenter.z) * forward.z;
                double pitchTan = Math.tan(Math.toRadians(-pitchDeg));
                targetY = axleBaseY + offU * rollTangent + offV * pitchTan;
            }
            return Math.max(targetY, physicalDrumFloorY);
        };

        RoadPlanarShaper.PlanarSurfaceFunction asphaltSurfaceFunc = (wx, wz) -> {
            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget survey =
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, wx, wz);
            if (survey.inCorridor()) {
                return survey.targetY() + 0.10D;
            }
            BlockPos cellPos = BlockPos.containing(wx, axleBaseY, wz);
            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry entry =
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(cellPos);
            if (entry == null) {
                entry = com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level).getEntry(cellPos.below());
            }
            double targetY;
            if (entry != null && entry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.PAVED)) {
                targetY = entry.getY(wx, wz) - 0.02D;
            } else if (surface != null && surface.isValid()) {
                targetY = surface.getY(wx, wz);
            } else {
                double offU = (wx - axleCenter.x) * side.x + (wz - axleCenter.z) * side.z;
                double offV = (wx - axleCenter.x) * forward.x + (wz - axleCenter.z) * forward.z;
                double pitchTan = Math.tan(Math.toRadians(-pitchDeg));
                targetY = axleBaseY + offU * rollTangent + offV * pitchTan;
            }
            return Math.max(targetY, physicalDrumFloorY);
        };

        // Sampling across drum contact footprint: width (-1.0m to +1.0m) and longitudinal footprint
        double[] lateralOffsets = {-1.0D, -0.8D, -0.6D, -0.4D, -0.2D, 0.0D, 0.2D, 0.4D, 0.6D, 0.8D, 1.0D};
        double[] longitudinalOffsets = isFrontDrum ? new double[]{-0.30D, 0.0D, 0.30D} : new double[]{0.0D};
        Set<Long> targetColumns = new java.util.LinkedHashSet<>();

        boolean surveyActive = false;
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget centerSurvey =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, axleCenter.x, axleCenter.z);
        if (centerSurvey.inCorridor()) {
            surveyActive = true;
        }

        for (double dx : longitudinalOffsets) {
            Vec3 alongAxle = axleCenter.add(forward.scale(dx));
            for (double off : lateralOffsets) {
                Vec3 checkPt = alongAxle.add(side.scale(off));
                if (surveyActive) {
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, checkPt.x, checkPt.z);
                    if (!ptSurvey.inCorridor()) {
                        continue;
                    }
                }
                int bx = Mth.floor(checkPt.x);
                int bz = Mth.floor(checkPt.z);
                targetColumns.add((((long) bx) << 32) | (bz & 0xFFFFFFFFL));
            }
        }

        boolean granularCompacted = false;
        BlockPos effectPos = null;

        for (long colKey : targetColumns) {
            int bx = (int) (colKey >> 32);
            int bz = (int) colKey;

            double targetY = surfaceFunc.getY(bx + 0.5D, bz + 0.5D);
            int baseBlockY = Mth.floor(targetY - 0.0625D);
            double targetAsphaltY = asphaltSurfaceFunc.getY(bx + 0.5D, bz + 0.5D);
            int asphaltBlockY = Mth.floor(targetAsphaltY - 0.0625D);

            double minAsphaltY = asphaltSurfaceFunc.getY(bx + 0.125D, bz + 0.125D);
            double maxAsphaltY = minAsphaltY;
            for (double ox : new double[]{0.125D, 0.875D}) {
                for (double oz : new double[]{0.125D, 0.875D}) {
                    double ay = asphaltSurfaceFunc.getY(bx + ox, bz + oz);
                    if (ay < minAsphaltY) minAsphaltY = ay;
                    if (ay > maxAsphaltY) maxAsphaltY = ay;
                }
            }
            int minAsphaltBlockY = Mth.floor(minAsphaltY - 0.0625D);
            int maxAsphaltBlockY = Mth.floor(maxAsphaltY - 0.0625D);

            int minY = Math.min(baseBlockY, minAsphaltBlockY) - 1;
            int maxY = Math.max(baseBlockY, maxAsphaltBlockY) + 2;

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

            if (columnHasAsphalt) {
                // ASPHALT PAVEMENT COMPACTION PASS: compact fresh/loose asphalt to finished dense road
                for (int by = maxY; by >= minY; by--) {
                    BlockPos targetPos = new BlockPos(bx, by, bz);
                    if (!level.isInWorldBounds(targetPos)) continue;
                    BlockState state = level.getBlockState(targetPos);

                    // 1. Fresh asphalt: compact to compacted asphalt (on survey ramps, shape to exact 3D planar slope)
                    if (state.getBlock() instanceof FreshAsphaltBlock) {
                        if (surveyActive) {
                            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget cellSurvey =
                                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);
                            double curSlopeX = cellSurvey.inCorridor() ? cellSurvey.slopeX() : (surface != null ? surface.slopeX() : 0.0D);
                            double curSlopeZ = cellSurvey.inCorridor() ? cellSurvey.slopeZ() : (surface != null ? surface.slopeZ() : 0.0D);

                            RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                                    level,
                                    targetPos,
                                    asphaltSurfaceFunc,
                                    GranularMaterialRegistry.COMPACTED_ASPHALT,
                                    128,
                                    true,
                                    Integer.MAX_VALUE,
                                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.FINISHED,
                                    curSlopeX,
                                    curSlopeZ,
                                    true
                            );
                            if (result.changed()) {
                                granularCompacted = true;
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
                            granularCompacted = true;
                            effectPos = targetPos;
                            continue;
                        }
                    }

                    // 2. Existing asphalt pavement (microvoxel granular mat or slab)
                    boolean hasAsphalt = isAsphaltBlock(state) || hasAsphaltInCell(level, targetPos);

                    if (hasAsphalt) {
                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget cellSurvey =
                                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);
                        double curSlopeX = cellSurvey.inCorridor() ? cellSurvey.slopeX() : (surface != null ? surface.slopeX() : 0.0D);
                        double curSlopeZ = cellSurvey.inCorridor() ? cellSurvey.slopeZ() : (surface != null ? surface.slopeZ() : 0.0D);

                        RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                                level,
                                targetPos,
                                asphaltSurfaceFunc,
                                GranularMaterialRegistry.COMPACTED_ASPHALT,
                                128,
                                true,
                                Integer.MAX_VALUE,
                                com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.FINISHED,
                                curSlopeX,
                                curSlopeZ,
                                true
                        );
                        if (result.changed()) {
                            granularCompacted = true;
                            effectPos = targetPos;
                        }
                    }
                }
            } else {
                // SUBGRADE / EARTHWORKS COMPACTION PASS
                for (int by = maxY; by >= minY; by--) {
                    BlockPos targetPos = new BlockPos(bx, by, bz);
                    if (!level.isInWorldBounds(targetPos)) continue;
                    BlockState state = level.getBlockState(targetPos);

                    // Clear vegetation (grass, flowers) resting on or within compaction zone
                    if (isVegetationOrReplaceable(state)) {
                        level.destroyBlock(targetPos, false, this);
                        state = level.getBlockState(targetPos);
                    }

                    boolean canShape = isLooseRoadbedBlock(state)
                            || GroundworksApi.isDiggable(level, targetPos)
                            || state.is(GroundworksMod.GRANULAR_BLOCK)
                            || state.isAir()
                            || state.canBeReplaced();

                    if (canShape) {
                        GranularCell contextCell = GroundworksApi.queryCell(level, targetPos);
                        GranularMaterial fillMat = determineFillMaterial(contextCell, state);
                        int fillUnits = fillMat != null ? Math.max(192, carried.unitsOf(fillMat.id())) : 192;
                        int excavationCapacity = Math.max(0, MAX_CARRIED_UNITS - carried.totalUnits());

                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget cellSurvey =
                                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, targetPos.getX() + 0.5D, targetPos.getZ() + 0.5D);
                        double curSlopeX = cellSurvey.inCorridor() ? cellSurvey.slopeX() : (surface != null ? surface.slopeX() : 0.0D);
                        double curSlopeZ = cellSurvey.inCorridor() ? cellSurvey.slopeZ() : (surface != null ? surface.slopeZ() : 0.0D);
                        RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                                level,
                                targetPos,
                                surfaceFunc,
                                fillMat,
                                fillUnits,
                                false,
                                excavationCapacity,
                                com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.COMPACTED,
                                curSlopeX,
                                curSlopeZ,
                                true
                        );

                        if (result.changed()) {
                            if (result.unitsExcavated() > 0 && result.material() != null && result.material().id() > 0) {
                                if (carried.totalUnits() < MAX_CARRIED_UNITS) {
                                    carried.add(result.material(), result.unitsExcavated());
                                }
                            }
                            if (result.unitsDeposited() > 0 && fillMat != null && carried.unitsOf(fillMat.id()) > 0) {
                                carried.remove(fillMat.id(), result.unitsDeposited());
                            }
                            granularCompacted = true;
                            effectPos = targetPos;
                        }
                    }
                }
            }
        }


        if (granularCompacted && effectPos != null && isFrontDrum) {
            BlockState effectState = level.getBlockState(effectPos);
            if (!effectState.isAir()) {
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, effectState),
                        checkPosCenter(effectPos).x, effectPos.getY() + 0.15D, checkPosCenter(effectPos).z,
                        4,
                        0.25D, 0.05D, 0.25D,
                        0.02D
                );
            } else {
                level.sendParticles(
                        ParticleTypes.POOF,
                        checkPosCenter(effectPos).x, effectPos.getY() + 0.15D, checkPosCenter(effectPos).z,
                        3,
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

    public record SubgradeSample(double y, double weightMultiplier) {}

    /**
     * Samples the surface height of road subgrade materials at a single column (bx, bz).
     * Returns null if no valid subgrade or if asphalt is detected.
     */
    public static SubgradeSample sampleSubgradeColumn(
            ServerLevel level, int bx, int bz, int searchCenterY, double expectedY
    ) {
        double px = bx + 0.5D;
        double pz = bz + 0.5D;

        // Check if this column already has an established roadbed corridor mesh or survey stake alignment
        if (level != null) {
            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget survey =
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, px, pz);
            if (survey.inCorridor() && Math.abs(survey.targetY() - expectedY) <= 2.2D) {
                return new SubgradeSample(survey.targetY(), 2.0D);
            }

            com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry entry =
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level)
                            .getEntry(new BlockPos(bx, searchCenterY, bz));
            if (entry != null && entry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED)) {
                double roadY = entry.getY(px, pz);
                if (Math.abs(roadY - expectedY) <= 2.2D) {
                    return new SubgradeSample(roadY, 1.5D);
                }
            }
        }

        for (int dy = 3; dy >= -3; dy--) {
            int by = searchCenterY + dy;
            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState state = level.getBlockState(pos);

            // Soil compactor skips asphalt completely
            if (isAsphaltBlock(state)) {
                return null;
            }

            // Skip grass, flowers and other vegetation resting on top of subgrade
            if (isVegetationOrReplaceable(state)) {
                continue;
            }

            // 1. Granular terrain cells (subgrade materials)
            GranularCell cell = GroundworksApi.queryCell(level, pos);
            if (cell != null && !cell.isEmpty()) {
                if (hasAsphaltInCell(level, pos)) {
                    return null;
                }
                double surfaceY = GroundworksApi.getSurfaceWorldY(level, pos, px, pz);
                if (Double.isFinite(surfaceY) && Math.abs(surfaceY - expectedY) <= 2.2D) {
                    return new SubgradeSample(surfaceY, 1.0D);
                }
            }

            // If pos is a GranularBlock with empty/invalid cell, do not treat as solid block
            if (state.is(GroundworksMod.GRANULAR_BLOCK)) {
                continue;
            }

            // 2. Loose roadbed blocks (gravel, sand, dirt, cobblestone, coarse dirt, mud, etc.) or diggable
            if (isLooseRoadbedBlock(state) || GroundworksApi.isDiggable(level, pos)) {
                double topY = by + 1.0D;
                if (Math.abs(topY - expectedY) <= 2.2D) {
                    return new SubgradeSample(topY, 1.0D);
                }
            }

            // 3. Solid roadbed subgrade foundation
            if (!state.isAir() && state.isSolid()) {
                double topY = by + 1.0D;
                if (Math.abs(topY - expectedY) <= 2.2D) {
                    return new SubgradeSample(topY, 0.8D);
                }
            }
        }
        return null;
    }

    /**
     * Detects road materials (loose subgrade materials like gravel, sand, dirt, cobblestone)
     * by taking multipoint samples from in front of and behind the roller along the road profile.
     * Uses outward center-first scanning: when driving near the edge of an embankment (drop-off)
     * or trench (rising wall), terrain beyond the road edge is excluded from the mesh calculation.
     * Calculates the best-fit 3D planar surface to ensure the compacted roadbed is fully smooth.
     */
    public RoadSurface3D computeLocalRoadSurface(ServerLevel level, Vec3 forward, Vec3 side) {
        Vec3 rollerPos = this.position();

        List<Vec3> subgradePoints = new ArrayList<>();
        List<Double> subgradeWeights = new ArrayList<>();

        // Longitudinal stations along vehicle heading:
        // Rear contact zone: -3.8m to -1.5m
        // Chassis & drum zone: -0.7m, 0.0m, 0.8m, 1.4m, 1.8m
        // Ahead probe zone (2-3 blocks in front of the front drum axle at 1.8m): 3.8m, 4.3m, 4.8m
        double[] uStations = {-3.8D, -3.0D, -2.2D, -1.5D, -0.7D, 0.0D, 0.8D, 1.4D, 1.8D, 3.8D, 4.3D, 4.8D};

        // Outward lateral scan: center (0.0), then right, then left
        double[] lateralRight = {0.8D, 1.6D, 2.4D};
        double[] lateralLeft = {-0.8D, -1.6D, -2.4D};

        Set<BlockPos> visitedColumns = new HashSet<>();

        double pitchRad = Math.toRadians(-this.getVehiclePitch());
        double pitchTan = Math.tan(pitchRad);

        for (double u : uStations) {
            Vec3 alongU = rollerPos.add(forward.scale(u));
            double expectedY = rollerPos.y + u * pitchTan;
            int searchCenterY = Mth.floor(expectedY);
            boolean isAheadProbe = (u >= 3.5D);
            double probeWeight = isAheadProbe ? 2.0D : 1.0D;

            // Center of roadbed profile (v = 0.0D)
            int bx0 = Mth.floor(alongU.x);
            int bz0 = Mth.floor(alongU.z);
            BlockPos centerColKey = new BlockPos(bx0, 0, bz0);
            Double centerY = null;
            if (visitedColumns.add(centerColKey)) {
                SubgradeSample s0 = sampleSubgradeColumn(level, bx0, bz0, searchCenterY, expectedY);
                if (s0 != null) {
                    centerY = s0.y();
                    subgradePoints.add(new Vec3(bx0 + 0.5D, s0.y(), bz0 + 0.5D));
                    subgradeWeights.add(1.0D * probeWeight * s0.weightMultiplier());
                }
            }

            double refY = (centerY != null) ? centerY : expectedY;

            // Scan outwards to the RIGHT: stop immediately upon encountering the edge of the road
            // (steep embankment drop-off or rising trench wall).
            double lastRightY = refY;
            for (double v : lateralRight) {
                Vec3 pt = alongU.add(side.scale(v));
                int bx = Mth.floor(pt.x);
                int bz = Mth.floor(pt.z);
                BlockPos colKey = new BlockPos(bx, 0, bz);
                if (!visitedColumns.add(colKey)) {
                    continue;
                }
                SubgradeSample sample = sampleSubgradeColumn(level, bx, bz, searchCenterY, expectedY);
                if (sample == null) {
                    // Air cliff or no road subgrade -> road edge reached
                    break;
                }
                // Edge break check:
                // Terrain beyond the road edge (drop down embankment or wall of trench) must NOT
                // be factored into the road surface mesh!
                if (Math.abs(sample.y() - lastRightY) > 0.38D || Math.abs(sample.y() - refY) > 0.55D) {
                    break;
                }
                double latWeight = 1.0D / (1.0D + 0.15D * v * v);
                subgradePoints.add(new Vec3(bx + 0.5D, sample.y(), bz + 0.5D));
                subgradeWeights.add(latWeight * probeWeight * sample.weightMultiplier());
                lastRightY = sample.y();
            }

            // Scan outwards to the LEFT: stop immediately upon encountering the edge of the road
            double lastLeftY = refY;
            for (double v : lateralLeft) {
                Vec3 pt = alongU.add(side.scale(v));
                int bx = Mth.floor(pt.x);
                int bz = Mth.floor(pt.z);
                BlockPos colKey = new BlockPos(bx, 0, bz);
                if (!visitedColumns.add(colKey)) {
                    continue;
                }
                SubgradeSample sample = sampleSubgradeColumn(level, bx, bz, searchCenterY, expectedY);
                if (sample == null) {
                    break;
                }
                if (Math.abs(sample.y() - lastLeftY) > 0.38D || Math.abs(sample.y() - refY) > 0.55D) {
                    break;
                }
                double latWeight = 1.0D / (1.0D + 0.15D * v * v);
                subgradePoints.add(new Vec3(bx + 0.5D, sample.y(), bz + 0.5D));
                subgradeWeights.add(latWeight * probeWeight * sample.weightMultiplier());
                lastLeftY = sample.y();
            }
        }

        if (subgradePoints.size() >= 6) {
            return fitPlane(subgradePoints, subgradeWeights, false, forward);
        }

        double fallbackY = this.getY();
        return new RoadSurface3D(false, 0, this.getX(), fallbackY, this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    /**
     * Fits a 3D plane Y(x, z) = meanY + slopeX * (x - meanX) + slopeZ * (z - meanZ)
     * using weighted least-squares regression.
     */
    public static RoadSurface3D fitPlane(List<Vec3> points, List<Double> weights, boolean hasAsphalt, Vec3 forward) {
        int n = points.size();
        if (n == 0) {
            return new RoadSurface3D(hasAsphalt, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double totalW = 0.0D;
        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;

        for (int i = 0; i < n; i++) {
            double w = weights.get(i);
            Vec3 pt = points.get(i);
            totalW += w;
            sumX += w * pt.x;
            sumY += w * pt.y;
            sumZ += w * pt.z;
        }

        if (totalW < 1e-6D) {
            Vec3 first = points.get(0);
            return new RoadSurface3D(hasAsphalt, n, first.x, first.y, first.z, 0.0D, 0.0D, 0.0D);
        }

        double meanX = sumX / totalW;
        double meanY = sumY / totalW;
        double meanZ = sumZ / totalW;

        double Sxx = 0.0D;
        double Szz = 0.0D;
        double Sxz = 0.0D;
        double Sxy = 0.0D;
        double Szy = 0.0D;

        for (int i = 0; i < n; i++) {
            double w = weights.get(i);
            Vec3 pt = points.get(i);
            double dx = pt.x - meanX;
            double dy = pt.y - meanY;
            double dz = pt.z - meanZ;

            Sxx += w * dx * dx;
            Szz += w * dz * dz;
            Sxz += w * dx * dz;
            Sxy += w * dx * dy;
            Szy += w * dz * dy;
        }

        double det = Sxx * Szz - Sxz * Sxz;
        double slopeX = 0.0D;
        double slopeZ = 0.0D;

        if (n >= 3 && Math.abs(det) > 0.02D) {
            slopeX = (Sxy * Szz - Szy * Sxz) / det;
            slopeZ = (Szy * Sxx - Sxy * Sxz) / det;
        } else {
            // 1D projection along heading vector
            double projVar = Sxx * forward.x * forward.x + 2.0D * Sxz * forward.x * forward.z + Szz * forward.z * forward.z;
            if (projVar > 0.01D) {
                double projCov = Sxy * forward.x + Szy * forward.z;
                double fwdSlope = projCov / projVar;
                slopeX = fwdSlope * forward.x;
                slopeZ = fwdSlope * forward.z;
            }
        }

        slopeX = Mth.clamp(slopeX, -0.30D, 0.30D);
        slopeZ = Mth.clamp(slopeZ, -0.30D, 0.30D);

        double residualSum = 0.0D;
        for (int i = 0; i < n; i++) {
            double w = weights.get(i);
            Vec3 pt = points.get(i);
            double predY = meanY + slopeX * (pt.x - meanX) + slopeZ * (pt.z - meanZ);
            double err = pt.y - predY;
            residualSum += w * err * err;
        }
        double rms = Math.sqrt(residualSum / totalW);

        return new RoadSurface3D(hasAsphalt, n, meanX, meanY, meanZ, slopeX, slopeZ, rms);
    }

    /**
     * 3D Planar road surface representation computed from surrounding asphalt or subgrade.
     */
    public record RoadSurface3D(
            boolean hasAsphalt,
            int sampleCount,
            double meanX,
            double meanY,
            double meanZ,
            double slopeX,
            double slopeZ,
            double rmsResidual
    ) {
        public boolean isValid() {
            return sampleCount >= (hasAsphalt ? 4 : 6) && rmsResidual < (hasAsphalt ? 0.45D : 1.6D);
        }

        public double getY(double x, double z) {
            return meanY + slopeX * (x - meanX) + slopeZ * (z - meanZ);
        }
    }

    private GranularMaterial determineFillMaterial(@Nullable GranularCell cell) {
        return determineFillMaterial(cell, null);
    }

    private GranularMaterial determineFillMaterial(@Nullable GranularCell cell, @Nullable BlockState state) {
        if (cell != null) {
            boolean isAsphalt = (cell.surfaceCapMaterialId() == GranularMaterialRegistry.ASPHALT.id())
                    || (cell.surfaceCapMaterialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                    || (cell.materialId() == GranularMaterialRegistry.ASPHALT.id())
                    || (cell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id())
                    || (cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()) > 0)
                    || (cell.unitsOfMaterial(GranularMaterialRegistry.COMPACTED_ASPHALT.id()) > 0);
            if (isAsphalt) {
                return GranularMaterialRegistry.COMPACTED_ASPHALT;
            }
            if (cell.material() != null && cell.material().id() > 0) {
                return cell.material();
            }
        }
        if (state != null) {
            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
                return GranularMaterialRegistry.SAND;
            }
            if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.PODZOL) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.ROOTED_DIRT)
                    || state.is(Blocks.MUD)) {
                return GranularMaterialRegistry.DIRT;
            }
        }
        if (!carried.isEmpty()) {
            return GranularMaterialRegistry.byId(carried.dominantMaterialId());
        }
        return GranularMaterialRegistry.GRAVEL;
    }

    public static boolean isVegetationOrReplaceable(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return state.canBeReplaced()
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.DANDELION)
                || state.is(Blocks.POPPY)
                || state.is(Blocks.BLUE_ORCHID)
                || state.is(Blocks.ALLIUM)
                || state.is(Blocks.AZURE_BLUET)
                || state.is(Blocks.RED_TULIP)
                || state.is(Blocks.ORANGE_TULIP)
                || state.is(Blocks.WHITE_TULIP)
                || state.is(Blocks.PINK_TULIP)
                || state.is(Blocks.OXEYE_DAISY)
                || state.is(Blocks.CORNFLOWER)
                || state.is(Blocks.LILY_OF_THE_VALLEY)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.SUNFLOWER)
                || state.is(Blocks.LILAC)
                || state.is(Blocks.ROSE_BUSH)
                || state.is(Blocks.PEONY)
                || state.is(Blocks.TORCHFLOWER)
                || state.is(Blocks.PITCHER_PLANT);
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

    public static boolean isLooseRoadbedBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MUD)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || GranularMaterialRegistry.isAuriferousGravel(state)) {
            return true;
        }
        return false;
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

    public static double sampleGroundHeight(
            ServerLevel level, Vec3 pos, double maxAllowedGroundY, boolean ignoreLooseBumpsAboveChassis, double chassisY
    ) {
        return sampleGroundHeight(level, pos, maxAllowedGroundY);
    }

    public static double sampleGroundHeight(ServerLevel level, Vec3 pos, double maxAllowedGroundY) {
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        if (pos.y < minY + 1.0D) {
            return minY + 1.0D;
        }
        double clampedY = Mth.clamp(pos.y + 0.5D, minY + 1.0D, Math.min(maxY - 1.0D, maxAllowedGroundY));
        BlockPos start = BlockPos.containing(pos.x, clampedY, pos.z);

        for (int dy = 0; dy <= 16; dy++) {
            BlockPos check = start.below(dy);
            if (check.getY() < minY) {
                return minY + 1.0D;
            }

            double surfaceY = GroundworksApi.getSurfaceWorldY(level, check, pos.x, pos.z);
            if (Double.isFinite(surfaceY)) {
                if (surfaceY <= maxAllowedGroundY) {
                    return surfaceY;
                }
            }

            BlockState state = level.getBlockState(check);
            if (!state.isAir()) {
                // Ignore flowers, short grass, tall grass, and replaceable vegetation during height sampling
                if (isVegetationOrReplaceable(state)) {
                    continue;
                }
                net.minecraft.world.phys.shapes.VoxelShape shape = state.getCollisionShape(level, check);
                if (!shape.isEmpty()) {
                    double top = check.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                    if (top <= maxAllowedGroundY) {
                        return top;
                    }
                }
                if (state.isSolid()) {
                    double top = check.getY() + 1.0D;
                    if (top <= maxAllowedGroundY) {
                        return top;
                    }
                }
            }
        }

        // Return current pos.y rather than Double.NaN so missing samples never sink the vehicle.
        return pos.y;
    }

    /**
     * Returns an absolute entity-foot Y for escaping an already intersecting granular collision
     * shape. Upward velocity alone is insufficient once Minecraft's collision solver sees the AABB
     * as embedded and clips the recovery movement. This helper can only move upward.
     */
    public static double calculateBuriedRecoveryY(
            double currentY, double physicalSupportY, boolean hasPhysicalSupport
    ) {
        if (hasPhysicalSupport && Double.isFinite(currentY) && Double.isFinite(physicalSupportY)
                && currentY < physicalSupportY - 0.03D) {
            return physicalSupportY;
        }
        return currentY;
    }

    /**
     * Collision-driven vertical motion shared by the roller and grader. This deliberately restores
     * the proven vanilla-style gravity from 0.1.0: collision shapes decide how far the vehicle can
     * descend. Terrain samples are allowed only to rescue a chassis that is already buried; they
     * can never pull it downward through a partial or changing granular cell.
     */
    public static double calculateCollisionDrivenVerticalSpeed(
            double currentY, double physicalSupportY, boolean hasPhysicalSupport, boolean onGround
    ) {
        if (hasPhysicalSupport && Double.isFinite(currentY) && Double.isFinite(physicalSupportY)) {
            if (currentY < physicalSupportY - 0.03D) {
                return Math.min(0.25D, (physicalSupportY - currentY) * 0.35D);
            }
            // Hold exactly on a known collision floor. Thin 1/8 asphalt and changing granular
            // shapes must not depend on a negative gravity step being clipped by move().
            if (currentY <= physicalSupportY + 0.03D) {
                return 0.0D;
            }
        }
        return onGround ? -0.04D : -0.25D;
    }

    /**
     * Smooth, continuous vertical step conforming vehicle suspension to terrain elevation.
     * Prevents vertical jitter/hopping on flat ground (dy=0 when grounded) while providing
     * speed-proportional climbing up layers/slopes and controlled descent.
     */
    public static double calculateSmoothVerticalStep(double targetGroundY, double currentY, double speed, boolean onGround) {
        if (!onGround) {
            return -0.25D; // airborne gravity
        }
        if (!Double.isFinite(targetGroundY) || !Double.isFinite(currentY)) {
            return 0.0D;
        }
        double heightDiff = targetGroundY - currentY;
        if (heightDiff > 0.03D) {
            double climbSpeed = Math.min(heightDiff, Math.max(0.08D, Math.abs(speed) * 0.75D));
            return Math.min(0.85D, climbSpeed);
        } else if (heightDiff < -0.05D) {
            return Math.max(heightDiff, -0.25D);
        } else {
            return 0.0D;
        }
    }

    public static double highestFiniteSupport(double... supports) {
        double highest = Double.NaN;
        for (double support : supports) {
            if (Double.isFinite(support) && (!Double.isFinite(highest) || support > highest)) {
                highest = support;
            }
        }
        return highest;
    }

    /**
     * Robust center support for the four wheel/drum contacts. Invalid contacts are ignored. For a
     * cliff wider than the vehicle's negotiable step, the upper supported pair wins instead of
     * averaging the chassis into mid-air between two terrain levels.
     */
    public static double robustSupportHeight(double flY, double frY, double rlY, double rrY) {
        double[] raw = {flY, frY, rlY, rrY};
        double[] supports = new double[4];
        int count = 0;
        for (double support : raw) {
            if (Double.isFinite(support)) {
                supports[count++] = support;
            }
        }
        if (count == 0) {
            return Double.NaN;
        }
        Arrays.sort(supports, 0, count);
        if (count == 1) {
            return supports[0];
        }
        if (count == 2) {
            return supports[1] - supports[0] > 1.25D
                    ? supports[1]
                    : (supports[0] + supports[1]) * 0.5D;
        }
        if (count == 3) {
            // The median follows the two-contact majority and rejects either one deep hole or
            // one isolated high bump.
            return supports[1];
        }
        if (supports[2] - supports[1] > 1.25D) {
            return (supports[2] + supports[3]) * 0.5D;
        }
        return (supports[1] + supports[2]) * 0.5D;
    }

    /**
     * Keep real one-block steps usable while bounding broken/hole samples before pitch and roll
     * are calculated. Invalid samples inherit the robust center rather than pretending that the
     * vehicle's current Y coordinate is physical ground.
     */
    public static double clampSupportOutlier(double sampleY, double robustCenterY) {
        if (!Double.isFinite(sampleY)) {
            return robustCenterY;
        }
        return Mth.clamp(sampleY, robustCenterY - 1.25D, robustCenterY + 1.25D);
    }

    public Item getDropItem() {
        return GroundworksAsphaltMod.ROAD_ROLLER_ITEM;
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
                if (this.level() instanceof ServerLevel serverLevel) {
                    flushCarriedMaterial(serverLevel);
                }
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
                this.setCompacting(true);
                this.setVibrating(true);
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
            this.setVibrating(false);
            this.setCompacting(false);
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
        // Operator suspension seat in elevated cab: centered laterally (X=0.0m),
        // attachment height 2.41m (seats player buttocks at 2.59m firmly on cushion, head safely below roof ceiling at 3.94m),
        // longitudinal position -0.47m (-7.5 px in model), rotated dynamically with vehicle 3D pitch and roll
        return computeSeatOffset(0.0D, 2.41D, -0.47D, getVehiclePitch(), getVehicleRoll());
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
        // Dismount at the base of the left cab access staircase
        return this.position().add(left.scale(1.9D)).add(forward.scale(-0.7D)).add(0.0D, 0.25D, 0.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, getDropItem());
            }
            this.ejectPassengers();
            flushCarriedMaterial(level);
            this.discard();
            return true;
        }
        this.ejectPassengers();
        flushCarriedMaterial(level);
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

    public void setSteerAngle(float steerAngle) {
        this.entityData.set(STEER_ANGLE, steerAngle);
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getWheelRotation() {
        return this.entityData.get(WHEEL_ROTATION);
    }

    public float getDrumRotation() {
        return this.entityData.get(DRUM_ROTATION);
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

    public void setCompacting(boolean compacting) {
        this.entityData.set(IS_COMPACTING, compacting);
    }

    public boolean isVibrating() {
        return this.entityData.get(IS_VIBRATING);
    }

    public void setVibrating(boolean vibrating) {
        this.entityData.set(IS_VIBRATING, vibrating);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public void setEngineRunning(boolean engineRunning) {
        this.entityData.set(ENGINE_RUNNING, engineRunning);
    }

    public boolean isBraking() {
        return this.entityData.get(IS_BRAKING);
    }

    public void setBraking(boolean braking) {
        this.entityData.set(IS_BRAKING, braking);
    }

    public boolean isWaterSpraying() {
        return this.entityData.get(WATER_SPRAYING);
    }

    public void setWaterSpraying(boolean waterSpraying) {
        this.entityData.set(WATER_SPRAYING, waterSpraying);
    }

    public boolean isLightsOn() {
        return this.entityData.get(LIGHTS_ON);
    }

    public void setLightsOn(boolean lightsOn) {
        this.entityData.set(LIGHTS_ON, lightsOn);
    }

    private void flushCarriedMaterial(ServerLevel level) {
        if (this.carried.isEmpty()) {
            return;
        }

        BlockPos origin = this.blockPosition();
        BlockPos[] depositOrigins = {
                origin, origin.below(), origin.north(), origin.south(), origin.east(), origin.west()
        };
        int[] counts = this.carried.toArray();
        for (int materialId = 1; materialId < counts.length; materialId++) {
            int remaining = counts[materialId];
            GranularMaterial material = GranularMaterialRegistry.byId(materialId);
            if (remaining <= 0 || material == null || material.id() <= 0) {
                continue;
            }
            for (BlockPos depositOrigin : depositOrigins) {
                if (remaining <= 0) {
                    break;
                }
                DepositResult deposited = GroundworksApi.depositWithOverflow(
                        level, depositOrigin, material, remaining
                );
                int accepted = Math.max(0, deposited.unitsDeposited());
                if (accepted > 0) {
                    this.carried.remove(materialId, accepted);
                    remaining -= accepted;
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if ((reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) && !this.carried.isEmpty()) {
                flushCarriedMaterial(serverLevel);
            }
            AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
        }
        super.remove(reason);
    }
}
