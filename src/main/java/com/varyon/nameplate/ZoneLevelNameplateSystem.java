package com.varyon.nameplate;

import com.frotty27.nameplatebuilder.api.NameplateData;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MobFragmentsConfig;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tick system that pushes the Varyon monster tier (1–10) to NameplateBuilder.
 *
 * Resolution order:
 *   1. Look up the NPC role name in mob_special_rates.toml → tier 1–10
 *   2. If not found (unknown mob), fall back to the zone ID where the NPC stands
 *
 * Segment : "monster_level"
 *   Variant 0 (default) : "Nv.5"
 *   Variant 1            : "5"
 */
public class ZoneLevelNameplateSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, NPCEntity>     npcType;
    private final ComponentType<EntityStore, NameplateData> nameplateDataType;
    private final MobFragmentsConfig                        mobFragmentsConfig;
    private final ConfigManager                             configManager;

    /** Role names logged once to avoid flooding the console. */
    private final Set<String> loggedUnknown = ConcurrentHashMap.newKeySet();

    public ZoneLevelNameplateSystem(
            @Nonnull ComponentType<EntityStore, NameplateData> nameplateDataType,
            @Nonnull MobFragmentsConfig mobFragmentsConfig,
            @Nonnull ConfigManager configManager) {
        this.npcType           = NPCEntity.getComponentType();
        this.nameplateDataType = nameplateDataType;
        this.mobFragmentsConfig = mobFragmentsConfig;
        this.configManager     = configManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return npcType;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            NPCEntity npc = chunk.getComponent(index, npcType);
            if (npc == null) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            // --- 1. Try tier from mob config ---
            int tier = -1;
            String roleName = npc.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                tier = mobFragmentsConfig.getFragments(roleName);
                if (tier < 0 && loggedUnknown.add(roleName)) {
                    LOGGER.at(Level.INFO).log("[MonsterLevel] Mob not in config: \"" + roleName + "\" → fallback to zone");
                }
            }

            // --- 2. Fallback: zone ID ---
            if (tier < 0) {
                DifficultyZone zone = ZoneCalculator.getCurrentZone(store, ref, configManager.getZoneConfig());
                tier = zone != null ? zone.getZoneId() : 1;
            }

            // --- 3. Push to NameplateData ---
            NameplateData existing = store.getComponent(ref, nameplateDataType);
            if (existing == null) {
                NameplateData data = new NameplateData();
                applyTier(data, tier);
                commandBuffer.putComponent(ref, nameplateDataType, data);
            } else {
                applyTier(existing, tier);
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("ZoneLevelNameplateSystem tick error: " + e.getMessage());
        }
    }

    private void applyTier(@Nonnull NameplateData data, int tier) {
        data.setText("monster_level",   "Nv." + tier);
        data.setText("monster_level.1", String.valueOf(tier));
    }
}
