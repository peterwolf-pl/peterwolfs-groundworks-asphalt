package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
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
import net.minecraft.world.level.block.Blocks;
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
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTS_ON =
            SynchedEntityData.defineId(CatGraderEntity.class, EntityDataSerializers.BOOLEAN);

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private float inputLift = 0.0F;
    private float inputAux = 0.0F;
    private int inputFreshTicks = 0;

    private float wheelRot = 0.0F;

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
        this.entityData.set(ENGINE_RUNNING, input.getBooleanOr("EngineRunning", false));
        this.entityData.set(LIGHTS_ON, input.getBooleanOr("LightsOn", true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("BladeHeight", this.entityData.get(BLADE_HEIGHT));
        output.putFloat("BladeAngle", this.entityData.get(BLADE_ANGLE));
        output.putFloat("BladeRoll", this.entityData.get(BLADE_ROLL));
        output.putFloat("BladeShift", this.entityData.get(BLADE_SHIFT));
        output.putFloat("RipperLowered", this.entityData.get(RIPPER_LOWERED));
        output.putBoolean("Grading", this.entityData.get(IS_GRADING));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
        output.putBoolean("LightsOn", this.entityData.get(LIGHTS_ON));
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputLift = Mth.clamp(lift, -1.0F, 1.0F);
        this.inputAux = Mth.clamp(aux, -1.0F, 1.0F);
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
                float currentAngle = this.entityData.get(BLADE_ANGLE);
                float newAngle = Mth.clamp(currentAngle + inputAux * 0.05F, -0.65F, 0.65F);
                this.entityData.set(BLADE_ANGLE, newAngle);
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

            float driveSpeed = inputThrottle * 0.16F;
            Vec3 motion = forward.scale(driveSpeed);
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

            // Blade terrain grading logic
            if ((isGrading() || (hasDriver && Math.abs(inputThrottle) > 0.01F))
                    && getBladeHeight() < 0.2F) {
                gradeTerrainWithBlade(serverLevel, forward);
            }
        }
    }

    /**
     * Precision cuts and levels terrain under the mid-mounted grader moldboard.
     */
    private void gradeTerrainWithBlade(ServerLevel level, Vec3 forward) {
        // Blade center: ~0.8m ahead of machine center
        Vec3 bladeCenter = this.position().add(forward.scale(0.8D));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

        double bladeYawOffset = getBladeAngle();
        Vec3 bladeNormal = new Vec3(
                side.x * Math.cos(bladeYawOffset) - forward.x * Math.sin(bladeYawOffset),
                0.0D,
                side.z * Math.cos(bladeYawOffset) - forward.z * Math.sin(bladeYawOffset)
        );

        double[] offsets = {-1.2D, 0.0D, 1.2D};
        boolean scrapedAny = false;

        for (double off : offsets) {
            Vec3 pt = bladeCenter.add(bladeNormal.scale(off));
            BlockPos pos = BlockPos.containing(pt.x, this.getY(), pt.z);

            for (int dy = 1; dy >= -1; dy--) {
                BlockPos targetPos = pos.above(dy);
                BlockState state = level.getBlockState(targetPos);

                if (isGradable(state)) {
                    // Shave uneven dirt, grass, sand
                    if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
                        level.setBlockAndUpdate(targetPos, Blocks.DIRT_PATH.defaultBlockState());
                        scrapedAny = true;
                    } else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
                        level.setBlockAndUpdate(targetPos, Blocks.DIRT.defaultBlockState());
                        scrapedAny = true;
                    }

                    // Cutting edge dust particles
                    level.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            targetPos.getX() + 0.5D, targetPos.getY() + 0.95D, targetPos.getZ() + 0.5D,
                            3,
                            0.2D, 0.05D, 0.2D,
                            0.02D
                    );
                }
            }
        }

        if (scrapedAny && this.tickCount % 8 == 0) {
            level.playSound(
                    null, bladeCenter.x, this.getY(), bladeCenter.z,
                    GroundworksAsphaltMod.GRADER_SCRAPE, SoundSource.BLOCKS,
                    0.85F, 1.0F
            );
        }
    }

    private boolean isGradable(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.MUD);
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
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        // High visibility grader ROPS cab: ~0.4m behind pivot, 2.15m height
        return forward.scale(-0.4D).add(0.0D, 2.15D, 0.0D);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        return this.position().add(left.scale(2.2D)).add(0.0D, 0.25D, 0.0D);
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
}
