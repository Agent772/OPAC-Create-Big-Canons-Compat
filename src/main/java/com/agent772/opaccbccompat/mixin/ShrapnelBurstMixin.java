package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.agent772.opaccbccompat.compat.OPACBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import rbasamoyai.createbigcannons.CBCCompatTransformers;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelBurst;
import rbasamoyai.ritchiesprojectilelib.projectile_burst.ProjectileBurst;

/**
 * Routes shrapnel sub-projectile hits through OPAC for both block and entity
 * damage. The burst entity itself is the accessor, so owner attribution (see the
 * fire-path and burst-propagation mixins) enables party/ally exceptions. Also
 * covers flak bursts, which extend {@link ShrapnelBurst} and inherit these hit
 * methods. The terrain position is mapped through
 * {@link CBCCompatTransformers#transformBlockPos} to query OPAC in world space,
 * matching what {@code canDamageTerrain} does internally.
 */
@Mixin(ShrapnelBurst.class)
public abstract class ShrapnelBurstMixin {

    @WrapOperation(
            method = "onSubProjectileHitBlock(Lnet/minecraft/world/phys/BlockHitResult;Lrbasamoyai/ritchiesprojectilelib/projectile_burst/ProjectileBurst$SubProjectile;)V",
            at = @At(value = "INVOKE", target = "Lrbasamoyai/createbigcannons/munitions/ProjectileDamageHooks;canDamageTerrain(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean opaccbccompat$gateTerrain(Level level, BlockPos pos, Operation<Boolean> original) {
        if (!original.call(level, pos)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockPos realPos = CBCCompatTransformers.transformBlockPos(level, pos);
            if (OPACBridge.blocksBlockDamage((Projectile) (Object) this, serverLevel, realPos)) {
                return false;
            }
        }
        return true;
    }

    @Inject(
            method = "onSubProjectileHitEntity(Lnet/minecraft/world/phys/EntityHitResult;Lrbasamoyai/ritchiesprojectilelib/projectile_burst/ProjectileBurst$SubProjectile;)V",
            at = @At("HEAD"), cancellable = true)
    private void opaccbccompat$gateEntity(EntityHitResult result, ProjectileBurst.SubProjectile subProjectile, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (OPACBridge.blocksEntityDamage(self.getOwner(), self, result.getEntity())) {
            ci.cancel();
        }
    }
}
