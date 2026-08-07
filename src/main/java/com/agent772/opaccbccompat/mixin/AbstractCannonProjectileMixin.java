package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.agent772.opaccbccompat.compat.OPACBridge;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;

/**
 * Routes direct cannon-projectile entity hits (solid shot, HE shell, autocannon
 * rounds, etc.) through OPAC. When the target is protected, the hit is treated as
 * a no-op (returns false, the same result CBC uses for a massless projectile), so
 * the projectile keeps flying without dealing damage.
 */
@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileMixin {

    @Inject(
            method = "onHitEntity(Lnet/minecraft/world/entity/Entity;Lrbasamoyai/createbigcannons/munitions/ProjectileContext;)Z",
            at = @At("HEAD"), cancellable = true)
    private void opaccbccompat$gateEntity(Entity entity, ProjectileContext projectileContext, CallbackInfoReturnable<Boolean> cir) {
        Projectile self = (Projectile) (Object) this;
        if (OPACBridge.blocksEntityDamage(self.getOwner(), self, entity)) {
            cir.setReturnValue(false);
        }
    }
}
