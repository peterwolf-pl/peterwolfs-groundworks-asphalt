package com.piotrek.groundworksasphalt.entity;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Road Survey Stake entity (Tyczka geodezyjna drogowa) for Option B alignment engineering.
 * Marks target elevation benchmarks, corridor boundary width, and crown cross-slope.
 */
public class RoadStakeEntity extends Entity {

    private static final EntityDataAccessor<Float> CORRIDOR_WIDTH =
            SynchedEntityData.defineId(RoadStakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CROWN_PERCENT =
            SynchedEntityData.defineId(RoadStakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ELEVATION_OFFSET =
            SynchedEntityData.defineId(RoadStakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW =
            SynchedEntityData.defineId(RoadStakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HAS_CUSTOM_YAW =
            SynchedEntityData.defineId(RoadStakeEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float DEFAULT_CORRIDOR_WIDTH = 8.0F; // 8 meters total (4m left, 4m right)
    public static final float DEFAULT_CROWN_PERCENT = 0.0F;   // Flat cross-section (0.0% crown slope)

    public RoadStakeEntity(EntityType<? extends RoadStakeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CORRIDOR_WIDTH, DEFAULT_CORRIDOR_WIDTH);
        builder.define(CROWN_PERCENT, DEFAULT_CROWN_PERCENT);
        builder.define(ELEVATION_OFFSET, 0.0F);
        builder.define(YAW, 0.0F);
        builder.define(HAS_CUSTOM_YAW, false);
    }

    public float getYaw() {
        return this.entityData.get(YAW);
    }

    public boolean hasCustomYaw() {
        return this.entityData.get(HAS_CUSTOM_YAW);
    }

    public void setYaw(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        this.entityData.set(YAW, normalized);
        this.entityData.set(HAS_CUSTOM_YAW, true);
        this.setYRot(normalized);
        this.yRotO = normalized;
    }

    public void clearCustomYaw() {
        this.entityData.set(HAS_CUSTOM_YAW, false);
    }

    public float getCorridorWidth() {
        return this.entityData.get(CORRIDOR_WIDTH);
    }

    public void setCorridorWidth(float width) {
        this.entityData.set(CORRIDOR_WIDTH, Math.max(2.0F, width));
        updateLabel();
    }

    public float getCrownPercent() {
        return this.entityData.get(CROWN_PERCENT);
    }

    public void setCrownPercent(float percent) {
        this.entityData.set(CROWN_PERCENT, Math.max(0.0F, percent));
        updateLabel();
    }

    public float getElevationOffset() {
        return this.entityData.get(ELEVATION_OFFSET);
    }

    public void setElevationOffset(float offset) {
        this.entityData.set(ELEVATION_OFFSET, offset);
        updateLabel();
    }

    public double getBenchmarkY() {
        return this.getY() + getElevationOffset();
    }

    public void updateLabel() {
        String yawStr = hasCustomYaw() ? String.format("%.0f°", getYaw()) : "Auto (Krzywa)";
        if (getCrownPercent() > 0.0F) {
            this.setCustomName(Component.literal(String.format(
                    "§6Tyczka Drogowa §7[Y: §b%.2f§7, Kierunek: §e%s§7, Szer: §e%.1fm§7, Korona: §e%.1f%%§7]",
                    getBenchmarkY(), yawStr, getCorridorWidth(), getCrownPercent()
            )));
        } else {
            this.setCustomName(Component.literal(String.format(
                    "§6Tyczka Drogowa §7[Y: §b%.2f§7, Kierunek: §e%s§7, Szer: §e%.1fm§7, Płaska]",
                    getBenchmarkY(), yawStr, getCorridorWidth()
            )));
        }
        this.setCustomNameVisible(true);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        this.dropAndDiscard(level);
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean isTool = !held.isEmpty() && (
                held.is(net.minecraft.tags.ItemTags.SHOVELS)
                || held.is(net.minecraft.tags.ItemTags.PICKAXES)
                || held.is(net.minecraft.tags.ItemTags.AXES)
                || held.is(net.minecraft.world.item.Items.STICK)
        );

        if (isTool) {
            // Rotate direction: +15 degrees (or +45 with Shift)
            float step = player.isShiftKeyDown() ? 45.0F : 15.0F;
            float newYaw = (getYaw() + step) % 360.0F;
            setYaw(newYaw);
            updateLabel();

            if (!this.level().isClientSide()) {
                player.sendSystemMessage(Component.literal(String.format(
                        "§6[Tyczka Geodezyjna] §fZmieniono kierunek osi drogi: §e%.0f° %s",
                        newYaw, getFacingDescription(newYaw)
                )));
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            // Cycle corridor width: 6m -> 8m -> 10m -> 12m -> 16m -> 4m -> 6m
            float current = getCorridorWidth();
            float next;
            if (current < 5.0F) next = 6.0F;
            else if (current < 7.0F) next = 8.0F;
            else if (current < 9.0F) next = 10.0F;
            else if (current < 11.0F) next = 12.0F;
            else if (current < 15.0F) next = 16.0F;
            else next = 4.0F;
            setCorridorWidth(next);

            if (!this.level().isClientSide()) {
                player.sendSystemMessage(Component.literal(String.format(
                        "§6[Tyczka Geodezyjna] §fZmieniono szerokość korytarza: §e%.1f m §7(po %.1f m od osi)",
                        next, next / 2.0F
                )));
            }
            return InteractionResult.SUCCESS;
        } else {
            // Show status
            if (!this.level().isClientSide()) {
                player.sendSystemMessage(Component.literal(String.format(
                        "§6[Tyczka Geodezyjna] §fRzędna niwelety: §b%.2f m §f| Kierunek: §e%.0f° %s §f| Szerokość: §e%.1f m §f(Profil płaski)\n§7(PPM z patykiem/łopatą obraca o 15° | Shift+PPM z narzędziem o 45° | Shift+PPM pustą ręką zmienia szerokość)",
                        getBenchmarkY(), getYaw(), getFacingDescription(getYaw()), getCorridorWidth()
                )));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static String getFacingDescription(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        if (normalized >= 337.5F || normalized < 22.5F) return "(Południe / +Z)";
        if (normalized >= 22.5F && normalized < 67.5F) return "(Południowy Zachód)";
        if (normalized >= 67.5F && normalized < 112.5F) return "(Zachód / -X)";
        if (normalized >= 112.5F && normalized < 157.5F) return "(Północny Zachód)";
        if (normalized >= 157.5F && normalized < 202.5F) return "(Północ / -Z)";
        if (normalized >= 202.5F && normalized < 247.5F) return "(Północny Wschód)";
        if (normalized >= 247.5F && normalized < 292.5F) return "(Wschód / +X)";
        return "(Południowy Wschód)";
    }

    private void dropAndDiscard(ServerLevel level) {
        this.spawnAtLocation(level, new ItemStack(GroundworksAsphaltMod.ROAD_STAKE_ITEM));
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setCorridorWidth(input.getFloatOr("Width", DEFAULT_CORRIDOR_WIDTH));
        setCrownPercent(input.getFloatOr("Crown", DEFAULT_CROWN_PERCENT));
        setElevationOffset(input.getFloatOr("YOffset", 0.0F));
        boolean hasCustom = input.getBooleanOr("HasCustomYaw", false);
        this.entityData.set(HAS_CUSTOM_YAW, hasCustom);
        float loadedYaw = input.getFloatOr("Yaw", this.getYRot());
        this.entityData.set(YAW, loadedYaw);
        this.setYRot(loadedYaw);
        this.yRotO = loadedYaw;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Width", getCorridorWidth());
        output.putFloat("Crown", getCrownPercent());
        output.putFloat("YOffset", getElevationOffset());
        output.putFloat("Yaw", getYaw());
        output.putBoolean("HasCustomYaw", hasCustomYaw());
    }
}
