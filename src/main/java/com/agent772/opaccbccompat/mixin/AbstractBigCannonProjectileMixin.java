package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.agent772.opaccbccompat.compat.OPACBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

/**
 * Routes big-cannon shell penetration (solid shot punching through walls) through
 * OPAC. When a claim protects the block, {@code canDamageTerrain} is forced to
 * false, which CBC treats as an unbreakable block: the shell stops or bounces but
 * the block stays intact.
 */
@Mixin(AbstractBigCannonProjectile.class)
public abstract class AbstractBigCannonProjectileMixin {

    @WrapOperation(
            method = "calculateBlockPenetration(Lrbasamoyai/createbigcannons/munitions/ProjectileContext;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;)Lrbasamoyai/createbigcannons/munitions/AbstractCannonProjectile$ImpactResult;",
            at = @At(value = "INVOKE", target = "Lrbasamoyai/createbigcannons/munitions/ProjectileDamageHooks;canDamageTerrain(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean opaccbccompat$gateTerrain(Level level, BlockPos pos, Operation<Boolean> original) {
        if (!original.call(level, pos)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel
                && OPACBridge.blocksBlockDamage((Projectile) (Object) this, serverLevel, pos)) {
            return false;
        }
        return true;
    }
}
