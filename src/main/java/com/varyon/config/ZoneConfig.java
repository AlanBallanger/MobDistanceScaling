package com.varyon.config;

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
    
    private String rtpvMessageTeleporting;
    private String rtpvMessageSuccess;
    private String rtpvMessageNoSafeLocation;
    private String rtpvMessageError;
    private String rtpvMessageWorldNotSupported;
    private String rtpvMessageZoneNotFound;
    private String rtpvMessageAvailableZones;
    private String rtpvMessageRandomZone;

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
        
        this.rtpvMessageTeleporting = "Teleporting...";
        this.rtpvMessageSuccess = "Teleported to zone: {zone} ({x}, {y}, {z})";
        this.rtpvMessageNoSafeLocation = "Could not find a safe location after {attempts} attempts";
        this.rtpvMessageError = "Teleportation error";
        this.rtpvMessageWorldNotSupported = "This world does not support zone teleportation";
        this.rtpvMessageZoneNotFound = "Zone '{zone}' not found.";
        this.rtpvMessageAvailableZones = "Available zones";
        this.rtpvMessageRandomZone = "random";
    }

    public ZoneConfig(@Nonnull List<String> enabledWorlds, @Nonnull List<DifficultyZone> zones,
                      boolean minimapEnabled, int minimapOpacity, String minimapPattern, int minimapPatternSize,
                      boolean zoneEnterNotification, String zoneEnterTopText, float notificationDuration,
                      boolean zoneSoundEnabled, String zoneSoundId, float zoneSoundVolume, float zoneSoundPitch,
                      boolean zoneHudEnabled, String hudLabelHealth, String hudLabelDamage, 
                      String hudLabelLoot, String hudLabelMultipliers,
                      String rtpvMessageTeleporting, String rtpvMessageSuccess, String rtpvMessageNoSafeLocation,
                      String rtpvMessageError, String rtpvMessageWorldNotSupported, String rtpvMessageZoneNotFound,
                      String rtpvMessageAvailableZones, String rtpvMessageRandomZone) {
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
        
        this.rtpvMessageTeleporting = rtpvMessageTeleporting != null ? rtpvMessageTeleporting : "Teleporting...";
        this.rtpvMessageSuccess = rtpvMessageSuccess != null ? rtpvMessageSuccess : "Teleported to zone: {zone} ({x}, {y}, {z})";
        this.rtpvMessageNoSafeLocation = rtpvMessageNoSafeLocation != null ? rtpvMessageNoSafeLocation : "Could not find a safe location after {attempts} attempts";
        this.rtpvMessageError = rtpvMessageError != null ? rtpvMessageError : "Teleportation error";
        this.rtpvMessageWorldNotSupported = rtpvMessageWorldNotSupported != null ? rtpvMessageWorldNotSupported : "This world does not support zone teleportation";
        this.rtpvMessageZoneNotFound = rtpvMessageZoneNotFound != null ? rtpvMessageZoneNotFound : "Zone '{zone}' not found.";
        this.rtpvMessageAvailableZones = rtpvMessageAvailableZones != null ? rtpvMessageAvailableZones : "Available zones";
        this.rtpvMessageRandomZone = rtpvMessageRandomZone != null ? rtpvMessageRandomZone : "random";
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
    public String getRtpvMessageTeleporting() {
        return rtpvMessageTeleporting;
    }
    
    @Nonnull
    public String getRtpvMessageSuccess() {
        return rtpvMessageSuccess;
    }
    
    @Nonnull
    public String getRtpvMessageNoSafeLocation() {
        return rtpvMessageNoSafeLocation;
    }
    
    @Nonnull
    public String getRtpvMessageError() {
        return rtpvMessageError;
    }
    
    @Nonnull
    public String getRtpvMessageWorldNotSupported() {
        return rtpvMessageWorldNotSupported;
    }
    
    @Nonnull
    public String getRtpvMessageZoneNotFound() {
        return rtpvMessageZoneNotFound;
    }
    
    @Nonnull
    public String getRtpvMessageAvailableZones() {
        return rtpvMessageAvailableZones;
    }
    
    @Nonnull
    public String getRtpvMessageRandomZone() {
        return rtpvMessageRandomZone;
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
        
        config.rtpvMessageTeleporting = "Teleporting...";
        config.rtpvMessageSuccess = "Teleported to zone: {zone} ({x}, {y}, {z})";
        config.rtpvMessageNoSafeLocation = "Could not find a safe location after {attempts} attempts";
        config.rtpvMessageError = "Teleportation error";
        config.rtpvMessageWorldNotSupported = "This world does not support zone teleportation";
        config.rtpvMessageZoneNotFound = "Zone '{zone}' not found.";
        config.rtpvMessageAvailableZones = "Available zones";
        config.rtpvMessageRandomZone = "random";

        // Zone constructor: (id, color, healthMultiplier, damageMultiplier, lootMultiplier, essenceMultiplier, radiusStart, name)
        config.addZone(new DifficultyZone(1, "#55FF55", 1.5, 1.25, 1.2, 1.2, 0, "Easy"));
        config.addZone(new DifficultyZone(2, "#55FFAA", 2.0, 1.5, 1.4, 1.4, 5000, "Normal"));
        config.addZone(new DifficultyZone(3, "#E6FF33", 2.75, 1.9, 1.6, 1.6, 7500, "Moderate"));
        config.addZone(new DifficultyZone(4, "#FFC533", 3.5, 2.25, 1.8, 1.8, 10000, "Challenging"));
        config.addZone(new DifficultyZone(5, "#FF7A1A", 4.5, 2.8, 2.0, 2.0, 12500, "Hard"));
        config.addZone(new DifficultyZone(6, "#FF3030", 5.5, 3.5, 2.25, 2.25, 15000, "Very Hard"));
        config.addZone(new DifficultyZone(7, "#C0003A", 7.0, 4.25, 2.5, 2.5, 17500, "Extreme"));
        config.addZone(new DifficultyZone(8, "#8A2BFF", 8.5, 5.0, 2.75, 2.75, 20000, "Nightmare"));
        config.addZone(new DifficultyZone(9, "#05000A", 11.0, 6.0, 3.0, 3.0, 22500, "Hell"));

        return config;
    }
}
