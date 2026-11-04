package com.piotrek.groundworksasphalt.block;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hot, uncompacted fresh asphalt mat laid down by the asphalt paver.
 *
 * <p>Warm and sticky: slows down foot and vehicle travel until compacted.
 * Emits rising heat vapor and bitumen smoke.
 * Transforms into {@link CompactedAsphaltBlock} when rolled over by a {@code RoadRollerEntity}
 * or manually tamped by a player using a shovel.
 */
public class FreshAsphaltBlock extends Block {

    public FreshAsphaltBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Compacts this fresh asphalt block into permanent dense road pavement.
     */
    public static boolean compact(Level level, BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof FreshAsphaltBlock)) {
            return false;
        }

        level.setBlock(pos, GroundworksAsphaltMod.COMPACTED_ASPHALT.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(
                null, pos,
                SoundEvents.STONE_PLACE, SoundSource.BLOCKS,
                0.8F, 0.7F
        );
        return true;
    }

    @Override
    public InteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        // Allow manual tamping with shovels
        if (stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide()) {
                compact(level, pos);
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Emit hot bitumen steam and faint heat vapor
        if (random.nextInt(3) == 0) {
            double px = pos.getX() + 0.15D + random.nextDouble() * 0.7D;
            double py = pos.getY() + 1.02D;
            double pz = pos.getZ() + 0.15D + random.nextDouble() * 0.7D;

            level.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    px, py, pz,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.03D + random.nextDouble() * 0.02D,
                    (random.nextDouble() - 0.5D) * 0.02D
            );
        }

        if (random.nextInt(20) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                    0.15F, 1.8F + random.nextFloat() * 0.4F,
                    false
            );
        }
    }
}
