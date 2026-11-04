package com.piotrek.groundworksasphalt.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Dense, smooth, rolled asphalt roadway pavement.
 *
 * <p>Engineered for high-speed highway and haul road transport:
 * provides increased movement velocity (speedFactor = 1.35F) and smooth rolling friction.
 */
public class CompactedAsphaltBlock extends Block {

    public CompactedAsphaltBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
