package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.agent772.opaccbccompat.compat.AttributedDamageSource;
import com.agent772.opaccbccompat.compat.OPACBridge;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;

/**
 * Routes direct cannon-projectile entity hits (solid shot, HE shell, autocannon
 * rounds, etc.) through OPAC. When the target is protected, the hit is treated as
 * a no-op (returns false, the same result CBC uses for a massless projectile), so
 * the projectile keeps flying without dealing damage.
 *
 * <p>Also attributes CBC's damage sources: {@code CannonDamageSource} carries no
 * entities, so OPAC's own hurt-event check would treat every cannon hurt as
 * anonymous and block it in protected claims regardless of the bridge's verdict.
 * {@code indirectArtilleryFire} covers shell/impact explosions and default direct
 * hits; the {@code getEntityDamage} wrap additionally covers subclasses that
 * build their own source (e.g. the machine-gun round).
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

    @ModifyReturnValue(
            method = "indirectArtilleryFire(Z)Lnet/minecraft/world/damagesource/DamageSource;",
            at = @At("RETURN"))
    private DamageSource opaccbccompat$attributeArtilleryFire(DamageSource original) {
        return AttributedDamageSource.attribute(original, (Entity) (Object) this);
    }

    @WrapOperation(
            method = "onHitEntity(Lnet/minecraft/world/entity/Entity;Lrbasamoyai/createbigcannons/munitions/ProjectileContext;)Z",
            at = @At(value = "INVOKE", target = "Lrbasamoyai/createbigcannons/munitions/AbstractCannonProjectile;getEntityDamage(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource opaccbccompat$attributeEntityDamage(AbstractCannonProjectile instance, Entity target, Operation<DamageSource> original) {
        return AttributedDamageSource.attribute(original.call(instance, target), (Entity) (Object) this);
    }
}
