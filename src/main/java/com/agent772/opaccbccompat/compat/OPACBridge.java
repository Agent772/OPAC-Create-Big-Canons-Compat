package com.agent772.opaccbccompat.compat;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import com.agent772.opaccbccompat.OPACBigCannonsCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

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

    /**
     * Latches the first OPAC query failure of the session so it can be surfaced at
     * WARN once (protection silently failing open is the worst failure mode to
     * hide); subsequent failures stay at debug to avoid log spam.
     */
    private static final AtomicBoolean QUERY_FAILURE_WARNED = new AtomicBoolean(false);

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
        } else if (OPACBigCannonsCompat.DEBUG_LOGGING) {
            String reason;
            if (owner == null) {
                reason = "no firing player (e.g. redstone-triggered cannon) - projectile stays anonymous";
            } else if (!(projectile instanceof Projectile)) {
                reason = "spawned entity is not a Projectile (e.g. an ejected casing)";
            } else {
                reason = "projectile already has an owner";
            }
            OPACBigCannonsCompat.LOGGER.info(
                    "[OPAC-CBC] owner attribution skipped for {}: {}", describe(projectile), reason);
        }
    }

    /**
     * Resends the real block state at {@code pos} to tracking clients. CBC predicts
     * block destruction on the client (both penetration and cosmetic explosion
     * edits set the block locally), so blocking it only on the server leaves a
     * client-only ghost hole until the next relog. This resync makes the protected
     * block reappear immediately.
     *
     * <p>Callers pass the <em>pre-transform</em> position (the block CBC actually
     * predicted client-side), which on Sable/VS2 sub-levels differs from the
     * world-space position used for the OPAC query.
     */
    public static void resyncBlock(ServerLevel level, BlockPos pos) {
        level.getChunkSource().blockChanged(pos);
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
        try {
            IChunkProtectionAPI protection = protectionFor(level);
            if (protection == null) {
                return false;
            }
            boolean blocked = protection.onBlockInteraction(
                    source, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
                    level, pos, Direction.UP,
                    true,   // breaking
                    false,  // messages
                    true    // targetExceptions (enables admin exception groups)
            );
            if (OPACBigCannonsCompat.DEBUG_LOGGING) {
                logVerdict("block damage", protection, source, level, pos, blocked, true);
            }
            return blocked;
        } catch (Throwable t) {
            // Throwable (not Exception) is intentional: a broken OPAC relaunch or
            // API change surfaces as LinkageError / NoClassDefFoundError, which we
            // still want to fail open on rather than crash CBC.
            warnQueryFailure("block-interaction", t);
            return false;
        }
    }

    /**
     * Whether OPAC protects {@code target} from being damaged by the projectile.
     * The projectile's owner (if any) is used as the indirect accessor so that
     * party/ally exceptions and owner redirection apply. {@code projectile} may be
     * {@code null} for source-less CBC explosions (flak, fluid, mortar-stone,
     * smoke); OPAC tolerates a null direct entity, treating the hit as anonymous.
     */
    public static boolean blocksEntityDamage(@Nullable Entity indirectOwner, @Nullable Entity projectile, Entity target) {
        return blocksEntityDamage(indirectOwner, projectile, target, "direct hit");
    }

    /**
     * Same as {@link #blocksEntityDamage(Entity, Entity, Entity)} but labels the
     * damage path ("direct hit", "sub-projectile hit", "explosion") in the debug
     * log so verdicts from different code paths can be told apart.
     */
    public static boolean blocksEntityDamage(@Nullable Entity indirectOwner, @Nullable Entity projectile, Entity target, String pathLabel) {
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        try {
            IChunkProtectionAPI protection = protectionFor(level);
            if (protection == null) {
                return false;
            }
            boolean blocked = protection.onEntityInteraction(
                    indirectOwner, projectile, target,
                    ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    true,   // attack
                    false,  // messages
                    true    // targetExceptions
            );
            if (OPACBigCannonsCompat.DEBUG_LOGGING) {
                logVerdict("entity damage via " + pathLabel + " (target " + describe(target) + ")",
                        protection, projectile, level, target.blockPosition(), blocked, false);
            }
            return blocked;
        } catch (Throwable t) {
            warnQueryFailure("entity-interaction", t);
            return false;
        }
    }

    /**
     * Reports an OPAC query failure. The first failure of the session is logged at
     * WARN because a persistent failure means protection has silently stopped
     * working (every query fails open); later failures drop to debug to avoid spam.
     */
    private static void warnQueryFailure(String query, Throwable t) {
        if (QUERY_FAILURE_WARNED.compareAndSet(false, true)) {
            OPACBigCannonsCompat.LOGGER.warn(
                    "OPAC {} query failed; allowing the damage (failing open). CBC projectile protection "
                            + "is NOT being enforced for this query and may be broken - check for an OPAC "
                            + "version mismatch. Further failures are logged at debug.", query, t);
        } else {
            OPACBigCannonsCompat.LOGGER.debug("OPAC {} query failed; allowing damage", query, t);
        }
    }

    /**
     * Logs a single OPAC verdict with enough context to explain it: the source
     * projectile, its resolved owner, the claim at the position and - crucially -
     * whether the owner has full chunk access. Full access (claim owner, admin
     * mode, server claiming mode against a server claim) makes OPAC allow the
     * action before per-claim options or exception groups are ever consulted,
     * which is invisible without this log line.
     */
    private static void logVerdict(String action, IChunkProtectionAPI protection,
                                   @Nullable Entity source, ServerLevel level, BlockPos pos, boolean blocked,
                                   boolean blockAction) {
        try {
            Entity accessor = source instanceof Projectile p && p.getOwner() != null ? p.getOwner() : source;
            IPlayerChunkClaimAPI claim = OpenPACServerAPI.get(level.getServer())
                    .getServerClaimsManager().get(level.dimension().location(), pos);
            IPlayerConfigAPI config = claim == null ? null : protection.getConfig(claim);
            String reason;
            if (blocked) {
                reason = "blocked by the claim's protection options / exception groups";
            } else if (claim == null) {
                reason = "position is unclaimed (wilderness)";
            } else if (accessor != null && protection.hasChunkAccess(config, accessor)) {
                reason = "accessor has FULL chunk access to this claim (claim owner, admin mode or "
                        + "server claiming mode) - OPAC allows before claim options or exception groups are checked";
            } else {
                reason = "allowed by the claim's protection options / exception groups";
            }
            String options = config == null ? "" : " | " + optionDiagnostics(level, config, blockAction);
            OPACBigCannonsCompat.LOGGER.info(
                    "[OPAC-CBC] {} {} at {} in {} | projectile={} accessor={} claim={} | {}{}",
                    action, blocked ? "BLOCKED" : "ALLOWED", pos.toShortString(),
                    level.dimension().location(), describe(source), describe(accessor),
                    claim == null ? "none" : claim.getPlayerId(), reason, options);
        } catch (Throwable t) {
            OPACBigCannonsCompat.LOGGER.info("Failed to log OPAC verdict", t);
        }
    }

    private static final String GROUP_OPTION_ROOT = "playerConfig.claims.protection.exceptions.groups.entity.";

    /**
     * Summarizes the claim config values that decide a CBC verdict: the general
     * "Allow Blocks/Entities By Players" option (which governs the redirected
     * player action) and every entity-access exception group option that can
     * additionally grant CBC projectiles access. Group options are listed with
     * their UI label (e.g. "Mine (CBC)") so they can be matched to the claim
     * config screen; if none exist, the OPAC server config has no such groups.
     */
    @SuppressWarnings("unchecked")
    private static String optionDiagnostics(ServerLevel level, IPlayerConfigAPI config, boolean blockAction) {
        StringBuilder sb = new StringBuilder("options:");
        if (blockAction) {
            sb.append(" blocksRedirect=").append(config.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_REDIRECT));
            sb.append(" allowBlocksByPlayers=").append(describeGroupValue(
                    config.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_PLAYERS)));
        } else {
            sb.append(" entitiesRedirect=").append(config.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_REDIRECT));
            sb.append(" allowEntitiesByPlayers=").append(describeGroupValue(
                    config.getEffective(PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_PLAYERS)));
        }
        String breakPrefix = GROUP_OPTION_ROOT + (blockAction ? "blockBreakAccess." : "entityAttackAccess.");
        String fullPrefix = GROUP_OPTION_ROOT + (blockAction ? "blockAccess." : "entityAccess.");
        String breakLabel = blockAction ? "Mine" : "Attack By";
        String fullLabel = blockAction ? "Blocks" : "Entities By";
        List<? extends IPlayerConfigOptionSpecAPI<?>> groupOptions = OpenPACServerAPI.get(level.getServer())
                .getPlayerConfigManager().getAllOptionsStream()
                .filter(o -> o.getId().startsWith(breakPrefix) || o.getId().startsWith(fullPrefix))
                .toList();
        if (groupOptions.isEmpty()) {
            sb.append(" | no entity-access exception groups are defined - check the '")
                    .append(blockAction ? "blockAccessEntityGroups" : "entityAccessEntityGroups")
                    .append("' list in the OPAC server config");
        } else {
            for (IPlayerConfigOptionSpecAPI<?> o : groupOptions) {
                boolean isBreak = o.getId().startsWith(breakPrefix);
                String name = o.getId().substring(isBreak ? breakPrefix.length() : fullPrefix.length());
                sb.append(" \"").append(isBreak ? breakLabel : fullLabel).append(" (").append(name).append(")\"=")
                        .append(describeGroupValue(config.getEffective((IPlayerConfigOptionSpecAPI<String>) o)));
            }
        }
        return sb.toString();
    }

    private static String describeGroupValue(String value) {
        return switch (value) {
            case "E" -> "E(everyone)";
            case "N" -> "N(nobody)";
            case "P" -> "P(party)";
            case "A" -> "A(party+allies)";
            default -> value;
        };
    }

    private static String describe(@Nullable Entity entity) {
        if (entity == null) {
            return "none";
        }
        return EntityType.getKey(entity.getType())
                + "[" + entity.getName().getString() + "/" + entity.getUUID() + "]";
    }

    /**
     * Removes claim-protected positions from a CBC explosion's block-destruction
     * list before it is applied. The explosion's own source entity (null for CBC
     * shells) is used as the accessor, so protected chunks are spared while
     * wilderness blocks in the same blast still break.
     */
    public static void filterExplosionBlocks(Explosion explosion, ServerLevel level) {
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
