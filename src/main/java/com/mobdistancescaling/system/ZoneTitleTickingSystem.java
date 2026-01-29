package com.mobdistancescaling.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;
import com.mobdistancescaling.util.ZoneCalculator;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneTitleTickingSystem extends EntityTickingSystem<EntityStore> {
    private final ConfigManager configManager;
    private final Map<UUID, Integer> playerLastZoneId = new ConcurrentHashMap<>();

    public ZoneTitleTickingSystem(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void tick(float deltaTime, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        ZoneConfig config = configManager.getZoneConfig();

        // Skip if notifications are disabled
        if (!config.isZoneEnterNotification()) {
            return;
        }

        Ref ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) {
            return;
        }

        // Check if scaling is enabled for this world
        String worldName = player.getWorld().getName();
        if (!config.isWorldEnabled(worldName)) {
            return;
        }

        // Get player position
        double x = playerRef.getTransform().getPosition().getX();
        double z = playerRef.getTransform().getPosition().getZ();

        // Get current zone
        DifficultyZone currentZone = ZoneCalculator.getZoneAtPosition(x, z, config);
        int currentZoneId = currentZone != null ? currentZone.getZoneId() : 0;

        // Get previous zone
        UUID playerId = playerRef.getUuid();
        Integer previousZoneId = playerLastZoneId.get(playerId);

        // Check if zone changed
        if (previousZoneId == null || previousZoneId != currentZoneId) {
            playerLastZoneId.put(playerId, currentZoneId);

            // Only show notification if we have a zone (and it's not the first tick)
            if (currentZone != null && previousZoneId != null) {
                showZoneNotification(playerRef, currentZone, config);
            } else if (currentZone != null && previousZoneId == null) {
                // First zone entry - store but don't notify
                playerLastZoneId.put(playerId, currentZoneId);
            }
        }
    }

    private void showZoneNotification(PlayerRef playerRef, DifficultyZone zone, ZoneConfig config) {
        // Zone name as main title
        Color zoneColor = zone.getParsedColor();
        Message titleMessage = Message.raw(zone.getName()).color(zoneColor);

        // Top text (small text above)
        String topText = config.getZoneEnterTopText();
        Message topMessage = Message.raw(topText);

        // Duration settings
        float duration = config.getNotificationDuration();
        float fadeIn = 0.3f;
        float fadeOut = 0.5f;

        EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, topMessage, false, null, duration, fadeIn, fadeOut);
    }

    public void removePlayer(UUID playerId) {
        playerLastZoneId.remove(playerId);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
