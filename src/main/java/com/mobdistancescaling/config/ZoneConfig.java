package com.mobdistancescaling.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZoneConfig {
    private final List<String> enabledWorlds;
    private final List<DifficultyZone> zones;
    private boolean minimapEnabled;
    private int minimapOpacity;
    private String minimapPattern;
    private int minimapPatternSize;
    private boolean zoneEnterNotification;
    private String zoneEnterTopText;
    private float notificationDuration;
    private boolean zoneSoundEnabled;
    private String zoneSoundId;
    private float zoneSoundVolume;
    private float zoneSoundPitch;
    private boolean zoneHudEnabled;
    private String hudLabelHealth;
    private String hudLabelDamage;
    private String hudLabelLoot;
    private String hudLabelMultipliers;

    public ZoneConfig() {
        this.enabledWorlds = new ArrayList<>();
        this.zones = new ArrayList<>();
        this.minimapEnabled = true;
        this.minimapOpacity = 50;
        this.minimapPattern = "SOLID";
        this.minimapPatternSize = 4;
        this.zoneEnterNotification = true;
        this.zoneEnterTopText = "Zone";
        this.notificationDuration = 2.0f;
        this.zoneSoundEnabled = true;
        this.zoneSoundId = "SFX_Axe_Special_Swing";
        this.zoneSoundVolume = 1.0f;
        this.zoneSoundPitch = 1.0f;
        this.zoneHudEnabled = true;
        this.hudLabelHealth = "HP";
        this.hudLabelDamage = "DMG";
        this.hudLabelLoot = "Loot";
        this.hudLabelMultipliers = "Multiplicateurs";
    }

    public ZoneConfig(@Nonnull List<String> enabledWorlds, @Nonnull List<DifficultyZone> zones,
                      boolean minimapEnabled, int minimapOpacity, String minimapPattern, int minimapPatternSize,
                      boolean zoneEnterNotification, String zoneEnterTopText, float notificationDuration,
                      boolean zoneSoundEnabled, String zoneSoundId, float zoneSoundVolume, float zoneSoundPitch,
                      boolean zoneHudEnabled, String hudLabelHealth, String hudLabelDamage, 
                      String hudLabelLoot, String hudLabelMultipliers) {
        this.enabledWorlds = new ArrayList<>(enabledWorlds);
        this.zones = new ArrayList<>(zones);
        this.minimapEnabled = minimapEnabled;
        this.minimapOpacity = minimapOpacity;
        this.minimapPattern = minimapPattern != null ? minimapPattern : "SOLID";
        this.minimapPatternSize = minimapPatternSize > 0 ? minimapPatternSize : 4;
        this.zoneEnterNotification = zoneEnterNotification;
        this.zoneEnterTopText = zoneEnterTopText != null ? zoneEnterTopText : "Zone";
        this.notificationDuration = notificationDuration > 0 ? notificationDuration : 2.0f;
        this.zoneSoundEnabled = zoneSoundEnabled;
        this.zoneSoundId = zoneSoundId != null ? zoneSoundId : "SFX_Axe_Special_Swing";
        this.zoneSoundVolume = zoneSoundVolume > 0 ? zoneSoundVolume : 1.0f;
        this.zoneSoundPitch = zoneSoundPitch > 0 ? zoneSoundPitch : 1.0f;
        this.zoneHudEnabled = zoneHudEnabled;
        this.hudLabelHealth = hudLabelHealth != null ? hudLabelHealth : "HP";
        this.hudLabelDamage = hudLabelDamage != null ? hudLabelDamage : "DMG";
        this.hudLabelLoot = hudLabelLoot != null ? hudLabelLoot : "Loot";
        this.hudLabelMultipliers = hudLabelMultipliers != null ? hudLabelMultipliers : "Multiplicateurs";
    }

    @Nonnull
    public List<String> getEnabledWorlds() {
        return Collections.unmodifiableList(enabledWorlds);
    }

    public boolean isWorldEnabled(@Nonnull String worldName) {
        return enabledWorlds.contains(worldName);
    }

    @Nonnull
    public List<DifficultyZone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public void addZone(@Nonnull DifficultyZone zone) {
        zones.add(zone);
    }

    public boolean isMinimapEnabled() {
        return minimapEnabled;
    }

    public int getMinimapOpacity() {
        return minimapOpacity;
    }

    @Nonnull
    public String getMinimapPattern() {
        return minimapPattern != null ? minimapPattern : "SOLID";
    }

    public int getMinimapPatternSize() {
        return minimapPatternSize > 0 ? minimapPatternSize : 4;
    }

    public boolean isZoneEnterNotification() {
        return zoneEnterNotification;
    }

    @Nonnull
    public String getZoneEnterTopText() {
        return zoneEnterTopText != null ? zoneEnterTopText : "Zone";
    }

    public float getNotificationDuration() {
        return notificationDuration > 0 ? notificationDuration : 2.0f;
    }

    public boolean isZoneSoundEnabled() {
        return zoneSoundEnabled;
    }

    @Nonnull
    public String getZoneSoundId() {
        return zoneSoundId != null ? zoneSoundId : "SFX_Axe_Special_Swing";
    }

    public float getZoneSoundVolume() {
        return zoneSoundVolume > 0 ? zoneSoundVolume : 1.0f;
    }

    public float getZoneSoundPitch() {
        return zoneSoundPitch > 0 ? zoneSoundPitch : 1.0f;
    }

    public boolean isZoneHudEnabled() {
        return zoneHudEnabled;
    }

    @Nonnull
    public String getHudLabelHealth() {
        return hudLabelHealth;
    }

    public void setHudLabelHealth(@Nonnull String hudLabelHealth) {
        this.hudLabelHealth = hudLabelHealth;
    }

    @Nonnull
    public String getHudLabelDamage() {
        return hudLabelDamage;
    }

    public void setHudLabelDamage(@Nonnull String hudLabelDamage) {
        this.hudLabelDamage = hudLabelDamage;
    }

    @Nonnull
    public String getHudLabelLoot() {
        return hudLabelLoot;
    }

    public void setHudLabelLoot(@Nonnull String hudLabelLoot) {
        this.hudLabelLoot = hudLabelLoot;
    }

    @Nonnull
    public String getHudLabelMultipliers() {
        return hudLabelMultipliers;
    }

    public void setHudLabelMultipliers(@Nonnull String hudLabelMultipliers) {
        this.hudLabelMultipliers = hudLabelMultipliers;
    }

    @Nonnull
    public static ZoneConfig createDefault() {
        List<String> worlds = Arrays.asList("default");

        ZoneConfig config = new ZoneConfig();
        config.enabledWorlds.addAll(worlds);
        config.minimapEnabled = true;
        config.minimapOpacity = 50;
        config.minimapPattern = "SOLID";
        config.minimapPatternSize = 4;
        config.zoneEnterNotification = true;
        config.zoneEnterTopText = "Zone";
        config.notificationDuration = 2.0f;
        config.zoneSoundEnabled = true;
        config.zoneSoundId = "SFX_Axe_Special_Swing";
        config.zoneSoundVolume = 1.0f;
        config.zoneSoundPitch = 1.0f;
        config.zoneHudEnabled = true;
        config.hudLabelHealth = "HP";
        config.hudLabelDamage = "DMG";
        config.hudLabelLoot = "Loot";
        config.hudLabelMultipliers = "Multiplicateurs";

        // Zone constructor: (id, color, healthMultiplier, damageMultiplier, lootMultiplier, radiusStart, name)
        config.addZone(new DifficultyZone(1, "WHITE", 1.0, 1.0, 1.0, 0, "Safe Zone"));
        config.addZone(new DifficultyZone(2, "#55FF55", 1.5, 1.25, 1.5, 2000, "Easy"));
        config.addZone(new DifficultyZone(3, "LIME", 2.0, 1.5, 2.0, 4000, "Normal"));
        config.addZone(new DifficultyZone(4, "YELLOW", 2.5, 1.75, 2.5, 6000, "Moderate"));
        config.addZone(new DifficultyZone(5, "GOLD", 3.0, 2.0, 3.0, 8000, "Challenging"));
        config.addZone(new DifficultyZone(6, "ORANGE", 3.5, 2.25, 3.5, 10000, "Hard"));
        config.addZone(new DifficultyZone(7, "RED", 4.0, 2.5, 4.0, 12000, "Very Hard"));
        config.addZone(new DifficultyZone(8, "#8B0000", 4.5, 2.75, 4.5, 14000, "Extreme"));
        config.addZone(new DifficultyZone(9, "PURPLE", 5.0, 3.0, 5.0, 16000, "Nightmare"));
        config.addZone(new DifficultyZone(10, "#1A1A1A", 5.5, 3.5, 6.0, 18000, "Hell"));

        return config;
    }
}
