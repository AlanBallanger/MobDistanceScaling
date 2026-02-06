package com.mobdistancescaling.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.faction.FactionManager;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MdsCommand extends AbstractAsyncCommand {
    private final MobDistanceScalingPlugin plugin;
    private final FactionManager factionManager;

    public MdsCommand(MobDistanceScalingPlugin plugin, FactionManager factionManager) {
        super("mds", "MobDistanceScaling commands");
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.requirePermission("mobdistancescaling.admin");
        this.addSubCommand(new ReloadSubCommand(plugin));
        this.addSubCommand(new ClearMapSubCommand());
        this.addSubCommand(new ClearMapAllSubCommand());
        this.addSubCommand(new FactionSubCommand(factionManager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("MobDistanceScaling commands:").color(Color.YELLOW));
        commandContext.sendMessage(Message.raw("  /mds reload - Reload configuration").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /mds clearmap - Clear map cache around you").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /mds clearmapall - Clear map cache for all players").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /mds faction <noyau|fracture> - Join a faction").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }

    public static class ReloadSubCommand extends AbstractAsyncCommand {
        private final MobDistanceScalingPlugin plugin;

        public ReloadSubCommand(MobDistanceScalingPlugin plugin) {
            super("reload", "Reload MobDistanceScaling configuration");
            this.plugin = plugin;
            this.requirePermission("mobdistancescaling.admin.reload");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            try {
                ConfigManager configManager = plugin.getConfigManager();
                configManager.reload();

                int zoneCount = configManager.getZoneConfig().getZones().size();
                String pattern = configManager.getZoneConfig().getMinimapPattern();
                int opacity = configManager.getZoneConfig().getMinimapOpacity();

                commandContext.sendMessage(Message.raw("MobDistanceScaling configuration reloaded!").color(Color.GREEN));
                commandContext.sendMessage(Message.raw("  Zones: " + zoneCount).color(Color.WHITE));
                commandContext.sendMessage(Message.raw("  Pattern: " + pattern).color(Color.WHITE));
                commandContext.sendMessage(Message.raw("  Opacity: " + opacity + "%").color(Color.WHITE));
                commandContext.sendMessage(Message.raw("Note: Map cache may need to be cleared for visual changes.").color(Color.YELLOW));
            } catch (Exception e) {
                commandContext.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()).color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ClearMapSubCommand extends AbstractAsyncCommand {
        private static final int CLEAR_RADIUS = 16; // Clear 16 chunks around player (32x32 chunk area)

        public ClearMapSubCommand() {
            super("clearmap", "Clear map cache around your position");
            this.requirePermission("mobdistancescaling.admin.clearmap");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            CommandSender sender = commandContext.sender();
            if (!(sender instanceof Player)) {
                commandContext.sendMessage(Message.raw("This command can only be used by players.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Player player = (Player) sender;
            Ref ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                commandContext.sendMessage(Message.raw("Player reference not valid.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            World world = ((EntityStore) ref.getStore().getExternalData()).getWorld();

            // Run store operations on the world thread
            return CompletableFuture.runAsync(() -> {
                TransformComponent transform = (TransformComponent) ref.getStore().getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    commandContext.sendMessage(Message.raw("Could not get player position.").color(Color.RED));
                    return;
                }

                int centerChunkX = ChunkUtil.chunkCoordinate((double) transform.getPosition().getX());
                int centerChunkZ = ChunkUtil.chunkCoordinate((double) transform.getPosition().getZ());

                // Build set of chunk keys to clear
                LongSet chunks = new LongOpenHashSet();
                for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
                    for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
                        long chunkKey = ChunkUtil.indexChunk(centerChunkX + dx, centerChunkZ + dz);
                        chunks.add(chunkKey);
                    }
                }

                // Clear the map cache
                world.getWorldMapManager().clearImagesInChunks(chunks);
                for (PlayerRef playerRef : world.getPlayerRefs()) {
                    Player p = (Player) world.getEntityStore().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
                    if (p != null) {
                        p.getWorldMapTracker().clearChunks(chunks);
                    }
                }

                int totalChunks = (CLEAR_RADIUS * 2 + 1) * (CLEAR_RADIUS * 2 + 1);
                commandContext.sendMessage(Message.raw("Map cache cleared! (" + totalChunks + " chunks around your position)").color(Color.GREEN));
                commandContext.sendMessage(Message.raw("The map will refresh as you move around.").color(Color.YELLOW));
            }, (Executor) world);
        }
    }

    public static class ClearMapAllSubCommand extends AbstractAsyncCommand {
        public ClearMapAllSubCommand() {
            super("clearmapall", "Clear entire map cache for all players in all worlds");
            this.requirePermission("mobdistancescaling.admin.clearmapall");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            // Count worlds to process
            int worldCount = Universe.get().getWorlds().size();
            if (worldCount == 0) {
                commandContext.sendMessage(Message.raw("No worlds found.").color(Color.YELLOW));
                return CompletableFuture.completedFuture(null);
            }

            // Process each world on its own thread
            for (World world : Universe.get().getWorlds().values()) {
                world.execute(() -> {
                    // Clear ALL server-side map cache for this world
                    world.getWorldMapManager().clearImages();

                    // Clear each player's client-side map cache entirely
                    for (PlayerRef playerRef : world.getPlayerRefs()) {
                        Ref ref = playerRef.getReference();
                        if (ref == null || !ref.isValid()) continue;

                        Player player = (Player) world.getEntityStore().getStore().getComponent(ref, Player.getComponentType());
                        if (player == null) continue;

                        // clear() sends ClearWorldMap packet and resets everything
                        player.getWorldMapTracker().clear();
                    }
                });
            }

            commandContext.sendMessage(Message.raw("Entire map cache cleared for all players in " + worldCount + " world(s)!").color(Color.GREEN));
            commandContext.sendMessage(Message.raw("Maps will regenerate as players move around.").color(Color.YELLOW));

            return CompletableFuture.completedFuture(null);
        }
    }

    public static class FactionSubCommand extends AbstractAsyncCommand {
        private final FactionManager factionManager;
        private final RequiredArg<String> factionArg;

        public FactionSubCommand(FactionManager factionManager) {
            super("faction", "Join a faction (noyau or fracture)");
            this.factionManager = factionManager;
            this.factionArg = this.withRequiredArg("faction", "Faction name (noyau/fracture)", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            CommandSender sender = commandContext.sender();
            if (!(sender instanceof Player)) {
                commandContext.sendMessage(Message.raw("This command can only be used by players.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            String factionName = commandContext.get(factionArg);
            FactionManager.Faction faction = FactionManager.Faction.fromString(factionName);
            
            if (faction == null) {
                commandContext.sendMessage(Message.raw("Invalid faction. Choose: noyau or fracture").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Player player = (Player) sender;
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null) {
                commandContext.sendMessage(Message.raw("Could not get player reference.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            factionManager.setFaction(playerRef.getUuid(), faction);
            commandContext.sendMessage(Message.raw("You have joined faction: " + faction.getDisplayName()).color(Color.GREEN));
            
            return CompletableFuture.completedFuture(null);
        }
    }
}
