package com.agent772.opaccbccompat.compat;

import com.agent772.opaccbccompat.config.OBCServerConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * A {@link DamageSource} wrapper that carries the CBC projectile as the direct
 * entity and its owner as the causing entity. CBC's {@code CannonDamageSource}
 * is constructed with no entities at all, so OPAC's {@code LivingIncomingDamageEvent}
 * handler - which re-checks every hurt through {@code onEntityInteraction} using
 * the damage source's entities - sees an anonymous action and blocks it in any
 * protected claim, no matter what the bridge's earlier checks decided. Wrapping
 * the source restores attribution for that final check, so exception groups
 * (matched against the projectile's entity type) and owner redirection apply.
 *
 * <p>Every behavioural override of {@code DamageSource} is delegated to the
 * wrapped source so CBC's customisations (e.g. {@code CannonDamageSource}'s
 * bypass-armor / always-kills-armor-stands tag behaviour, death message and
 * difficulty scaling) are preserved rather than silently reverting to vanilla.
 * Only the carried entities change.
 */
public final class AttributedDamageSource extends DamageSource {

    private final DamageSource delegate;

    private AttributedDamageSource(DamageSource delegate, Entity direct, Entity owner) {
        super(delegate.typeHolder(), direct, owner);
        this.delegate = delegate;
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        return delegate.is(tag);
    }

    @Override
    public boolean is(ResourceKey<DamageType> damageTypeKey) {
        return delegate.is(damageTypeKey);
    }

    @Override
    public boolean scalesWithDifficulty() {
        return delegate.scalesWithDifficulty();
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        return delegate.getLocalizedDeathMessage(entity);
    }

    /**
     * Attributes {@code source} to {@code projectile} (and its owner, if any).
     * Sources that already carry an entity are returned unchanged, as is
     * everything when entity protection is disabled.
     */
    public static DamageSource attribute(DamageSource source, Entity projectile) {
        if (!OBCServerConfig.protectEntities()
                || source.getDirectEntity() != null || source.getEntity() != null) {
            return source;
        }
        Entity owner = projectile instanceof Projectile p ? p.getOwner() : null;
        return new AttributedDamageSource(source, projectile, owner);
    }
}
