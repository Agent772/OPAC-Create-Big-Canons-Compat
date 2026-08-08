package com.agent772.opaccbccompat.compat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.agent772.opaccbccompat.config.OBCServerConfig;

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
 * entity that the attributed check (owner redirection + exception groups)
 * allows. It only ever re-adds entities OPAC removed - it never removes any -
 * so it can only grant access, mirroring OPAC's own exception-group semantics.
 */
public final class ExplosionEntityGate {

    private final Map<Explosion, List<Entity>> snapshots = new IdentityHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeProtection(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getExplosion() instanceof CustomExplosion)
                || !OBCServerConfig.protectEntities()) {
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
        for (Entity entity : snapshot) {
            if (!affected.contains(entity)
                    && !OPACBridge.blocksEntityDamage(
                            explosion.getIndirectSourceEntity(), explosion.getDirectSourceEntity(), entity)) {
                affected.add(entity);
            }
        }
    }
}
