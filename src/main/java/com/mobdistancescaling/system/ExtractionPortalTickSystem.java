package com.mobdistancescaling.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.config.ExtractionConfig;
import com.mobdistancescaling.extraction.ExtractionPortalManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ExtractionPortalTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double PORTAL_RADIUS_XZ = 1.5;
    private static final double PORTAL_HEIGHT = 3.5;
    private static final long DENY_MESSAGE_COOLDOWN_MS = 3000;

    private final Map<UUID, Long> lastDenyMessage = new ConcurrentHashMap<>();

    @Override
    public void tick(float deltaTime, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();
        if (manager == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        double px = playerRef.getTransform().getPosition().getX();
        double py = playerRef.getTransform().getPosition().getY();
        double pz = playerRef.getTransform().getPosition().getZ();

        for (Map.Entry<UUID, ExtractionPortalManager.PortalData> entry : manager.getActivePortals().entrySet()) {
            ExtractionPortalManager.PortalData portal = entry.getValue();
            UUID ownerId = entry.getKey();

            double dx = px - (portal.x() + 0.5);
            double dz = pz - (portal.z() + 0.5);
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            double dy = py - portal.y();

            if (distXZ > PORTAL_RADIUS_XZ || dy < 0 || dy > PORTAL_HEIGHT) {
                continue;
            }

            if (!ownerId.equals(playerId)) {
                long now = System.currentTimeMillis();
                Long lastDeny = lastDenyMessage.get(playerId);
                if (lastDeny == null || now - lastDeny > DENY_MESSAGE_COOLDOWN_MS) {
                    lastDenyMessage.put(playerId, now);
                    ExtractionConfig config = manager.getConfig();
                    player.sendMessage(Message.raw(config.getMessageNotYourPortal()).color(Color.RED));
                }
                return;
            }

            World world = ((EntityStore) store.getExternalData()).getWorld();
            ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
            if (spawnProvider == null) {
                LOGGER.at(Level.WARNING).log("No spawn provider for world " + world.getName());
                return;
            }

            Transform spawnPoint = spawnProvider.getSpawnPoint(world, playerId);
            Vector3d spawnPos = spawnPoint.getPosition();

            Teleport teleport = Teleport.createForPlayer(world, spawnPos, new Vector3f(0, 0, 0));
            commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);

            ExtractionConfig config = manager.getConfig();
            player.sendMessage(Message.raw(config.getMessageTeleporting()).color(Color.GREEN));

            manager.consumePortal(ownerId);

            LOGGER.at(Level.INFO).log("Player " + playerId + " used extraction portal at " + portal.x() + "," + portal.y() + "," + portal.z());
            return;
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
