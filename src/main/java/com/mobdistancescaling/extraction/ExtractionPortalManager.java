package com.mobdistancescaling.extraction;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.mobdistancescaling.config.ExtractionConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ExtractionPortalManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PORTAL_BLOCK_TYPE = "ExtractionPortal";

    private static ExtractionPortalManager instance;

    private final Map<UUID, PortalData> playerPortals = new ConcurrentHashMap<>();
    private final Map<String, UUID> positionToOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private ExtractionConfig config;

    public ExtractionPortalManager(@Nonnull ExtractionConfig config) {
        this.config = config;
        instance = this;
        LOGGER.at(Level.INFO).log("ExtractionPortalManager initialized");
    }

    @Nullable
    public static ExtractionPortalManager getInstance() {
        return instance;
    }

    public void setConfig(@Nonnull ExtractionConfig config) {
        this.config = config;
    }

    @Nonnull
    public ExtractionConfig getConfig() {
        return config;
    }

    @Nonnull
    public Map<UUID, PortalData> getActivePortals() {
        return playerPortals;
    }

    public boolean hasActivePortal(@Nonnull UUID playerId) {
        return playerPortals.containsKey(playerId);
    }

    public boolean isOnCooldown(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    public long getCooldownRemainingSeconds(@Nonnull UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Nullable
    public UUID getPortalOwner(int x, int y, int z) {
        String key = positionKey(x, y, z);
        return positionToOwner.get(key);
    }

    public void placePortal(@Nonnull UUID ownerId, @Nonnull World world, int x, int y, int z) {
        LOGGER.at(Level.INFO).log("Placing extraction portal for player " + ownerId + " at " + x + ", " + y + ", " + z);

        String posKey = positionKey(x, y, z);

        int blockIndex = BlockType.getAssetMap().getIndex(PORTAL_BLOCK_TYPE);
        if (blockIndex == Integer.MIN_VALUE) {
            LOGGER.at(Level.SEVERE).log("Block type '" + PORTAL_BLOCK_TYPE + "' not found in asset map! Check that the ExtractionPortal JSON is valid and loaded.");
            return;
        }
        BlockType portalBlockType = BlockType.getAssetMap().getAsset(blockIndex);
        LOGGER.at(Level.INFO).log("Block type resolved: " + PORTAL_BLOCK_TYPE + " -> index=" + blockIndex + ", type=" + portalBlockType.getId());

        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                LOGGER.at(Level.SEVERE).log("Chunk not loaded at block position " + x + ", " + z + " (chunk index: " + chunkIndex + ")");
                world.getChunkAsync(chunkIndex).thenAccept(asyncChunk -> {
                    placeBlockInChunk(asyncChunk, x, y, z, blockIndex, portalBlockType, ownerId);
                }).exceptionally(ex -> {
                    LOGGER.at(Level.SEVERE).log("Failed to load chunk for portal: " + ex.getMessage());
                    return null;
                });
            } else {
                placeBlockInChunk(chunk, x, y, z, blockIndex, portalBlockType, ownerId);
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Exception placing portal block: " + e.getClass().getName() + " - " + e.getMessage());
        }

        ScheduledFuture<?> expiryTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> expirePortal(ownerId),
            config.getPortalDurationSeconds(),
            TimeUnit.SECONDS
        );

        PortalData portalData = new PortalData(ownerId, world, x, y, z, System.currentTimeMillis(), expiryTask);
        playerPortals.put(ownerId, portalData);
        positionToOwner.put(posKey, ownerId);

        LOGGER.at(Level.INFO).log("Portal registered for player " + ownerId + ", expires in " + config.getPortalDurationSeconds() + "s");
    }

    private void placeBlockInChunk(@Nonnull WorldChunk chunk, int x, int y, int z,
                                    int blockIndex, @Nonnull BlockType portalBlockType, @Nonnull UUID ownerId) {
        for (int dy = 0; dy < 4; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    chunk.setBlock(x + dx, y + dy, z + dz, BlockType.EMPTY);
                }
            }
        }
        boolean placed = chunk.setBlock(x, y, z, blockIndex, portalBlockType, 0, 0, 0);
        LOGGER.at(Level.INFO).log("Portal block placed at " + x + ", " + y + ", " + z + " for player " + ownerId + " (success=" + placed + ", blockIndex=" + blockIndex + ")");
    }

    public void consumePortal(@Nonnull UUID ownerId) {
        PortalData data = playerPortals.remove(ownerId);
        if (data == null) {
            LOGGER.at(Level.WARNING).log("Tried to consume portal for player {0} but no portal found", ownerId);
            return;
        }

        data.expiryTask().cancel(false);
        String posKey = positionKey(data.x(), data.y(), data.z());
        positionToOwner.remove(posKey);

        removePortalBlock(data);

        cooldowns.put(ownerId, System.currentTimeMillis() + (config.getCooldownSeconds() * 1000L));

        LOGGER.at(Level.INFO).log("Portal consumed by player " + ownerId + " at " + data.x() + ", " + data.y() + ", " + data.z() + ". Cooldown set for " + config.getCooldownSeconds() + "s");
    }

    private void expirePortal(@Nonnull UUID ownerId) {
        PortalData data = playerPortals.remove(ownerId);
        if (data == null) {
            return;
        }

        String posKey = positionKey(data.x(), data.y(), data.z());
        positionToOwner.remove(posKey);

        removePortalBlock(data);

        LOGGER.at(Level.INFO).log("Portal expired for player " + ownerId + " at " + data.x() + ", " + data.y() + ", " + data.z());

        try {
            PlayerRef playerRef = Universe.get().getPlayer(ownerId);
            if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().isValid()) {
                playerRef.sendMessage(Message.raw(config.getMessagePortalExpired()).color(Color.YELLOW));
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to notify player " + ownerId + " of portal expiry: " + e.getMessage());
        }
    }

    private void removePortalBlock(@Nonnull PortalData data) {
        World world = data.world();
        int x = data.x();
        int y = data.y();
        int z = data.z();

        world.getChunkAsync(ChunkUtil.indexChunkFromBlock(x, z)).thenAcceptAsync(chunk -> {
            chunk.setBlock(x, y, z, BlockType.EMPTY);
            LOGGER.at(Level.INFO).log("Portal block removed at " + x + ", " + y + ", " + z);
        }, (Executor) world);
    }

    public void removePlayerPortal(@Nonnull UUID playerId) {
        PortalData data = playerPortals.remove(playerId);
        if (data == null) {
            return;
        }
        data.expiryTask().cancel(false);
        String posKey = positionKey(data.x(), data.y(), data.z());
        positionToOwner.remove(posKey);
        removePortalBlock(data);
        LOGGER.at(Level.INFO).log("Portal removed for disconnected player {0}", playerId);
    }

    public void shutdown() {
        for (Map.Entry<UUID, PortalData> entry : playerPortals.entrySet()) {
            PortalData data = entry.getValue();
            data.expiryTask().cancel(false);
            removePortalBlock(data);
        }
        playerPortals.clear();
        positionToOwner.clear();
        cooldowns.clear();
        LOGGER.at(Level.INFO).log("ExtractionPortalManager shut down, all portals removed");
    }

    private static String positionKey(int x, int y, int z) {
        return x + "_" + y + "_" + z;
    }

    public record PortalData(
        UUID ownerId,
        World world,
        int x, int y, int z,
        long creationTime,
        ScheduledFuture<?> expiryTask
    ) {}
}
