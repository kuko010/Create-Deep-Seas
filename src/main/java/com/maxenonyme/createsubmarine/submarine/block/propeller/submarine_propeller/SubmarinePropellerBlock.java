package com.maxenonyme.createsubmarine.submarine.block.propeller.submarine_propeller;

import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SubmarinePropellerBlock extends BasePropellerBlock {
    public SubmarinePropellerBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends BasePropellerBlockEntity> getBlockEntityType() {
        return com.maxenonyme.createsubmarine.CreateSubmarine.SUBMARINE_PROPELLER_BE.get();
    }
}
