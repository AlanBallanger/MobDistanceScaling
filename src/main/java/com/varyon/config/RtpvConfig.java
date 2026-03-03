package com.varyon.config;

import java.util.ArrayList;
import java.util.List;

public class RtpvConfig {
    private final List<Integer> zoneCosts;
    private final double safeCostMultiplier;
    private final boolean economyEnabled;

    public RtpvConfig(List<Integer> zoneCosts, double safeCostMultiplier, boolean economyEnabled) {
        this.zoneCosts = zoneCosts;
        this.safeCostMultiplier = safeCostMultiplier;
        this.economyEnabled = economyEnabled;
    }

    public static RtpvConfig createDefault() {
        List<Integer> defaultCosts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            defaultCosts.add(i * 100);
        }
        return new RtpvConfig(defaultCosts, 2.0, true);
    }

    public int getCostForZone(int zoneNumber) {
        int idx = zoneNumber - 1;
        if (idx >= 0 && idx < zoneCosts.size()) {
            return zoneCosts.get(idx);
        }
        return zoneNumber * 100;
    }

    public double getSafeCostMultiplier() { return safeCostMultiplier; }
    public boolean isEconomyEnabled() { return economyEnabled; }
    public List<Integer> getZoneCosts() { return zoneCosts; }
}
