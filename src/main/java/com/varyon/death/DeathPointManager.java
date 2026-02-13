package com.varyon.death;

import com.hypixel.hytale.logger.HytaleLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DeathPointManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEATH_POINTS_FILE = "death_points.json";
    
    private final Path dataDirectory;
    private final Map<UUID, DeathPoint> deathPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static class DeathPoint {
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        private final long timestamp;
        private boolean used;
        
        public DeathPoint(String world, double x, double y, double z, long timestamp, boolean used) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
            this.used = used;
        }
        
        public String getWorld() {
            return world;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isUsed() {
            return used;
        }
        
        public void markUsed() {
            this.used = true;
        }
        
        public boolean isExpired(int expirationMinutes) {
            long now = System.currentTimeMillis();
            long expirationMs = expirationMinutes * 60 * 1000L;
            return (now - timestamp) > expirationMs;
        }
    }
    
    public DeathPointManager(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        load();
    }
    
    public void recordDeathPoint(@Nonnull UUID playerId, @Nonnull String world, double x, double y, double z) {
        DeathPoint point = new DeathPoint(world, x, y, z, System.currentTimeMillis(), false);
        deathPoints.put(playerId, point);
        save();
        LOGGER.at(Level.INFO).log("Recorded death point for " + playerId + " at " + world + ":" + (int)x + "," + (int)y + "," + (int)z);
    }
    
    @Nullable
    public DeathPoint getDeathPoint(@Nonnull UUID playerId) {
        return deathPoints.get(playerId);
    }
    
    public void removeDeathPoint(@Nonnull UUID playerId) {
        deathPoints.remove(playerId);
        save();
        LOGGER.at(Level.INFO).log("Removed death point for " + playerId);
    }
    
    public void markDeathPointUsed(@Nonnull UUID playerId) {
        DeathPoint point = deathPoints.get(playerId);
        if (point != null) {
            point.markUsed();
            save();
            LOGGER.at(Level.INFO).log("Marked death point as used for " + playerId);
        }
    }
    
    public boolean isOnCooldown(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }
        return true;
    }
    
    public void setCooldown(@Nonnull UUID playerId, int cooldownSeconds) {
        long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(playerId, cooldownEnd);
    }
    
    public long getCooldownRemainingSeconds(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    private void load() {
        File file = dataDirectory.resolve(DEATH_POINTS_FILE).toFile();
        if (!file.exists()) {
            LOGGER.at(Level.INFO).log("No death points file found, starting fresh");
            return;
        }
        
        try (Reader reader = new FileReader(file)) {
            Map<UUID, DeathPoint> loaded = gson.fromJson(reader, 
                new TypeToken<Map<UUID, DeathPoint>>(){}.getType());
            
            if (loaded != null) {
                deathPoints.clear();
                deathPoints.putAll(loaded);
                LOGGER.at(Level.INFO).log("Loaded " + deathPoints.size() + " death point(s)");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load death points: " + e.getMessage());
        }
    }
    
    private void save() {
        try {
            dataDirectory.toFile().mkdirs();
            File file = dataDirectory.resolve(DEATH_POINTS_FILE).toFile();
            
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(deathPoints, writer);
                LOGGER.at(Level.FINE).log("Saved death points");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save death points: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        save();
    }
}
