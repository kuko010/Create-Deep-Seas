package com.maxenonyme.createsubmarine.submarine.block.entity;

import com.maxenonyme.createsubmarine.CreateSubmarine;
import com.maxenonyme.createsubmarine.submarine.util.SablePhysicsHelper;
import com.maxenonyme.createsubmarine.submarine.util.SubLevelRegistry;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FloaterBlockEntity extends BlockEntity {
    private static final int PRESSURE_THRESHOLD = 50;
    private static final int PRESSURE_CHECK_INTERVAL = 20;
    private static final int MAX_WATER_SCAN = 200;

    private int pressureTickCounter = 0;

    public FloaterBlockEntity(BlockPos pos, BlockState state) {
        super(CreateSubmarine.FLOATER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, FloaterBlockEntity be) {
        SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, pos);

        if (++be.pressureTickCounter >= PRESSURE_CHECK_INTERVAL) {
            be.pressureTickCounter = 0;
            Level worldLevel = level;
            BlockPos worldPos = pos;
            if (sub != null) {
                Level parent = SubLevelRegistry.getLevel(sub.getUniqueId());
                if (parent != null)
                    worldLevel = parent;
                Vector3d wp = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                sub.logicalPose().transformPosition(wp);
                worldPos = BlockPos.containing(wp.x, wp.y, wp.z);
            }
            if (countWaterAbove(worldLevel, worldPos) > PRESSURE_THRESHOLD) {
                burst(level, pos);
                return;
            }
        }

        if (sub == null)
            return;

        Vector3d worldPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        sub.logicalPose().transformPosition(worldPos);

        Level parentLevel = SubLevelRegistry.getLevel(sub.getUniqueId());
        if (parentLevel == null && level.getServer() != null) {
            parentLevel = level.getServer().overworld();
        }
        if (parentLevel == null)
            return;

        Object handle = SablePhysicsHelper.getHandle(sub);
        Vector3dc currentVel = SablePhysicsHelper.getVelocity(handle);

        BlockPos parentPos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
        checkCrash(level, pos, parentLevel, parentPos, currentVel);

        boolean isUnderWater = false;
        net.minecraft.world.level.chunk.LevelChunk chunk = parentLevel.getChunkAt(parentPos);
        if (chunk != null) {
            int sectionY = chunk.getSectionIndex(parentPos.getY());
            if (sectionY >= 0 && sectionY < chunk.getSections().length) {
                net.minecraft.world.level.chunk.LevelChunkSection section = chunk.getSections()[sectionY];
                if (section != null) {
                    if (section.getFluidState(parentPos.getX() & 15, parentPos.getY() & 15, parentPos.getZ() & 15)
                            .is(FluidTags.WATER)) {
                        isUnderWater = true;
                    }
                }
            }
        }
        if (!isUnderWater || handle == null)
            return;

        double seaLevel = parentLevel.getSeaLevel();
        double taperDist = com.maxenonyme.createsubmarine.submarine.config.FloaterTuning.surfaceTaperDistance();
        double aboveSurface = worldPos.y - seaLevel;
        // Archimedes: fully submerged (center >= taperDist below surface) = full lift,
        // fully emerged (center >= taperDist above surface) = 0 lift, linear in between
        double taper = Math.max(0.0, Math.min(1.0, 0.5 - aboveSurface / (taperDist * 2.0)));

        double mass = SablePhysicsHelper.readMass(sub);
        double forceMult = com.maxenonyme.createsubmarine.submarine.config.SubmarineConfig.BALLAST_FORCE_MULTIPLIER.get();
        double lift = com.maxenonyme.createsubmarine.submarine.config.FloaterTuning.liftPerBlock();
        double vDrag = com.maxenonyme.createsubmarine.submarine.config.FloaterTuning.verticalDrag();
        double hDrag = com.maxenonyme.createsubmarine.submarine.config.FloaterTuning.horizontalDrag();

        double vx = (currentVel != null) ? currentVel.x() : 0;
        double vy = (currentVel != null) ? currentVel.y() : 0;
        double vz = (currentVel != null) ? currentVel.z() : 0;

        double impulseY = (lift * taper - vDrag * vy) * mass * forceMult;
        double impulseX = -hDrag * vx * mass * forceMult;
        double impulseZ = -hDrag * vz * mass * forceMult;

        if (Double.isFinite(impulseY) && Double.isFinite(impulseX) && Double.isFinite(impulseZ)) {
            SablePhysicsHelper.wakeUp(handle);
            Vector3d impulse = new Vector3d(impulseX, impulseY, impulseZ);
            sub.logicalPose().orientation().conjugate(new org.joml.Quaterniond()).transform(impulse);
            SablePhysicsHelper.applyLinearImpulse(handle, impulse);
        }
    }

    private static int countWaterAbove(Level level, BlockPos pos) {
        int depth = 0;
        for (int y = pos.getY() + 1; y < pos.getY() + 1 + MAX_WATER_SCAN; y++) {
            if (level.getFluidState(new BlockPos(pos.getX(), y, pos.getZ())).is(FluidTags.WATER)) {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    private static void burst(Level level, BlockPos pos) {
        level.playSound(null, pos, CreateSubmarine.IMPLOSION_SOUND.get(), SoundSource.BLOCKS, 1.2f, 1.4f);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20,
                    0.4, 0.2, 0.4, 0.1);
        }
        level.destroyBlock(pos, false);
    }

    private static void checkCrash(Level subLevel, BlockPos localPos, Level parentLevel, BlockPos parentPos,
            Vector3dc vel) {
        if (vel == null)
            return;
        double speed = vel.length();
        if (speed < 0.35)
            return;

        net.minecraft.world.level.block.state.BlockState parentState = parentLevel.getBlockState(parentPos);
        if (parentState.isSolid() && !parentState.is(net.minecraft.tags.BlockTags.LEAVES)) {
            subLevel.destroyBlock(localPos, false);
            parentLevel.playSound(null, parentPos, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.BLOCKS, 1.0F, 1.5F);
            parentLevel.playSound(null, parentPos, net.minecraft.sounds.SoundEvents.WOOL_BREAK, SoundSource.BLOCKS,
                    1.0F, 0.8F);
        }
    }

}
