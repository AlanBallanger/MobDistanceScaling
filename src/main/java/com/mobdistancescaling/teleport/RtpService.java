package com.mobdistancescaling.teleport;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.zone.Zone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.logging.Level;

public class RtpService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_SEARCH_RADIUS = 10000;
    private static final int START_Y = 200;
    private static final int MIN_Y = 0;
    private final Random random = new Random();

    @Nullable
    public Vector3d findSafePosition(@Nonnull World world, @Nonnull ChunkGenerator generator, 
                                     @Nullable Zone targetZone, int maxAttempts) {
        return findSafePosition(world, generator, targetZone, maxAttempts, null, null);
    }

    @Nullable
    public Vector3d findSafePosition(@Nonnull World world, @Nonnull ChunkGenerator generator, 
                                     @Nullable Zone targetZone, int maxAttempts, Double targetX, Double targetZ) {
        int seed = (int) world.getWorldConfig().getSeed();
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x, z;
            
            if (targetX != null && targetZ != null) {
                double radiusVariation = 50.0;
                double angleVariation = random.nextDouble() * 2 * Math.PI;
                double distVariation = random.nextDouble() * radiusVariation;
                x = (int) (targetX + Math.cos(angleVariation) * distVariation);
                z = (int) (targetZ + Math.sin(angleVariation) * distVariation);
            } else {
                x = random.nextInt(MAX_SEARCH_RADIUS * 2) - MAX_SEARCH_RADIUS;
                z = random.nextInt(MAX_SEARCH_RADIUS * 2) - MAX_SEARCH_RADIUS;
            }
            
            ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
            Zone foundZone = result.getZoneResult().getZone();
            
            if (targetZone != null && foundZone.id() != targetZone.id()) {
                continue;
            }
            
            Double safeY = findSafeRtpY(world, x, z);
            if (safeY != null) {
                LOGGER.at(Level.INFO).log("Position trouvée après " + (attempt + 1) + " tentatives dans: " + foundZone.name());
                return new Vector3d(x + 0.5, safeY, z + 0.5);
            }
        }
        
        LOGGER.at(Level.WARNING).log("Aucune position sûre trouvée après " + maxAttempts + " tentatives");
        return null;
    }

    @Nullable
    private Double findSafeRtpY(@Nonnull World world, int x, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            return null;
        }
        return findSafeRtpYFromChunk(chunk, x, z);
    }

    @Nullable
    private Double findSafeRtpYFromChunk(@Nonnull WorldChunk chunk, int blockX, int blockZ) {
        for (int checkY = START_Y; checkY >= MIN_Y; checkY--) {
            try {
                // Vérifier s'il y a du fluide à cette hauteur
                if (hasFluid(chunk, blockX, checkY, blockZ)) {
                    // Si fluide détecté, passer à la coordonnée suivante
                    return null;
                }
                
                if (isSolidBlock(chunk, blockX, checkY, blockZ)) {
                    int spawnY = checkY + 1;
                    
                    // Vérifier qu'il n'y a pas de fluide au niveau du spawn
                    if (hasFluid(chunk, blockX, spawnY, blockZ) || 
                        hasFluid(chunk, blockX, spawnY + 1, blockZ)) {
                        return null;
                    }
                    
                    // Vérifier qu'il y a 2 blocs d'espace au-dessus
                    if (isSolidBlock(chunk, blockX, spawnY + 1, blockZ)) {
                        continue;
                    }
                    
                    return (double) spawnY;
                }
            } catch (Exception e) {
                // Si erreur, continuer la recherche
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
