package com.mobdistancescaling.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;
import com.mobdistancescaling.essence.EssenceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class ZoneHUD extends CustomUIHud {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    @Nonnull
    private final ZoneConfig zoneConfig;
    
    @Nullable
    private DifficultyZone currentZone;
    private double distanceFromSpawn;
    private int essence;
    private boolean built;

    public ZoneHUD(@Nonnull PlayerRef playerRef, @Nonnull ZoneConfig zoneConfig) {
        super(playerRef);
        this.zoneConfig = zoneConfig;
        this.distanceFromSpawn = 0.0;
        this.essence = 0;
        this.built = false;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/ZoneHUD.ui");
            
            String zoneName = currentZone != null ? "Zone " + currentZone.getName() : "Spawn";
            int distance = (int) Math.round(distanceFromSpawn);
            
            builder.set("#ZoneName.Text", zoneName + " - " + distance + "m");
            builder.set("#Essence.Text", "Essence: " + essence + "/1000");
            
            updateEssenceBar(builder);
            
            if (currentZone != null) {
                builder.set("#HPMult.Text", zoneConfig.getHudLabelHealth() + ": x" + String.format("%.1f", currentZone.getHealthMultiplier()));
                builder.set("#DMGMult.Text", zoneConfig.getHudLabelDamage() + ": x" + String.format("%.1f", currentZone.getDamageMultiplier()));
                builder.set("#LootMult.Text", zoneConfig.getHudLabelLoot() + ": x" + String.format("%.1f", currentZone.getLootMultiplier()));
            } else {
                builder.set("#HPMult.Text", "");
                builder.set("#DMGMult.Text", "");
                builder.set("#LootMult.Text", "");
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to build zone HUD: " + e.getMessage());
            return;
        }
        built = true;
    }

    public void updateZoneInfo(@Nullable DifficultyZone zone, double distance) {
        if (!built) {
            return;
        }
        boolean changed = false;
        
        if (this.currentZone != zone) {
            this.currentZone = zone;
            changed = true;
        }
        
        if (Math.abs(this.distanceFromSpawn - distance) > 1.0) {
            this.distanceFromSpawn = distance;
            changed = true;
        }
        
        // Récupérer l'essence du joueur
        EssenceManager essenceManager = MobDistanceScalingPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            int currentEssence = essenceManager.getEssence(getPlayerRef().getUuid());
            if (this.essence != currentEssence) {
                this.essence = currentEssence;
                changed = true;
            }
        }
        
        if (changed) {
            UICommandBuilder builder = new UICommandBuilder();
            
            String zoneName = currentZone != null ? "Zone " + currentZone.getName() : "Spawn";
            int dist = (int) Math.round(distanceFromSpawn);
            
            builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
            builder.set("#Essence.Text", "Essence: " + essence + "/1000");
            
            updateEssenceBar(builder);
            
            if (currentZone != null) {
                builder.set("#HPMult.Text", zoneConfig.getHudLabelHealth() + ": x" + String.format("%.1f", currentZone.getHealthMultiplier()));
                builder.set("#DMGMult.Text", zoneConfig.getHudLabelDamage() + ": x" + String.format("%.1f", currentZone.getDamageMultiplier()));
                builder.set("#LootMult.Text", zoneConfig.getHudLabelLoot() + ": x" + String.format("%.1f", currentZone.getLootMultiplier()));
            } else {
                builder.set("#HPMult.Text", "");
                builder.set("#DMGMult.Text", "");
                builder.set("#LootMult.Text", "");
            }
            
            update(false, builder);
        }
    }

    @Nullable
    public DifficultyZone getCurrentZone() {
        return currentZone;
    }

    private void updateEssenceBar(@Nonnull UICommandBuilder builder) {
        int totalWidth = 320;
        int halfWidth = totalWidth / 2;
        int clamped = Math.max(-1000, Math.min(1000, essence));
        int width = (int) Math.round(Math.abs(clamped) / 1000.0 * halfWidth);
        int left = clamped >= 0 ? halfWidth : halfWidth - width;

        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(left));
        fillAnchor.setWidth(Value.of(width));
        fillAnchor.setHeight(Value.of(14));
        builder.setObject("#EssenceBar.Anchor", fillAnchor);

        Anchor effectAnchor = new Anchor();
        effectAnchor.setLeft(Value.of(left));
        effectAnchor.setWidth(Value.of(width));
        effectAnchor.setHeight(Value.of(12));
        builder.setObject("#EssenceBarEffect.Anchor", effectAnchor);

        int labelWidth = 60;
        int labelLeft = left + width - (labelWidth / 2);
        if (labelLeft < 0) {
            labelLeft = 0;
        } else if (labelLeft > totalWidth - labelWidth) {
            labelLeft = totalWidth - labelWidth;
        }
        Anchor labelAnchor = new Anchor();
        labelAnchor.setLeft(Value.of(labelLeft));
        labelAnchor.setWidth(Value.of(labelWidth));
        labelAnchor.setHeight(Value.of(14));
        builder.setObject("#EssenceValue.Anchor", labelAnchor);
        builder.set("#EssenceValue.Text", String.valueOf(clamped));
    }
}
