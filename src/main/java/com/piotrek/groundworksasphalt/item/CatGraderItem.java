package com.piotrek.groundworksasphalt.item;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Item used to deploy the Cat Motor Grader into the world.
 */
public class CatGraderItem extends Item {

    public CatGraderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(clickedFace);

        Vec3 spawnVec = new Vec3(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D
        );

        AABB bounds = GroundworksAsphaltMod.CAT_GRADER.getDimensions().makeBoundingBox(spawnVec);
        if (!level.noCollision(bounds)) {
            bounds = bounds.move(0.0D, 0.5D, 0.0D);
            if (!level.noCollision(bounds)) {
                return InteractionResult.FAIL;
            }
            spawnVec = spawnVec.add(0.0D, 0.5D, 0.0D);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        CatGraderEntity vehicle = GroundworksAsphaltMod.CAT_GRADER.create(
                serverLevel,
                EntitySpawnReason.SPAWN_ITEM_USE
        );

        if (vehicle != null) {
            float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
            vehicle.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, playerYaw, 0.0F);
            vehicle.setYHeadRot(playerYaw);
            vehicle.setYBodyRot(playerYaw);

            serverLevel.addFreshEntity(vehicle);

            ItemStack itemStack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
