package com.mobdistancescaling.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.essence.EssenceManager;

import javax.annotation.Nonnull;
import java.awt.Color;

public class EssenceCommand extends AbstractPlayerCommand {
    
    private final OptionalArg<Integer> amountArg;

    public EssenceCommand() {
        super("essence", "Give yourself essence. Usage: /essence [amount]");
        this.amountArg = this.withOptionalArg("amount", "Amount of essence to give (default: 10)", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        
        EssenceManager essenceManager = MobDistanceScalingPlugin.getStaticEssenceManager();
        if (essenceManager == null) {
            context.sendMessage(Message.raw("Essence system not initialized").color(Color.RED));
            return;
        }

        Integer amount = context.get(amountArg);
        if (amount == null) {
            amount = 10;
        }

        int currentEssence = essenceManager.getEssence(playerRef.getUuid());
        essenceManager.addEssence(playerRef.getUuid(), amount);
        int newEssence = essenceManager.getEssence(playerRef.getUuid());

        context.sendMessage(Message.raw("Added " + amount + " essence. Total: " + newEssence + "/1000").color(Color.GREEN));
    }
}
