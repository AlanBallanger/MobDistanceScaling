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
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobScalingRefSystem extends RefSystem<EntityStore> {
    private static final String HEALTH_MODIFIER_KEY = "MobDistanceScaling_Health";

    private final ConfigManager configManager;

    public MobScalingRefSystem(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        System.out.println("========================================");
        System.out.println("ENTITY ADDED! Reason: " + reason);
        System.out.println("========================================");

        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            System.out.println("Not an NPC, skipping");
            return;
        }

        System.out.println("NPC DETECTED!");

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            System.out.println("No TransformComponent, skipping");
            return;
        }

        Vector3d pos = transform.getPosition();
        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(pos.getX(), pos.getZ(), configManager.getZoneConfig());

        System.out.println("NPC at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") - Zone: " + (zone != null ? zone.toString() : "null"));

        if (zone == null || zone.getMultiplier() == 1.0) {
            System.out.println("Zone multiplier is 1.0 or null, no scaling needed");
            return;
        }

        applyHealthScaling(ref, store, zone);

        System.out.println("Applied scaling for " + zone + " - HP multiplier: " + zone.getMultiplier() + "x");
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Nothing to do on remove
    }

    private void applyHealthScaling(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull DifficultyZone zone) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());

        if (statMap == null) {
            System.out.println("WARNING: EntityStatMap not found!");
            return;
        }

        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthIndex);

        if (healthStat == null) {
            System.out.println("WARNING: Health stat not found!");
            return;
        }

        float originalMaxHealth = healthStat.getMax();
        float multiplier = (float) zone.getMultiplier();

        System.out.println("Applying health scaling: " + originalMaxHealth + " HP -> " + (originalMaxHealth * multiplier) + " HP (x" + multiplier + ")");

        StaticModifier healthModifier = new StaticModifier(
                StaticModifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                multiplier
        );

        statMap.putModifier(healthIndex, HEALTH_MODIFIER_KEY, healthModifier);

        float newMaxHealth = originalMaxHealth * multiplier;
        statMap.setStatValue(healthIndex, newMaxHealth);

        System.out.println("Health scaling applied: " + originalMaxHealth + " -> " + newMaxHealth);
    }
}
