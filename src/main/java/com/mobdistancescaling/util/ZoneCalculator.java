package com.mobdistancescaling.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ZoneCalculator {

    @Nullable
    public static DifficultyZone getZoneAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        double distance = calculate2DDistance(x, z);

        // Find the zone with the highest radiusStart that is <= distance
        DifficultyZone currentZone = null;
        List<DifficultyZone> zones = config.getZones();

        for (DifficultyZone zone : zones) {
            if (distance >= zone.getRadiusStart()) {
                // Keep updating to get the zone with highest radiusStart <= distance
                if (currentZone == null || zone.getRadiusStart() > currentZone.getRadiusStart()) {
                    currentZone = zone;
                }
            }
        }

        return currentZone;
    }

    @Nullable
    public static DifficultyZone getCurrentZone(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull ZoneConfig config) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }
        
        double x = transform.getPosition().getX();
        double z = transform.getPosition().getZ();
        
        return getZoneAtPosition(x, z, config);
    }

    public static double calculate2DDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static int getZoneIdAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getZoneId() : 0;
    }

    public static double getMaxMultiplierAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getMaxMultiplier() : 1.0;
    }
}
