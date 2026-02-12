package com.varyon.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.component.MobScalingComponent;

import javax.annotation.Nonnull;

/**
 * System that scales outgoing damage from NPCs based on their spawn zone.
 * Intercepts damage events and multiplies damage if source NPC has MobScalingComponent.
 */
public class MobDamageScalingSystem extends DamageEventSystem {
    // Query for all living entities that can receive damage
    private static final Query<EntityStore> QUERY = AllLegacyLivingEntityTypesQuery.INSTANCE;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {

        // Get the source of damage (the attacker)
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return; // Not from an entity (e.g., environmental damage)
        }

        // Get the attacker's Ref
        Ref<EntityStore> attackerRef = ((Damage.EntitySource) source).getRef();

        // Check if attacker has scaling component
        MobScalingComponent scalingComponent = store.getComponent(attackerRef, MobScalingComponent.getComponentType());
        if (scalingComponent == null) {
            return; // Attacker is not a scaled NPC
        }

        // Apply damage multiplier
        float damageMultiplier = scalingComponent.getDamageMultiplier();
        if (damageMultiplier != 1.0f) {
            float currentDamage = damage.getAmount();
            float newDamage = currentDamage * damageMultiplier;
            damage.setAmount(newDamage);
        }
    }
}
