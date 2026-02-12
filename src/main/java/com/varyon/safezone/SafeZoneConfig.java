package com.varyon.safezone;

public class SafeZoneConfig {
    private boolean enabled;
    private int minRotationTimeMinutes;
    private int maxRotationTimeMinutes;
    private int overlapDurationMinutes;
    private int maxRadius;
    private String enterSafeZoneTitle;
    private String enterSafeZoneSubtitle;
    private String enterPvpZoneTitle;
    private String enterPvpZoneSubtitle;

    public SafeZoneConfig() {
        this.enabled = true;
        this.minRotationTimeMinutes = 60;
        this.maxRotationTimeMinutes = 120;
        this.overlapDurationMinutes = 10;
        this.maxRadius = -1;
        this.enterSafeZoneTitle = "Zone Safe";
        this.enterSafeZoneSubtitle = "PvP Désactivé";
        this.enterPvpZoneTitle = "Zone PvP";
        this.enterPvpZoneSubtitle = "Attention !";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinRotationTimeMinutes() {
        return minRotationTimeMinutes;
    }

    public void setMinRotationTimeMinutes(int minRotationTimeMinutes) {
        this.minRotationTimeMinutes = minRotationTimeMinutes;
    }

    public int getMaxRotationTimeMinutes() {
        return maxRotationTimeMinutes;
    }

    public void setMaxRotationTimeMinutes(int maxRotationTimeMinutes) {
        this.maxRotationTimeMinutes = maxRotationTimeMinutes;
    }

    public int getOverlapDurationMinutes() {
        return overlapDurationMinutes;
    }

    public void setOverlapDurationMinutes(int overlapDurationMinutes) {
        this.overlapDurationMinutes = overlapDurationMinutes;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    public long getMinRotationTimeMillis() {
        return minRotationTimeMinutes * 60L * 1000L;
    }

    public long getMaxRotationTimeMillis() {
        return maxRotationTimeMinutes * 60L * 1000L;
    }

    public long getOverlapDurationMillis() {
        return overlapDurationMinutes * 60L * 1000L;
    }

    public String getEnterSafeZoneTitle() {
        return enterSafeZoneTitle;
    }

    public void setEnterSafeZoneTitle(String enterSafeZoneTitle) {
        this.enterSafeZoneTitle = enterSafeZoneTitle;
    }

    public String getEnterSafeZoneSubtitle() {
        return enterSafeZoneSubtitle;
    }

    public void setEnterSafeZoneSubtitle(String enterSafeZoneSubtitle) {
        this.enterSafeZoneSubtitle = enterSafeZoneSubtitle;
    }

    public String getEnterPvpZoneTitle() {
        return enterPvpZoneTitle;
    }

    public void setEnterPvpZoneTitle(String enterPvpZoneTitle) {
        this.enterPvpZoneTitle = enterPvpZoneTitle;
    }

    public String getEnterPvpZoneSubtitle() {
        return enterPvpZoneSubtitle;
    }

    public void setEnterPvpZoneSubtitle(String enterPvpZoneSubtitle) {
        this.enterPvpZoneSubtitle = enterPvpZoneSubtitle;
    }
}
