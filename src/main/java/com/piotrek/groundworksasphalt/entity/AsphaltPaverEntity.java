package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworks.api.container.IGranularContainer;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Heavy Tracked Asphalt Paver (Rozściełacz Asfaltu) Entity.
 *
 * <p>Equipped with a 14-tonne capacity receiving hopper, dual hydraulic folding wings,
 * twin feeder conveyors, spreading augers, and a heavy 3-block wide floating screed.
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

    // Stored asphalt mix material units
    private int storedAsphaltUnits = 8192; // starts half full

    // Operator control inputs
    private float inputThrottle = 0.0F;
    private float inputSteer = 0.0F;
    private float inputLift = 0.0F;
    private float inputAux = 0.0F;
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
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("StoredAsphaltUnits", this.storedAsphaltUnits);
        output.putFloat("HopperFold", this.entityData.get(HOPPER_FOLD));
        output.putFloat("ScreedHeight", this.entityData.get(SCREED_HEIGHT));
        output.putBoolean("Paving", this.entityData.get(IS_PAVING));
        output.putBoolean("EngineRunning", this.entityData.get(ENGINE_RUNNING));
        output.putBoolean("WorkLightsOn", this.entityData.get(WORK_LIGHTS_ON));
    }

    public void setControlInputs(float throttle, float steer, float lift, float aux, boolean action1, boolean action2) {
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

            Vec3 motion = forward.scale(forwardDrive);
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
        }
    }

    /**
     * Lays a 3-block wide course of fresh hot asphalt behind the floating screed in max 1/8 block layer.
     */
    private void paveAsphaltCourse(ServerLevel level, Vec3 forward) {
        // Screed trailing location: ~2.4m behind machine pivot
        Vec3 screedCenter = this.position().subtract(forward.scale(2.4D));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

        double[] laneOffsets = {-1.0D, 0.0D, 1.0D};
        boolean placedAny = false;

        for (double off : laneOffsets) {
            Vec3 pt = screedCenter.add(side.scale(off));
            BlockPos targetPos = BlockPos.containing(pt.x, this.getY(), pt.z);

            // Find valid paving layer (ground surface)
            BlockPos placementPos = null;
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos candidate = targetPos.above(dy);
                BlockState candidateState = level.getBlockState(candidate);
                BlockState belowState = level.getBlockState(candidate.below());

                // If candidate is already asphalt (fresh or compacted), do not lay another layer (max 1/8 block layer)
                if (candidateState.getBlock() instanceof FreshAsphaltBlock
                        || candidateState.getBlock() instanceof CompactedAsphaltBlock) {
                    break;
                }

                if ((candidateState.isAir() || candidateState.canBeReplaced())
                        && belowState.isSolid()
                        && !(belowState.getBlock() instanceof FreshAsphaltBlock)
                        && !(belowState.getBlock() instanceof CompactedAsphaltBlock)) {
                    placementPos = candidate;
                    break;
                }
            }

            if (placementPos != null && this.storedAsphaltUnits >= UNITS_PER_LAYER) {
                level.setBlockAndUpdate(
                        placementPos,
                        GroundworksAsphaltMod.FRESH_ASPHALT.defaultBlockState().setValue(FreshAsphaltBlock.LAYERS, 1)
                );
                this.storedAsphaltUnits -= UNITS_PER_LAYER;
                updateFillRatio();
                placedAny = true;

                // Hot asphalt steam & smoke emission
                level.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        placementPos.getX() + 0.5D, placementPos.getY() + 0.15D, placementPos.getZ() + 0.5D,
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
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        // Rear operator platform: ~1.2m behind pivot, 1.85m elevation
        return forward.scale(-1.2D).add(0.0D, 1.85D, 0.0D);
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
        return 1.0F;
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
}
