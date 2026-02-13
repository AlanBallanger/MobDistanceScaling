package com.varyon.config;

import javax.annotation.Nonnull;

public class ReturnConfig {
    private boolean enabled;
    private int cooldownSeconds;
    private int minDistance;
    private int maxDistance;
    private int expirationMinutes;
    private String messageSuccess;
    private String messageCooldown;
    private String messageNoDeathPoint;
    private String messageExpired;
    private String messageAlreadyUsed;
    private String messageTeleporting;
    private String messageNoSafeLocation;
    private String messageError;
    private String messageFirstUseWarning;
    
    public ReturnConfig(boolean enabled, int cooldownSeconds, int minDistance, int maxDistance,
                       int expirationMinutes, String messageSuccess, String messageCooldown,
                       String messageNoDeathPoint, String messageExpired, String messageAlreadyUsed,
                       String messageTeleporting, String messageNoSafeLocation, String messageError,
                       String messageFirstUseWarning) {
        this.enabled = enabled;
        this.cooldownSeconds = cooldownSeconds;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.expirationMinutes = expirationMinutes;
        this.messageSuccess = messageSuccess;
        this.messageCooldown = messageCooldown;
        this.messageNoDeathPoint = messageNoDeathPoint;
        this.messageExpired = messageExpired;
        this.messageAlreadyUsed = messageAlreadyUsed;
        this.messageTeleporting = messageTeleporting;
        this.messageNoSafeLocation = messageNoSafeLocation;
        this.messageError = messageError;
        this.messageFirstUseWarning = messageFirstUseWarning;
    }
    
    public static ReturnConfig createDefault() {
        return new ReturnConfig(
            true,
            1800, // 30 minutes
            100,
            200,
            30, // 30 minutes expiration
            "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})",
            "Cooldown actif. Temps restant: {remaining} secondes",
            "Aucun point de mort enregistré",
            "Votre point de mort a expiré",
            "Vous avez déjà utilisé votre téléportation pour cette mort",
            "Recherche d'un emplacement sûr près de votre point de mort...",
            "Impossible de trouver un emplacement sûr après {attempts} tentatives",
            "Erreur lors de la téléportation",
            "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!"
        );
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    public int getMinDistance() {
        return minDistance;
    }
    
    public int getMaxDistance() {
        return maxDistance;
    }
    
    public int getExpirationMinutes() {
        return expirationMinutes;
    }
    
    public String getMessageSuccess() {
        return messageSuccess;
    }
    
    public String getMessageCooldown() {
        return messageCooldown;
    }
    
    public String getMessageNoDeathPoint() {
        return messageNoDeathPoint;
    }
    
    public String getMessageExpired() {
        return messageExpired;
    }
    
    public String getMessageAlreadyUsed() {
        return messageAlreadyUsed;
    }
    
    public String getMessageTeleporting() {
        return messageTeleporting;
    }
    
    public String getMessageNoSafeLocation() {
        return messageNoSafeLocation;
    }
    
    public String getMessageError() {
        return messageError;
    }
    
    public String getMessageFirstUseWarning() {
        return messageFirstUseWarning;
    }
}
