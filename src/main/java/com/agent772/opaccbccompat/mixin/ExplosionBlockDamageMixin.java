package com.agent772.opaccbccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.agent772.opaccbccompat.compat.OPACBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import rbasamoyai.createbigcannons.munitions.ImpactExplosion;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;
import rbasamoyai.createbigcannons.munitions.big_cannon.mortar_stone.MortarStoneExplosion;

/**
 * Cancels the cosmetic block transformation CBC explosions apply along their
 * blast (cracking/denting via {@code editBlock}). This path bypasses the block
 * list filtered in {@link CreateBigCannonsMixin}, so it needs its own per-block
 * OPAC check to keep protected blocks visually intact in cross-border blasts.
 */
@Mixin({ ShellExplosion.class, ImpactExplosion.class, MortarStoneExplosion.class })
public abstract class ExplosionBlockDamageMixin {

    @Inject(
            method = "editBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;F)V",
            at = @At("HEAD"), cancellable = true)
    private void opaccbccompat$gateEdit(Level level, BlockPos pos, BlockState blockState, FluidState fluidState, float power, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            Explosion self = (Explosion) (Object) this;
            if (OPACBridge.blocksBlockDamage(self.getDirectSourceEntity(), serverLevel, pos)) {
                ci.cancel();
            }
        }
    }
}
