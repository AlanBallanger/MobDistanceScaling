package com.varyon.faction;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FactionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Map<UUID, Faction> playerFactions = new ConcurrentHashMap<>();

    public enum Faction {
        NOYAU("Noyau", -1),
        FRACTURE("Fracture", 1);

        private final String displayName;
        private final int balanceMultiplier;

        Faction(String displayName, int balanceMultiplier) {
            this.displayName = displayName;
            this.balanceMultiplier = balanceMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getBalanceMultiplier() {
            return balanceMultiplier;
        }

        @Nullable
        public static Faction fromString(String name) {
            for (Faction faction : values()) {
                if (faction.name().equalsIgnoreCase(name)) {
                    return faction;
                }
            }
            return null;
        }
    }

    public void setFaction(@Nonnull UUID playerUuid, @Nonnull Faction faction) {
        playerFactions.put(playerUuid, faction);
        LOGGER.at(Level.INFO).log("Player " + playerUuid + " joined faction: " + faction.getDisplayName());
    }

    @Nullable
    public Faction getFaction(@Nonnull UUID playerUuid) {
        return playerFactions.get(playerUuid);
    }

    public boolean hasFaction(@Nonnull UUID playerUuid) {
        return playerFactions.containsKey(playerUuid);
    }

    public void removeFaction(@Nonnull UUID playerUuid) {
        playerFactions.remove(playerUuid);
    }
}
