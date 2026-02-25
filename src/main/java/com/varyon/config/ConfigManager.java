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
    private static final String CONFIG_FILENAME = "config.toml";

    private final Path configPath;
    private final Path pluginDataFolder;
    private ZoneConfig zoneConfig;
    private SafeZoneConfig safeZoneConfig;
    private ExtractionConfig extractionConfig;
    private GlobalRewardsConfig globalRewardsConfig;
    private ReturnConfig returnConfig;
    private MessagesConfig messagesConfig;
    private ZoneLootConfig zoneLootConfig;
    private MobFragmentsConfig mobFragmentsConfig;
    private ZonePermissionsConfig zonePermissionsConfig;

    public ConfigManager(@Nonnull Path pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
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
            returnConfig = ReturnConfig.createDefault();
            messagesConfig = MessagesConfig.createDefault();
            messagesConfig.save(pluginDataFolder);
            zoneLootConfig = ZoneLootConfig.createDefault();
            zoneLootConfig.save(pluginDataFolder);
            mobFragmentsConfig = MobFragmentsConfig.createDefault();
            mobFragmentsConfig.save(pluginDataFolder);
            zonePermissionsConfig = ZonePermissionsConfig.createDefault();
            zonePermissionsConfig.save(pluginDataFolder);
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
            messagesConfig = MessagesConfig.load(pluginDataFolder);
            zoneLootConfig = ZoneLootConfig.load(pluginDataFolder);
            mobFragmentsConfig = MobFragmentsConfig.load(pluginDataFolder);
            zonePermissionsConfig = ZonePermissionsConfig.load(pluginDataFolder);
            LOGGER.at(Level.INFO).log("Loaded configuration with {0} zones", zoneConfig.getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load config, using default configuration", e);
            zoneConfig = ZoneConfig.createDefault();
            safeZoneConfig = new SafeZoneConfig();
            extractionConfig = new ExtractionConfig();
            globalRewardsConfig = GlobalRewardsConfig.createDefault();
            returnConfig = ReturnConfig.createDefault();
            messagesConfig = MessagesConfig.createDefault();
            zoneLootConfig = ZoneLootConfig.createDefault();
            mobFragmentsConfig = MobFragmentsConfig.createDefault();
            zonePermissionsConfig = ZonePermissionsConfig.createDefault();
        }
    }

    public void save() {
        try {
            File configFile = configPath.toFile();
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (globalRewardsConfig == null) {
                globalRewardsConfig = GlobalRewardsConfig.createDefault();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(generateToml());
                LOGGER.at(Level.INFO).log("Configuration saved to: {0}", configPath);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save configuration", e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();

        sb.append("enabledWorlds = [");
        List<String> worlds = zoneConfig.getEnabledWorlds();
        for (int i = 0; i < worlds.size(); i++) {
            sb.append("\"").append(worlds.get(i)).append("\"");
            if (i < worlds.size() - 1) sb.append(", ");
        }
        sb.append("]\n\n");

        sb.append("[safezone]\n");
        sb.append("enabled = ").append(safeZoneConfig.isEnabled()).append("\n");
        sb.append("minRotationTimeMinutes = ").append(safeZoneConfig.getMinRotationTimeMinutes()).append("\n");
        sb.append("maxRotationTimeMinutes = ").append(safeZoneConfig.getMaxRotationTimeMinutes()).append("\n");
        sb.append("overlapDurationMinutes = ").append(safeZoneConfig.getOverlapDurationMinutes()).append("\n");
        sb.append("maxRadius = ").append(safeZoneConfig.getMaxRadius()).append("\n\n");

        sb.append("[minimap]\n");
        sb.append("enabled = ").append(zoneConfig.isMinimapEnabled()).append("\n");
        sb.append("opacity = ").append(zoneConfig.getMinimapOpacity()).append("\n");
        sb.append("pattern = \"").append(zoneConfig.getMinimapPattern()).append("\"\n");
        sb.append("patternSize = ").append(zoneConfig.getMinimapPatternSize()).append("\n\n");

        sb.append("[notifications]\n");
        sb.append("zoneEnterEnabled = ").append(zoneConfig.isZoneEnterNotification()).append("\n");
        sb.append("zoneEnterTopText = \"").append(zoneConfig.getZoneEnterTopText()).append("\"\n");
        sb.append("duration = ").append(zoneConfig.getNotificationDuration()).append("\n");
        sb.append("soundEnabled = ").append(zoneConfig.isZoneSoundEnabled()).append("\n");
        sb.append("soundId = \"").append(zoneConfig.getZoneSoundId()).append("\"\n");
        sb.append("soundVolume = ").append(zoneConfig.getZoneSoundVolume()).append("\n");
        sb.append("soundPitch = ").append(zoneConfig.getZoneSoundPitch()).append("\n");
        sb.append("hudEnabled = ").append(zoneConfig.isZoneHudEnabled()).append("\n\n");

        sb.append("[extraction]\n");
        sb.append("enabled = ").append(extractionConfig.isEnabled()).append("\n");
        sb.append("minDistance = ").append(extractionConfig.getMinDistance()).append("\n");
        sb.append("maxDistance = ").append(extractionConfig.getMaxDistance()).append("\n");
        sb.append("portalDurationSeconds = ").append(extractionConfig.getPortalDurationSeconds()).append("\n");
        sb.append("cooldownSeconds = ").append(extractionConfig.getCooldownSeconds()).append("\n\n");

        sb.append("[return]\n");
        sb.append("enabled = ").append(returnConfig != null ? returnConfig.isEnabled() : true).append("\n");
        sb.append("cooldownSeconds = ").append(returnConfig != null ? returnConfig.getCooldownSeconds() : 1800).append("\n");
        sb.append("minDistance = ").append(returnConfig != null ? returnConfig.getMinDistance() : 100).append("\n");
        sb.append("maxDistance = ").append(returnConfig != null ? returnConfig.getMaxDistance() : 200).append("\n");
        sb.append("expirationMinutes = ").append(returnConfig != null ? returnConfig.getExpirationMinutes() : 30).append("\n\n");

        sb.append("[global_rewards]\n");
        sb.append("cooldownMinutes = ").append(globalRewardsConfig.getRewardCooldownMinutes()).append("\n\n");
        for (int i = 0; i < globalRewardsConfig.getTiers().size(); i++) {
            GlobalRewardsConfig.RewardTier tier = globalRewardsConfig.getTiers().get(i);
            sb.append("[[global_rewards.tiers]]\n");
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

        for (DifficultyZone zone : zoneConfig.getZones()) {
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
    private ZoneConfig parseZoneConfig(@Nonnull Toml toml) {
        List<String> enabledWorlds = toml.getList("enabledWorlds");
        if (enabledWorlds == null) {
            enabledWorlds = new ArrayList<>();
            enabledWorlds.add("default");
        }

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

        Toml notifToml = toml.getTable("notifications");
        boolean zoneEnterNotification = true;
        String zoneEnterTopText = "Zone";
        float notificationDuration = 2.0f;
        boolean zoneSoundEnabled = true;
        String zoneSoundId = "SFX_Axe_Special_Swing";
        float zoneSoundVolume = 1.0f;
        float zoneSoundPitch = 1.0f;
        boolean zoneHudEnabled = true;
        if (notifToml != null) {
            zoneEnterNotification = notifToml.getBoolean("zoneEnterEnabled", true);
            zoneEnterTopText = notifToml.getString("zoneEnterTopText", "Zone");
            notificationDuration = notifToml.getDouble("duration", 2.0).floatValue();
            zoneSoundEnabled = notifToml.getBoolean("soundEnabled", true);
            zoneSoundId = notifToml.getString("soundId", "SFX_Axe_Special_Swing");
            zoneSoundVolume = notifToml.getDouble("soundVolume", 1.0).floatValue();
            zoneSoundPitch = notifToml.getDouble("soundPitch", 1.0).floatValue();
            zoneHudEnabled = notifToml.getBoolean("hudEnabled", true);
        }

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
                zones.add(new DifficultyZone(id, color, healthMultiplier, damageMultiplier,
                        lootMultiplier, essenceMultiplier, radiusStart, name));
            }
        }

        return new ZoneConfig(enabledWorlds, zones, minimapEnabled, minimapOpacity,
                minimapPattern, minimapPatternSize, zoneEnterNotification,
                zoneEnterTopText, notificationDuration, zoneSoundEnabled,
                zoneSoundId, zoneSoundVolume, zoneSoundPitch, zoneHudEnabled);
    }

    @Nonnull
    private SafeZoneConfig parseSafeZoneConfig(@Nonnull Toml toml) {
        SafeZoneConfig config = new SafeZoneConfig();
        Toml safeToml = toml.getTable("safezone");
        if (safeToml != null) {
            config.setEnabled(safeToml.getBoolean("enabled", true));
            config.setMinRotationTimeMinutes(safeToml.getLong("minRotationTimeMinutes", 60L).intValue());
            config.setMaxRotationTimeMinutes(safeToml.getLong("maxRotationTimeMinutes", 120L).intValue());
            config.setOverlapDurationMinutes(safeToml.getLong("overlapDurationMinutes", 10L).intValue());
            config.setMaxRadius(safeToml.getLong("maxRadius", -1L).intValue());
        }
        return config;
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
        }
        return config;
    }

    @Nonnull
    private ReturnConfig parseReturnConfig(@Nonnull Toml toml) {
        Toml returnToml = toml.getTable("return");
        if (returnToml == null) return ReturnConfig.createDefault();
        return new ReturnConfig(
            returnToml.getBoolean("enabled", true),
            returnToml.getLong("cooldownSeconds", 1800L).intValue(),
            returnToml.getLong("minDistance", 100L).intValue(),
            returnToml.getLong("maxDistance", 200L).intValue(),
            returnToml.getLong("expirationMinutes", 30L).intValue()
        );
    }

    @Nonnull
    private GlobalRewardsConfig parseGlobalRewardsConfig(@Nonnull Toml toml) {
        Toml rewardsToml = toml.getTable("global_rewards");
        if (rewardsToml == null) return GlobalRewardsConfig.createDefault();

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
                if (commands == null) commands = new ArrayList<>();
                config.addTier(new GlobalRewardsConfig.RewardTier(threshold, items, commands));
            }
        } else {
            return GlobalRewardsConfig.createDefault();
        }
        return config;
    }

    @Nonnull public ZoneConfig getZoneConfig()                    { return zoneConfig; }
    @Nonnull public SafeZoneConfig getSafeZoneConfig()            { return safeZoneConfig; }
    @Nonnull public ExtractionConfig getExtractionConfig()        { return extractionConfig; }
    @Nonnull public GlobalRewardsConfig getGlobalRewardsConfig()  { return globalRewardsConfig; }
    @Nonnull public ReturnConfig getReturnConfig()                { return returnConfig != null ? returnConfig : ReturnConfig.createDefault(); }
    @Nonnull public MessagesConfig getMessagesConfig()            { return messagesConfig != null ? messagesConfig : MessagesConfig.createDefault(); }
    @Nonnull public ZoneLootConfig getZoneLootConfig()            { return zoneLootConfig != null ? zoneLootConfig : ZoneLootConfig.createDefault(); }
    @Nonnull public MobFragmentsConfig getMobFragmentsConfig()    { return mobFragmentsConfig != null ? mobFragmentsConfig : MobFragmentsConfig.createDefault(); }
    @Nonnull public ZonePermissionsConfig getZonePermissionsConfig() { return zonePermissionsConfig != null ? zonePermissionsConfig : ZonePermissionsConfig.createDefault(); }

    public void reload() { load(); }
}
