package com.mobdistancescaling.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EssenceManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EssenceDatabase database;
    private final Map<UUID, Integer> essenceCache = new ConcurrentHashMap<>();

    public EssenceManager(@Nonnull File pluginFolder) {
        this.database = new EssenceDatabase(pluginFolder);
        this.database.initialize();
    }

    public int getEssence(UUID playerUuid) {
        return essenceCache.computeIfAbsent(playerUuid, database::getEssence);
    }

    public void addEssence(UUID playerUuid, String playerName, int amount) {
        if (amount == 0) {
            return;
        }

        int current = getEssence(playerUuid);
        int newAmount = current + amount;
        if (newAmount < 0) {
            newAmount = 0;
        }

        essenceCache.put(playerUuid, newAmount);
        database.setEssence(playerUuid, playerName, newAmount);

        if (amount > 0) {
            LOGGER.at(Level.INFO).log("Player " + playerName + " gained " + amount + " essence (total: " + newAmount + ")");
        } else {
            LOGGER.at(Level.INFO).log("Player " + playerName + " lost " + Math.abs(amount) + " essence (total: " + newAmount + ")");
        }
    }

    public void setEssence(UUID playerUuid, String playerName, int amount) {
        essenceCache.put(playerUuid, amount);
        database.setEssence(playerUuid, playerName, amount);
    }

    public List<PlayerEssenceData> getTopPlayers(int limit) {
        return database.getTopPlayers(limit);
    }

    public int getPlayerRank(UUID playerUuid) {
        return database.getPlayerRank(playerUuid);
    }

    public void loadPlayer(UUID playerUuid) {
        int essence = database.getEssence(playerUuid);
        essenceCache.put(playerUuid, essence);
    }

    public void savePlayer(UUID playerUuid) {
        essenceCache.remove(playerUuid);
    }

    public void saveAll() {
        essenceCache.clear();
    }

    public void shutdown() {
        saveAll();
        database.close();
    }
}
