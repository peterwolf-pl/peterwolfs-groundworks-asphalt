package com.piotrek.groundworksasphalt.item;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.entity.RoadStakeEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Item used to place a Road Survey Stake on the terrain to engineer a road alignment (Option B).
 */
public class RoadStakeItem extends Item {

    public RoadStakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        var clicked = context.getClickedPos().relative(context.getClickedFace());
        double x = clicked.getX() + 0.5D;
        double y = clicked.getY();
        double z = clicked.getZ() + 0.5D;

        AABB occupied = new AABB(x - 0.3D, y, z - 0.3D, x + 0.3D, y + 1.6D, z + 0.3D);
        if (!level.getEntitiesOfClass(RoadStakeEntity.class, occupied).isEmpty()) {
            return InteractionResult.FAIL;
        }

        RoadStakeEntity stake = GroundworksAsphaltMod.ROAD_STAKE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (stake == null) {
            return InteractionResult.FAIL;
        }

        float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        stake.snapTo(x, y, z, normalized, 0.0F);
        stake.clearCustomYaw(); // Default to Auto (Krzywa) so spline naturally flows between stakes without manual yaw twist
        stake.updateLabel();
        level.addFreshEntity(stake);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
