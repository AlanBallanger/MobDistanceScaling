package com.mobdistancescaling.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.mobdistancescaling.component.MobScalingComponent;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobScalingRefSystem extends RefSystem<EntityStore> {
    private static final String HEALTH_MODIFIER_KEY = "MobDistanceScaling_Health";

    private final ConfigManager configManager;
    private Query<EntityStore> query;

    public MobScalingRefSystem(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        // Use empty archetype because NPCEntity.getComponentType() is not available at plugin setup time
        // We filter for NPCs manually in onEntityAdded()
        if (query == null) {
            query = Archetype.empty();
        }
        return query;
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Filter for NPCs only
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return; // Not an NPC
        }

        // Only process NPCs that are freshly spawned, not loaded from disk
        // (loaded NPCs already have their MobScalingComponent from when they were spawned)
        if (reason != AddReason.SPAWN) {
            return;
        }

        // Check if scaling is enabled for this world
        String worldName = store.getExternalData().getWorld().getName();
        if (!configManager.getZoneConfig().isWorldEnabled(worldName)) {
            return; // Scaling not enabled for this world
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d pos = transform.getPosition();
        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(pos.getX(), pos.getZ(), configManager.getZoneConfig());

        // No scaling needed for zone 1 (multiplier 1.0) or if zone not found
        if (zone == null || zone.getMultiplier() == 1.0) {
            return;
        }

        applyScaling(ref, store, commandBuffer, zone);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Nothing to do on remove
    }

    private void applyScaling(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DifficultyZone zone) {
        float multiplier = (float) zone.getMultiplier();

        // Check if component already exists (safety check)
        MobScalingComponent existing = store.getComponent(ref, MobScalingComponent.getComponentType());
        if (existing != null) {
            return;
        }

        // Add MobScalingComponent to store multiplier for damage scaling
        commandBuffer.addComponent(ref, MobScalingComponent.getComponentType(), new MobScalingComponent(multiplier));

        // Apply health scaling
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            return;
        }

        float originalMaxHealth = healthStat.getMax();

        // Apply multiplicative modifier to max health
        StaticModifier healthModifier = new StaticModifier(
                StaticModifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                multiplier
        );
        statMap.putModifier(healthIndex, HEALTH_MODIFIER_KEY, healthModifier);

        // Set current health to new max
        float newMaxHealth = originalMaxHealth * multiplier;
        statMap.setStatValue(healthIndex, newMaxHealth);
    }
}
