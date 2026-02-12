package com.varyon.deposit;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.essence.EssenceManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.logging.Level;

public class DepositUIManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EssenceManager essenceManager;
    
    public DepositUIManager(@Nonnull EssenceManager essenceManager) {
        this.essenceManager = essenceManager;
    }
    
    public void openDepositUI(@Nonnull Player player) {
        try {
            PlayerRef playerRef = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(player.getUuid());
            if (playerRef == null) {
                LOGGER.at(Level.WARNING).log("PlayerRef is null for player");
                return;
            }
            
            int currentEssence = (int) essenceManager.getEssence(playerRef.getUuid());
            
            if (currentEssence <= 0) {
                player.sendMessage(Message.raw("Vous n'avez pas d'essence à déposer.").color(Color.YELLOW));
                return;
            }
            
            String command = "essence deposit " + currentEssence;
            
            LOGGER.at(Level.INFO).log("Executing deposit command: " + command);
            
            CommandManager commandManager = com.hypixel.hytale.server.core.HytaleServer.get().getCommandManager();
            commandManager.handleCommand(player, command);
            
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to execute deposit: " + e.getMessage());
            player.sendMessage(Message.raw("Erreur lors du dépôt.").color(Color.RED));
        }
    }
}
