package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.faction.FactionManager;
import com.varyon.deposit.DepositBlockManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VaryonCommand extends AbstractAsyncCommand {
    private final VaryonPlugin plugin;
    private final FactionManager factionManager;
    private final DepositBlockManager depositBlockManager;

    public VaryonCommand(VaryonPlugin plugin, FactionManager factionManager, DepositBlockManager depositBlockManager) {
        super("varyon", "Varyon commands");
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.depositBlockManager = depositBlockManager;
        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new ExtractSubCommand());
        this.addSubCommand(new ReloadSubCommand(plugin));
        this.addSubCommand(new ClearMapSubCommand());
        this.addSubCommand(new FactionSubCommand(factionManager));
        this.addSubCommand(new ResetRewardsSubCommand());
        this.addSubCommand(new ResetBalanceSubCommand());
        this.addSubCommand(new com.varyon.command.CreateDepositSubCommand(depositBlockManager));
        this.addSubCommand(new com.varyon.command.ResetDepositSubCommand(depositBlockManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        return new HelpSubCommand().executeAsync(context);
    }

    public class HelpSubCommand extends AbstractAsyncCommand {
        public HelpSubCommand() {
            super("help", "Show available commands");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            boolean isAdmin = false;
            boolean hasRtp = false;

            if (sender instanceof Player player) {
                isAdmin = player.hasPermission("varyon.admin");
                hasRtp = player.hasPermission("varyon.rtp");
            } else {
                isAdmin = true;
                hasRtp = true;
            }

            context.sendMessage(Message.raw("=== Varyon ===").color(Color.YELLOW));
            context.sendMessage(Message.raw("  /varyon extract - Invoque un portail d'extraction").color(Color.WHITE));
            context.sendMessage(Message.raw("  /varyon faction <nom> - Rejoindre une faction").color(Color.WHITE));
            context.sendMessage(Message.raw("  /essence - Voir votre essence").color(Color.WHITE));
            context.sendMessage(Message.raw("  /essence top - Classement d'essence").color(Color.WHITE));
            context.sendMessage(Message.raw("  /essence deposit <montant> - Déposer de l'essence").color(Color.WHITE));

            if (isAdmin) {
                context.sendMessage(Message.raw("  /varyon reload - Recharger la configuration").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon clearmap - Vider le cache de la map").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon createdeposit - Créer un bloc de dépôt").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetdeposit - Supprimer tous les blocs de dépôt").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetrewards - Reset cooldowns des récompenses").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetbalance - Reset la jauge globale à 0").color(Color.WHITE));
                context.sendMessage(Message.raw("  /essence give <joueur> <montant>").color(Color.WHITE));
                context.sendMessage(Message.raw("  /essence take <joueur> <montant>").color(Color.WHITE));
                context.sendMessage(Message.raw("  /essence setmax <joueur> <montant>").color(Color.WHITE));
            }

            if (hasRtp) {
                context.sendMessage(Message.raw("  /rtpv <zone> - TP zone mod").color(Color.WHITE));
                context.sendMessage(Message.raw("  /rtpz [zone] - TP zone vanilla").color(Color.WHITE));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ExtractSubCommand extends AbstractAsyncCommand {
        public ExtractSubCommand() {
            super("extract", "Spawn an extraction portal nearby");
            this.requirePermission("varyon.extract");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof Player player)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Ref ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Message.raw("Référence joueur invalide.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            EntityStore entityStore = (EntityStore) ref.getStore().getExternalData();
            World world = entityStore.getWorld();
            PlayerRef playerRef = (PlayerRef) ref.getStore().getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) {
                context.sendMessage(Message.raw("Impossible d'obtenir la référence joueur.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            ExtractCommand.executeExtract(context, ref.getStore(), ref, playerRef, world, player);
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ReloadSubCommand extends AbstractAsyncCommand {
        private final VaryonPlugin plugin;

        public ReloadSubCommand(VaryonPlugin plugin) {
            super("reload", "Reload configuration");
            this.plugin = plugin;
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            try {
                ConfigManager configManager = plugin.getConfigManager();
                configManager.reload();

                int zoneCount = configManager.getZoneConfig().getZones().size();
                context.sendMessage(Message.raw("Configuration rechargée! (" + zoneCount + " zones)").color(Color.GREEN));
            } catch (Exception e) {
                context.sendMessage(Message.raw("Erreur: " + e.getMessage()).color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ClearMapSubCommand extends AbstractAsyncCommand {
        public ClearMapSubCommand() {
            super("clearmap", "Clear entire map cache");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            int worldCount = Universe.get().getWorlds().size();
            if (worldCount == 0) {
                context.sendMessage(Message.raw("Aucun monde trouvé.").color(Color.YELLOW));
                return CompletableFuture.completedFuture(null);
            }

            for (World world : Universe.get().getWorlds().values()) {
                world.execute(() -> {
                    world.getWorldMapManager().clearImages();
                    for (PlayerRef playerRef : world.getPlayerRefs()) {
                        Ref ref = playerRef.getReference();
                        if (ref == null || !ref.isValid()) continue;
                        Player player = (Player) world.getEntityStore().getStore().getComponent(ref, Player.getComponentType());
                        if (player == null) continue;
                        player.getWorldMapTracker().clear();
                    }
                });
            }

            context.sendMessage(Message.raw("Cache map vidé pour " + worldCount + " monde(s)!").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class FactionSubCommand extends AbstractAsyncCommand {
        private final FactionManager factionManager;
        private final RequiredArg<String> factionArg;

        public FactionSubCommand(FactionManager factionManager) {
            super("faction", "Join a faction");
            this.factionManager = factionManager;
            this.factionArg = this.withRequiredArg("faction", "Faction name (noyau/fracture)", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof Player player)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            String factionName = context.get(factionArg);
            FactionManager.Faction faction = FactionManager.Faction.fromString(factionName);

            if (faction == null) {
                context.sendMessage(Message.raw("Faction invalide. Choix: noyau ou fracture").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null) {
                context.sendMessage(Message.raw("Impossible d'obtenir la référence joueur.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            factionManager.setFaction(playerRef.getUuid(), faction);
            context.sendMessage(Message.raw("Vous avez rejoint: " + faction.getDisplayName()).color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ResetRewardsSubCommand extends AbstractAsyncCommand {
        public ResetRewardsSubCommand() {
            super("resetrewards", "Reset all reward tier cooldowns");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            com.varyon.essence.GlobalRewardsManager rewardsManager = VaryonPlugin.getStaticGlobalRewardsManager();
            
            if (rewardsManager == null) {
                context.sendMessage(Message.raw("Système de récompenses non initialisé.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            rewardsManager.resetAllCooldowns();
            context.sendMessage(Message.raw("Tous les cooldowns de récompenses ont été réinitialisés.").color(Color.GREEN));
            
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ResetBalanceSubCommand extends AbstractAsyncCommand {
        public ResetBalanceSubCommand() {
            super("resetbalance", "Reset global essence balance to 0");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            com.varyon.essence.EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
            
            if (essenceManager == null) {
                context.sendMessage(Message.raw("Système d'essence non initialisé.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            int oldBalance = essenceManager.getGlobalBalance();
            essenceManager.setGlobalBalance(0);
            
            context.sendMessage(Message.raw("Balance globale réinitialisée: " + oldBalance + " → 0").color(Color.GREEN));
            
            // Update all HUDs
            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }
}
