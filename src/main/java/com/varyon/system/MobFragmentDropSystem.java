package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.MobFragmentsConfig;
import com.varyon.config.ZoneLootConfig;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class MobFragmentDropSystem extends DeathSystems.OnDeathSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final MobFragmentsConfig mobConfig;
    private final ZoneLootConfig zoneConfig;

    public MobFragmentDropSystem(@Nonnull MobFragmentsConfig mobConfig, @Nonnull ZoneLootConfig zoneConfig) {
        this.mobConfig = mobConfig;
        this.zoneConfig = zoneConfig;
    }

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
            LOGGER.at(Level.WARNING).log("MobFragmentDropSystem error: " + e.getMessage());
        }
    }

    private int resolveZoneId(int mobLevel) {
        return Math.max(1, (int) Math.ceil(mobLevel / 10.0));
    }
}
