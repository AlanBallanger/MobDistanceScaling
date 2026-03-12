package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.RtpvConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.rtpv.RtpvJoinManager;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class JoinCommand extends AbstractAsyncCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RequiredArg<String> playerArg;

    public JoinCommand() {
        super("join", "Rejoindre un ami qui vient de faire /rtpv");
        this.requirePermission("varyon.rtp");
        this.playerArg = this.withRequiredArg("joueur", "Nom du joueur", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Cette commande doit être exécutée par un joueur.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Player joinerPlayer = (Player) context.sender();
        if (joinerPlayer == null) return CompletableFuture.completedFuture(null);

        String targetName = context.get(playerArg).trim();
        if (targetName.isEmpty()) {
            context.sendMessage(Message.raw("Spécifiez le nom du joueur à rejoindre.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef joinerRef = Universe.get().getPlayer(joinerPlayer.getUuid());
        if (joinerRef == null) {
            context.sendMessage(Message.raw("Vous devez être connecté.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef targetRef = Universe.get().getPlayers().stream()
                .filter(p -> p.getUsername() != null && p.getUsername().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Joueur introuvable ou hors ligne.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (targetRef.getUuid().equals(joinerRef.getUuid())) {
            context.sendMessage(Message.raw("Vous ne pouvez pas vous rejoindre vous-même.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> targetEntityRef = targetRef.getReference();
        if (targetEntityRef == null || !targetEntityRef.isValid()) {
            context.sendMessage(Message.raw("Le joueur n'est pas dans un monde.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> targetStore = targetEntityRef.getStore();
        Object ext = targetStore.getExternalData();
        if (!(ext instanceof EntityStore targetEntityStore) || targetEntityStore.getWorld() == null) {
            context.sendMessage(Message.raw("Impossible de localiser le joueur.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        World targetWorld = targetEntityStore.getWorld();
        String targetWorldName = targetWorld.getName();

        String joinerWorldName = joinerPlayer.getWorld() != null ? joinerPlayer.getWorld().getName() : "";
        if (!targetWorldName.equals(joinerWorldName)) {
            context.sendMessage(Message.raw("Vous devez être dans le même monde (" + targetWorldName + ").").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
        if (joinMgr == null) {
            context.sendMessage(Message.raw("Système de join indisponible.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        RtpvJoinManager.JoinableEntry entry = joinMgr.getJoinable(targetRef.getUuid(), targetWorldName, zoneConfig);
        if (entry == null) {
            context.sendMessage(Message.raw(targetName + " n'est pas rejoignable (utilisez /rtpv <zone> pour être rejoint).").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        ZonePermissionsConfig zonePerms = VaryonPlugin.getStaticConfigManager().getZonePermissionsConfig();
        if (!zonePerms.canAccessZone(joinerPlayer, entry.zoneId())) {
            String required = zonePerms.getPermissionForZone(entry.zoneId());
            context.sendMessage(Message.raw("Vous n'avez pas accès à la zone " + entry.zoneId() + ". Permission : " + required).color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        DifficultyZone zone = zoneConfig.getZoneById(entry.zoneId());
        if (zone == null) {
            context.sendMessage(Message.raw("Zone invalide.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        RtpvConfig rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
        int baseCost = zone.getTeleportCost();
        int finalCost = (int) Math.ceil(baseCost);
        BigDecimal costBD = BigDecimal.valueOf(finalCost);

        if (rtpvConfig.isEconomyEnabled()) {
            try {
                Economy economy = VaultUnlockedServicesManager.get().economyObj();
                if (economy != null && economy.isEnabled()) {
                    if (!economy.has("Varyon", joinerRef.getUuid(), costBD)) {
                        BigDecimal balance = economy.getBalance("Varyon", joinerRef.getUuid());
                        context.sendMessage(Message.raw(
                            "Coins insuffisants. Coût : " + finalCost + " | Solde : " + balance.intValue()
                        ).color(Color.RED));
                        return CompletableFuture.completedFuture(null);
                    }
                }
            } catch (NoClassDefFoundError e) {
                LOGGER.at(Level.WARNING).log("Vault non disponible");
            }
        }

        TransformComponent targetTransform = targetStore.getComponent(targetEntityRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            context.sendMessage(Message.raw("Impossible de récupérer la position.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Vector3d pos = targetTransform.getPosition().clone();
        Vector3f rot = targetTransform.getRotation() != null ? targetTransform.getRotation().clone() : new Vector3f(0, 0, 0);

        targetWorld.execute(() -> {
            try {
                Ref<EntityStore> joinerEntityRef = joinerRef.getReference();
                if (joinerEntityRef == null || !joinerEntityRef.isValid()) return;

                Store<EntityStore> joinerStore = joinerEntityRef.getStore();
                if (rtpvConfig.isEconomyEnabled()) {
                    try {
                        Economy economy = VaultUnlockedServicesManager.get().economyObj();
                        if (economy != null && economy.isEnabled()) {
                            EconomyResponse response = economy.withdraw("Varyon", joinerRef.getUuid(), costBD);
                            if (!response.transactionSuccess()) {
                                LOGGER.at(Level.WARNING).log("Join: failed to deduct " + finalCost + " from " + joinerRef.getUuid());
                                context.sendMessage(Message.raw("Erreur: " + response.errorMessage).color(Color.RED));
                                return;
                            }
                        }
                    } catch (NoClassDefFoundError ignored) {}
                }

                Teleport teleport = Teleport.createForPlayer(targetWorld, pos, rot);
                joinerStore.addComponent(joinerEntityRef, Teleport.getComponentType(), teleport);

                String costSuffix = rtpvConfig.isEconomyEnabled() ? " (-" + finalCost + " coins)" : "";
                context.sendMessage(Message.raw("Rejoint " + targetName + " en zone " + entry.zoneId() + costSuffix).color(Color.GREEN));
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Join error: " + e.getMessage());
                context.sendMessage(Message.raw("Échec de la téléportation.").color(Color.RED));
            }
        });
        return CompletableFuture.completedFuture(null);
    }
}
