package com.mobdistancescaling.util;

import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ZoneCalculator {

    @Nullable
    public static DifficultyZone getZoneAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        double distance = calculate2DDistance(x, z);

        DifficultyZone currentZone = null;
        for (DifficultyZone zone : config.getZones()) {
            if (distance >= zone.getRadiusStart()) {
                currentZone = zone;
            } else {
                break;
            }
        }

        return currentZone;
    }

    public static double calculate2DDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static int getZoneIdAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getZoneId() : 0;
    }

    public static double getMultiplierAtPosition(double x, double z, @Nonnull ZoneConfig config) {
        DifficultyZone zone = getZoneAtPosition(x, z, config);
        return zone != null ? zone.getMultiplier() : 1.0;
    }
}
