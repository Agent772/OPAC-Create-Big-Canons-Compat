package com.agent772.opaccbccompat.compat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.agent772.opaccbccompat.OPACBigCannonsCompat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import rbasamoyai.createbigcannons.remix.CustomExplosion;

/**
 * Routes CBC explosion <em>entity</em> damage through the attributed attack
 * path. OPAC's own {@code ExplosionEvent.Detonate} handler filters affected
 * entities using only the general explosion/entity options and never consults
 * entity-access exception groups, so options like "Attack By (CBC)" could not
 * grant cannon explosions entity damage. This gate snapshots the affected
 * entities before OPAC's handler runs and afterwards re-adds every removed
 * entity that the attributed check allows. It only ever re-adds entities OPAC
 * removed - it never removes any - so it can only grant access.
 *
 * <p>The re-check runs the attributed <em>attack</em> path
 * ({@code onEntityInteraction} with {@code attack=true}), which consults the
 * whole claim chain, not just the CBC entity-access group: this deliberately
 * treats a cannon explosion as a player attack, so the general "Allow Entities
 * By Players" option also governs it (mirroring how block damage is documented).
 * A claim that wants explosion kills blocked despite allowing player attacks
 * must set "Attack By (CBC)" and the general player option accordingly; this
 * behaviour is spelled out in the README's explosion section.
 *
 * <p>Registered as a single instance in the mod constructor; its only state is
 * the transient per-detonation snapshot map below.
 */
public final class ExplosionEntityGate {

    /**
     * Snapshots keyed by explosion, using a {@link WeakHashMap} so a snapshot is
     * reclaimed with its explosion even if the {@code LOWEST} cleanup never runs
     * (e.g. an intervening listener throws), bounding the retention. Explosion has
     * no {@code equals} override, so weak-key identity matches the previous
     * {@link IdentityHashMap} semantics.
     */
    private final Map<Explosion, List<Entity>> snapshots = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeProtection(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getExplosion() instanceof CustomExplosion)) {
            return;
        }
        snapshots.put(event.getExplosion(), List.copyOf(event.getAffectedEntities()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterProtection(ExplosionEvent.Detonate event) {
        List<Entity> snapshot = snapshots.remove(event.getExplosion());
        if (snapshot == null) {
            return;
        }
        Explosion explosion = event.getExplosion();
        List<Entity> affected = event.getAffectedEntities();
        // Identity set built once: List.contains is O(n) (and uses equals, not
        // identity), which makes the loop O(n^2) over a big blast's entity list.
        Set<Entity> stillAffected = Collections.newSetFromMap(new IdentityHashMap<>());
        stillAffected.addAll(affected);
        int removed = 0;
        int restored = 0;
        for (Entity entity : snapshot) {
            if (stillAffected.contains(entity)) {
                continue;
            }
            removed++;
            if (!OPACBridge.blocksEntityDamage(
                    explosion.getIndirectSourceEntity(), explosion.getDirectSourceEntity(), entity, "explosion")) {
                affected.add(entity);
                restored++;
            }
        }
        if (removed > 0 && OPACBigCannonsCompat.DEBUG_LOGGING) {
            OPACBigCannonsCompat.LOGGER.info(
                    "[OPAC-CBC] OPAC's explosion filter removed {} of {} entities from a CBC explosion at {}; "
                            + "the attributed re-check restored {} (see the 'entity damage via explosion' verdicts above)",
                    removed, snapshot.size(), explosion.center(), restored);
        }
    }
}
