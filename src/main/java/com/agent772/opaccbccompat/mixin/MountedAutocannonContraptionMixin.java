package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.agent772.opaccbccompat.compat.OPACBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

/**
 * Attributes autocannon rounds to the firing player, mirroring
 * {@link MountedBigCannonContraptionMixin}. {@code fireShot} spawns two entities
 * through {@code addFreshEntity} — the spent-casing item and the projectile — so
 * the wrap only tags actual projectiles ({@link OPACBridge#attributeOwner}
 * ignores non-projectiles such as the ejected casing).
 *
 * <p>Uses MixinExtras' {@code @WrapOperation} rather than {@code @Redirect} so a
 * mod that wraps the same {@code addFreshEntity} call does not hard-conflict.
 */
@Mixin(MountedAutocannonContraption.class)
public abstract class MountedAutocannonContraptionMixin {

    @WrapOperation(
            method = "fireShot(Lnet/minecraft/server/level/ServerLevel;Lrbasamoyai/createbigcannons/cannon_control/contraption/PitchOrientedContraptionEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean opaccbccompat$attributeOwner(ServerLevel level, Entity entity, Operation<Boolean> original, ServerLevel level2, PitchOrientedContraptionEntity contraption) {
        OPACBridge.attributeOwner(entity, contraption.getControllingPassenger());
        return original.call(level, entity);
    }
}
