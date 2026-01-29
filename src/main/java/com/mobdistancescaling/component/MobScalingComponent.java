package com.mobdistancescaling.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component that stores the damage/health multiplier for a scaled NPC.
 * This is calculated once at spawn based on distance from world origin.
 */
public class MobScalingComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, MobScalingComponent> COMPONENT_TYPE;

    private final float multiplier;

    public MobScalingComponent(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return multiplier;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new MobScalingComponent(multiplier);
    }

    @Nonnull
    public static ComponentType<EntityStore, MobScalingComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, MobScalingComponent> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
