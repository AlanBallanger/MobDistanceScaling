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
        String minimapPattern = "SOLID";
        int minimapPatternSize = 4;

        if (minimapToml != null) {
            minimapEnabled = minimapToml.getBoolean("enabled", true);
            minimapOpacity = minimapToml.getLong("opacity", 50L).intValue();
            minimapPattern = minimapToml.getString("pattern", "SOLID");
            minimapPatternSize = minimapToml.getLong("patternSize", 4L).intValue();
        }

        // Parse notification settings
        Toml notificationToml = toml.getTable("notifications");
        boolean zoneEnterNotification = true;
        String zoneEnterTopText = "Zone";
        float notificationDuration = 2.0f;
        boolean zoneSoundEnabled = true;
        String zoneSoundId = "SFX_Axe_Special_Swing";
        float zoneSoundVolume = 1.0f;
        float zoneSoundPitch = 1.0f;
        boolean zoneHudEnabled = true;

        if (notificationToml != null) {
            zoneEnterNotification = notificationToml.getBoolean("zoneEnterEnabled", true);
            zoneEnterTopText = notificationToml.getString("zoneEnterTopText", "Zone");
            notificationDuration = notificationToml.getDouble("duration", 2.0).floatValue();
            zoneSoundEnabled = notificationToml.getBoolean("soundEnabled", true);
            zoneSoundId = notificationToml.getString("soundId", "SFX_Axe_Special_Swing");
            zoneSoundVolume = notificationToml.getDouble("soundVolume", 1.0).floatValue();
            zoneSoundPitch = notificationToml.getDouble("soundPitch", 1.0).floatValue();
            zoneHudEnabled = notificationToml.getBoolean("hudEnabled", true);
        }

        // Parse zones
        List<DifficultyZone> zones = new ArrayList<>();
        List<Toml> zonesList = toml.getTables("zones");

        if (zonesList != null) {
            for (Toml zoneToml : zonesList) {
                int id = zoneToml.getLong("id", 1L).intValue();
                String color = zoneToml.getString("color", "WHITE");
                double healthMultiplier = zoneToml.getDouble("healthMultiplier", 1.0);
                double damageMultiplier = zoneToml.getDouble("damageMultiplier", 1.0);
                double lootMultiplier = zoneToml.getDouble("lootMultiplier", 1.0);
                int radiusStart = zoneToml.getLong("radiusStart", 0L).intValue();
                String name = zoneToml.getString("name", "Zone " + id);

                zones.add(new DifficultyZone(id, color, healthMultiplier, damageMultiplier, lootMultiplier, radiusStart, name));
            }
        }

        String hudLabelHealth = toml.getString("hud.labelHealth", "HP");
        String hudLabelDamage = toml.getString("hud.labelDamage", "DMG");
        String hudLabelLoot = toml.getString("hud.labelLoot", "Loot");
        String hudLabelMultipliers = toml.getString("hud.labelMultipliers", "Multiplicateurs");
        
        String rtpvMessageTeleporting = toml.getString("rtpv.messageTeleporting", "Teleporting...");
        String rtpvMessageSuccess = toml.getString("rtpv.messageSuccess", "Teleported to zone: {zone} ({x}, {y}, {z})");
        String rtpvMessageNoSafeLocation = toml.getString("rtpv.messageNoSafeLocation", "Could not find a safe location after {attempts} attempts");
        String rtpvMessageError = toml.getString("rtpv.messageError", "Teleportation error");
        String rtpvMessageWorldNotSupported = toml.getString("rtpv.messageWorldNotSupported", "This world does not support zone teleportation");
        String rtpvMessageZoneNotFound = toml.getString("rtpv.messageZoneNotFound", "Zone '{zone}' not found.");
        String rtpvMessageAvailableZones = toml.getString("rtpv.messageAvailableZones", "Available zones");
        String rtpvMessageRandomZone = toml.getString("rtpv.messageRandomZone", "random");

        return new ZoneConfig(enabledWorlds, zones, minimapEnabled, minimapOpacity,
                minimapPattern, minimapPatternSize, zoneEnterNotification,
                zoneEnterTopText, notificationDuration, zoneSoundEnabled,
                zoneSoundId, zoneSoundVolume, zoneSoundPitch, zoneHudEnabled,
                hudLabelHealth, hudLabelDamage, hudLabelLoot, hudLabelMultipliers,
                rtpvMessageTeleporting, rtpvMessageSuccess, rtpvMessageNoSafeLocation,
                rtpvMessageError, rtpvMessageWorldNotSupported, rtpvMessageZoneNotFound,
                rtpvMessageAvailableZones, rtpvMessageRandomZone);
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

        sb.append("# Pattern options: SOLID, SOLID, DOTS, CROSSHATCH, GRID, CHECKER\n");
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

        sb.append("# Play a sound when entering a new zone\n");
        sb.append("soundEnabled = ").append(config.isZoneSoundEnabled()).append("\n\n");

        sb.append("# Sound event ID to play (use WORLD > Play Sound menu in-game to browse sounds)\n");
        sb.append("# Examples: \"SFX_Axe_Special_Swing\", \"SFX_Attn_VeryQuiet\", \"SFX_Avatar_Powers_Enable\"\n");
        sb.append("soundId = \"").append(config.getZoneSoundId()).append("\"\n\n");

        sb.append("# Volume modifier (0.0 to 2.0, where 1.0 is normal volume)\n");
        sb.append("soundVolume = ").append(config.getZoneSoundVolume()).append("\n\n");

        sb.append("# Pitch modifier (0.5 to 2.0, where 1.0 is normal pitch)\n");
        sb.append("soundPitch = ").append(config.getZoneSoundPitch()).append("\n\n");

        sb.append("# Show a persistent HUD with current zone info\n");
        sb.append("hudEnabled = ").append(config.isZoneHudEnabled()).append("\n\n");

        sb.append("# HUD text labels (customizable for translations)\n");
        sb.append("[hud]\n");
        sb.append("labelHealth = \"").append(config.getHudLabelHealth()).append("\"\n");
        sb.append("labelDamage = \"").append(config.getHudLabelDamage()).append("\"\n");
        sb.append("labelLoot = \"").append(config.getHudLabelLoot()).append("\"\n");
        sb.append("labelMultipliers = \"").append(config.getHudLabelMultipliers()).append("\"\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# RTPV COMMAND MESSAGES\n");
        sb.append("# Random teleport to vanilla zones - customizable messages\n");
        sb.append("# Placeholders: {zone}, {x}, {y}, {z}, {attempts}\n");
        sb.append("# ==========================================================\n");
        sb.append("[rtpv]\n");
        sb.append("messageTeleporting = \"").append(config.getRtpvMessageTeleporting()).append("\"\n");
        sb.append("messageSuccess = \"").append(config.getRtpvMessageSuccess()).append("\"\n");
        sb.append("messageNoSafeLocation = \"").append(config.getRtpvMessageNoSafeLocation()).append("\"\n");
        sb.append("messageError = \"").append(config.getRtpvMessageError()).append("\"\n");
        sb.append("messageWorldNotSupported = \"").append(config.getRtpvMessageWorldNotSupported()).append("\"\n");
        sb.append("messageZoneNotFound = \"").append(config.getRtpvMessageZoneNotFound()).append("\"\n");
        sb.append("messageAvailableZones = \"").append(config.getRtpvMessageAvailableZones()).append("\"\n");
        sb.append("messageRandomZone = \"").append(config.getRtpvMessageRandomZone()).append("\"\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# DIFFICULTY ZONES\n");
        sb.append("# Each zone has separate multipliers for HP, damage, and loot\n");
        sb.append("# \n");
        sb.append("# healthMultiplier: Mob max HP multiplier (2.0 = double HP)\n");
        sb.append("# damageMultiplier: Mob damage output multiplier (2.0 = double damage)\n");
        sb.append("# lootMultiplier: Loot drop multiplier (5.0 = 5x vanilla drops)\n");
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
            sb.append("healthMultiplier = ").append(zone.getHealthMultiplier()).append("\n");
            sb.append("damageMultiplier = ").append(zone.getDamageMultiplier()).append("\n");
            sb.append("lootMultiplier = ").append(zone.getLootMultiplier()).append("\n");
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
