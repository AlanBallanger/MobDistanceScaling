package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.varyon.VaryonPlugin;
import com.varyon.config.ZoneConfig;
import com.varyon.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class RtpzCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();
    private final DefaultArg<String> zoneArg;

    public RtpzCommand() {
        super("rtpz", "Random teleport to a vanilla zone");
        this.requirePermission("varyon.rtp");
        this.rtpService = new RtpService();
        this.zoneArg = this.withDefaultArg(
            "zone",
            "Zone to teleport to (zone1, zone2, zone3, zone4)",
            ArgTypes.STRING,
            null,
            "random"
        );
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String targetZonePrefix = context.get(zoneArg);
        teleportToRandomZone(context, store, ref, playerRef, world, targetZonePrefix);
    }

    void teleportToRandomZone(CommandContext context, Store<EntityStore> store, 
                              Ref<EntityStore> ref, PlayerRef playerRef, World world, String targetZonePrefix) {
        ZoneConfig config = VaryonPlugin.getStaticConfigManager().getZoneConfig();
        
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw(config.getRtpvMessageWorldNotSupported()).color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        Zone[] zones = generator.getZonePatternProvider().getZones();

        Zone targetZone = null;
        Integer extractedZoneNumber = null;
        
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
            
            // Extraire le numéro de zone pour la vérification de permission
            String zoneName = targetZone.name().toLowerCase();
            if (zoneName.startsWith("zone") && zoneName.length() > 4) {
                try {
                    extractedZoneNumber = Integer.parseInt(zoneName.substring(4, 5));
                } catch (NumberFormatException e) {
                    // Pas un numéro, on ignore
                }
            }
            
            String zoneDisplay = extractZoneNumber(targetZone.name());
            LOGGER.at(Level.INFO).log("Selected zone: " + targetZone.name() + " (" + zoneDisplay + ") from " + matchingZones.size() + " matching zones for prefix: " + prefix);
        }
        
        // Vérifier la permission pour cette zone vanilla
        if (extractedZoneNumber != null) {
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                (com.hypixel.hytale.server.core.entity.entities.Player) store.getComponent(ref, 
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            
            if (player != null && !player.hasPermission("varyon.rtp")) {
                // Pas de permission globale, vérifier les permissions granulaires
                boolean hasAccess = false;
                for (int i = extractedZoneNumber; i <= 10; i++) {
                    if (player.hasPermission("varyon.rtp." + i)) {
                        hasAccess = true;
                        break;
                    }
                }
                
                if (!hasAccess) {
                    context.sendMessage(Message.raw("Vous n'avez pas la permission pour cette zone. Permission requise: varyon.rtp." + extractedZoneNumber).color(Color.RED));
                    return;
                }
            }
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
