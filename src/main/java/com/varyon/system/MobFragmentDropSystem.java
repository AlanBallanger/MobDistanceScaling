package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.MobFragmentsConfig;
import com.varyon.config.ZoneLootConfig;
import com.varyon.config.ZonePermissionsConfig;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MobFragmentDropSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Cache : System.identityHashCode(victimRef) → whether the killer has zone permission.
     * Populated by KillerPermissionTracker (KillFeedEvent — no entity spawning allowed).
     * Read by DropOnDeath (OnDeathSystem — commandBuffer.addEntities is safe here).
     * Both systems fire within the same store tick for the same mob death, so the ref
     * object identity is stable across the two calls.
     */
    private final ConcurrentHashMap<Integer, Boolean> killerHasPermission = new ConcurrentHashMap<>();

    private final MobFragmentsConfig mobConfig;
    private final ZoneLootConfig zoneConfig;
    private final ZonePermissionsConfig zonePermsConfig;
    private final ConfigManager configManager;

    public MobFragmentDropSystem(@Nonnull MobFragmentsConfig mobConfig,
                                 @Nonnull ZoneLootConfig zoneConfig,
                                 @Nonnull ZonePermissionsConfig zonePermsConfig,
                                 @Nonnull ConfigManager configManager) {
        this.mobConfig = mobConfig;
        this.zoneConfig = zoneConfig;
        this.zonePermsConfig = zonePermsConfig;
        this.configManager = configManager;
    }

    // -------------------------------------------------------------------------
    // Step 1 — fires on KillFeedEvent (player context, no store writes allowed)
    // Just records whether the killer has zone permission. No entity spawning.
    // -------------------------------------------------------------------------
    public final class KillerPermissionTracker extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {

        @Nonnull
        private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();

        public KillerPermissionTracker() {
            super(KillFeedEvent.KillerMessage.class);
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return playerRefType;
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull KillFeedEvent.KillerMessage event) {
            try {
                Ref<EntityStore> victimRef = event.getTargetRef();
                if (victimRef == null || !victimRef.isValid()) return;

                int victimId = System.identityHashCode(victimRef);

                NPCEntity npc = (NPCEntity) store.getComponent(victimRef, NPCEntity.getComponentType());
                if (npc == null) return;

                MobScalingComponent scaling = (MobScalingComponent) store.getComponent(victimRef, MobScalingComponent.getComponentType());
                int zoneId = scaling != null ? resolveZoneId(scaling.getMobLevel()) : 1;

                Ref<EntityStore> killerRef = archetypeChunk.getReferenceTo(index);
                Player killerPlayer = (Player) store.getComponent(killerRef, Player.getComponentType());

                boolean hasAccess = (killerPlayer == null) || zonePermsConfig.canAccessZone(killerPlayer, zoneId);
                killerHasPermission.put(victimId, hasAccess);

            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("KillerPermissionTracker error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 — fires on mob death (OnDeathSystem, commandBuffer writes are safe)
    // Reads permission cache and spawns items if allowed.
    // -------------------------------------------------------------------------
    public final class DropOnDeath extends DeathSystems.OnDeathSystem {

        @Override
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void onComponentAdded(
                @Nonnull Ref ref,
                @Nonnull DeathComponent death,
                @Nonnull Store store,
                @Nonnull CommandBuffer commandBuffer) {
            try {
                int victimId = System.identityHashCode(ref);

                String worldName = ((EntityStore) store.getExternalData()).getWorld().getName();
                if (!configManager.getZoneConfig().isWorldEnabled(worldName)) {
                    killerHasPermission.remove(victimId);
                    return;
                }

                Boolean hasAccess = killerHasPermission.remove(victimId);
                if (Boolean.FALSE.equals(hasAccess)) {
                    return;
                }

                NPCEntity npc = (NPCEntity) store.getComponent(ref, NPCEntity.getComponentType());
                if (npc == null) return;

                String roleName = npc.getRoleName();
                if (roleName == null || roleName.isBlank()) return;

                int fragments = mobConfig.getFragments(roleName.toLowerCase(Locale.ROOT));
                if (fragments <= 0) return;

                MobScalingComponent scaling = (MobScalingComponent) store.getComponent(ref, MobScalingComponent.getComponentType());
                int zoneId = scaling != null ? resolveZoneId(scaling.getMobLevel()) : 1;

                String itemId = zoneConfig.getItemForZone(zoneId);
                if (itemId == null || itemId.isBlank()) return;

                TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) return;

                Vector3d pos = transform.getPosition().clone().add(0.0, 1.0, 0.0);
                HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
                Vector3f rot = headRotation != null ? headRotation.getRotation().clone() : new Vector3f(0f, 0f, 0f);

                Holder[] itemEntities = ItemComponent.generateItemDrops(store, List.of(new ItemStack(itemId, fragments)), pos, rot);
                commandBuffer.addEntities(itemEntities, AddReason.SPAWN);

            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("DropOnDeath error: " + e.getMessage());
            }
        }
    }

    private static int resolveZoneId(int mobLevel) {
        return Math.max(1, (int) Math.ceil(mobLevel / 10.0));
    }

    public KillerPermissionTracker createTracker() {
        return new KillerPermissionTracker();
    }

    public DropOnDeath createDropSystem() {
        return new DropOnDeath();
    }
}
