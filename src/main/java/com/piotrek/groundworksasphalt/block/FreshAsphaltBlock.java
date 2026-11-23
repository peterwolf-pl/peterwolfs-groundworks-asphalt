package com.piotrek.groundworksasphalt.block;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Hot, uncompacted fresh asphalt mat laid down by the asphalt paver in layers (max 1/8 block per layer).
 *
 * <p>Warm and sticky: slows down foot and vehicle travel until compacted.
 * Emits rising heat vapor and bitumen smoke.
 * Transforms into {@link CompactedAsphaltBlock} when rolled over by a {@code RoadRollerEntity}
 * or manually tamped by a player using a shovel.
 */
public class FreshAsphaltBlock extends Block {

    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    protected static final VoxelShape[] SHAPES = Block.boxes(8, layer -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, (double) ((layer + 1) * 2), 16.0D));

    public FreshAsphaltBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return !belowState.isAir() && !belowState.canBeReplaced();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        return !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        int currentLayers = state.getValue(LAYERS);
        if (context.getItemInHand().is(this.asItem()) && currentLayers < 8) {
            if (context.replacingClickedOnBlock()) {
                return context.getClickedFace() == Direction.UP;
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState currentState = context.getLevel().getBlockState(context.getClickedPos());
        if (currentState.is(this)) {
            int currentLayers = currentState.getValue(LAYERS);
            return currentState.setValue(LAYERS, Math.min(8, currentLayers + 1));
        }
        return this.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    /**
     * Compacts this fresh asphalt block into permanent dense road pavement.
     */
    public static boolean compact(Level level, BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof FreshAsphaltBlock)) {
            return false;
        }

        int layers = currentState.hasProperty(LAYERS) ? currentState.getValue(LAYERS) : 1;
        BlockState newState = GroundworksAsphaltMod.COMPACTED_ASPHALT.defaultBlockState();
        if (newState.hasProperty(CompactedAsphaltBlock.LAYERS)) {
            newState = newState.setValue(CompactedAsphaltBlock.LAYERS, layers);
        }

        level.setBlock(pos, newState, Block.UPDATE_ALL);
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
            int layers = state.getValue(LAYERS);
            double height = layers * 0.125D;
            double px = pos.getX() + 0.15D + random.nextDouble() * 0.7D;
            double py = pos.getY() + height + 0.02D;
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
