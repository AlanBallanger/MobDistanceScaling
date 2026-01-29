package com.mobdistancescaling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_FILENAME = "mob-distance-scaling.json";

    private final Path configPath;
    private final Gson gson;
    private ZoneConfig zoneConfig;

    public ConfigManager(@Nonnull Path pluginDataFolder) {
        this.configPath = pluginDataFolder.resolve(CONFIG_FILENAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load() {
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("Configuration file not found, creating default config at: {0}", configPath);
            zoneConfig = ZoneConfig.createDefault();
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            zoneConfig = parseZoneConfig(json);
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

            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject json = serializeZoneConfig(zoneConfig);
                gson.toJson(json, writer);
                LOGGER.at(Level.INFO).log("Configuration saved to: {0}", configPath);
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save configuration", e);
        }
    }

    @Nonnull
    private ZoneConfig parseZoneConfig(@Nonnull JsonObject json) {
        // Parse enabled worlds
        List<String> enabledWorlds = new ArrayList<>();
        JsonArray worldsArray = json.getAsJsonArray("enabledWorlds");
        if (worldsArray != null) {
            for (int i = 0; i < worldsArray.size(); i++) {
                enabledWorlds.add(worldsArray.get(i).getAsString());
            }
        } else {
            // Default to "default" world if not specified
            enabledWorlds.add("default");
        }

        // Parse minimap settings
        boolean minimapEnabled = true;
        int minimapOpacity = 50;
        String minimapPattern = "STRIPES";
        int minimapPatternSize = 4;

        if (json.has("minimapEnabled")) {
            minimapEnabled = json.get("minimapEnabled").getAsBoolean();
        }
        if (json.has("minimapOpacity")) {
            minimapOpacity = json.get("minimapOpacity").getAsInt();
        }
        if (json.has("minimapPattern")) {
            minimapPattern = json.get("minimapPattern").getAsString();
        }
        if (json.has("minimapPatternSize")) {
            minimapPatternSize = json.get("minimapPatternSize").getAsInt();
        }

        // Parse zones
        List<DifficultyZone> zones = new ArrayList<>();
        JsonArray zonesArray = json.getAsJsonArray("zones");

        if (zonesArray != null) {
            for (int i = 0; i < zonesArray.size(); i++) {
                JsonObject zoneObj = zonesArray.get(i).getAsJsonObject();
                int id = zoneObj.get("id").getAsInt();
                String color = zoneObj.get("color").getAsString();
                double multiplier = zoneObj.get("multiplier").getAsDouble();
                int radiusStart = zoneObj.get("radiusStart").getAsInt();

                zones.add(new DifficultyZone(id, color, multiplier, radiusStart));
            }
        }

        return new ZoneConfig(enabledWorlds, zones, minimapEnabled, minimapOpacity, minimapPattern, minimapPatternSize);
    }

    @Nonnull
    private JsonObject serializeZoneConfig(@Nonnull ZoneConfig config) {
        JsonObject json = new JsonObject();

        // Serialize enabled worlds
        json.addProperty("_comment_enabledWorlds", "List of world names where zone scaling is active");
        JsonArray worldsArray = new JsonArray();
        for (String world : config.getEnabledWorlds()) {
            worldsArray.add(world);
        }
        json.add("enabledWorlds", worldsArray);

        // Serialize minimap settings with comments
        json.addProperty("_comment_minimap", "=== MINIMAP OVERLAY SETTINGS ===");
        json.addProperty("minimapEnabled", config.isMinimapEnabled());

        json.addProperty("_comment_minimapOpacity", "Opacity of zone colors (0-100). Higher = more visible");
        json.addProperty("minimapOpacity", config.getMinimapOpacity());

        json.addProperty("_comment_minimapPattern", "Pattern options: SOLID, STRIPES, DOTS, CROSSHATCH, GRID, CHECKER");
        json.addProperty("minimapPattern", config.getMinimapPattern());

        json.addProperty("_comment_minimapPatternSize", "Pattern spacing (2=dense, 8=sparse). Recommended: 3-6");
        json.addProperty("minimapPatternSize", config.getMinimapPatternSize());

        // Serialize zones with comment
        json.addProperty("_comment_zones", "=== DIFFICULTY ZONES === Each zone has: id, color, multiplier (HP/damage), radiusStart (distance from 0,0)");
        json.addProperty("_comment_colors", "Available colors: WHITE, GREEN, LIME, YELLOW, GOLD, ORANGE, RED, DARK_RED, PURPLE, BLACK");
        JsonArray zonesArray = new JsonArray();
        for (DifficultyZone zone : config.getZones()) {
            JsonObject zoneObj = new JsonObject();
            zoneObj.addProperty("id", zone.getZoneId());
            zoneObj.addProperty("color", zone.getColor());
            zoneObj.addProperty("multiplier", zone.getMultiplier());
            zoneObj.addProperty("radiusStart", zone.getRadiusStart());
            zonesArray.add(zoneObj);
        }

        json.add("zones", zonesArray);
        return json;
    }

    @Nonnull
    public ZoneConfig getZoneConfig() {
        return zoneConfig;
    }

    public void reload() {
        load();
    }
}
