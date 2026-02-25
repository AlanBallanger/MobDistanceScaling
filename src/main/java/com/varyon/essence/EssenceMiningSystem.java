package com.varyon.essence;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceMiningSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    private final EssenceManager        essenceManager;
    private final ConfigManager         configManager;
    private final EssenceRewardsConfig  rewardsConfig;
    private final ZonePermissionsConfig zonePermsConfig;

    public EssenceMiningSystem(@Nonnull EssenceManager essenceManager, @Nonnull ConfigManager configManager,
                               @Nonnull EssenceRewardsConfig rewardsConfig,
                               @Nonnull ZonePermissionsConfig zonePermsConfig) {
        super(BreakBlockEvent.class);
        this.essenceManager  = essenceManager;
        this.configManager   = configManager;
        this.rewardsConfig   = rewardsConfig;
        this.zonePermsConfig = zonePermsConfig;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                      @Nonnull BreakBlockEvent event) {

        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
            if (playerRef == null) {
                return;
            }

            String blockId = event.getBlockType().getId().toLowerCase();

            double baseReward = rewardsConfig.getOreReward(blockId);
            if (baseReward <= 0) {
                return;
            }

            UUID playerUuid = playerRef.getUuid();

            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, archetypeChunk.getReferenceTo(index), configManager.getZoneConfig());
            double zoneMultiplier = zone != null ? zone.getEssenceMultiplier() : 1.0;
            double lootMultiplier = zone != null ? zone.getLootMultiplier() : 1.0;

            double essenceGained = baseReward * zoneMultiplier * lootMultiplier;
            if (essenceGained <= 0) return;

            Ref<EntityStore> minerRef = archetypeChunk.getReferenceTo(index);
            Player player = null;
            try { player = (Player) store.getComponent(minerRef, Player.getComponentType()); } catch (Exception ignored) {}
            if (player != null) {
                double current = essenceManager.getEssence(playerUuid);
                int cap = zonePermsConfig.getEffectiveCap(player, current);
                essenceManager.addEssenceCapped(playerUuid, playerUuid.toString(), essenceGained, cap);
            } else {
                essenceManager.addEssence(playerUuid, playerUuid.toString(), essenceGained);
            }

            LOGGER.at(Level.INFO).log("Mine: block=" + blockId + " +" + String.format("%.2f", essenceGained) + " essence (base=" + baseReward + " zone=" + zoneMultiplier + " loot=" + String.format("%.2f", lootMultiplier) + ")");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in EssenceMiningSystem: " + e.getMessage());
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
