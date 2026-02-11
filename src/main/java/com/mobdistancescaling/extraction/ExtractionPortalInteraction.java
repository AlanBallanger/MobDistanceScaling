package com.mobdistancescaling.extraction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.config.ExtractionConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.UUID;
import java.util.logging.Level;

public class ExtractionPortalInteraction extends SimpleBlockInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<ExtractionPortalInteraction> CODEC =
        BuilderCodec.builder(ExtractionPortalInteraction.class, ExtractionPortalInteraction::new, SimpleBlockInteraction.CODEC).build();

    @Override
    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUID playerUuid = uuidComponent.getUuid();
        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();
        if (manager == null) {
            LOGGER.at(Level.WARNING).log("ExtractionPortalManager not initialized");
            context.getState().state = InteractionState.Failed;
            return;
        }

        ExtractionConfig config = manager.getConfig();
        UUID portalOwner = manager.getPortalOwner(targetBlock.x, targetBlock.y, targetBlock.z);

        if (portalOwner == null) {
            LOGGER.at(Level.WARNING).log("No portal owner found at " + targetBlock.x + ", " + targetBlock.y + ", " + targetBlock.z);
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (!portalOwner.equals(playerUuid)) {
            LOGGER.at(Level.INFO).log("Player " + playerUuid + " tried to use portal owned by " + portalOwner);
            playerComponent.sendMessage(Message.raw(config.getMessageNotYourPortal()).color(Color.RED));
            context.getState().state = InteractionState.Failed;
            return;
        }

        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            LOGGER.at(Level.WARNING).log("No spawn provider found for world {0}", world.getName());
            playerComponent.sendMessage(Message.raw(config.getMessageError()).color(Color.RED));
            context.getState().state = InteractionState.Failed;
            return;
        }

        Transform spawnPoint = spawnProvider.getSpawnPoint(world, playerUuid);
        Vector3d spawnPos = spawnPoint.getPosition();

        LOGGER.at(Level.INFO).log("Teleporting player " + playerUuid + " to spawn at " + (int) spawnPos.x + ", " + (int) spawnPos.y + ", " + (int) spawnPos.z);

        Teleport teleport = Teleport.createForPlayer(
            world,
            spawnPos,
            new Vector3f(0, 0, 0)
        );
        commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);

        playerComponent.sendMessage(Message.raw(config.getMessageTeleporting()).color(Color.GREEN));

        manager.consumePortal(portalOwner);

        LOGGER.at(Level.INFO).log("Player " + playerUuid + " used extraction portal at " + targetBlock.x + ", " + targetBlock.y + ", " + targetBlock.z + " -> teleported to spawn");
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand, @Nonnull World world,
                                             @Nonnull Vector3i targetBlock) {
    }
}
