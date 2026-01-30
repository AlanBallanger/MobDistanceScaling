package com.mobdistancescaling.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mobdistancescaling.config.DifficultyZone;
import com.mobdistancescaling.config.ZoneConfig;

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

    public ZoneHUD(@Nonnull PlayerRef playerRef, @Nonnull ZoneConfig zoneConfig) {
        super(playerRef);
        this.zoneConfig = zoneConfig;
        this.distanceFromSpawn = 0.0;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("Hud/ZoneHUD.ui");
            
            String zoneName = currentZone != null ? "Zone " + currentZone.getName() : "Spawn";
            int distance = (int) Math.round(distanceFromSpawn);
            
            builder.set("#ZoneName.Text", zoneName + " - " + distance + "m");
            
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
        }
    }

    public void updateZoneInfo(@Nullable DifficultyZone zone, double distance) {
        boolean changed = false;
        
        if (this.currentZone != zone) {
            this.currentZone = zone;
            changed = true;
        }
        
        if (Math.abs(this.distanceFromSpawn - distance) > 1.0) {
            this.distanceFromSpawn = distance;
            changed = true;
        }
        
        if (changed) {
            UICommandBuilder builder = new UICommandBuilder();
            
            String zoneName = currentZone != null ? "Zone " + currentZone.getName() : "Spawn";
            int dist = (int) Math.round(distanceFromSpawn);
            
            builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
            
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
}
