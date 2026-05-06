package com.varyon.integration;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public final class FactionDepositEcoSync {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FactionDepositEcoSync() {}

    public static void applyEcoFactionTokenForDeposit(@Nonnull PlayerRef playerRef, long depositedFactionPoints) {
        if (depositedFactionPoints <= 0) {
            return;
        }
        try {
            String name = playerRef.getUsername();
            if (name == null || name.isBlank()) {
                return;
            }
            String cmd = "eco settoken faction " + name + " " + depositedFactionPoints;
            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, cmd).exceptionally(ex -> {
                LOGGER.at(Level.WARNING).log("Faction deposit eco settoken failed for " + name + ": " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Faction deposit eco settoken error: " + e.getMessage());
        }
    }
}
