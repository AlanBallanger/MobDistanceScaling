package com.varyon.essence;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.VaryonPlugin;

import javax.annotation.Nonnull;

import java.awt.Color;

public final class GuildPointsVaryonExitHelper {

    private GuildPointsVaryonExitHelper() {}

    public static void applyOnLeavingVaryonWorld(@Nonnull PlayerRef playerRef, @Nonnull EssenceManager essenceManager) {
        double current = essenceManager.getEssence(playerRef.getUuid());
        if (current <= 0) {
            return;
        }
        int amount = essenceManager.getEssenceDisplay(playerRef.getUuid());
        essenceManager.setEssence(playerRef.getUuid(), playerRef.getUsername(), 0);
        try {
            if (amount > 0) {
                String unit = amount == 1 ? "essence" : "essences";
                String text = "Tu as quitté Varyon avec " + amount + " " + unit
                    + " dans les poches. Au revoir, et merci pour le cadeau à l'univers !";
                playerRef.sendMessage(Message.raw(text).color(Color.ORANGE));
            }
        } catch (Exception ignored) {
        }
        VaryonPlugin plugin = VaryonPlugin.getInstance();
        if (plugin != null && plugin.getHudManager() != null) {
            plugin.getHudManager().broadcastBalanceUpdate();
        }
    }
}
