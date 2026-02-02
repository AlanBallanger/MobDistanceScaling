package com.mobdistancescaling.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mobdistancescaling.essence.EssenceManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class GiveEssenceCommand extends AbstractAsyncCommand {
    private final EssenceManager essenceManager;

    public GiveEssenceCommand(@Nonnull EssenceManager essenceManager) {
        super("giveessence", "Give essence to yourself (admin)");
        this.essenceManager = essenceManager;
        this.requirePermission("mobdistancescaling.admin");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();
        
        if (!(sender instanceof Player player)) {
            context.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            context.sendMessage(Message.raw("Error: Could not get player reference").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        int amount = 100;
        String playerName = playerRef.getUuid().toString();
        essenceManager.addEssence(playerRef.getUuid(), playerName, amount);
        
        context.sendMessage(Message.raw("Added " + amount + " essence!").color(Color.GREEN));
        context.sendMessage(Message.raw("Total: " + essenceManager.getEssence(playerRef.getUuid())).color(Color.YELLOW));
        
        return CompletableFuture.completedFuture(null);
    }
}
