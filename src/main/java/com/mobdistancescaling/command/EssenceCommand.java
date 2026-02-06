package com.mobdistancescaling.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mobdistancescaling.essence.EssenceManager;
import com.mobdistancescaling.essence.PlayerEssenceData;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EssenceCommand extends AbstractAsyncCommand {
    private final EssenceManager essenceManager;

    public EssenceCommand(@Nonnull EssenceManager essenceManager) {
        super("essence", "Check your essence or view leaderboard");
        this.essenceManager = essenceManager;
        this.addSubCommand(new TopSubCommand(essenceManager));
        this.addSubCommand(new GiveSubCommand(essenceManager));
        this.addSubCommand(new TakeSubCommand(essenceManager));
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

        int essence = essenceManager.getEssence(playerRef.getUuid());
        int rank = essenceManager.getPlayerRank(playerRef.getUuid());
        
        context.sendMessage(Message.raw("Your Essence: " + essence).color(Color.YELLOW));
        context.sendMessage(Message.raw("Your Rank: #" + rank).color(Color.YELLOW));
        
        return CompletableFuture.completedFuture(null);
    }

    public static class TopSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;

        public TopSubCommand(@Nonnull EssenceManager essenceManager) {
            super("top", "View essence leaderboard");
            this.essenceManager = essenceManager;
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            List<PlayerEssenceData> topPlayers = essenceManager.getTopPlayers(10);
            
            context.sendMessage(Message.raw("=== Essence Leaderboard ===").color(Color.ORANGE));
            
            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerEssenceData data = topPlayers.get(i);
                String position = "#" + (i + 1);
                context.sendMessage(Message.raw(position + " " + data.name() + " - " + data.essence() + " essence").color(Color.WHITE));
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class GiveSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public GiveSubCommand(@Nonnull EssenceManager essenceManager) {
            super("give", "Give essence to a player");
            this.essenceManager = essenceManager;
            this.requirePermission("mobdistancescaling.admin");
            this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Amount to give", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Amount must be > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("Added " + amount + " essence to " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class TakeSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        public TakeSubCommand(@Nonnull EssenceManager essenceManager) {
            super("take", "Remove essence from a player");
            this.essenceManager = essenceManager;
            this.requirePermission("mobdistancescaling.admin");
            this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Amount to remove", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Amount must be > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), -amount);
            context.sendMessage(Message.raw("Removed " + amount + " essence from " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }
}
