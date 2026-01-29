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
    private String minimapPattern;  // SOLID, DOTS, STRIPES, CROSSHATCH, GRID, BORDER
    private int minimapPatternSize; // Size/spacing of the pattern (2-10)

    public ZoneConfig() {
        this.enabledWorlds = new ArrayList<>();
        this.zones = new ArrayList<>();
        this.minimapEnabled = true;
        this.minimapOpacity = 30;
        this.minimapPattern = "STRIPES";
        this.minimapPatternSize = 4;
    }

    public ZoneConfig(@Nonnull List<String> enabledWorlds, @Nonnull List<DifficultyZone> zones,
                      boolean minimapEnabled, int minimapOpacity, String minimapPattern, int minimapPatternSize) {
        this.enabledWorlds = new ArrayList<>(enabledWorlds);
        this.zones = new ArrayList<>(zones);
        this.minimapEnabled = minimapEnabled;
        this.minimapOpacity = minimapOpacity;
        this.minimapPattern = minimapPattern != null ? minimapPattern : "STRIPES";
        this.minimapPatternSize = minimapPatternSize > 0 ? minimapPatternSize : 4;
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
        return minimapPattern != null ? minimapPattern : "STRIPES";
    }

    public int getMinimapPatternSize() {
        return minimapPatternSize > 0 ? minimapPatternSize : 4;
    }

    @Nonnull
    public static ZoneConfig createDefault() {
        List<String> worlds = Arrays.asList("default");

        ZoneConfig config = new ZoneConfig();
        config.enabledWorlds.addAll(worlds);
        config.minimapEnabled = true;
        config.minimapOpacity = 50;
        config.minimapPattern = "STRIPES";
        config.minimapPatternSize = 4;

        config.addZone(new DifficultyZone(1, "WHITE", 1.0, 0));
        config.addZone(new DifficultyZone(2, "GREEN", 1.5, 2000));
        config.addZone(new DifficultyZone(3, "LIME", 2.0, 4000));
        config.addZone(new DifficultyZone(4, "YELLOW", 2.5, 6000));
        config.addZone(new DifficultyZone(5, "GOLD", 3.0, 8000));
        config.addZone(new DifficultyZone(6, "ORANGE", 3.5, 10000));
        config.addZone(new DifficultyZone(7, "RED", 4.0, 12000));
        config.addZone(new DifficultyZone(8, "DARK_RED", 4.5, 14000));
        config.addZone(new DifficultyZone(9, "PURPLE", 5.0, 16000));
        config.addZone(new DifficultyZone(10, "BLACK", 5.5, 18000));

        return config;
    }
}
