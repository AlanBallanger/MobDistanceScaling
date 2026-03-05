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
    }

    public ZoneConfig(@Nonnull List<String> enabledWorlds, @Nonnull List<DifficultyZone> zones,
                      boolean minimapEnabled, int minimapOpacity, String minimapPattern, int minimapPatternSize,
                      boolean zoneEnterNotification, String zoneEnterTopText, float notificationDuration,
                      boolean zoneSoundEnabled, String zoneSoundId, float zoneSoundVolume, float zoneSoundPitch,
                      boolean zoneHudEnabled) {
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
    }

    @Nonnull
    public List<String> getEnabledWorlds() { return Collections.unmodifiableList(enabledWorlds); }

    public boolean isWorldEnabled(@Nonnull String worldName) { return enabledWorlds.contains(worldName); }

    @Nonnull
    public List<DifficultyZone> getZones() { return Collections.unmodifiableList(zones); }

    public void addZone(@Nonnull DifficultyZone zone) { zones.add(zone); }

    public boolean isMinimapEnabled()           { return minimapEnabled; }
    public int getMinimapOpacity()              { return minimapOpacity; }
    @Nonnull public String getMinimapPattern()  { return minimapPattern != null ? minimapPattern : "SOLID"; }
    public int getMinimapPatternSize()          { return minimapPatternSize > 0 ? minimapPatternSize : 4; }
    public boolean isZoneEnterNotification()    { return zoneEnterNotification; }
    @Nonnull public String getZoneEnterTopText(){ return zoneEnterTopText != null ? zoneEnterTopText : "Zone"; }
    public float getNotificationDuration()      { return notificationDuration > 0 ? notificationDuration : 2.0f; }
    public boolean isZoneSoundEnabled()         { return zoneSoundEnabled; }
    @Nonnull public String getZoneSoundId()     { return zoneSoundId != null ? zoneSoundId : "SFX_Axe_Special_Swing"; }
    public float getZoneSoundVolume()           { return zoneSoundVolume > 0 ? zoneSoundVolume : 1.0f; }
    public float getZoneSoundPitch()            { return zoneSoundPitch > 0 ? zoneSoundPitch : 1.0f; }
    public boolean isZoneHudEnabled()           { return zoneHudEnabled; }

    @Nonnull
    public static ZoneConfig createDefault() {
        ZoneConfig config = new ZoneConfig();
        config.enabledWorlds.addAll(Arrays.asList("default"));
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

        config.addZone(new DifficultyZone(1,  "#55FF55", 1.5,  1.25, 1.2,  1.2,  0,     "Easy",        100));
        config.addZone(new DifficultyZone(2,  "#55FFAA", 2.0,  1.5,  1.4,  1.4,  5000,  "Normal",      200));
        config.addZone(new DifficultyZone(3,  "#E6FF33", 2.75, 1.9,  1.6,  1.6,  7500,  "Moderate",    300));
        config.addZone(new DifficultyZone(4,  "#FFC533", 3.5,  2.25, 1.8,  1.8,  10000, "Challenging", 400));
        config.addZone(new DifficultyZone(5,  "#FF7A1A", 4.5,  2.8,  2.0,  2.0,  12500, "Hard",        500));
        config.addZone(new DifficultyZone(6,  "#FF3030", 5.5,  3.5,  2.25, 2.25, 15000, "Very Hard",   600));
        config.addZone(new DifficultyZone(7,  "#C0003A", 7.0,  4.25, 2.5,  2.5,  17500, "Extreme",     700));
        config.addZone(new DifficultyZone(8,  "#8A2BFF", 8.5,  5.0,  2.75, 2.75, 20000, "Nightmare",   800));
        config.addZone(new DifficultyZone(9,  "#05000A", 11.0, 6.0,  3.0,  3.0,  22500, "Hell",        900));
        config.addZone(new DifficultyZone(10, "#3D0000", 14.0, 7.5,  3.25, 3.25, 25000, "Abyss",       1000));

        return config;
    }
}
