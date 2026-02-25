package com.varyon.config;

public class ExtractionConfig {
    private boolean enabled;
    private int minDistance;
    private int maxDistance;
    private int portalDurationSeconds;
    private int cooldownSeconds;

    public ExtractionConfig() {
        this.enabled = true;
        this.minDistance = 100;
        this.maxDistance = 200;
        this.portalDurationSeconds = 300;
        this.cooldownSeconds = 300;
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
}
