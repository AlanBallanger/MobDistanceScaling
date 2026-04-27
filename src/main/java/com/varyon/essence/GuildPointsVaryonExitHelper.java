package com.varyon.essence;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.VaryonPlugin;

import javax.annotation.Nonnull;

import java.awt.Color;

public final class GuildPointsVaryonExitHelper {

    private GuildPointsVaryonExitHelper() {}

    public static void applyOnLeavingVaryonWorld(@Nonnull PlayerRef playerRef, @Nonnull EssenceManager essenceManager) {
        int amount = essenceManager.getEssenceDisplay(playerRef.getUuid());
        if (amount <= 0) {
            return;
        }
        essenceManager.addEssence(playerRef.getUuid(), playerRef.getUsername(), -amount);
        try {
            String unit = amount == 1 ? "essence" : "essences";
            String text = "Tu as quitté Varyon avec " + amount + " " + unit
                + " dans les poches. Au revoir, et merci pour le cadeau à l'univers !";
            playerRef.sendMessage(Message.raw(text).color(Color.ORANGE));
        } catch (Exception ignored) {
        }
        VaryonPlugin plugin = VaryonPlugin.getInstance();
        if (plugin != null && plugin.getHudManager() != null) {
            plugin.getHudManager().broadcastBalanceUpdate();
        }
    }
}
