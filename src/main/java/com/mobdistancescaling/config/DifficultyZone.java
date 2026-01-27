package com.mobdistancescaling.config;

import javax.annotation.Nonnull;

public class DifficultyZone {
    private final int zoneId;
    private final String color;
    private final double multiplier;
    private final int radiusStart;

    public DifficultyZone(int zoneId, @Nonnull String color, double multiplier, int radiusStart) {
        this.zoneId = zoneId;
        this.color = color;
        this.multiplier = multiplier;
        this.radiusStart = radiusStart;
    }

    public int getZoneId() {
        return zoneId;
    }

    @Nonnull
    public String getColor() {
        return color;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getRadiusStart() {
        return radiusStart;
    }

    @Override
    public String toString() {
        return "Zone " + zoneId + " (x" + multiplier + ", " + radiusStart + "+ blocks, " + color + ")";
    }
}
