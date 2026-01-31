package com.mobdistancescaling.essence;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PlayerEssenceData {
    private final UUID playerUuid;
    private int essence;
    private long lastModified;
    private boolean dirty; // Indique si les données ont changé depuis la dernière sauvegarde

    public PlayerEssenceData(@Nonnull UUID playerUuid, int essence) {
        this.playerUuid = playerUuid;
        this.essence = Math.min(essence, 1000); // Cap à 1000
        this.lastModified = System.currentTimeMillis();
        this.dirty = false;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getEssence() {
        return essence;
    }

    public void addEssence(int amount) {
        if (amount > 0) {
            this.essence = Math.min(this.essence + amount, 1000); // Cap à 1000
            this.lastModified = System.currentTimeMillis();
            this.dirty = true;
        }
    }

    public void setEssence(int essence) {
        this.essence = Math.min(Math.max(essence, 0), 1000); // Entre 0 et 1000
        this.lastModified = System.currentTimeMillis();
        this.dirty = true;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }
}
