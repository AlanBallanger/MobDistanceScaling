package com.mobdistancescaling.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZoneConfig {
    private final List<DifficultyZone> zones;

    public ZoneConfig() {
        this.zones = new ArrayList<>();
    }

    public ZoneConfig(@Nonnull List<DifficultyZone> zones) {
        this.zones = new ArrayList<>(zones);
    }

    @Nonnull
    public List<DifficultyZone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public void addZone(@Nonnull DifficultyZone zone) {
        zones.add(zone);
    }

    @Nonnull
    public static ZoneConfig createDefault() {
        ZoneConfig config = new ZoneConfig();

        config.addZone(new DifficultyZone(0, "WHITE", 1.0, 0));
        config.addZone(new DifficultyZone(1, "GREEN", 1.5, 2000));
        config.addZone(new DifficultyZone(2, "LIME", 2.0, 4000));
        config.addZone(new DifficultyZone(3, "YELLOW", 2.5, 6000));
        config.addZone(new DifficultyZone(4, "GOLD", 3.0, 8000));
        config.addZone(new DifficultyZone(5, "ORANGE", 3.5, 10000));
        config.addZone(new DifficultyZone(6, "RED", 4.0, 12000));
        config.addZone(new DifficultyZone(7, "DARK_RED", 4.5, 14000));
        config.addZone(new DifficultyZone(8, "PURPLE", 5.0, 16000));
        config.addZone(new DifficultyZone(9, "BLACK", 5.5, 18000));

        return config;
    }
}
