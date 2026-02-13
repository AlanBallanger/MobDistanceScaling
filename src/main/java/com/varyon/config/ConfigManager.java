package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;
import com.varyon.safezone.SafeZoneConfig;

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
    private SafeZoneConfig safeZoneConfig;
    private ExtractionConfig extractionConfig;
    private GlobalRewardsConfig globalRewardsConfig;
    private ReturnConfig returnConfig;

    public ConfigManager(@Nonnull Path pluginDataFolder) {
        this.configPath = pluginDataFolder.resolve(CONFIG_FILENAME);
    }

    public void load() {
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("Configuration file not found, creating default config at: {0}", configPath);
            zoneConfig = ZoneConfig.createDefault();
            safeZoneConfig = new SafeZoneConfig();
            extractionConfig = new ExtractionConfig();
            globalRewardsConfig = GlobalRewardsConfig.createDefault();
            save();
            return;
        }

        try {
            Toml toml = new Toml().read(configFile);
            zoneConfig = parseZoneConfig(toml);
            safeZoneConfig = parseSafeZoneConfig(toml);
            extractionConfig = parseExtractionConfig(toml);
            globalRewardsConfig = parseGlobalRewardsConfig(toml);
            returnConfig = parseReturnConfig(toml);
            LOGGER.at(Level.INFO).log("Loaded configuration with {0} zones", zoneConfig.getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load config, using default configuration", e);
            zoneConfig = ZoneConfig.createDefault();
            safeZoneConfig = new SafeZoneConfig();
            extractionConfig = new ExtractionConfig();
            globalRewardsConfig = GlobalRewardsConfig.createDefault();
            returnConfig = ReturnConfig.createDefault();
        }
    }

    public void save() {
        try {
            File configFile = configPath.toFile();
            File parentDir = configFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // S'assurer que globalRewardsConfig est initialisé
            if (globalRewardsConfig == null) {
                globalRewardsConfig = GlobalRewardsConfig.createDefault();
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
                double essenceMultiplier = zoneToml.getDouble("essenceMultiplier", 1.0);
                int radiusStart = zoneToml.getLong("radiusStart", 0L).intValue();
                String name = zoneToml.getString("name", "Zone " + id);

                zones.add(new DifficultyZone(id, color, healthMultiplier, damageMultiplier, lootMultiplier, essenceMultiplier, radiusStart, name));
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

        sb.append("# Varyon Configuration\n");
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
        sb.append("# SAFE ZONE ROTATION SYSTEM\n");
        sb.append("# One quarter of the map is non-PvP and rotates clockwise\n");
        sb.append("# ==========================================================\n");
        sb.append("[safezone]\n");
        sb.append("# Enable rotating safe zone system\n");
        sb.append("enabled = ").append(safeZoneConfig.isEnabled()).append("\n\n");
        sb.append("# Minimum duration before rotation (minutes)\n");
        sb.append("minRotationTimeMinutes = ").append(safeZoneConfig.getMinRotationTimeMinutes()).append("\n\n");
        sb.append("# Maximum duration before rotation (minutes)\n");
        sb.append("maxRotationTimeMinutes = ").append(safeZoneConfig.getMaxRotationTimeMinutes()).append("\n\n");
        sb.append("# Overlap duration (both zones are safe) in minutes\n");
        sb.append("overlapDurationMinutes = ").append(safeZoneConfig.getOverlapDurationMinutes()).append("\n\n");
        sb.append("# Maximum radius (-1 = unlimited)\n");
        sb.append("maxRadius = ").append(safeZoneConfig.getMaxRadius()).append("\n\n");
        sb.append("# Title displayed when entering non-PvP zone\n");
        sb.append("enterSafeZoneTitle = \"").append(safeZoneConfig.getEnterSafeZoneTitle()).append("\"\n\n");
        sb.append("# Subtitle displayed when entering non-PvP zone\n");
        sb.append("enterSafeZoneSubtitle = \"").append(safeZoneConfig.getEnterSafeZoneSubtitle()).append("\"\n\n");
        sb.append("# Title displayed when entering PvP zone\n");
        sb.append("enterPvpZoneTitle = \"").append(safeZoneConfig.getEnterPvpZoneTitle()).append("\"\n\n");
        sb.append("# Subtitle displayed when entering PvP zone\n");
        sb.append("enterPvpZoneSubtitle = \"").append(safeZoneConfig.getEnterPvpZoneSubtitle()).append("\"\n\n");

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
        sb.append("# EXTRACTION PORTAL SYSTEM\n");
        sb.append("# /extract spawns a portal at a random distance from the player\n");
        sb.append("# Walking into the portal teleports the owner back to spawn\n");
        sb.append("# Placeholders: {distance}, {x}, {y}, {z}, {remaining}\n");
        sb.append("# ==========================================================\n");
        sb.append("[extraction]\n");
        sb.append("enabled = ").append(extractionConfig.isEnabled()).append("\n\n");
        sb.append("# Distance min/max of portal from player (in blocks)\n");
        sb.append("minDistance = ").append(extractionConfig.getMinDistance()).append("\n");
        sb.append("maxDistance = ").append(extractionConfig.getMaxDistance()).append("\n\n");
        sb.append("# Portal lifetime (seconds)\n");
        sb.append("portalDurationSeconds = ").append(extractionConfig.getPortalDurationSeconds()).append("\n\n");
        sb.append("# Cooldown between each /extract usage (seconds)\n");
        sb.append("cooldownSeconds = ").append(extractionConfig.getCooldownSeconds()).append("\n\n");
        sb.append("# Messages\n");
        sb.append("messagePortalSpawned = \"").append(extractionConfig.getMessagePortalSpawned()).append("\"\n");
        sb.append("messagePortalExpired = \"").append(extractionConfig.getMessagePortalExpired()).append("\"\n");
        sb.append("messageCooldown = \"").append(extractionConfig.getMessageCooldown()).append("\"\n");
        sb.append("messageTeleporting = \"").append(extractionConfig.getMessageTeleporting()).append("\"\n");
        sb.append("messageNotYourPortal = \"").append(extractionConfig.getMessageNotYourPortal()).append("\"\n");
        sb.append("messageNoSafeLocation = \"").append(extractionConfig.getMessageNoSafeLocation()).append("\"\n");
        sb.append("messageError = \"").append(extractionConfig.getMessageError()).append("\"\n");
        sb.append("messageAlreadyHasPortal = \"").append(extractionConfig.getMessageAlreadyHasPortal()).append("\"\n\n");

        sb.append("# ==========================================================\n");
        sb.append("# GLOBAL ESSENCE REWARDS\n");
        sb.append("# Rewards given to faction members when global balance reaches thresholds\n");
        sb.append("# Thresholds apply in both directions (+/- for Fracture/Noyau)\n");
        sb.append("# ==========================================================\n");
        sb.append("[global_rewards]\n");
        sb.append("# Cooldown (in minutes) before same tier can reward again\n");
        sb.append("cooldownMinutes = ").append(globalRewardsConfig.getRewardCooldownMinutes()).append("\n\n");
        
        for (int i = 0; i < globalRewardsConfig.getTiers().size(); i++) {
            GlobalRewardsConfig.RewardTier tier = globalRewardsConfig.getTiers().get(i);
            sb.append("[[global_rewards.tiers]]\n");
            sb.append("# Tier ").append(i + 1).append(" - Triggers at +/-").append(tier.getThreshold()).append(" balance\n");
            sb.append("threshold = ").append(tier.getThreshold()).append("\n");
            
            for (GlobalRewardsConfig.RewardItem item : tier.getItems()) {
                sb.append("\n[[global_rewards.tiers.items]]\n");
                sb.append("itemId = \"").append(item.getItemId()).append("\"\n");
                sb.append("amount = ").append(item.getAmount()).append("\n");
            }
            
            if (!tier.getCommands().isEmpty()) {
                sb.append("\ncommands = [");
                for (int j = 0; j < tier.getCommands().size(); j++) {
                    sb.append("\"").append(tier.getCommands().get(j)).append("\"");
                    if (j < tier.getCommands().size() - 1) sb.append(", ");
                }
                sb.append("]\n");
            }
            sb.append("\n");
        }

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
    private ExtractionConfig parseExtractionConfig(@Nonnull Toml toml) {
        ExtractionConfig config = new ExtractionConfig();

        Toml extractionToml = toml.getTable("extraction");
        if (extractionToml != null) {
            config.setEnabled(extractionToml.getBoolean("enabled", true));
            config.setMinDistance(extractionToml.getLong("minDistance", 100L).intValue());
            config.setMaxDistance(extractionToml.getLong("maxDistance", 200L).intValue());
            config.setPortalDurationSeconds(extractionToml.getLong("portalDurationSeconds", 300L).intValue());
            config.setCooldownSeconds(extractionToml.getLong("cooldownSeconds", 300L).intValue());
            config.setMessagePortalSpawned(extractionToml.getString("messagePortalSpawned", config.getMessagePortalSpawned()));
            config.setMessagePortalExpired(extractionToml.getString("messagePortalExpired", config.getMessagePortalExpired()));
            config.setMessageCooldown(extractionToml.getString("messageCooldown", config.getMessageCooldown()));
            config.setMessageTeleporting(extractionToml.getString("messageTeleporting", config.getMessageTeleporting()));
            config.setMessageNotYourPortal(extractionToml.getString("messageNotYourPortal", config.getMessageNotYourPortal()));
            config.setMessageNoSafeLocation(extractionToml.getString("messageNoSafeLocation", config.getMessageNoSafeLocation()));
            config.setMessageError(extractionToml.getString("messageError", config.getMessageError()));
            config.setMessageAlreadyHasPortal(extractionToml.getString("messageAlreadyHasPortal", config.getMessageAlreadyHasPortal()));
        }

        return config;
    }

    @Nonnull
    private SafeZoneConfig parseSafeZoneConfig(@Nonnull Toml toml) {
        SafeZoneConfig config = new SafeZoneConfig();
        
        Toml safeZoneToml = toml.getTable("safezone");
        if (safeZoneToml != null) {
            config.setEnabled(safeZoneToml.getBoolean("enabled", true));
            config.setMinRotationTimeMinutes(safeZoneToml.getLong("minRotationTimeMinutes", 60L).intValue());
            config.setMaxRotationTimeMinutes(safeZoneToml.getLong("maxRotationTimeMinutes", 120L).intValue());
            config.setOverlapDurationMinutes(safeZoneToml.getLong("overlapDurationMinutes", 10L).intValue());
            config.setMaxRadius(safeZoneToml.getLong("maxRadius", -1L).intValue());
            config.setEnterSafeZoneTitle(safeZoneToml.getString("enterSafeZoneTitle", "Zone Safe"));
            config.setEnterSafeZoneSubtitle(safeZoneToml.getString("enterSafeZoneSubtitle", "PvP Désactivé"));
            config.setEnterPvpZoneTitle(safeZoneToml.getString("enterPvpZoneTitle", "Zone PvP"));
            config.setEnterPvpZoneSubtitle(safeZoneToml.getString("enterPvpZoneSubtitle", "Attention !"));
        }
        
        return config;
    }

    @Nonnull
    private GlobalRewardsConfig parseGlobalRewardsConfig(@Nonnull Toml toml) {
        Toml rewardsToml = toml.getTable("global_rewards");
        
        // Si la section n'existe pas, retourner la config par défaut
        if (rewardsToml == null) {
            return GlobalRewardsConfig.createDefault();
        }
        
        GlobalRewardsConfig config = new GlobalRewardsConfig();
        config.setRewardCooldownMinutes(rewardsToml.getLong("cooldownMinutes", 30L).intValue());
        
        List<Toml> tiersList = rewardsToml.getTables("tiers");
        if (tiersList != null && !tiersList.isEmpty()) {
            for (Toml tierToml : tiersList) {
                int threshold = tierToml.getLong("threshold", 3300L).intValue();
                
                List<GlobalRewardsConfig.RewardItem> items = new ArrayList<>();
                List<Toml> itemsList = tierToml.getTables("items");
                if (itemsList != null) {
                    for (Toml itemToml : itemsList) {
                        String itemId = itemToml.getString("itemId", "soil_grass");
                        int amount = itemToml.getLong("amount", 1L).intValue();
                        items.add(new GlobalRewardsConfig.RewardItem(itemId, amount));
                    }
                }
                
                List<String> commands = tierToml.getList("commands");
                if (commands == null) {
                    commands = new ArrayList<>();
                }
                
                config.addTier(new GlobalRewardsConfig.RewardTier(threshold, items, commands));
            }
        } else {
            // Si aucun tier n'est défini, utiliser les valeurs par défaut
            return GlobalRewardsConfig.createDefault();
        }
        
        return config;
    }

    @Nonnull
    public ZoneConfig getZoneConfig() {
        return zoneConfig;
    }

    @Nonnull
    public SafeZoneConfig getSafeZoneConfig() {
        return safeZoneConfig;
    }

    @Nonnull
    public ExtractionConfig getExtractionConfig() {
        return extractionConfig;
    }

    @Nonnull
    public GlobalRewardsConfig getGlobalRewardsConfig() {
        return globalRewardsConfig;
    }
    
    @Nonnull
    public ReturnConfig getReturnConfig() {
        return returnConfig != null ? returnConfig : ReturnConfig.createDefault();
    }
    
    @Nonnull
    private ReturnConfig parseReturnConfig(@Nonnull Toml toml) {
        Toml returnToml = toml.getTable("return");
        
        if (returnToml == null) {
            return ReturnConfig.createDefault();
        }
        
        boolean enabled = returnToml.getBoolean("enabled", true);
        int cooldownSeconds = returnToml.getLong("cooldownSeconds", 1800L).intValue();
        int minDistance = returnToml.getLong("minDistance", 100L).intValue();
        int maxDistance = returnToml.getLong("maxDistance", 200L).intValue();
        int expirationMinutes = returnToml.getLong("expirationMinutes", 30L).intValue();
        
        String messageSuccess = returnToml.getString("messageSuccess", 
            "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})");
        String messageCooldown = returnToml.getString("messageCooldown", 
            "Cooldown actif. Temps restant: {remaining} secondes");
        String messageNoDeathPoint = returnToml.getString("messageNoDeathPoint", 
            "Aucun point de mort enregistré");
        String messageExpired = returnToml.getString("messageExpired", 
            "Votre point de mort a expiré");
        String messageAlreadyUsed = returnToml.getString("messageAlreadyUsed", 
            "Vous avez déjà utilisé votre téléportation pour cette mort");
        String messageTeleporting = returnToml.getString("messageTeleporting", 
            "Recherche d'un emplacement sûr près de votre point de mort...");
        String messageNoSafeLocation = returnToml.getString("messageNoSafeLocation", 
            "Impossible de trouver un emplacement sûr après {attempts} tentatives");
        String messageError = returnToml.getString("messageError", 
            "Erreur lors de la téléportation");
        String messageFirstUseWarning = returnToml.getString("messageFirstUseWarning", 
            "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!");
        
        return new ReturnConfig(enabled, cooldownSeconds, minDistance, maxDistance, expirationMinutes,
            messageSuccess, messageCooldown, messageNoDeathPoint, messageExpired, messageAlreadyUsed,
            messageTeleporting, messageNoSafeLocation, messageError, messageFirstUseWarning);
    }

    public void reload() {
        load();
    }
}
