package com.varyon.config;

public class RtpvConfig {
    private final double safeCostMultiplier;
    private final boolean economyEnabled;

    public RtpvConfig(double safeCostMultiplier, boolean economyEnabled) {
        this.safeCostMultiplier = safeCostMultiplier;
        this.economyEnabled = economyEnabled;
    }

    public static RtpvConfig createDefault() {
        return new RtpvConfig(2.0, true);
    }

    public double getSafeCostMultiplier() { return safeCostMultiplier; }
    public boolean isEconomyEnabled()     { return economyEnabled; }
}
