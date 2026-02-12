package com.varyon.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.ZoneConfig;
import com.varyon.essence.EssenceManager;
import com.varyon.safezone.SafeZoneManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class ZoneHUD extends CustomUIHud {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PAGE_COUNT = 2;
    private static final int PAGE_ZONE = 0;
    private static final int PAGE_PVP = 1;

    @Nonnull
    private final ZoneConfig zoneConfig;

    @Nullable
    private DifficultyZone currentZone;
    private double distanceFromSpawn;
    private int globalBalance;
    private double playerEssence;
    private boolean built;
    private int currentPage = PAGE_ZONE;
    private boolean inSafeZone;
    private String safeQuadrantName = "";
    private long safeTimeRemaining;

    public ZoneHUD(@Nonnull PlayerRef playerRef, @Nonnull ZoneConfig zoneConfig) {
        super(playerRef);
        this.zoneConfig = zoneConfig;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/ZoneHUD.ui");
            applyZonePage(builder);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to build zone HUD: " + e.getMessage());
            return;
        }
        built = true;
    }

    public void nextPage() {
        currentPage = (currentPage + 1) % PAGE_COUNT;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void updateZoneInfo(@Nullable DifficultyZone zone, double distance, boolean inSafe, @Nonnull String quadrantName, long timeRemaining, boolean forceUpdate) {
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
        if (this.inSafeZone != inSafe) {
            this.inSafeZone = inSafe;
            changed = true;
        }
        if (!this.safeQuadrantName.equals(quadrantName)) {
            this.safeQuadrantName = quadrantName;
            changed = true;
        }
        long timeDiff = Math.abs(this.safeTimeRemaining - timeRemaining);
        if (timeDiff > 1000) {
            this.safeTimeRemaining = timeRemaining;
            changed = true;
        }

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            double currentPlayerEssence = essenceManager.getEssence(getPlayerRef().getUuid());
            int currentGlobalBalance = essenceManager.getGlobalBalance();
            if (Math.abs(this.playerEssence - currentPlayerEssence) > 0.01 || this.globalBalance != currentGlobalBalance) {
                this.playerEssence = currentPlayerEssence;
                this.globalBalance = currentGlobalBalance;
                changed = true;
            }
        }

        if (changed || forceUpdate) {
            sendPageUpdate();
        }
    }

    private void sendPageUpdate() {
        UICommandBuilder builder = new UICommandBuilder();
        if (currentPage == PAGE_ZONE) {
            applyZonePage(builder);
        } else {
            applyPvpPage(builder);
        }
        update(false, builder);
    }

    private void applyZonePage(@Nonnull UICommandBuilder builder) {
        String zoneName;
        if (currentZone != null) {
            String name = currentZone.getName();
            zoneName = name.toLowerCase().startsWith("zone") ? name : "Zone " + name;
        } else {
            zoneName = "Spawn";
        }
        int dist = (int) Math.round(distanceFromSpawn);

        builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
        builder.set("#ZoneName.Style.TextColor", "#FFFFFF");

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            playerEssence = essenceManager.getEssence(getPlayerRef().getUuid());
            globalBalance = essenceManager.getGlobalBalance();
        }
        builder.set("#Essence.Text", "Essence: " + (int) Math.floor(playerEssence) + "/1000");
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        if (currentZone != null) {
            builder.set("#HPMult.Text", zoneConfig.getHudLabelHealth() + ": x" + String.format("%.1f", currentZone.getHealthMultiplier()));
            builder.set("#DMGMult.Text", zoneConfig.getHudLabelDamage() + ": x" + String.format("%.1f", currentZone.getDamageMultiplier()));
            builder.set("#LootMult.Text", zoneConfig.getHudLabelLoot() + ": x" + String.format("%.1f", currentZone.getLootMultiplier()));
        } else {
            builder.set("#HPMult.Text", "");
            builder.set("#DMGMult.Text", "");
            builder.set("#LootMult.Text", "");
        }

        builder.set("#HPMult.Style.TextColor", "#FFFFFF");
        builder.set("#DMGMult.Style.TextColor", "#FFAA55");
        builder.set("#LootMult.Style.TextColor", "#55FF55");

        updateEssenceBar(builder);
    }

    private void applyPvpPage(@Nonnull UICommandBuilder builder) {
        String zoneName;
        if (currentZone != null) {
            String name = currentZone.getName();
            zoneName = name.toLowerCase().startsWith("zone") ? name : "Zone " + name;
        } else {
            zoneName = "Spawn";
        }
        int dist = (int) Math.round(distanceFromSpawn);
        builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
        builder.set("#ZoneName.Style.TextColor", "#FFFFFF");

        builder.set("#Essence.Text", "Essence: " + (int) Math.floor(playerEssence) + "/1000");
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        if (inSafeZone) {
            builder.set("#HPMult.Text", "PvP : OFF");
            builder.set("#HPMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#HPMult.Text", "PvP : Actif");
            builder.set("#HPMult.Style.TextColor", "#FF5555");
        }

        builder.set("#DMGMult.Text", "");
        builder.set("#LootMult.Text", "");

        updateEssenceBar(builder);
    }

    @Nullable
    public DifficultyZone getCurrentZone() {
        return currentZone;
    }

    private void updateEssenceBar(@Nonnull UICommandBuilder builder) {
        int totalWidth = 320;
        int halfWidth = totalWidth / 2;
        int clamped = Math.max(-10000, Math.min(10000, globalBalance));
        int width = (int) Math.round(Math.abs(clamped) / 10000.0 * halfWidth);
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

        int labelWidth = 80;
        int barEnd = left + width;
        int labelLeft = barEnd - (labelWidth / 2);

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

    public void updateGlobalBalance() {
        if (!built) {
            return;
        }

        EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
        if (essenceManager != null) {
            this.globalBalance = essenceManager.getGlobalBalance();

            UICommandBuilder builder = new UICommandBuilder();
            updateEssenceBar(builder);
            update(false, builder);
        }
    }
}
