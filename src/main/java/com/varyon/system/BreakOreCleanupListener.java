package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class BreakOreCleanupListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlacedOreTracker tracker;

    public BreakOreCleanupListener(@Nonnull PlacedOreTracker tracker) {
        super(BreakBlockEvent.class);
        this.tracker = tracker;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        try {
            Vector3i pos = event.getTargetBlock();
            if (pos == null) return;

            String world = resolveWorld(store);
            tracker.remove(world, pos);

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in BreakOreCleanupListener: " + e.getMessage());
        }
    }

    private String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {}
        return "world";
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
