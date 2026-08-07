package com.agent772.opaccbccompat.compat;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.agent772.opaccbccompat.OPACBigCannonsCompat;
import com.agent772.opaccbccompat.config.OBCServerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI;

/**
 * Bridge between Create: Big Cannons' custom projectile destruction and OPAC's
 * chunk protection. CBC bypasses the standard block-break / explosion events, so
 * OPAC never sees the damage. Each method here asks OPAC whether a specific
 * action should be blocked, using OPAC's normal per-claim options (including
 * admin-defined exception groups and owner redirection).
 *
 * <p>All queries fail open: if OPAC is missing or its API throws, the action is
 * allowed so that a compatibility problem never breaks CBC gameplay.
 */
public final class OPACBridge {

    private OPACBridge() {}

    /**
     * Tags a freshly-fired CBC projectile with the player who fired it. CBC never
     * sets an owner, so without this the projectile is anonymous and OPAC cannot
     * apply party/ally exceptions or owner redirection. A {@code null} owner (e.g.
     * a redstone-triggered cannon with no rider) leaves the projectile anonymous,
     * which OPAC treats as having no claim access.
     */
    public static void attributeOwner(Entity projectile, @Nullable Entity owner) {
        if (owner != null && projectile instanceof Projectile p && p.getOwner() == null) {
            p.setOwner(owner);
        }
    }

    @Nullable
    private static IChunkProtectionAPI protectionFor(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        return OpenPACServerAPI.get(server).getChunkProtection();
    }

    /**
     * Whether OPAC protects the block at {@code pos} from being broken by the
     * given source entity (usually the projectile). A {@code null} source is
     * treated as an anonymous action with no claim access.
     */
    public static boolean blocksBlockDamage(@Nullable Entity source, ServerLevel level, BlockPos pos) {
        if (!OBCServerConfig.protectBlocks()) {
            return false;
        }
        try {
            IChunkProtectionAPI protection = protectionFor(level);
            if (protection == null) {
                return false;
            }
            return protection.onBlockInteraction(
                    source, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
                    level, pos, Direction.UP,
                    true,   // breaking
                    false,  // messages
                    true    // targetExceptions (enables admin exception groups)
            );
        } catch (Throwable t) {
            OPACBigCannonsCompat.LOGGER.debug("OPAC block-interaction query failed; allowing damage", t);
            return false;
        }
    }

    /**
     * Whether OPAC protects {@code target} from being damaged by the projectile.
     * The projectile's owner (if any) is used as the indirect accessor so that
     * party/ally exceptions and owner redirection apply.
     */
    public static boolean blocksEntityDamage(@Nullable Entity indirectOwner, Entity projectile, Entity target) {
        if (!OBCServerConfig.protectEntities()) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        try {
            IChunkProtectionAPI protection = protectionFor(level);
            if (protection == null) {
                return false;
            }
            return protection.onEntityInteraction(
                    indirectOwner, projectile, target,
                    ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    true,   // attack
                    false,  // messages
                    true    // targetExceptions
            );
        } catch (Throwable t) {
            OPACBigCannonsCompat.LOGGER.debug("OPAC entity-interaction query failed; allowing damage", t);
            return false;
        }
    }

    /**
     * Removes claim-protected positions from a CBC explosion's block-destruction
     * list before it is applied. The explosion's own source entity (null for CBC
     * shells) is used as the accessor, so protected chunks are spared while
     * wilderness blocks in the same blast still break.
     */
    public static void filterExplosionBlocks(Explosion explosion, ServerLevel level) {
        if (!OBCServerConfig.protectBlocks()) {
            return;
        }
        List<BlockPos> toBlow = explosion.getToBlow();
        if (toBlow.isEmpty()) {
            return;
        }
        Entity source = explosion.getDirectSourceEntity();
        Iterator<BlockPos> it = toBlow.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (blocksBlockDamage(source, level, pos)) {
                it.remove();
            }
        }
    }
}
