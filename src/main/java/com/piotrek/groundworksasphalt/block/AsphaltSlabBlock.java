package com.piotrek.groundworksasphalt.block;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Asphalt half-slab block with identical speed boost properties to compacted asphalt.
 * Useful for gradual ramps, curbs, gutters, and smooth elevation transitions.
 */
public class AsphaltSlabBlock extends SlabBlock {

    public AsphaltSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
