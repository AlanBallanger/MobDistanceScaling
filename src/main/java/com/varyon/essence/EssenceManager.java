package com.varyon.essence;

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
    private final Map<UUID, Double> essenceCache = new ConcurrentHashMap<>();

    public EssenceManager(@Nonnull File pluginFolder) {
        this.database = new EssenceDatabase(pluginFolder);
        this.database.initialize();
    }

    public double getEssence(UUID playerUuid) {
        return essenceCache.computeIfAbsent(playerUuid, database::getEssence);
    }

    public int getEssenceDisplay(UUID playerUuid) {
        return (int) Math.floor(getEssence(playerUuid));
    }

    public void addEssence(UUID playerUuid, String playerName, double amount) {
        if (amount == 0) {
            return;
        }

        double current = getEssence(playerUuid);
        double newAmount = current + amount;
        if (newAmount < 0) {
            newAmount = 0;
        }
        if (newAmount > 1000) {
            newAmount = 1000;
        }

        essenceCache.put(playerUuid, newAmount);
        database.setEssence(playerUuid, playerName, newAmount);

        LOGGER.at(Level.FINE).log("Player " + playerName + " " + (amount > 0 ? "+" : "") +
            String.format("%.2f", amount) + " essence (total: " + String.format("%.1f", newAmount) + ")");
    }

    public void setEssence(UUID playerUuid, String playerName, double amount) {
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
        double essence = database.getEssence(playerUuid);
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

    public int getGlobalBalance() {
        return database.getGlobalBalance();
    }

    public void addToGlobalBalance(int amount) {
        database.addToGlobalBalance(amount);
    }
}
