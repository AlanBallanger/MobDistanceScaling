package com.varyon.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.VaryonPlugin;
import com.varyon.config.DifficultyZone;
import com.varyon.config.MessagesConfig;
import com.varyon.essence.EssenceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class ZoneHUD extends CustomUIHud {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PAGE_COUNT = 2;
    private static final int PAGE_ZONE = 0;
    private static final int PAGE_PVP = 1;

    @Nonnull
    private final MessagesConfig messagesConfig;

    @Nullable
    private DifficultyZone currentZone;
    private double distanceFromSpawn;
    private int globalBalance;
    private double playerEssence;
    private boolean built;
    private long builtAt = 0;
    private static final long BUILD_GRACE_MS = 2000;
    private int currentPage = PAGE_ZONE;
    private boolean inSafeZone;
    private String safeQuadrantName = "";
    private long safeTimeRemaining;
    private boolean lootSpecialActive = false;
    private int maxEssenceCap = 1000;

    public ZoneHUD(@Nonnull PlayerRef playerRef, @Nonnull MessagesConfig messagesConfig) {
        super(playerRef);
        this.messagesConfig = messagesConfig;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        try {
            builder.append("HUD/ZoneHUD.ui");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to build zone HUD: " + e.getMessage());
            return;
        }
        built = true;
        builtAt = System.currentTimeMillis();
    }

    public void nextPage() {
        currentPage = (currentPage + 1) % PAGE_COUNT;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void updateZoneInfo(@Nullable DifficultyZone zone, double distance, boolean inSafe, @Nonnull String quadrantName, long timeRemaining, boolean forceUpdate, boolean lootSpecialActive, int maxEssenceCap) {
        if (this.maxEssenceCap != maxEssenceCap) {
            this.maxEssenceCap = maxEssenceCap;
            forceUpdate = true;
        }
        if (this.lootSpecialActive != lootSpecialActive) {
            this.lootSpecialActive = lootSpecialActive;
            forceUpdate = true;
        }
        if (!built || System.currentTimeMillis() - builtAt < BUILD_GRACE_MS) {
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
            zoneName = (name.toLowerCase().startsWith("zone") ? name : "Zone " + name) + " [" + currentZone.getZoneId() + "]";
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
        
        int currentEssence = (int) Math.floor(playerEssence);
        int displayMax = Math.max(maxEssenceCap, currentEssence);
        builder.set("#Essence.Text", "Essence: " + currentEssence + "/" + displayMax);
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        if (inSafeZone) {
            builder.set("#HPMult.Text", "PvP : OFF");
            builder.set("#HPMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#HPMult.Text", "PvP : Actif");
            builder.set("#HPMult.Style.TextColor", "#FF5555");
        }

        if (currentZone != null) {
            builder.set("#DMGMult.Text", messagesConfig.getHud().labelHealth + ": x" + String.format("%.1f", currentZone.getHealthMultiplier()));
            builder.set("#LootMult.Text", messagesConfig.getHud().labelDamage + ": x" + String.format("%.1f", currentZone.getDamageMultiplier()));
        } else {
            builder.set("#DMGMult.Text", "");
            builder.set("#LootMult.Text", "");
        }

        builder.set("#DMGMult.Style.TextColor", "#FFFFFF");
        builder.set("#LootMult.Style.TextColor", "#FFAA55");
        
        // Réinitialiser la largeur de DMGMult à sa taille normale sur cette page
        Anchor dmgAnchor = new Anchor();
        dmgAnchor.setWidth(Value.of(70));
        dmgAnchor.setHeight(Value.of(22));
        builder.setObject("#DMGMult.Anchor", dmgAnchor);
        
        // Remettre le séparateur 4 visible sur cette page
        builder.set("#Separator4.Text", "|");

        updateEssenceBar(builder);
    }

    private void applyPvpPage(@Nonnull UICommandBuilder builder) {
        String zoneName;
        if (currentZone != null) {
            String name = currentZone.getName();
            zoneName = (name.toLowerCase().startsWith("zone") ? name : "Zone " + name) + " [" + currentZone.getZoneId() + "]";
        } else {
            zoneName = "Spawn";
        }
        int dist = (int) Math.round(distanceFromSpawn);
        builder.set("#ZoneName.Text", zoneName + " - " + dist + "m");
        builder.set("#ZoneName.Style.TextColor", "#FFFFFF");

        int currentEssence = (int) Math.floor(playerEssence);
        int displayMax = Math.max(maxEssenceCap, currentEssence);
        builder.set("#Essence.Text", "Essence: " + currentEssence + "/" + displayMax);
        builder.set("#Essence.Style.TextColor", "#FFFF55");

        if (currentZone != null) {
            builder.set("#HPMult.Text", messagesConfig.getHud().labelLoot + ": x" + String.format("%.1f", currentZone.getLootMultiplier()));
            builder.set("#HPMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#HPMult.Text", "");
            builder.set("#HPMult.Style.TextColor", "#55FF55");
        }

        if (lootSpecialActive) {
            builder.set("#DMGMult.Text", "Loot spécial : Actif");
            builder.set("#DMGMult.Style.TextColor", "#55FF55");
        } else {
            builder.set("#DMGMult.Text", "Loot spécial : Inactif");
            builder.set("#DMGMult.Style.TextColor", "#FF5555");
        }
        
        // Étendre la largeur de DMGMult pour prendre l'espace de 2 zones + séparateur
        Anchor dmgAnchor = new Anchor();
        dmgAnchor.setWidth(Value.of(158)); // 70 + 18 + 70
        dmgAnchor.setHeight(Value.of(22));
        builder.setObject("#DMGMult.Anchor", dmgAnchor);

        // Cacher le séparateur 4 et la zone LootMult sur cette page
        builder.set("#Separator4.Text", "");
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
        
        if (clamped >= 0) {
            // Balance positive - barre Fracture (droite)
            int width = (int) Math.round(clamped / 10000.0 * halfWidth);
            
            Anchor fractureAnchor = new Anchor();
            fractureAnchor.setLeft(Value.of(halfWidth));
            fractureAnchor.setWidth(Value.of(width));
            fractureAnchor.setHeight(Value.of(14));
            builder.setObject("#EssenceBarFracture.Anchor", fractureAnchor);
            
            // Cache la barre Noyau
            Anchor noyauAnchor = new Anchor();
            noyauAnchor.setLeft(Value.of(halfWidth));
            noyauAnchor.setWidth(Value.of(0));
            noyauAnchor.setHeight(Value.of(14));
            builder.setObject("#EssenceBarNoyau.Anchor", noyauAnchor);
        } else {
            // Balance négative - barre Noyau (gauche)
            int width = (int) Math.round(Math.abs(clamped) / 10000.0 * halfWidth);
            int left = halfWidth - width;
            
            Anchor noyauAnchor = new Anchor();
            noyauAnchor.setLeft(Value.of(left));
            noyauAnchor.setWidth(Value.of(width));
            noyauAnchor.setHeight(Value.of(14));
            builder.setObject("#EssenceBarNoyau.Anchor", noyauAnchor);
            
            // Cache la barre Fracture
            Anchor fractureAnchor = new Anchor();
            fractureAnchor.setLeft(Value.of(halfWidth));
            fractureAnchor.setWidth(Value.of(0));
            fractureAnchor.setHeight(Value.of(14));
            builder.setObject("#EssenceBarFracture.Anchor", fractureAnchor);
        }

        // Position du label au centre de la barre active
        int labelWidth = 80;
        int labelLeft;
        
        if (clamped >= 0) {
            int width = (int) Math.round(clamped / 10000.0 * halfWidth);
            int barEnd = halfWidth + width;
            labelLeft = barEnd - (labelWidth / 2);
        } else {
            int width = (int) Math.round(Math.abs(clamped) / 10000.0 * halfWidth);
            int barStart = halfWidth - width;
            labelLeft = barStart + width - (labelWidth / 2);
        }
        
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
        builder.set("#EssenceValue.Text", String.valueOf(Math.abs(clamped)));
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
