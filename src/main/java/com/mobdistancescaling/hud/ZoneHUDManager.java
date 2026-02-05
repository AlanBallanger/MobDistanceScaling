package com.mobdistancescaling.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;
import com.mobdistancescaling.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ZoneHUDManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final String MULTIPLE_HUD_PLUGIN_ID = "Buuz135:MultipleHUD";
    private static final String MULTIPLE_CUSTOM_UI_HUD_CLASS = "com.buuz135.mhud.MultipleCustomUIHud";
    private static final int MULTIHUD_MAX_ATTEMPTS = 300;
    private static final long MULTIHUD_RETRY_DELAY_MS = 100;

    private final Map<UUID, ZoneHUD> playerHuds = new ConcurrentHashMap<>();
    private final ZoneConfig zoneConfig;
    private ScheduledFuture<?> updateTask;

    public ZoneHUDManager(@Nonnull ZoneConfig zoneConfig) {
        this.zoneConfig = zoneConfig;
        LOGGER.at(Level.INFO).log("ZoneHUDManager initialized with CustomUI HUD");
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                updateAllHuds();
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error updating HUDs: " + e.getMessage());
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        LOGGER.at(Level.INFO).log("HUD update task started (every " + UPDATE_INTERVAL_MS + "ms)");
    }

    private void updateAllHuds() {
        for (Map.Entry<UUID, ZoneHUD> entry : playerHuds.entrySet()) {
            UUID playerId = entry.getKey();
            ZoneHUD hud = entry.getValue();
            
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null || playerRef.getReference() == null) {
                continue;
            }
            
            try {
                Transform transform = playerRef.getTransform();
                double x = transform.getPosition().x;
                double z = transform.getPosition().z;
                double distance = ZoneCalculator.calculate2DDistance(x, z);
                DifficultyZone zone = ZoneCalculator.getZoneAtPosition(x, z, zoneConfig);
                
                hud.updateZoneInfo(zone, distance);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error updating HUD for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
            LOGGER.at(Level.INFO).log("HUD update task stopped");
        }
    }


    public boolean isAvailable() {
        return true;
    }

    public void registerPlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        ZoneHUD hud = playerHuds.get(playerId);

        if (hud == null) {
            LOGGER.at(Level.INFO).log("Creating new ZoneHUD for player " + playerId);
            hud = new ZoneHUD(playerRef, zoneConfig);
            playerHuds.put(playerId, hud);

            tryRegisterWithMultipleHud(player, playerRef, hud, 0);
        }
    }

    private void tryRegisterWithMultipleHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull ZoneHUD hud, int attempt) {
        PluginBase pluginBase = PluginManager.get().getPlugin(PluginIdentifier.fromString(MULTIPLE_HUD_PLUGIN_ID));
        boolean multipleHudEnabled = pluginBase != null && pluginBase.isEnabled();

        if (!multipleHudEnabled) {
            try {
                player.getHudManager().setCustomHud(playerRef, hud);
                LOGGER.at(Level.INFO).log("HUD registered with direct API for player " + playerRef.getUuid());
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Failed to register HUD with direct API: " + e.getMessage());
            }
            return;
        }

        try {
            CustomUIHud currentHud = player.getHudManager().getCustomHud();
            if (currentHud != null && MULTIPLE_CUSTOM_UI_HUD_CLASS.equals(currentHud.getClass().getName())) {
                currentHud.getClass()
                    .getMethod("add", String.class, CustomUIHud.class)
                    .invoke(currentHud, "MobDistanceScaling_Zone", hud);
                LOGGER.at(Level.INFO).log("Added HUD to existing MultipleCustomUIHud for player " + playerRef.getUuid());
                return;
            }

            if (currentHud == null && attempt >= 20) {
                LOGGER.at(Level.INFO).log("No custom HUD yet; creating MultipleCustomUIHud for player " + playerRef.getUuid());
                MultipleHUD.getInstance().setCustomHud(player, playerRef, "MobDistanceScaling_Zone", hud);
                LOGGER.at(Level.INFO).log("HUD registered with MultipleHUD for player " + playerRef.getUuid());
                return;
            }

            if (attempt >= MULTIHUD_MAX_ATTEMPTS) {
                String currentHudName = currentHud != null ? currentHud.getClass().getName() : "null";
                LOGGER.at(Level.WARNING).log(
                    "MultipleCustomUIHud not ready after retries for player " + playerRef.getUuid()
                        + " (currentHud=" + currentHudName + ")"
                );
                return;
            }

            int nextAttempt = attempt + 1;
            if (nextAttempt % 20 == 0) {
                String currentHudName = currentHud != null ? currentHud.getClass().getName() : "null";
                LOGGER.at(Level.INFO).log(
                    "Waiting for MultipleCustomUIHud... attempt " + nextAttempt + " (currentHud=" + currentHudName + ")"
                );
            }
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> tryRegisterWithMultipleHud(player, playerRef, hud, nextAttempt),
                MULTIHUD_RETRY_DELAY_MS,
                TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to register HUD with MultipleHUD: " + e.getMessage());
        }
    }

    public void removePlayer(@Nonnull UUID playerId) {
        playerHuds.remove(playerId);
    }
}
