package com.mobdistancescaling.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.config.ExtractionConfig;
import com.mobdistancescaling.extraction.ExtractionPortalManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;

public class ExtractCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PERM_USE = "mobdistancescaling.extract";
    private static final String PERM_BYPASS = "mobdistancescaling.extract.bypass";
    private static final int START_Y = 200;
    private static final int MIN_Y = 0;
    private final Random random = new Random();

    public ExtractCommand() {
        super("extract", "Spawn an extraction portal nearby");
        this.setPermissionGroup(GameMode.Adventure);
        this.requirePermission(PERM_USE);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID playerId = playerRef.getUuid();
        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();

        if (manager == null) {
            context.sendMessage(Message.raw("Extraction system not available.").color(Color.RED));
            return;
        }

        ExtractionConfig config = manager.getConfig();

        if (!config.isEnabled()) {
            context.sendMessage(Message.raw("Extraction is disabled.").color(Color.RED));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        boolean bypass = player != null && player.hasPermission(PERM_BYPASS);

        if (manager.hasActivePortal(playerId)) {
            if (bypass) {
                manager.removePlayerPortal(playerId);
            } else {
                context.sendMessage(Message.raw(config.getMessageAlreadyHasPortal()).color(Color.RED));
                return;
            }
        }

        if (!bypass && manager.isOnCooldown(playerId)) {
            long remaining = manager.getCooldownRemainingSeconds(playerId);
            String msg = config.getMessageCooldown().replace("{remaining}", String.valueOf(remaining));
            context.sendMessage(Message.raw(msg).color(Color.RED));
            return;
        }

        Transform playerTransform = playerRef.getTransform();
        double playerX = playerTransform.getPosition().x;
        double playerZ = playerTransform.getPosition().z;

        int minDist = config.getMinDistance();
        int maxDist = config.getMaxDistance();

        context.sendMessage(Message.raw("Recherche d'un emplacement pour le portail...").color(Color.YELLOW));

        world.execute(() -> {
            try {
                Vector3d portalPos = findPortalPosition(world, playerX, playerZ, minDist, maxDist, 30);

                if (portalPos == null) {
                    context.sendMessage(Message.raw(config.getMessageNoSafeLocation()).color(Color.RED));
                    LOGGER.at(Level.WARNING).log("No safe portal location found for player {0}", playerId);
                    return;
                }

                int px = (int) portalPos.x;
                int py = (int) portalPos.y;
                int pz = (int) portalPos.z;
                double distance = Math.sqrt(Math.pow(px - playerX, 2) + Math.pow(pz - playerZ, 2));

                manager.placePortal(playerId, world, px, py, pz);

                String msg = config.getMessagePortalSpawned()
                    .replace("{distance}", String.valueOf((int) distance))
                    .replace("{x}", String.valueOf(px))
                    .replace("{y}", String.valueOf(py))
                    .replace("{z}", String.valueOf(pz));
                context.sendMessage(Message.raw(msg).color(Color.GREEN));

                LOGGER.at(Level.INFO).log("Portal spawned for " + playerId + " at " + px + "," + py + "," + pz + " dist=" + (int) distance);

            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error spawning extraction portal for player " + playerId + ": " + e.getMessage(), e);
                context.sendMessage(Message.raw(config.getMessageError()).color(Color.RED));
            }
        });
    }

    @Nullable
    private Vector3d findPortalPosition(@Nonnull World world, double playerX, double playerZ,
                                        int minDist, int maxDist, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDist + random.nextDouble() * (maxDist - minDist);

            int targetX = (int) (playerX + Math.cos(angle) * distance);
            int targetZ = (int) (playerZ + Math.sin(angle) * distance);

            Double safeY = findSafeY(world, targetX, targetZ);
            if (safeY != null) {
                return new Vector3d(targetX, safeY, targetZ);
            }
        }
        return null;
    }

    @Nullable
    private Double findSafeY(@Nonnull World world, int x, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            return null;
        }

        for (int checkY = START_Y; checkY >= MIN_Y; checkY--) {
            try {
                if (hasFluid(chunk, x, checkY, z)) {
                    return null;
                }

                if (isSolidBlock(chunk, x, checkY, z)) {
                    int spawnY = checkY + 1;

                    if (hasFluid(chunk, x, spawnY, z) || hasFluid(chunk, x, spawnY + 1, z)) {
                        return null;
                    }

                    boolean hasSpace = true;
                    for (int dy = 0; dy < 4; dy++) {
                        if (isSolidBlock(chunk, x, spawnY + dy, z)) {
                            hasSpace = false;
                            break;
                        }
                    }

                    if (hasSpace) {
                        return (double) spawnY;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private boolean isSolidBlock(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null) {
                return false;
            }
            return blockType.getMaterial() == BlockMaterial.Solid;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasFluid(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return chunk.getFluidId(x, y, z) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
