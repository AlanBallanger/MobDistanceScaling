package com.varyon.essence;

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
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class EssenceKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final AtomicBoolean API_LOGGED = new AtomicBoolean(false);

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    private final EssenceManager essenceManager;
    private final ConfigManager configManager;
    private final EssenceRewardsConfig rewardsConfig;

    public EssenceKillSystem(@Nonnull EssenceManager essenceManager, @Nonnull ConfigManager configManager,
                             @Nonnull EssenceRewardsConfig rewardsConfig) {
        super(KillFeedEvent.KillerMessage.class);
        this.essenceManager = essenceManager;
        this.configManager = configManager;
        this.rewardsConfig = rewardsConfig;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                      @Nonnull KillFeedEvent.KillerMessage event) {

        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
            if (playerRef == null) {
                return;
            }

            UUID playerUuid = playerRef.getUuid();

            if (!API_LOGGED.getAndSet(true)) {
                logEventApi(event);
            }

            String mobId = resolveMobId(event, commandBuffer);

            double baseReward = rewardsConfig.getMobReward(mobId);

            LOGGER.at(Level.INFO).log("Kill: mob=" + mobId + " base=" + baseReward);

            if (baseReward <= 0) {
                return;
            }

            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, archetypeChunk.getReferenceTo(index), configManager.getZoneConfig());
            double zoneMultiplier = zone != null ? zone.getEssenceMultiplier() : 1.0;

            int essenceGained = (int) Math.ceil(baseReward * zoneMultiplier);
            if (essenceGained <= 0) {
                return;
            }

            essenceManager.addEssence(playerUuid, playerUuid.toString(), essenceGained);

            LOGGER.at(Level.INFO).log("Kill essence: +" + essenceGained +
                " (mob=" + mobId + " base=" + baseReward + " zone=" + zoneMultiplier + ")");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in EssenceKillSystem: " + e.getMessage());
        }
    }

    @Nonnull
    private String resolveMobId(@Nonnull KillFeedEvent.KillerMessage event,
                                @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            Method[] methods = event.getClass().getMethods();
            for (Method m : methods) {
                String name = m.getName().toLowerCase();
                if ((name.contains("victim") || name.contains("target") || name.contains("killed"))
                    && m.getParameterCount() == 0) {
                    Object result = m.invoke(event);
                    if (result instanceof Ref) {
                        @SuppressWarnings("unchecked")
                        Ref<EntityStore> victimRef = (Ref<EntityStore>) result;
                        if (victimRef.isValid()) {
                            return resolveEntityName(victimRef, commandBuffer);
                        }
                    }
                    if (result instanceof String s) {
                        return s.toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.INFO).log("Could not resolve mob id via reflection: " + e.getMessage());
        }
        return "unknown";
    }

    @Nonnull
    private String resolveEntityName(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            Object npc = commandBuffer.getComponent(ref, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
            if (npc != null) {
                for (Method m : npc.getClass().getMethods()) {
                    String name = m.getName().toLowerCase();
                    if ((name.equals("getname") || name.equals("gettype") || name.equals("getid")
                        || name.equals("gettypename") || name.equals("getentitytype"))
                        && m.getParameterCount() == 0 && m.getReturnType() == String.class) {
                        String result = (String) m.invoke(npc);
                        if (result != null && !result.isEmpty()) {
                            LOGGER.at(Level.INFO).log("Resolved mob name via NPCEntity." + m.getName() + "(): " + result);
                            return result.toLowerCase();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.INFO).log("Could not resolve entity name: " + e.getMessage());
        }
        return "unknown";
    }

    private void logEventApi(@Nonnull KillFeedEvent.KillerMessage event) {
        StringBuilder sb = new StringBuilder("[EssenceKillSystem] KillerMessage API discovery:\n");
        for (Method m : event.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            sb.append("  ").append(m.getReturnType().getSimpleName()).append(" ").append(m.getName()).append("(");
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")\n");
        }
        LOGGER.at(Level.INFO).log(sb.toString());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
