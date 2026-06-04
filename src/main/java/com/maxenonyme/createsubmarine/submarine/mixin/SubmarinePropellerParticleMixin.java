package com.maxenonyme.createsubmarine.submarine.mixin;

import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import com.maxenonyme.createsubmarine.submarine.block.propeller.submarine_propeller.SubmarinePropellerBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BasePropellerBlockEntity.class)
public class SubmarinePropellerParticleMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), remap = true, require = 0)
    private void createsubmarine$redirectAddParticle(Level level, ParticleOptions particleOptions, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if ((Object) this instanceof SubmarinePropellerBlockEntity) {
            return;
        }
        level.addParticle(particleOptions, x, y, z, xSpeed, ySpeed, zSpeed);
    }
    
    @Redirect(method = "onActiveTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), remap = true, require = 0)
    private void createsubmarine$redirectAddParticleActive(Level level, ParticleOptions particleOptions, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if ((Object) this instanceof SubmarinePropellerBlockEntity) {
            return;
        }
        level.addParticle(particleOptions, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
