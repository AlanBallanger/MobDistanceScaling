package com.mobdistancescaling.essence;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();
    
    private final EssenceManager essenceManager;
    private final ConfigManager configManager;

    public EssenceKillSystem(@Nonnull EssenceManager essenceManager, @Nonnull ConfigManager configManager) {
        super(KillFeedEvent.KillerMessage.class);
        this.essenceManager = essenceManager;
        this.configManager = configManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull KillFeedEvent.KillerMessage event) {
        
        LOGGER.at(Level.INFO).log("=== EssenceKillSystem.handle() called ===");
        
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        
        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
            if (playerRef == null) {
                LOGGER.at(Level.INFO).log("PlayerRef is null, not a player");
                return;
            }

            UUID playerUuid = playerRef.getUuid();
            LOGGER.at(Level.INFO).log("Player UUID: " + playerUuid + " killed entity");
            
            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, ref, configManager.getZoneConfig());
            
            if (zone == null) {
                LOGGER.at(Level.INFO).log("Zone is null for player " + playerUuid);
                return;
            }
            
            LOGGER.at(Level.INFO).log("Zone: " + zone.getName() + ", multiplier: " + zone.getEssenceMultiplier());
            
            int essenceGained = (int) Math.ceil(1 * zone.getEssenceMultiplier());
            essenceManager.addEssence(playerUuid, playerUuid.toString(), essenceGained);
            int totalEssence = essenceManager.getEssence(playerUuid);
            
            LOGGER.at(Level.INFO).log("Player " + playerUuid + " gained " + essenceGained + 
                " essence from mob kill in zone " + zone.getName() + " (total: " + totalEssence + ")");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error gaining essence from kill: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
