package com.maxenonyme.createsubmarine.submarine.block.propeller.submarine_propeller;

import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import com.maxenonyme.createsubmarine.submarine.util.SubLevelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class SubmarinePropellerBlockEntity extends BasePropellerBlockEntity {

    public SubmarinePropellerBlockEntity(final BlockPos pos, final BlockState state) {
        super(com.maxenonyme.createsubmarine.CreateSubmarine.SUBMARINE_PROPELLER_BE.get(), pos, state);
    }

    @Override
    public double getConfigThrust() {
        if (isSubmerged()) {
            return 3.0 * AeroConfig.server().physics.andesitePropellerThrust.get();
        }
        return 0.0;
    }

    @Override
    public double getConfigAirflow() {
        if (isSubmerged()) {
            return 3.0 * AeroConfig.server().physics.andesitePropellerAirflow.get();
        }
        return 0.0;
    }

    @Override
    public float getRadius() {
        return 1.0f;
    }

    @Override
    public float getOffset() {
        return 3 / 16f;
    }

    private boolean isSubmerged() {
        if (level == null) {
            return false;
        }
        SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, worldPosition);
        if (sub == null) {
            return level.getFluidState(worldPosition).is(FluidTags.WATER);
        }
        Vector3d worldPos = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        sub.logicalPose().transformPosition(worldPos);
        BlockPos wPos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
        Level parentLevel = SubLevelRegistry.getLevel(sub.getUniqueId());
        if (parentLevel == null && sub instanceof dev.ryanhcode.sable.sublevel.SubLevel sl) {
            parentLevel = sl.getLevel();
        }
        if (parentLevel == null) {
            return false;
        }
        return com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker.realFluidState(parentLevel, wPos).is(FluidTags.WATER);
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && level.isClientSide() && isSubmerged() && isActive()) {
            net.minecraft.util.RandomSource random = level.getRandom();
            for (int i = 0; i < 2; i++) {
                double x = worldPosition.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
                double y = worldPosition.getY() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
                double z = worldPosition.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE, x, y, z, 
                        (random.nextDouble() - 0.5) * 0.1, 
                        random.nextDouble() * 0.2, 
                        (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }
}

