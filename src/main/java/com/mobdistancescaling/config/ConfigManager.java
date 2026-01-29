package com.mobdistancescaling.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_FILENAME = "mdsconfig.toml";

    private final Path configPath;
    private ZoneConfig zoneConfig;

    public ConfigManager(@Nonnull Path pluginDataFolder) {
        this.configPath = pluginDataFolder.resolve(CONFIG_FILENAME);
    }

    public void load() {
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("Configuration file not found, creating default config at: {0}", configPath);
            zoneConfig = ZoneConfig.createDefault();
            save();
            return;
        }

        try {
            Toml toml = new Toml().read(configFile);
            zoneConfig = parseZoneConfig(toml);
            LOGGER.at(Level.INFO).log("Loaded configuration with {0} zones", zoneConfig.getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load config, using default configuration", e);
            zoneConfig = ZoneConfig.createDefault();
        }
    }

    public void save() {
        try {
            File configFile = configPath.toFile();
            File parentDir = configFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            String tomlContent = generateTomlWithComments(zoneConfig);
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(tomlContent);
                LOGGER.at(Level.INFO).log("Configuration saved to: {0}", configPath);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save configuration", e);
        }
    }

    @Nonnull
    private ZoneConfig parseZoneConfig(@Nonnull Toml toml) {
        // Parse enabled worlds
        List<String> enabledWorlds = toml.getList("enabledWorlds");
        if (enabledWorlds == null) {
            enabledWorlds = new ArrayList<>();
            enabledWorlds.add("default");
        }

        // Parse minimap settings
        Toml minimapToml = toml.getTable("minimap");
        boolean minimapEnabled = true;
        int minimapOpacity = 50;
        String minimapPattern = "STRIPES";
        int minimapPatternSize = 4;

        if (minimapToml != null) {
            minimapEnabled = minimapToml.getBoolean("enabled", true);
            minimapOpacity = minimapToml.getLong("opacity", 50L).intValue();
            minimapPattern = minimapToml.getString("pattern", "STRIPES");
            minimapPatternSize = minimapToml.getLong("patternSize", 4L).intValue();
        }

        // Parse notification settings
        Toml notificationToml = toml.getTable("notifications");
        boolean zoneEnterNotification = true;
        String zoneEnterTopText = "Zone";
        float notificationDuration = 2.0f;

        if (notificationToml != null) {
            zoneEnterNotification = notificationToml.getBoolean("zoneEnterEnabled", true);
            zoneEnterTopText = notificationToml.getString("zoneEnterTopText", "Zone");
            notificationDuration = notificationToml.getDouble("duration", 2.0).floatValue();
        }

        // Parse zones
        List<DifficultyZone> zones = new ArrayList<>();
        List<Toml> zonesList = toml.getTables("zones");

        if (zonesList != null) {
            for (Toml zoneToml : zonesList) {
                int id = zoneToml.getLong("id", 1L).intValue();
                String color = zoneToml.getString("color", "WHITE");
                double multiplier = zoneToml.getDouble("multiplier", 1.0);
                int radiusStart = zoneToml.getLong("radiusStart", 0L).intValue();
                String name = zoneToml.getString("name", "Zone " + id);

                zones.add(new DifficultyZone(id, color, multiplier, radiusStart, name));
            }
        }

        return new ZoneConfig(enabledWorlds, zones, minimapEnabled, minimapOpacity,
                minimapPattern, minimapPatternSize, zoneEnterNotification,
                zoneEnterTopText, notificationDuration);
    }

    @Nonnull
    private String generateTomlWithComments(@Nonnull ZoneConfig config) {
        StringBuilder sb = new StringBuilder();

        sb.append("# MobDistanceScaling Configuration\n");
        sb.append("# Mobs scale in HP and damage based on distance from world spawn (0,0)\n\n");

        sb.append("# Worlds where the plugin is active\n");
        sb.append("enabledWorlds = [");
        List<String> worlds = config.getEnabledWorlds();
        for (int i = 0; i < worlds.size(); i++) {
            sb.append("\"").append(worlds.get(i)).append("\"");
            if (i < worlds.size() - 1) sb.append(", ");
        }
        sb.append("]\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# MINIMAP OVERLAY SETTINGS\n");
        sb.append("# ==========================================================\n");
        sb.append("[minimap]\n");
        sb.append("enabled = ").append(config.isMinimapEnabled()).append("\n\n");

        sb.append("# Opacity of zone colors (0-100). Higher = more visible\n");
        sb.append("opacity = ").append(config.getMinimapOpacity()).append("\n\n");

        sb.append("# Pattern options: SOLID, STRIPES, DOTS, CROSSHATCH, GRID, CHECKER\n");
        sb.append("pattern = \"").append(config.getMinimapPattern()).append("\"\n\n");

        sb.append("# Pattern spacing (2=dense, 8=sparse). Recommended: 3-6\n");
        sb.append("patternSize = ").append(config.getMinimapPatternSize()).append("\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# ZONE ENTRY NOTIFICATIONS\n");
        sb.append("# ==========================================================\n");
        sb.append("[notifications]\n");
        sb.append("# Show a title when entering a new zone\n");
        sb.append("zoneEnterEnabled = ").append(config.isZoneEnterNotification()).append("\n\n");

        sb.append("# Text shown above the zone name (small text on top)\n");
        sb.append("zoneEnterTopText = \"").append(config.getZoneEnterTopText()).append("\"\n\n");

        sb.append("# How long the notification stays on screen (seconds)\n");
        sb.append("duration = ").append(config.getNotificationDuration()).append("\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# DIFFICULTY ZONES\n");
        sb.append("# Each zone scales mob HP and damage by the multiplier\n");
        sb.append("# \n");
        sb.append("# Colors can be:\n");
        sb.append("#   - Named: WHITE, GREEN, LIME, YELLOW, GOLD, ORANGE, RED, DARK_RED, PURPLE, BLACK\n");
        sb.append("#   - Hex: \"#FF5500\" or \"#F50\"\n");
        sb.append("# ==========================================================\n\n");

        for (DifficultyZone zone : config.getZones()) {
            sb.append("[[zones]]\n");
            sb.append("id = ").append(zone.getZoneId()).append("\n");
            sb.append("name = \"").append(zone.getName()).append("\"\n");
            sb.append("color = \"").append(zone.getColor()).append("\"\n");
            sb.append("multiplier = ").append(zone.getMultiplier()).append("\n");
            sb.append("radiusStart = ").append(zone.getRadiusStart()).append("\n\n");
        }

        return sb.toString();
    }

    @Nonnull
    public ZoneConfig getZoneConfig() {
        return zoneConfig;
    }

    public void reload() {
        load();
    }
}
