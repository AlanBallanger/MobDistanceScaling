package com.varyon.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.essence.EssenceManager;
import com.varyon.essence.PlayerEssenceData;
import com.varyon.faction.FactionManager;
import com.varyon.hud.ZoneHUDManager;
import com.varyon.VaryonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EssenceCommand extends AbstractAsyncCommand {
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;

    public EssenceCommand(@Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
        super("essence", "Check your essence or view leaderboard");
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
        this.addSubCommand(new TopSubCommand(essenceManager));
        this.addSubCommand(new GiveSubCommand(essenceManager));
        this.addSubCommand(new TakeSubCommand(essenceManager));
        this.addSubCommand(new DepositSubCommand(essenceManager, factionManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();

        if (!(sender instanceof Player player)) {
            context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            context.sendMessage(Message.raw("Impossible d'obtenir la référence joueur.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        int essence = essenceManager.getEssence(playerRef.getUuid());
        int rank = essenceManager.getPlayerRank(playerRef.getUuid());

        context.sendMessage(Message.raw("Essence: " + essence).color(Color.YELLOW));
        context.sendMessage(Message.raw("Rang: #" + rank).color(Color.YELLOW));

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

            context.sendMessage(Message.raw("=== Classement Essence ===").color(Color.ORANGE));

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
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Amount to give", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), amount);
            context.sendMessage(Message.raw("+" + amount + " essence à " + target.getUsername()).color(Color.GREEN));
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
            this.requirePermission("varyon.admin");
            this.playerArg = this.withRequiredArg("player", "Player name", ArgTypes.PLAYER_REF);
            this.amountArg = this.withRequiredArg("amount", "Amount to remove", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            PlayerRef target = context.get(playerArg);
            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(target.getUuid(), target.getUsername(), -amount);
            context.sendMessage(Message.raw("-" + amount + " essence de " + target.getUsername()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class DepositSubCommand extends AbstractAsyncCommand {
        private final EssenceManager essenceManager;
        private final FactionManager factionManager;
        private final RequiredArg<Integer> amountArg;

        public DepositSubCommand(@Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
            super("deposit", "Deposit essence to your faction");
            this.essenceManager = essenceManager;
            this.factionManager = factionManager;
            this.requirePermission("varyon.admin");
            this.amountArg = this.withRequiredArg("amount", "Amount to deposit", ArgTypes.INTEGER);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof Player player)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef == null) {
                context.sendMessage(Message.raw("Impossible d'obtenir la référence joueur.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            FactionManager.Faction faction = factionManager.getFaction(playerRef.getUuid());
            if (faction == null) {
                context.sendMessage(Message.raw("Rejoignez une faction d'abord! /varyon faction <noyau|fracture>").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int amount = context.get(amountArg);
            if (amount <= 0) {
                context.sendMessage(Message.raw("Le montant doit être > 0").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            int currentEssence = essenceManager.getEssence(playerRef.getUuid());
            if (currentEssence < amount) {
                context.sendMessage(Message.raw("Vous n'avez que " + currentEssence + " essence").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            essenceManager.addEssence(playerRef.getUuid(), playerRef.getUsername(), -amount);
            int contribution = amount * faction.getBalanceMultiplier();
            essenceManager.addToGlobalBalance(contribution);

            int newBalance = essenceManager.getGlobalBalance();
            context.sendMessage(Message.raw("Déposé " + amount + " essence dans " + faction.getDisplayName()).color(Color.GREEN));
            context.sendMessage(Message.raw("Balance globale: " + newBalance + "/10000").color(Color.YELLOW));

            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }

            return CompletableFuture.completedFuture(null);
        }
    }
}
