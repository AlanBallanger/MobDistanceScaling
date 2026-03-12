package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.varyon.VaryonPlugin;
import com.varyon.config.RtpsConfig;
import com.varyon.teleport.RtpService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.logging.Level;

public class RtpsCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final RtpService rtpService = new RtpService();
    private final RequiredArg<String> sideArg;

    public RtpsCommand() {
        super("rtps", "Random teleport sur un côté de la carte (est, ouest, nord, sud)");
        this.requirePermission("varyon.rtps");
        this.sideArg = this.withRequiredArg("côté", "est, ouest, nord ou sud", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String sideRaw = context.get(sideArg).trim().toLowerCase();
        Side side;
        switch (sideRaw) {
            case "est" -> side = Side.EST;
            case "ouest" -> side = Side.OUEST;
            case "nord" -> side = Side.NORD;
            case "sud" -> side = Side.SUD;
            default -> {
                context.sendMessage(Message.raw("Côté invalide. Utilisez : est, ouest, nord ou sud").color(Color.RED));
                return;
            }
        }

        RtpsConfig config = VaryonPlugin.getStaticConfigManager().getRtpsConfig();
        int minBlocks = config.getMinBlocks();
        int maxBlocks = config.getMaxBlocks();

        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (!(worldGen instanceof ChunkGenerator)) {
            context.sendMessage(Message.raw("World generation not supported in this world").color(Color.RED));
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) worldGen;
        context.sendMessage(Message.raw("Téléportation vers le " + sideRaw + "...").color(Color.GREEN));

        world.execute(() -> {
            try {
                int minX, maxX, minZ, maxZ;
                switch (side) {
                    case OUEST -> {
                        minX = -maxBlocks;
                        maxX = -minBlocks;
                        minZ = -maxBlocks;
                        maxZ = maxBlocks;
                    }
                    case EST -> {
                        minX = minBlocks;
                        maxX = maxBlocks;
                        minZ = -maxBlocks;
                        maxZ = maxBlocks;
                    }
                    case NORD -> {
                        minX = -maxBlocks;
                        maxX = maxBlocks;
                        minZ = minBlocks;
                        maxZ = maxBlocks;
                    }
                    case SUD -> {
                        minX = -maxBlocks;
                        maxX = maxBlocks;
                        minZ = -maxBlocks;
                        maxZ = -minBlocks;
                    }
                    default -> {
                        context.sendMessage(Message.raw("Côté invalide").color(Color.RED));
                        return;
                    }
                }

                Vector3d safePosition = rtpService.findSafePositionInRect(world, generator, minX, maxX, minZ, maxZ, 50);

                if (safePosition != null) {
                    Teleport teleport = Teleport.createForPlayer(world, safePosition, new Vector3f(0, 0, 0));
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                    context.sendMessage(Message.raw("Téléporté vers le " + sideRaw + " en " +
                        (int) safePosition.x + ", " + (int) safePosition.y + ", " + (int) safePosition.z).color(Color.GREEN));
                } else {
                    context.sendMessage(Message.raw("Impossible de trouver un emplacement sûr vers le " + sideRaw).color(Color.RED));
                }
            } catch (Exception e) {
                LOGGER.at(Level.SEVERE).log("Erreur lors de la téléportation RTPS: " + e.getMessage(), e);
                context.sendMessage(Message.raw("Échec de la téléportation").color(Color.RED));
            }
        });
    }

    private enum Side { EST, OUEST, NORD, SUD }
}
