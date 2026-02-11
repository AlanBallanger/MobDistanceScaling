package com.mobdistancescaling.config;

import javax.annotation.Nonnull;

public class ExtractionConfig {
    private boolean enabled;
    private int minDistance;
    private int maxDistance;
    private int portalDurationSeconds;
    private int cooldownSeconds;
    private String messagePortalSpawned;
    private String messagePortalExpired;
    private String messageCooldown;
    private String messageTeleporting;
    private String messageNotYourPortal;
    private String messageNoSafeLocation;
    private String messageError;
    private String messageAlreadyHasPortal;

    public ExtractionConfig() {
        this.enabled = true;
        this.minDistance = 100;
        this.maxDistance = 200;
        this.portalDurationSeconds = 300;
        this.cooldownSeconds = 300;
        this.messagePortalSpawned = "Portail d'extraction apparu a {distance} blocs ! ({x}, {y}, {z})";
        this.messagePortalExpired = "Votre portail d'extraction a expire.";
        this.messageCooldown = "Cooldown: {remaining} secondes restantes.";
        this.messageTeleporting = "Teleportation vers le spawn...";
        this.messageNotYourPortal = "Ce portail ne vous appartient pas.";
        this.messageNoSafeLocation = "Impossible de trouver un emplacement sur pour le portail.";
        this.messageError = "Erreur lors de la creation du portail.";
        this.messageAlreadyHasPortal = "Vous avez deja un portail actif.";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public int getPortalDurationSeconds() {
        return portalDurationSeconds;
    }

    public void setPortalDurationSeconds(int portalDurationSeconds) {
        this.portalDurationSeconds = portalDurationSeconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    @Nonnull
    public String getMessagePortalSpawned() {
        return messagePortalSpawned;
    }

    public void setMessagePortalSpawned(@Nonnull String messagePortalSpawned) {
        this.messagePortalSpawned = messagePortalSpawned;
    }

    @Nonnull
    public String getMessagePortalExpired() {
        return messagePortalExpired;
    }

    public void setMessagePortalExpired(@Nonnull String messagePortalExpired) {
        this.messagePortalExpired = messagePortalExpired;
    }

    @Nonnull
    public String getMessageCooldown() {
        return messageCooldown;
    }

    public void setMessageCooldown(@Nonnull String messageCooldown) {
        this.messageCooldown = messageCooldown;
    }

    @Nonnull
    public String getMessageTeleporting() {
        return messageTeleporting;
    }

    public void setMessageTeleporting(@Nonnull String messageTeleporting) {
        this.messageTeleporting = messageTeleporting;
    }

    @Nonnull
    public String getMessageNotYourPortal() {
        return messageNotYourPortal;
    }

    public void setMessageNotYourPortal(@Nonnull String messageNotYourPortal) {
        this.messageNotYourPortal = messageNotYourPortal;
    }

    @Nonnull
    public String getMessageNoSafeLocation() {
        return messageNoSafeLocation;
    }

    public void setMessageNoSafeLocation(@Nonnull String messageNoSafeLocation) {
        this.messageNoSafeLocation = messageNoSafeLocation;
    }

    @Nonnull
    public String getMessageError() {
        return messageError;
    }

    public void setMessageError(@Nonnull String messageError) {
        this.messageError = messageError;
    }

    @Nonnull
    public String getMessageAlreadyHasPortal() {
        return messageAlreadyHasPortal;
    }

    public void setMessageAlreadyHasPortal(@Nonnull String messageAlreadyHasPortal) {
        this.messageAlreadyHasPortal = messageAlreadyHasPortal;
    }
}
