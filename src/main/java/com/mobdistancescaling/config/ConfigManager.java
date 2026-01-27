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

        return new ZoneConfig(zones);
    }

    @Nonnull
    private JsonObject serializeZoneConfig(@Nonnull ZoneConfig config) {
        JsonObject json = new JsonObject();
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
