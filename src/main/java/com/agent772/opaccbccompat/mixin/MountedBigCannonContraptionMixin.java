package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.agent772.opaccbccompat.compat.OPACBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

/**
 * Attributes big-cannon projectiles to the firing player. CBC spawns the shot via
 * {@code level.addFreshEntity} without ever calling {@code setOwner}, so the round
 * would otherwise be anonymous. Capturing the contraption's controlling passenger
 * lets OPAC apply party/ally exceptions and owner redirection to the shot.
 */
@Mixin(MountedBigCannonContraption.class)
public abstract class MountedBigCannonContraptionMixin {

    @Redirect(
            method = "fireShot(Lnet/minecraft/server/level/ServerLevel;Lrbasamoyai/createbigcannons/cannon_control/contraption/PitchOrientedContraptionEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean opaccbccompat$attributeOwner(ServerLevel level, Entity projectile, ServerLevel level2, PitchOrientedContraptionEntity contraption) {
        OPACBridge.attributeOwner(projectile, contraption.getControllingPassenger());
        return level.addFreshEntity(projectile);
    }
}
