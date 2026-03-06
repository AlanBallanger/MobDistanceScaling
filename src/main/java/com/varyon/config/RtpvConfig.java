package com.varyon.config;

public class RtpvConfig {
    private final double safeCostMultiplier;
    private final boolean economyEnabled;
    private final int joinDurationSeconds;

    public RtpvConfig(double safeCostMultiplier, boolean economyEnabled) {
        this(safeCostMultiplier, economyEnabled, 60);
    }

    public RtpvConfig(double safeCostMultiplier, boolean economyEnabled, int joinDurationSeconds) {
        this.safeCostMultiplier = safeCostMultiplier;
        this.economyEnabled = economyEnabled;
        this.joinDurationSeconds = joinDurationSeconds > 0 ? joinDurationSeconds : 60;
    }

    public static RtpvConfig createDefault() {
        return new RtpvConfig(2.0, true, 60);
    }

    public double getSafeCostMultiplier() { return safeCostMultiplier; }
    public boolean isEconomyEnabled()     { return economyEnabled; }
    public int getJoinDurationSeconds()   { return joinDurationSeconds; }
}
