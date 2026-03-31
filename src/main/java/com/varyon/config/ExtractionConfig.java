package com.varyon.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ExtractionConfig {
    private boolean enabled;
    private int minDistance;
    private int maxDistance;
    private int portalDurationSeconds;
    private int cooldownSeconds;
    private List<ExtractionZoneDistance> zoneRanges;

    public ExtractionConfig() {
        this.enabled = true;
        this.minDistance = 100;
        this.maxDistance = 200;
        this.portalDurationSeconds = 300;
        this.cooldownSeconds = 300;
        this.zoneRanges = new ArrayList<>();
    }

    public boolean isEnabled()                      { return enabled; }
    public void setEnabled(boolean v)               { this.enabled = v; }
    public int getMinDistance()                     { return minDistance; }
    public void setMinDistance(int v)               { this.minDistance = v; }
    public int getMaxDistance()                     { return maxDistance; }
    public void setMaxDistance(int v)               { this.maxDistance = v; }
    public int getPortalDurationSeconds()           { return portalDurationSeconds; }
    public void setPortalDurationSeconds(int v)     { this.portalDurationSeconds = v; }
    public int getCooldownSeconds()                 { return cooldownSeconds; }
    public void setCooldownSeconds(int v)           { this.cooldownSeconds = v; }

    @Nonnull
    public List<ExtractionZoneDistance> getZoneRanges() {
        return zoneRanges;
    }

    public void setZoneRanges(@Nonnull List<ExtractionZoneDistance> zoneRanges) {
        this.zoneRanges = new ArrayList<>(zoneRanges);
    }

    public int getEffectiveMinDistance(int zoneId) {
        for (ExtractionZoneDistance z : zoneRanges) {
            if (z.zoneId() == zoneId) {
                return z.minDistance();
            }
        }
        return minDistance;
    }

    public int getEffectiveMaxDistance(int zoneId) {
        for (ExtractionZoneDistance z : zoneRanges) {
            if (z.zoneId() == zoneId) {
                return z.maxDistance();
            }
        }
        return maxDistance;
    }

    public static void applyDefaultZoneRangesForNewConfig(@Nonnull ExtractionConfig config) {
        config.setZoneRanges(List.of(
            new ExtractionZoneDistance(1, 20, 50),
            new ExtractionZoneDistance(10, 400, 500)
        ));
    }
}
