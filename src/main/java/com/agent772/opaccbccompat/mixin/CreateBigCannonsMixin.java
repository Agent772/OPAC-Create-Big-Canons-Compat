package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.agent772.opaccbccompat.compat.OPACBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import rbasamoyai.createbigcannons.CreateBigCannons;

/**
 * Filters CBC explosion block destruction through OPAC. After the explosion has
 * computed its block list but before it is applied, claim-protected positions are
 * removed. This handles the cross-chunk case where a blast is centred in
 * wilderness but its radius reaches into a claim: wilderness blocks still break,
 * claimed blocks are spared.
 */
@Mixin(CreateBigCannons.class)
public abstract class CreateBigCannonsMixin {

    @Inject(
            method = "handleCustomExplosion(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/Explosion;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;explode()V", shift = At.Shift.AFTER))
    private static void opaccbccompat$filterExplosion(Level level, Explosion explosion, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            OPACBridge.filterExplosionBlocks(explosion, serverLevel);
        }
    }
}
