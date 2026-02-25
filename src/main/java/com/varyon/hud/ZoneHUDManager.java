package com.varyon.hud;

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
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ZoneConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.essence.EssenceManager;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.util.ZoneCalculator;

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
    private static final int PAGE_SWITCH_TICKS = 5;
    private static final String MULTIPLE_HUD_PLUGIN_ID = "Buuz135:MultipleHUD";
    private static final String MULTIPLE_CUSTOM_UI_HUD_CLASS = "com.buuz135.mhud.MultipleCustomUIHud";
    private static final int MULTIHUD_MAX_ATTEMPTS = 300;
    private static final long MULTIHUD_RETRY_DELAY_MS = 100;

    private final Map<UUID, ZoneHUD> playerHuds = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerCache = new ConcurrentHashMap<>();
    private final ZoneConfig zoneConfig;
    private final MessagesConfig messagesConfig;
    private final ZonePermissionsConfig zonePermsConfig;
    private ScheduledFuture<?> updateTask;
    private int tickCounter = 0;

    public ZoneHUDManager(@Nonnull ZoneConfig zoneConfig, @Nonnull MessagesConfig messagesConfig,
                          @Nonnull ZonePermissionsConfig zonePermsConfig) {
        this.zoneConfig = zoneConfig;
        this.messagesConfig = messagesConfig;
        this.zonePermsConfig = zonePermsConfig;
        LOGGER.at(Level.INFO).log("ZoneHUDManager initialized");
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tickCounter++;
                boolean switchPage = (tickCounter % PAGE_SWITCH_TICKS == 0);
                updateAllHuds(switchPage);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error updating HUDs: " + e.getMessage());
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void updateAllHuds(boolean switchPage) {
        SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
        boolean safeZoneAvailable = szm != null;
        String quadrantName = "";
        long timeRemaining = 0;

        if (safeZoneAvailable) {
            quadrantName = szm.getCurrentQuadrant() != null ? szm.getCurrentQuadrant().name() : "?";
            timeRemaining = szm.getTimeUntilRotation();
        }

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

                boolean inSafe = safeZoneAvailable && szm.isInSafeZone(x, z);

                boolean lootActive = false;
                int maxEssenceCap = 1000;
                Player player = playerCache.get(playerId);
                if (player != null) {
                    if (zone != null) lootActive = zonePermsConfig.canAccessZone(player, zone.getZoneId());
                    EssenceManager em = VaryonPlugin.getStaticEssenceManager();
                    double current = em != null ? em.getEssence(playerId) : 0.0;
                    maxEssenceCap = zonePermsConfig.getEffectiveCap(player, current);
                }

                if (switchPage) hud.nextPage();
                hud.updateZoneInfo(zone, distance, inSafe, quadrantName, timeRemaining, switchPage, lootActive, maxEssenceCap);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error updating HUD for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
    }

    public boolean isAvailable() {
        return true;
    }

    public void registerPlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        String worldName = player.getWorld().getName();
        if (!zoneConfig.isWorldEnabled(worldName)) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        ZoneHUD hud = playerHuds.get(playerId);

        if (hud == null) {
            hud = new ZoneHUD(playerRef, zoneConfig, messagesConfig);
            playerHuds.put(playerId, hud);
            playerCache.put(playerId, player);
            tryRegisterWithMultipleHud(player, playerRef, hud, 0);
        }
    }

    private void tryRegisterWithMultipleHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull ZoneHUD hud, int attempt) {
        PluginBase pluginBase = PluginManager.get().getPlugin(PluginIdentifier.fromString(MULTIPLE_HUD_PLUGIN_ID));
        boolean multipleHudEnabled = pluginBase != null && pluginBase.isEnabled();

        if (!multipleHudEnabled) {
            try {
                player.getHudManager().setCustomHud(playerRef, hud);
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Failed to register HUD: " + e.getMessage());
            }
            return;
        }

        try {
            CustomUIHud currentHud = player.getHudManager().getCustomHud();
            if (currentHud != null && MULTIPLE_CUSTOM_UI_HUD_CLASS.equals(currentHud.getClass().getName())) {
                currentHud.getClass()
                    .getMethod("add", String.class, CustomUIHud.class)
                    .invoke(currentHud, "Varyon_Zone", hud);
                return;
            }

            if (currentHud == null && attempt >= 20) {
                MultipleHUD.getInstance().setCustomHud(player, playerRef, "Varyon_Zone", hud);
                return;
            }

            if (attempt >= MULTIHUD_MAX_ATTEMPTS) {
                LOGGER.at(Level.WARNING).log("MultipleCustomUIHud not ready after retries for player " + playerRef.getUuid());
                return;
            }

            int nextAttempt = attempt + 1;
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
        playerCache.remove(playerId);
    }

    public void broadcastBalanceUpdate() {
        for (ZoneHUD hud : playerHuds.values()) {
            try {
                hud.updateGlobalBalance();
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to update global balance: " + e.getMessage());
            }
        }
    }
}
