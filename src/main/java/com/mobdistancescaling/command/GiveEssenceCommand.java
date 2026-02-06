package com.mobdistancescaling.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
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
    private final OptionalArg<Integer> amountArg;

    public GiveEssenceCommand(@Nonnull EssenceManager essenceManager) {
        super("giveessence", "Give or remove essence (admin)");
        this.essenceManager = essenceManager;
        this.requirePermission("mobdistancescaling.admin");
        this.amountArg = this.withOptionalArg("amount", "Amount to add (negative to remove)", ArgTypes.INTEGER);
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
        if (context.provided(amountArg)) {
            Integer parsed = context.get(amountArg);
            if (parsed != null) {
                amount = parsed;
            }
        }
        String playerName = playerRef.getUuid().toString();
        essenceManager.addEssence(playerRef.getUuid(), playerName, amount);
        
        if (amount >= 0) {
            context.sendMessage(Message.raw("Added " + amount + " essence!").color(Color.GREEN));
        } else {
            context.sendMessage(Message.raw("Removed " + Math.abs(amount) + " essence!").color(Color.RED));
        }
        context.sendMessage(Message.raw("Total: " + essenceManager.getEssence(playerRef.getUuid())).color(Color.YELLOW));
        
        return CompletableFuture.completedFuture(null);
    }
}
