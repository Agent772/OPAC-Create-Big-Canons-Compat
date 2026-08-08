package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.agent772.opaccbccompat.compat.OPACBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelShellProjectile;
import rbasamoyai.createbigcannons.munitions.fragment_burst.CBCProjectileBurst;
import rbasamoyai.ritchiesprojectilelib.projectile_burst.ProjectileBurst;

/**
 * Carries the shell's owner onto the shrapnel burst it spawns. The sub-projectile
 * damage mixins ({@link ShrapnelBurstMixin}) read the burst's owner for OPAC
 * party/ally exceptions, so the attribution must be propagated at detonation.
 *
 * <p>Uses MixinExtras' {@code @WrapOperation} rather than {@code @Redirect} so a
 * mod that wraps the same {@code spawnConeBurst} call does not hard-conflict.
 */
@Mixin(ShrapnelShellProjectile.class)
public abstract class ShrapnelShellProjectileMixin {

    @WrapOperation(
            method = "detonate(Lnet/minecraft/core/Position;)V",
            at = @At(value = "INVOKE", target = "Lrbasamoyai/createbigcannons/munitions/fragment_burst/CBCProjectileBurst;spawnConeBurst(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ID)Lrbasamoyai/ritchiesprojectilelib/projectile_burst/ProjectileBurst;"))
    private ProjectileBurst opaccbccompat$propagateOwner(Level level, EntityType<? extends ProjectileBurst> type, Vec3 position, Vec3 initialVelocity, int count, double spread, Operation<ProjectileBurst> original) {
        ProjectileBurst burst = original.call(level, type, position, initialVelocity, count, spread);
        OPACBridge.attributeOwner(burst, ((Projectile) (Object) this).getOwner());
        return burst;
    }
}
