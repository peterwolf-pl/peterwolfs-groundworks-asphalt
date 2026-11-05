package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private int inputFreshTicks = 0;

    private float wheelRot = 0.0F;
    private float drumRot = 0.0F;

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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(STEER_ANGLE, input.getFloatOr("SteerAngle", 0.0F));
        this.entityData.set(IS_COMPACTING, input.getBooleanOr("Compacting", true));
        this.entityData.set(IS_VIBRATING, input.getBooleanOr("Vibrating", true));
        this.entityData.set(ENGINE_RUNNING, input.getBooleanOr("EngineRunning", false));
        this.entityData.set(WATER_SPRAYING, input.getBooleanOr("WaterSpraying", true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("SteerAngle", this.entityData.get(STEER_ANGLE));
        output.putBoolean("Compacting", this.entityData.get(IS_COMPACTING));
        output.putBoolean("Vibrating", this.entityData.get(IS_VIBRATING));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
        output.putBoolean("WaterSpraying", this.entityData.get(WATER_SPRAYING));
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        if (action1) {
            setVibrating(!isVibrating());
        }
        if (action2) {
            setWaterSpraying(!isWaterSpraying());
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

            float targetSpeed = inputThrottle * 0.12F;
            Vec3 motion = forward.scale(targetSpeed);

            // Gravity
            if (!this.onGround()) {
                motion = motion.add(0.0D, -0.25D, 0.0D);
            } else {
                motion = motion.add(0.0D, -0.04D, 0.0D);
            }

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

            // Compaction logic
            if (isCompacting() && (hasDriver || fwdSpeed > 0.005F)) {
                compactUnderDrum(serverLevel, forward);
            }
        }
    }

    private void compactUnderDrum(ServerLevel level, Vec3 forward) {
        // Front drum contact point: ~1.8m ahead of pivot center
        Vec3 drumCenter = this.position().add(forward.scale(1.8D));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

        double[] drumOffsets = {-0.75D, 0.0D, 0.75D};

        for (double off : drumOffsets) {
            Vec3 checkPt = drumCenter.add(side.scale(off));
            BlockPos posAtDrum = BlockPos.containing(checkPt.x, this.getY(), checkPt.z);

            // Check surface and ground beneath
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos targetPos = posAtDrum.above(dy);
                if (level.getBlockState(targetPos).getBlock() instanceof FreshAsphaltBlock) {
                    FreshAsphaltBlock.compact(level, targetPos);

                    // Compaction effects
                    level.sendParticles(
                            ParticleTypes.POOF,
                            targetPos.getX() + 0.5D, targetPos.getY() + 0.15D, targetPos.getZ() + 0.5D,
                            3,
                            0.25D, 0.05D, 0.25D,
                            0.02D
                    );

                    if (isWaterSpraying()) {
                        level.sendParticles(
                                ParticleTypes.SPLASH,
                                targetPos.getX() + 0.5D, targetPos.getY() + 0.15D, targetPos.getZ() + 0.5D,
                                4,
                                0.2D, 0.02D, 0.2D,
                                0.01D
                        );
                    }

                    if (this.tickCount % 6 == 0) {
                        level.playSound(
                                null, targetPos,
                                GroundworksAsphaltMod.ROLLER_VIBRATE, SoundSource.BLOCKS,
                                0.75F, 0.85F
                        );
                    }
                }
            }
        }
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
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        // Cab seat location: slightly behind pivot center, raised 1.6m
        return forward.scale(-0.4D).add(0.0D, 1.6D, 0.0D);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        return this.position().add(left.scale(1.8D)).add(0.0D, 0.25D, 0.0D);
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
}
