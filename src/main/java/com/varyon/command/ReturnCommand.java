package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ReturnConfig;
import com.varyon.death.DeathPointManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Random;
import java.util.logging.Level;

public class ReturnCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PERM_USE = "varyon.return";
    private static final String PERM_BYPASS = "varyon.return.bypass";
    private static final int START_Y = 200;
    private static final int MIN_Y = 0;
    private static final Random random = new Random();
    
    public ReturnCommand() {
        super("return", "Teleport back near your death point");
        this.setPermissionGroup(GameMode.Adventure);
        this.requirePermission(PERM_USE);
    }
    
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        
        DeathPointManager deathManager = VaryonPlugin.getInstance().getDeathPointManager();
        if (deathManager == null) {
            context.sendMessage(Message.raw("Système de retour indisponible.").color(Color.RED));
            return;
        }
        
        ReturnConfig config = VaryonPlugin.getStaticConfigManager().getReturnConfig();
        MessagesConfig.ReturnMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getReturn();
        
        if (!config.isEnabled()) {
            context.sendMessage(Message.raw("Le système de retour est désactivé.").color(Color.RED));
            return;
        }
        
        boolean bypass = player != null && player.hasPermission(PERM_BYPASS);
        
        // Vérifier le cooldown
        if (!bypass && deathManager.isOnCooldown(playerRef.getUuid())) {
            long remaining = deathManager.getCooldownRemainingSeconds(playerRef.getUuid());
            String cooldownMsg = msg.cooldown.replace("{remaining}", String.valueOf(remaining));
            context.sendMessage(Message.raw(cooldownMsg).color(Color.RED));
            return;
        }
        
        // Récupérer le point de mort
        DeathPointManager.DeathPoint deathPoint = deathManager.getDeathPoint(playerRef.getUuid());
        
        if (deathPoint == null) {
            context.sendMessage(Message.raw(msg.noDeathPoint).color(Color.RED));
            return;
        }
        
        // Vérifier si expiré
        if (deathPoint.isExpired(config.getExpirationMinutes())) {
            deathManager.removeDeathPoint(playerRef.getUuid());
            context.sendMessage(Message.raw(msg.expired).color(Color.RED));
            return;
        }
        
        // Vérifier si déjà utilisé
        if (deathPoint.isUsed()) {
            context.sendMessage(Message.raw(msg.alreadyUsed).color(Color.YELLOW));
            return;
        }
        
        // Avertissement première utilisation
        context.sendMessage(Message.raw(msg.firstUseWarning).color(Color.YELLOW));
        context.sendMessage(Message.raw(msg.teleporting).color(Color.GREEN));
        
        world.execute(() -> {
            try {
                Vector3d targetPos = findSafePositionNearDeath(world, deathPoint, 
                    config.getMinDistance(), config.getMaxDistance(), 30);
                
                if (targetPos == null) {
                    context.sendMessage(Message.raw(msg.noSafeLocation
                        .replace("{attempts}", "30")).color(Color.RED));
                    return;
                }
                
                // Téléporter le joueur
                Teleport teleport = Teleport.createForPlayer(world, targetPos, new Vector3f(0, 0, 0));
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                
                // Calculer la distance
                double distance = Math.sqrt(
                    Math.pow(targetPos.x - deathPoint.getX(), 2) + 
                    Math.pow(targetPos.z - deathPoint.getZ(), 2));
                
                String successMsg = msg.success
                    .replace("{distance}", String.valueOf((int)distance))
                    .replace("{x}", String.valueOf((int)targetPos.x))
                    .replace("{y}", String.valueOf((int)targetPos.y))
                    .replace("{z}", String.valueOf((int)targetPos.z));
                context.sendMessage(Message.raw(successMsg).color(Color.GREEN));
                
                // Marquer comme utilisé et définir le cooldown
                deathManager.markDeathPointUsed(playerRef.getUuid());
                if (!bypass) {
                    deathManager.setCooldown(playerRef.getUuid(), config.getCooldownSeconds());
                }
                
                LOGGER.at(Level.INFO).log("Player " + playerRef.getUuid() + " returned to death point at " + 
                    (int)targetPos.x + "," + (int)targetPos.y + "," + (int)targetPos.z + " (distance: " + (int)distance + "m)");
                
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error during return teleport: " + e.getMessage(), e);
                context.sendMessage(Message.raw(msg.error).color(Color.RED));
            }
        });
    }
    
    @Nullable
    private Vector3d findSafePositionNearDeath(@Nonnull World world, @Nonnull DeathPointManager.DeathPoint deathPoint,
                                                int minDist, int maxDist, int maxAttempts) {
        double deathX = deathPoint.getX();
        double deathZ = deathPoint.getZ();
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDist + random.nextDouble() * (maxDist - minDist);
            
            int targetX = (int) (deathX + Math.cos(angle) * distance);
            int targetZ = (int) (deathZ + Math.sin(angle) * distance);
            
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
