package com.mobdistancescaling.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;
import com.mobdistancescaling.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class RtpvCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService;
    private final Random random = new Random();
    private final RequiredArg<Integer> zoneArg;

    public RtpvCommand() {
        super("rtpv", "Random teleport to a mod zone");
        this.rtpService = new RtpService();
        this.zoneArg = this.withRequiredArg("zone", "Zone number (1-6)", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        int zoneNumber = context.get(zoneArg);
        
        ZoneConfig config = MobDistanceScalingPlugin.getStaticConfigManager().getZoneConfig();
        List<DifficultyZone> zones = config.getZones();
        
        if (zoneNumber < 1 || zoneNumber > zones.size()) {
            context.sendMessage(Message.raw("Invalid zone number. Valid zones: 1-" + zones.size()).color(Color.RED));
            return;
        }
        
        DifficultyZone targetZone = zones.get(zoneNumber - 1);
        
        double minDist = targetZone.getRadiusStart();
        double maxDist;
        if (zoneNumber < zones.size()) {
            maxDist = zones.get(zoneNumber).getRadiusStart();
        } else {
            maxDist = minDist + 5000;
        }
        
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw("World generation not supported in this world").color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        
        context.sendMessage(Message.raw("Teleporting to " + targetZone.getName() + "...").color(Color.GREEN));
        
        world.execute(() -> {
            try {
                double targetDistance = minDist + random.nextDouble() * (maxDist - minDist);
                double angle = random.nextDouble() * 2 * Math.PI;
                
                double targetX = Math.cos(angle) * targetDistance;
                double targetZ = Math.sin(angle) * targetDistance;
                
                Vector3d safePosition = rtpService.findSafePosition(world, generator, null, 50, targetX, targetZ);
                
                if (safePosition != null) {
                    teleportPlayer(store, ref, world, safePosition);
                    context.sendMessage(Message.raw("Teleported to " + targetZone.getName() + 
                        " at " + (int)safePosition.x + ", " + (int)safePosition.y + ", " + (int)safePosition.z).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw("Could not find safe location in " + targetZone.getName() + " after 50 attempts").color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Error during RTP: " + e.getMessage(), e);
                context.sendMessage(Message.raw("Teleportation failed").color(Color.RED));
            }
        });
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
