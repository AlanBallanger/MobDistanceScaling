package com.mobdistancescaling.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.config.ZoneConfig;
import com.mobdistancescaling.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class RtpvCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();
    private final OptionalArg<String> zoneArg;

    public RtpvCommand() {
        super("rtpv", "Random teleport to a vanilla zone. Usage: /rtpv [zone]");
        this.rtpService = new RtpService();
        this.zoneArg = this.withOptionalArg("zone", "Zone to teleport to (zone1, zone2, zone3, zone4)", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        ZoneConfig config = MobDistanceScalingPlugin.getStaticConfigManager().getZoneConfig();
        
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw(config.getRtpvMessageWorldNotSupported()).color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        Zone[] zones = generator.getZonePatternProvider().getZones();

        String targetZonePrefix = context.get(zoneArg);
        Zone targetZone = null;
        
        if (targetZonePrefix != null && !targetZonePrefix.isEmpty()) {
            String prefix = targetZonePrefix.toLowerCase();
            
            List<Zone> matchingZones = new ArrayList<>();
            for (Zone zone : zones) {
                String zoneName = zone.name().toLowerCase();
                if (zoneName.startsWith(prefix) && !zoneName.contains("ocean")) {
                    matchingZones.add(zone);
                }
            }
            
            if (matchingZones.isEmpty()) {
                for (Zone zone : zones) {
                    if (zone.name().toLowerCase().startsWith(prefix)) {
                        matchingZones.add(zone);
                    }
                }
            }
            
            if (matchingZones.isEmpty()) {
                StringBuilder availableZones = new StringBuilder(config.getRtpvMessageAvailableZones() + ": ");
                for (int i = 0; i < zones.length; i++) {
                    availableZones.append(zones[i].name());
                    if (i < zones.length - 1) availableZones.append(", ");
                }
                context.sendMessage(Message.raw(config.getRtpvMessageZoneNotFound()
                    .replace("{zone}", prefix) + " " + availableZones.toString()).color(Color.RED));
                return;
            }
            
            targetZone = matchingZones.get(random.nextInt(matchingZones.size()));
            
            String zoneDisplay = extractZoneNumber(targetZone.name());
            LOGGER.at(Level.INFO).log("Selected zone: " + targetZone.name() + " (" + zoneDisplay + ") from " + matchingZones.size() + " matching zones for prefix: " + prefix);
        }

        final Zone finalTargetZone = targetZone;
        context.sendMessage(Message.raw(config.getRtpvMessageTeleporting()).color(Color.GREEN));
        
        world.execute(() -> {
            try {
                Vector3d safePosition = rtpService.findSafePosition(world, generator, finalTargetZone, 50);
                
                if (safePosition != null) {
                    teleportPlayer(store, ref, world, safePosition);
                    String zoneName = finalTargetZone != null ? extractZoneNumber(finalTargetZone.name()) : config.getRtpvMessageRandomZone();
                    context.sendMessage(Message.raw(config.getRtpvMessageSuccess()
                        .replace("{zone}", zoneName)
                        .replace("{x}", String.valueOf((int)safePosition.x))
                        .replace("{y}", String.valueOf((int)safePosition.y))
                        .replace("{z}", String.valueOf((int)safePosition.z))).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw(config.getRtpvMessageNoSafeLocation()
                        .replace("{attempts}", "50")).color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur lors de la téléportation RTP: " + e.getMessage(), e);
                context.sendMessage(Message.raw(config.getRtpvMessageError()).color(Color.RED));
            }
        });
    }
    
    private String extractZoneNumber(String zoneName) {
        if (zoneName.toLowerCase().startsWith("zone")) {
            String num = zoneName.substring(4, 5);
            return "Zone " + num;
        }
        return zoneName;
    }

    private void teleportPlayer(Store<EntityStore> store, Ref<EntityStore> ref, World world, Vector3d position) {
        Teleport teleport = Teleport.createForPlayer(
            world,
            position,
            new Vector3f(0, 0, 0)
        );
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }
}
