package com.mobdistancescaling.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EssenceManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long AUTO_SAVE_INTERVAL_MS = 60000; // 60 secondes
    
    private final EssenceDatabase database;
    private final ConcurrentHashMap<UUID, PlayerEssenceData> cache;
    private long lastAutoSave;

    public EssenceManager(@Nonnull File dataFolder) {
        this.database = new EssenceDatabase(dataFolder);
        this.cache = new ConcurrentHashMap<>();
        this.lastAutoSave = System.currentTimeMillis();
        
        // Charger toutes les données en mémoire au démarrage
        loadAll();
    }

    private void loadAll() {
        List<PlayerEssenceData> all = database.loadAll();
        for (PlayerEssenceData data : all) {
            cache.put(data.getPlayerUuid(), data);
        }
        LOGGER.at(Level.INFO).log("Chargé " + all.size() + " joueurs en cache");
    }

    @Nonnull
    public PlayerEssenceData getOrCreate(@Nonnull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid -> {
            PlayerEssenceData data = database.load(uuid);
            if (data == null) {
                data = new PlayerEssenceData(uuid, 0);
            }
            return data;
        });
    }

    public int getEssence(@Nonnull UUID playerUuid) {
        return getOrCreate(playerUuid).getEssence();
    }

    public void addEssence(@Nonnull UUID playerUuid, int amount) {
        PlayerEssenceData data = getOrCreate(playerUuid);
        data.addEssence(amount);
    }

    public void setEssence(@Nonnull UUID playerUuid, int amount) {
        PlayerEssenceData data = getOrCreate(playerUuid);
        data.setEssence(amount);
    }

    /**
     * Sauvegarde automatique périodique - à appeler dans un système de tick
     */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastAutoSave >= AUTO_SAVE_INTERVAL_MS) {
            saveAllDirty();
            lastAutoSave = now;
        }
    }

    /**
     * Sauvegarde toutes les données modifiées
     */
    public void saveAllDirty() {
        List<PlayerEssenceData> dirtyData = new ArrayList<>();
        
        for (PlayerEssenceData data : cache.values()) {
            if (data.isDirty()) {
                dirtyData.add(data);
            }
        }
        
        if (!dirtyData.isEmpty()) {
            database.saveBatch(dirtyData);
            LOGGER.at(Level.INFO).log("Sauvegardé " + dirtyData.size() + " joueurs (auto-save)");
        }
    }

    /**
     * Sauvegarde immédiate d'un joueur (au disconnect par exemple)
     */
    public void savePlayer(@Nonnull UUID playerUuid) {
        PlayerEssenceData data = cache.get(playerUuid);
        if (data != null && data.isDirty()) {
            database.save(data);
            LOGGER.at(Level.FINE).log("Sauvegardé joueur: " + playerUuid);
        }
    }

    /**
     * Sauvegarde finale avant shutdown
     */
    public void shutdown() {
        LOGGER.at(Level.INFO).log("Sauvegarde finale de toutes les données...");
        saveAllDirty();
        database.close();
    }

    /**
     * Retire un joueur du cache (optionnel, pour libérer mémoire)
     */
    public void unloadPlayer(@Nonnull UUID playerUuid) {
        savePlayer(playerUuid);
        cache.remove(playerUuid);
    }
}
