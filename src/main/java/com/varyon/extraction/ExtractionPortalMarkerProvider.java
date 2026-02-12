package com.varyon.extraction;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerTracker;
import com.hypixel.hytale.server.core.util.PositionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ExtractionPortalMarkerProvider implements WorldMapManager.MarkerProvider {
    
    @Override
    public void update(@Nonnull World world, @Nonnull MapMarkerTracker tracker, int chunkViewRadius, int playerChunkX, int playerChunkZ) {
        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();
        if (manager == null) {
            return;
        }

        UUID playerUuid = tracker.getPlayer().getPlayerRef().getUuid();
        
        ExtractionPortalManager.PortalData portalData = manager.getActivePortals().get(playerUuid);
        if (portalData == null) {
            return;
        }

        if (portalData.world() != world) {
            return;
        }

        Vector3d position = new Vector3d(portalData.x() + 0.5, portalData.y(), portalData.z() + 0.5);
        Transform transform = new Transform(position);

        tracker.trySendMarker(
            chunkViewRadius, 
            playerChunkX, 
            playerChunkZ, 
            position,
            0.0f,
            "extraction_portal_" + playerUuid,
            "Extraction Portal",
            transform,
            (id, name, tf) -> new MapMarker(id, name, "Portal.png", PositionUtil.toTransformPacket(tf), null)
        );
    }
}
