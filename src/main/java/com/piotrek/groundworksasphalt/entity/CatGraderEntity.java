package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularComposition;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
import com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 6-Wheeled Cat Motor Grader (Równiarka Drogowa) Entity.
 *
 * <p>Equipped with a mid-mounted heavy moldboard blade capable of precision cross-slope
 * trimming, height adjustment, circle rotation, side-shifting, and rear multi-shank ripper.
 * Shapes and levels dirt, sand, gravel, and unpaved sub-bases before asphalt laying.
 */
public class CatGraderEntity extends Entity {

    // ── Synched Entity Data ───────────────────────────────────────────
    private static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_LEAN =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_ROTATION =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> BLADE_HEIGHT =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLADE_ANGLE =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLADE_ROLL =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLADE_SHIFT =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> RIPPER_LOWERED =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_GRADING =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ROLL_MODE =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_AUTO_LEVEL =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTS_ON =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);

    // Work lights placed in the world
    private final List<BlockPos> workLights = new ArrayList<>();

    private static final int MAX_BLADE_UNITS = 4096;
    private static final int CUT_UNITS_PER_COLUMN = 256;
    private static final int FILL_UNITS_PER_COLUMN = 192;

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private float inputLift = 0.0F;
    private float inputAux = 0.0F;
    private boolean inputHorn = false;
    private int hornCooldown = 0;
    private boolean lastAuxMode = false;
    private int inputFreshTicks = 0;

    private float wheelRot = 0.0F;
    private final GranularComposition carried = new GranularComposition();
    private double smoothedGradeBaselineY = Double.NaN;

    public CatGraderEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STEER_ANGLE, 0.0F);
        builder.define(WHEEL_LEAN, 0.0F);
        builder.define(FORWARD_SPEED, 0.0F);
        builder.define(WHEEL_ROTATION, 0.0F);
        builder.define(VEHICLE_PITCH, 0.0F);
        builder.define(VEHICLE_ROLL, 0.0F);
        builder.define(BLADE_HEIGHT, 0.0F);
        builder.define(BLADE_ANGLE, 0.0F);
        builder.define(BLADE_ROLL, 0.0F);
        builder.define(BLADE_SHIFT, 0.0F);
        builder.define(RIPPER_LOWERED, 0.0F);
        builder.define(IS_GRADING, false);
        builder.define(IS_ROLL_MODE, false);
        builder.define(IS_AUTO_LEVEL, false);
        builder.define(ENGINE_RUNNING, false);
        builder.define(LIGHTS_ON, true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(BLADE_HEIGHT, input.getFloatOr("BladeHeight", 0.0F));
        this.entityData.set(BLADE_ANGLE, input.getFloatOr("BladeAngle", 0.0F));
        this.entityData.set(BLADE_ROLL, input.getFloatOr("BladeRoll", 0.0F));
        this.entityData.set(BLADE_SHIFT, input.getFloatOr("BladeShift", 0.0F));
        this.entityData.set(RIPPER_LOWERED, input.getFloatOr("RipperLowered", 0.0F));
        this.entityData.set(IS_GRADING, input.getBooleanOr("Grading", false));
        this.entityData.set(IS_ROLL_MODE, input.getBooleanOr("RollMode", false));
        this.entityData.set(IS_AUTO_LEVEL, input.getBooleanOr("AutoLevel", false));
        this.entityData.set(ENGINE_RUNNING, input.getBooleanOr("EngineRunning", false));
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
        output.putFloat("BladeHeight", this.entityData.get(BLADE_HEIGHT));
        output.putFloat("BladeAngle", this.entityData.get(BLADE_ANGLE));
        output.putFloat("BladeRoll", this.entityData.get(BLADE_ROLL));
        output.putFloat("BladeShift", this.entityData.get(BLADE_SHIFT));
        output.putFloat("RipperLowered", this.entityData.get(RIPPER_LOWERED));
        output.putBoolean("Grading", this.entityData.get(IS_GRADING));
        output.putBoolean("RollMode", this.entityData.get(IS_ROLL_MODE));
        output.putBoolean("AutoLevel", this.entityData.get(IS_AUTO_LEVEL));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
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
        setControlInputs(throttle, steer, lift, aux, action1, action2, false, false);
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2, boolean horn, boolean auxMode) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputLift = Mth.clamp(lift, -1.0F, 1.0F);
        this.inputAux = Mth.clamp(aux, -1.0F, 1.0F);
        this.inputHorn = horn;

        if (auxMode && !this.lastAuxMode) {
            boolean newMode = !isRollMode();
            setRollMode(newMode);
            if (this.getControllingPassenger() instanceof ServerPlayer player) {
                if (newMode) {
                    player.sendSystemMessage(
                            Component.translatable("message.pw_groundworks_asphalt.grader_blade_mode_roll"),
                            true
                    );
                } else {
                    player.sendSystemMessage(
                            Component.translatable("message.pw_groundworks_asphalt.grader_blade_mode_angle"),
                            true
                    );
                }
            }
        }
        this.lastAuxMode = auxMode;

        if (action1) {
            setGrading(!isGrading());
        }
        if (action2) {
            float rip = getRipperLowered() > 0.5F ? 0.0F : 1.0F;
            this.entityData.set(RIPPER_LOWERED, rip);
        }
        this.inputFreshTicks = 10;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            LivingEntity driver = this.getControllingPassenger();
            boolean hasDriver = driver != null;

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

            // Horn sound playback
            if (this.hornCooldown > 0) {
                this.hornCooldown--;
            }
            if (this.inputHorn && this.hornCooldown <= 0) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        GroundworksAsphaltMod.VEHICLE_HORN,
                        SoundSource.PLAYERS,
                        1.5F, 0.85F
                );
                this.hornCooldown = 14;
            }

            // Front steering angle & wheel leaning
            float targetSteer = inputSteer * 0.45F;
            float currentSteer = this.entityData.get(STEER_ANGLE);
            float newSteer = Mth.approach(currentSteer, targetSteer, 0.06F);
            this.entityData.set(STEER_ANGLE, newSteer);
            this.entityData.set(WHEEL_LEAN, newSteer * 0.5F); // Leaning wheels into turn

            // Blade controls
            if (Math.abs(inputLift) > 0.01F) {
                float currentHeight = this.entityData.get(BLADE_HEIGHT);
                float newHeight = Mth.clamp(currentHeight + inputLift * 0.03F, -0.6F, 0.8F);
                this.entityData.set(BLADE_HEIGHT, newHeight);
            }

            if (Math.abs(inputAux) > 0.01F) {
                if (isRollMode()) {
                    if (isAutoLevel()) {
                        setAutoLevel(false);
                        if (driver instanceof ServerPlayer player) {
                            player.sendSystemMessage(
                                    Component.translatable("message.pw_groundworks_asphalt.grader_autolevel_disabled_manual"),
                                    true
                            );
                        }
                    }
                    float currentRoll = this.entityData.get(BLADE_ROLL);
                    float newRoll = Mth.clamp(currentRoll + inputAux * 0.03F, -0.50F, 0.50F);
                    this.entityData.set(BLADE_ROLL, newRoll);
                } else {
                    float currentAngle = this.entityData.get(BLADE_ANGLE);
                    float newAngle = Mth.clamp(currentAngle + inputAux * 0.05F, -0.65F, 0.65F);
                    this.entityData.set(BLADE_ANGLE, newAngle);
                }
            }

            // Heading & forward speed
            if (Math.abs(inputThrottle) > 0.01F && Math.abs(newSteer) > 0.001F) {
                float yawDelta = newSteer * inputThrottle * 2.5F;
                this.setYRot(Mth.wrapDegrees(this.getYRot() + yawDelta));
                this.setYHeadRot(this.getYRot());
                this.setYBodyRot(this.getYRot());
            }

            double yawRad = Math.toRadians(this.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

            // Ground contact sampling for chassis orientation and elevation
            // Front axle steering pivot: ~3.1m ahead
            // Rear tandem drive bogies: ~2.9m behind
            // Half gauge is 1.1m
            Vec3 frontCenter = this.position().add(forward.scale(3.1D));
            Vec3 rearCenter = this.position().subtract(forward.scale(2.9D));

            double maxAllowedY = this.getY() + 2.2D;
            double flY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.subtract(side.scale(1.1D)), maxAllowedY);
            double frY = RoadRollerEntity.sampleGroundHeight(serverLevel, frontCenter.add(side.scale(1.1D)), maxAllowedY);
            double rlY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.subtract(side.scale(1.1D)), maxAllowedY);
            double rrY = RoadRollerEntity.sampleGroundHeight(serverLevel, rearCenter.add(side.scale(1.1D)), maxAllowedY);

            // A partial granular cell can leave one or more wheel probes without a local column.
            // Never average NaN into the grader suspension: use the same robust support cluster as
            // the roller, then bound individual contacts before pitch/roll interpolation.
            double actualPhysicalGroundY = RoadRollerEntity.robustSupportHeight(flY, frY, rlY, rrY);
            boolean hasPhysicalSupport = Double.isFinite(actualPhysicalGroundY);
            if (!hasPhysicalSupport) {
                actualPhysicalGroundY = this.getY();
            }

            flY = RoadRollerEntity.clampSupportOutlier(flY, actualPhysicalGroundY);
            frY = RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
            rlY = RoadRollerEntity.clampSupportOutlier(rlY, actualPhysicalGroundY);
            rrY = RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

            // Edge-isolation: if grader drives near the edge of an embankment (drop) or trench (wall),
            // do not allow a wheel over the edge to tilt the chassis or camber calculation.
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

            double targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, 6.0D));
            double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 2.2D));

            targetPitch = Mth.clamp(targetPitch, -35.0D, 35.0D);
            targetRoll = Mth.clamp(targetRoll, -15.0D, 15.0D);

            float currentPitch = this.entityData.get(VEHICLE_PITCH);
            float currentRoll = this.entityData.get(VEHICLE_ROLL);
            float newPitch = (float) Mth.lerp(0.2D, currentPitch, targetPitch);
            float newRoll = (float) Mth.lerp(0.2D, currentRoll, targetRoll);
            this.entityData.set(VEHICLE_PITCH, newPitch);
            this.entityData.set(VEHICLE_ROLL, newRoll);

            // Grader autolevel cross-slope compensation:
            // When terrain is banked/sloped, moldboard rotates inversely to vehicle camber
            // so the cutting edge remains perfectly level with the world horizon.
            // Edge-isolated contact heights ensure drop-offs or trench walls do NOT alter blade camber!
            double frontCamber = Mth.clamp((effFrY - effFlY) / 2.2D, -0.08D, 0.08D);
            double rearCamber = Mth.clamp((effRrY - effRlY) / 2.2D, -0.08D, 0.08D);
            double baselineCamber = Mth.clamp(rearCamber + (frontCamber - rearCamber) * (3.75D / 6.0D), -0.08D, 0.08D);
            if (isAutoLevel()) {
                float targetBladeRoll = (float) Math.atan(baselineCamber);
                targetBladeRoll = Mth.clamp(targetBladeRoll, -0.50F, 0.50F);
                float curBladeRoll = this.entityData.get(BLADE_ROLL);
                this.entityData.set(BLADE_ROLL, Mth.approach(curBladeRoll, targetBladeRoll, 0.04F));
            }

            float driveSpeed = inputThrottle * 0.16F;
            double targetGroundY = rearAvg + (frontAvg - rearAvg) * (2.9D / 6.0D);
            double dy = RoadRollerEntity.calculateSmoothVerticalStep(targetGroundY, this.getY(), driveSpeed, this.onGround());
            Vec3 motion = new Vec3(forward.x * driveSpeed, dy, forward.z * driveSpeed);

            Vec3 beforeMove = this.position();
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);

            Vec3 moved = this.position().subtract(beforeMove);
            float fwdSpeed = (float) Math.sqrt(moved.x * moved.x + moved.z * moved.z);
            this.entityData.set(FORWARD_SPEED, fwdSpeed);

            if (fwdSpeed > 0.001F || Math.abs(driveSpeed) > 0.001F) {
                float rotSpeed = fwdSpeed > 0.001F ? fwdSpeed : Math.abs(driveSpeed);
                float rotDelta = rotSpeed * 1.5F * (inputThrottle < 0 ? -1.0F : 1.0F);
                this.wheelRot += rotDelta;
                this.entityData.set(WHEEL_ROTATION, this.wheelRot);
            }

            if (hasDriver || fwdSpeed > 0.001F || Math.abs(inputThrottle) > 0.01F || Math.abs(inputSteer) > 0.01F) {
                this.syncPosition = true;
                this.needsSync = true;
                this.syncVelocity = true;
            }

            // Blade grades whenever it is down, including a stationary operator
            // holding the moldboard in a cut. Driving is not required.
            if (hasDriver && getBladeHeight() < 0.35F) {
                gradeTerrainWithBlade(serverLevel, forward, side, frontAvg, rearAvg, effFlY, effFrY, effRlY, effRrY);
            } else {
                setGrading(false);
            }

            // Update worklights
            if (this.isEngineRunning() && this.isLightsOn()) {
                // Front frame nose headlights
                List<BlockPos> beams = AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, 3.8D, 1.3D, 1.2D, new int[]{2, 5, 9, 14}, false
                );
                // Moldboard / cab work lights
                beams.addAll(AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, 0.2D, 1.4D, 2.8D, new int[]{2, 5, 8}, false
                ));
                // Rear work lamps
                beams.addAll(AsphaltVehicleLights.calculateBeams(
                        this.position(), forward, side, -3.2D, 1.6D, 1.5D, new int[]{2, 5, 8}, true
                ));
                AsphaltVehicleLights.syncLightBlocks(serverLevel, this.workLights, beams, AsphaltVehicleLights.HEAD_LIGHT_LEVEL);
            } else if (!this.workLights.isEmpty()) {
                AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
            }
        }
    }

    /**
     * Precision cut and fill under the mid-mounted moldboard.
     * Computes the roadbed baseline grade by interpolating the sampled ground heights
     * between the rear tandem bogies and the front steering axle (wheelbase averaging).
     * Material above the cutting grade is shaved into the blade; hollows below
     * that grade are filled from the blade so the pass leaves a level sub-base.
     */
    private void gradeTerrainWithBlade(
            ServerLevel level,
            Vec3 forward,
            Vec3 side,
            double frontAvg,
            double rearAvg,
            double flY,
            double frY,
            double rlY,
            double rrY
    ) {
        // Lookahead ground sampling 4.8m ahead of pivot to detect approaching slopes, crests, and ramps
        Vec3 lookaheadPos = this.position().add(forward.scale(4.8D));
        double maxLookaheadY = this.getY() + 2.5D;
        double lookaheadY = RoadRollerEntity.sampleGroundHeight(level, lookaheadPos, maxLookaheadY, true, this.getY());

        // Blade center is 0.85m ahead of pivot center.
        // Wheelbase: rear axle is at -2.9m, front axle is at +3.1m (distance = 6.0m).
        // Distance from rear axle to blade = 0.85 - (-2.9) = 3.75m.
        // Interpolation ratio: t = 3.75 / 6.0 = 0.625.
        double tBlade = 3.75D / 6.0D;
        double directGradeY = rearAvg + (frontAvg - rearAvg) * tBlade;

        // Adjacent pass / roadbed corridor datum lock:
        // If wheels on one side are driving on an already GRADED or COMPACTED roadbed corridor,
        // use the recorded roadbed mesh as the authoritative datum for that side.
        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage corridor =
                com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.get(level);
        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry flEntry =
                corridor.getEntry(BlockPos.containing(this.position().add(forward.scale(3.1D)).subtract(side.scale(1.1D))));
        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry frEntry =
                corridor.getEntry(BlockPos.containing(this.position().add(forward.scale(3.1D)).add(side.scale(1.1D))));
        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry rlEntry =
                corridor.getEntry(BlockPos.containing(this.position().subtract(forward.scale(2.9D)).subtract(side.scale(1.1D))));
        com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry rrEntry =
                corridor.getEntry(BlockPos.containing(this.position().subtract(forward.scale(2.9D)).add(side.scale(1.1D))));

        boolean leftIsRoadbed = (flEntry != null && flEntry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED))
                || (rlEntry != null && rlEntry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED));
        boolean rightIsRoadbed = (frEntry != null && frEntry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED))
                || (rrEntry != null && rrEntry.stage().isAtLeast(com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED));

        if (leftIsRoadbed && !rightIsRoadbed) {
            double leftRefY = (flEntry != null ? flEntry.targetY() : (rlEntry != null ? rlEntry.targetY() : flY));
            if (Math.abs(leftRefY - directGradeY) < 1.0D) {
                directGradeY = (directGradeY * 0.3D) + (leftRefY * 0.7D);
            }
        } else if (rightIsRoadbed && !leftIsRoadbed) {
            double rightRefY = (frEntry != null ? frEntry.targetY() : (rrEntry != null ? rrEntry.targetY() : frY));
            if (Math.abs(rightRefY - directGradeY) < 1.0D) {
                directGradeY = (directGradeY * 0.3D) + (rightRefY * 0.7D);
            }
        }

        // When approaching or climbing an elevation change, blend in lookahead so the blade profiles
        // a smooth, continuous vertical ramp curve instead of an abrupt step.
        // Clamp extendedRampSlope to standard road grades (-15% to +15%) so an embankment cliff
        // or trench wall detected by lookahead does not plunge the blade.
        double targetBaselineGradeY = directGradeY;
        double extendedRampSlope = 0.0D;
        if (Math.abs(lookaheadY - frontAvg) > 0.06D && Math.abs(lookaheadY - frontAvg) < 1.25D) {
            extendedRampSlope = Mth.clamp((lookaheadY - rearAvg) / 7.7D, -0.15D, 0.15D);
            double projectedRampY = rearAvg + extendedRampSlope * 3.75D;
            targetBaselineGradeY = (directGradeY * 0.40D) + (projectedRampY * 0.60D);
        } else {
            extendedRampSlope = Mth.clamp((frontAvg - rearAvg) / 6.0D, -0.15D, 0.15D);
        }

        Vec3 bladeCenter = this.position().add(forward.scale(0.85D));
        double bladeYawOffset = getBladeAngle();
        Vec3 alongBlade = new Vec3(
                side.x * Math.cos(bladeYawOffset) - forward.x * Math.sin(bladeYawOffset),
                0.0D,
                side.z * Math.cos(bladeYawOffset) - forward.z * Math.sin(bladeYawOffset)
        );
        double bladeRoll = getBladeRoll();
        double bladeHeight = getBladeHeight();

        // Cross slope / camber of existing roadbed (clamped to realistic road cross-slope)
        double frontCamber = Mth.clamp((frY - flY) / 2.2D, -0.08D, 0.08D);
        double rearCamber = Mth.clamp((rrY - rlY) / 2.2D, -0.08D, 0.08D);
        double baselineCamber = Mth.clamp(rearCamber + (frontCamber - rearCamber) * tBlade, -0.08D, 0.08D);

        // Option B: Survey alignment corridor
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget survey =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, bladeCenter.x, bladeCenter.z);

        if (survey.inCorridor()) {
            targetBaselineGradeY = survey.targetY();
            extendedRampSlope = survey.slopeX() * forward.x + survey.slopeZ() * forward.z;
            baselineCamber = survey.slopeX() * alongBlade.x + survey.slopeZ() * alongBlade.z;
        }

        if (Double.isNaN(smoothedGradeBaselineY) || Math.abs(this.getY() - smoothedGradeBaselineY) > 2.0D) {
            smoothedGradeBaselineY = targetBaselineGradeY;
        } else {
            smoothedGradeBaselineY = Mth.lerp(0.22D, smoothedGradeBaselineY, targetBaselineGradeY);
        }
        double baselineGradeY = smoothedGradeBaselineY;

        final double finalRampSlope = extendedRampSlope;
        final double finalBaselineCamber = baselineCamber;
        RoadPlanarShaper.PlanarSurfaceFunction bladeCutPlane = (wx, wz) -> {
            com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                    com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, wx, wz);
            if (ptSurvey.inCorridor()) {
                return ptSurvey.targetY() + bladeHeight * 0.35D;
            }
            double dx = wx - bladeCenter.x;
            double dz = wz - bladeCenter.z;
            double u = dx * alongBlade.x + dz * alongBlade.z;
            double v = dx * forward.x + dz * forward.z;
            return baselineGradeY + v * finalRampSlope + u * (finalBaselineCamber - Math.tan(bladeRoll)) + bladeHeight * 0.35D;
        };

        // Blade length is 4 blocks (4.0m) wide: sample across 11 points from -2.0m to +2.0m
        // Outward center-first scan from blade center (off = 0.0D) to the edges.
        // Stop extending when reaching terrain beyond the road edge (cliff drop-off or trench wall).
        double[] rightBladeOffsets = {0.4D, 0.8D, 1.2D, 1.6D, 2.0D};
        double[] leftBladeOffsets = {-0.4D, -0.8D, -1.2D, -1.6D, -2.0D};
        Set<BlockPos> candidatePositions = new java.util.LinkedHashSet<>();

        // Center point (off = 0.0D)
        double cutY0 = bladeCutPlane.getY(bladeCenter.x, bladeCenter.z);
        int baseBlockY0 = Mth.floor(cutY0);
        for (int dy = 2; dy >= 0; dy--) {
            candidatePositions.add(new BlockPos(Mth.floor(bladeCenter.x), baseBlockY0 + dy, Mth.floor(bladeCenter.z)));
        }

        // Outward right
        for (double off : rightBladeOffsets) {
            Vec3 pt = bladeCenter.add(alongBlade.scale(off));
            if (survey.inCorridor()) {
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, pt.x, pt.z);
                if (!ptSurvey.inCorridor()) {
                    break;
                }
            }
            double cutY = bladeCutPlane.getY(pt.x, pt.z);
            double sampleGroundY = RoadRollerEntity.sampleGroundHeight(level, pt, baselineGradeY + 2.5D);
            if (sampleGroundY < cutY - 0.50D || sampleGroundY > cutY + 0.65D) {
                break;
            }
            int baseBlockY = Mth.floor(cutY);
            for (int dy = 2; dy >= 0; dy--) {
                candidatePositions.add(new BlockPos(Mth.floor(pt.x), baseBlockY + dy, Mth.floor(pt.z)));
            }
        }

        // Outward left
        for (double off : leftBladeOffsets) {
            Vec3 pt = bladeCenter.add(alongBlade.scale(off));
            if (survey.inCorridor()) {
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget ptSurvey =
                        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(level, pt.x, pt.z);
                if (!ptSurvey.inCorridor()) {
                    break;
                }
            }
            double cutY = bladeCutPlane.getY(pt.x, pt.z);
            double sampleGroundY = RoadRollerEntity.sampleGroundHeight(level, pt, baselineGradeY + 2.5D);
            if (sampleGroundY < cutY - 0.50D || sampleGroundY > cutY + 0.65D) {
                break;
            }
            int baseBlockY = Mth.floor(cutY);
            for (int dy = 2; dy >= 0; dy--) {
                candidatePositions.add(new BlockPos(Mth.floor(pt.x), baseBlockY + dy, Mth.floor(pt.z)));
            }
        }

        boolean worked = false;
        BlockPos particlePos = null;

        for (BlockPos cell : candidatePositions) {
            BlockState candidateState = level.getBlockState(cell);
            if (RoadRollerEntity.isVegetationOrReplaceable(candidateState)) {
                level.destroyBlock(cell, false, this);
            }

            GranularMaterial fillMat = !carried.isEmpty()
                    ? GranularMaterialRegistry.byId(carried.dominantMaterialId())
                    : GranularMaterialRegistry.GRAVEL;
            int offer = Math.min(FILL_UNITS_PER_COLUMN, carried.unitsOf(fillMat.id()));

            double effectiveCrossSlope = baselineCamber - Math.tan(bladeRoll);
            double slopeX = survey.inCorridor() ? survey.slopeX() : (alongBlade.x * effectiveCrossSlope + forward.x * finalRampSlope);
            double slopeZ = survey.inCorridor() ? survey.slopeZ() : (alongBlade.z * effectiveCrossSlope + forward.z * finalRampSlope);

            RoadPlanarShaper.ShapeResult result = RoadPlanarShaper.shapeCellToSurface(
                    level,
                    cell,
                    bladeCutPlane,
                    fillMat,
                    offer,
                    false,
                    Integer.MAX_VALUE,
                    com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage.GRADED,
                    slopeX,
                    slopeZ
            );

            if (result.changed()) {
                worked = true;
                particlePos = cell;
                if (result.unitsExcavated() > 0) {
                    if (result.material() != null && result.material().id() > 0) {
                        if (carried.totalUnits() < MAX_BLADE_UNITS) {
                            carried.add(result.material(), result.unitsExcavated());
                        } else {
                            spillWindrow(level, bladeCenter, alongBlade, bladeYawOffset);
                        }
                    }
                }
                if (result.unitsDeposited() > 0) {
                    carried.remove(fillMat.id(), result.unitsDeposited());
                }
            }
        }

        // Spill windrow if blade is carrying excess or raised
        if (getBladeHeight() > 0.35F && !carried.isEmpty()) {
            spillWindrow(level, bladeCenter, alongBlade, bladeYawOffset);
        }

        setGrading(worked);

        if (worked && this.tickCount % 6 == 0) {
            if (particlePos != null) {
                BlockState state = level.getBlockState(particlePos);
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        particlePos.getX() + 0.5D, baselineGradeY + 0.05D, particlePos.getZ() + 0.5D,
                        6,
                        0.35D, 0.04D, 0.35D,
                        0.02D
                );
            }
            level.playSound(
                    null, bladeCenter.x, baselineGradeY, bladeCenter.z,
                    GroundworksAsphaltMod.GRADER_SCRAPE, SoundSource.BLOCKS,
                    0.85F, 0.9F
            );
        }
    }

    private void spillWindrow(ServerLevel level, Vec3 bladeCenter, Vec3 alongBlade, double bladeYawOffset) {
        if (carried.isEmpty()) {
            return;
        }
        GranularMaterial material = GranularMaterialRegistry.byId(carried.dominantMaterialId());
        if (material == null || material.id() == 0) {
            return;
        }
        // Material rolls off the trailing heel of the blade (at +/- 2.2m along the 4.0m blade)
        double heelOffset = (bladeYawOffset >= 0.0D) ? 2.2D : -2.2D;
        Vec3 dump = bladeCenter.add(alongBlade.scale(heelOffset));
        BlockPos pos = BlockPos.containing(dump.x, this.getY(), dump.z);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos test = pos.above(dy);
            BlockState st = level.getBlockState(test);
            if (!st.isAir() || !level.getBlockState(test.below()).isAir()) {
                pos = test;
                break;
            }
        }
        int offer = Math.min(48, carried.unitsOf(material));
        DepositResult placed = GroundworksApi.deposit(level, pos, material, offer);
        if (placed.success()) {
            carried.remove(material.id(), placed.unitsDeposited());
        }
    }

    public Item getDropItem() {
        return GroundworksAsphaltMod.CAT_GRADER_ITEM;
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
        // Operator suspension seat: centered laterally (X=0.0m), rider attachment height 1.47m above ground
        // (seats player buttocks at 1.65m directly on the seat cushion, head safely below cab roof at 3.06m),
        // longitudinal position -0.875m (-14 px in model), oriented with vehicle 3D pitch and roll
        return computeSeatOffset(0.0D, 1.47D, -0.875D, getVehiclePitch(), getVehicleRoll());
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
        // Dismount onto left walkway and access ladder
        return this.position().add(left.scale(1.8D)).add(forward.scale(-1.0D)).add(0.0D, 0.25D, 0.0D);
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

    public void setSteerAngle(float steerAngle) {
        this.entityData.set(STEER_ANGLE, steerAngle);
    }

    public float getWheelLean() {
        return this.entityData.get(WHEEL_LEAN);
    }

    public void setWheelLean(float wheelLean) {
        this.entityData.set(WHEEL_LEAN, wheelLean);
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getWheelRotation() {
        return this.entityData.get(WHEEL_ROTATION);
    }

    public float getVehiclePitch() {
        return this.entityData.get(VEHICLE_PITCH);
    }

    public float getVehicleRoll() {
        return this.entityData.get(VEHICLE_ROLL);
    }

    public float getBladeHeight() {
        return this.entityData.get(BLADE_HEIGHT);
    }

    public void setBladeHeight(float bladeHeight) {
        this.entityData.set(BLADE_HEIGHT, bladeHeight);
    }

    public float getBladeAngle() {
        return this.entityData.get(BLADE_ANGLE);
    }

    public void setBladeAngle(float bladeAngle) {
        this.entityData.set(BLADE_ANGLE, bladeAngle);
    }

    public float getBladeRoll() {
        return this.entityData.get(BLADE_ROLL);
    }

    public void setBladeRoll(float bladeRoll) {
        this.entityData.set(BLADE_ROLL, bladeRoll);
    }

    public float getBladeShift() {
        return this.entityData.get(BLADE_SHIFT);
    }

    public void setBladeShift(float bladeShift) {
        this.entityData.set(BLADE_SHIFT, bladeShift);
    }

    public float getRipperLowered() {
        return this.entityData.get(RIPPER_LOWERED);
    }

    public void setRipperLowered(float ripperLowered) {
        this.entityData.set(RIPPER_LOWERED, ripperLowered);
    }

    public boolean isGrading() {
        return this.entityData.get(IS_GRADING);
    }

    public void setGrading(boolean grading) {
        this.entityData.set(IS_GRADING, grading);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public void setEngineRunning(boolean engineRunning) {
        this.entityData.set(ENGINE_RUNNING, engineRunning);
    }

    public boolean isLightsOn() {
        return this.entityData.get(LIGHTS_ON);
    }

    public void setLightsOn(boolean lightsOn) {
        this.entityData.set(LIGHTS_ON, lightsOn);
    }

    public boolean isRollMode() {
        return this.entityData.get(IS_ROLL_MODE);
    }

    public void setRollMode(boolean rollMode) {
        this.entityData.set(IS_ROLL_MODE, rollMode);
    }

    public boolean isAutoLevel() {
        return this.entityData.get(IS_AUTO_LEVEL);
    }

    public void setAutoLevel(boolean autoLevel) {
        this.entityData.set(IS_AUTO_LEVEL, autoLevel);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            AsphaltVehicleLights.clearLights(serverLevel, this.workLights);
        }
        super.remove(reason);
    }
}
