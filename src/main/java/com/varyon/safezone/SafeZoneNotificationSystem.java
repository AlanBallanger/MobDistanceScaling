package com.varyon.safezone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SafeZoneNotificationSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static SafeZoneManager safeZoneManager;
    private final Map<UUID, Boolean> playerInSafeZone = new ConcurrentHashMap<>();
    private final SafeZoneConfig config;
    private final ZoneConfig zoneConfig;

    public SafeZoneNotificationSystem(@Nonnull SafeZoneConfig config, @Nonnull ZoneConfig zoneConfig) {
        this.config = config;
        this.zoneConfig = zoneConfig;
    }

    public static void setSafeZoneManager(@Nonnull SafeZoneManager manager) {
        safeZoneManager = manager;
    }

    @Override
    public void tick(float deltaTime, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        
        if (safeZoneManager == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) {
            return;
        }

        String worldName = ((EntityStore)store.getExternalData()).getWorld().getName();
        if (!zoneConfig.isWorldEnabled(worldName)) {
            return;
        }

        double x = playerRef.getTransform().getPosition().getX();
        double z = playerRef.getTransform().getPosition().getZ();
        boolean isInSafeZone = safeZoneManager.isInSafeZone(x, z);
        UUID playerId = playerRef.getUuid();

        Boolean wasInSafeZone = playerInSafeZone.get(playerId);

        if (wasInSafeZone == null || wasInSafeZone != isInSafeZone) {
            playerInSafeZone.put(playerId, isInSafeZone);
            
            if (wasInSafeZone != null) {
                if (isInSafeZone) {
                    showSafeZoneEnterNotification(playerRef);
                    LOGGER.at(Level.FINE).log("Player {} entered safe zone", playerId);
                } else {
                    showPvpZoneEnterNotification(playerRef);
                    LOGGER.at(Level.FINE).log("Player {} left safe zone", playerId);
                }
            }
        }
    }

    private void showSafeZoneEnterNotification(PlayerRef playerRef) {
        Message titleMessage = Message.raw(config.getEnterSafeZoneTitle()).color(Color.GREEN);
        Message topMessage = Message.raw(config.getEnterSafeZoneSubtitle());
        
        float duration = 2.0f;
        float fadeIn = 0.3f;
        float fadeOut = 0.5f;
        
        EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, topMessage, false, null, duration, fadeIn, fadeOut);
    }

    private void showPvpZoneEnterNotification(PlayerRef playerRef) {
        Message titleMessage = Message.raw(config.getEnterPvpZoneTitle()).color(Color.RED);
        Message topMessage = Message.raw(config.getEnterPvpZoneSubtitle());
        
        float duration = 2.0f;
        float fadeIn = 0.3f;
        float fadeOut = 0.5f;
        
        EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, topMessage, false, null, duration, fadeIn, fadeOut);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    public void removePlayer(UUID playerId) {
        playerInSafeZone.remove(playerId);
    }

    public void clearAll() {
        playerInSafeZone.clear();
    }
}
