package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.logging.Level;

public class MessagesConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private ExtractionMessages extraction;
    private ReturnMessages returnMessages;
    private SafeZoneMessages safeZone;
    private RtpMessages rtp;
    private EssenceMessages essence;
    
    public static class ExtractionMessages {
        public String portalSpawned;
        public String alreadyHasPortal;
        public String cooldown;
        public String noSafeLocation;
        public String teleporting;
        public String notYourPortal;
        public String error;
        
        public ExtractionMessages(String portalSpawned, String alreadyHasPortal, String cooldown,
                                 String noSafeLocation, String teleporting, String notYourPortal, String error) {
            this.portalSpawned = portalSpawned;
            this.alreadyHasPortal = alreadyHasPortal;
            this.cooldown = cooldown;
            this.noSafeLocation = noSafeLocation;
            this.teleporting = teleporting;
            this.notYourPortal = notYourPortal;
            this.error = error;
        }
    }
    
    public static class ReturnMessages {
        public String success;
        public String cooldown;
        public String noDeathPoint;
        public String expired;
        public String alreadyUsed;
        public String teleporting;
        public String noSafeLocation;
        public String error;
        public String firstUseWarning;
        
        public ReturnMessages(String success, String cooldown, String noDeathPoint, String expired,
                            String alreadyUsed, String teleporting, String noSafeLocation,
                            String error, String firstUseWarning) {
            this.success = success;
            this.cooldown = cooldown;
            this.noDeathPoint = noDeathPoint;
            this.expired = expired;
            this.alreadyUsed = alreadyUsed;
            this.teleporting = teleporting;
            this.noSafeLocation = noSafeLocation;
            this.error = error;
            this.firstUseWarning = firstUseWarning;
        }
    }
    
    public static class SafeZoneMessages {
        public String enterSafeZoneTitle;
        public String enterSafeZoneSubtitle;
        public String pvpDisabled;
        public String pvpEnabled;
        public String rotation;
        public String overlapStart;
        public String overlapEnd;
        public String timeRemaining;
        
        public SafeZoneMessages(String enterSafeZoneTitle, String enterSafeZoneSubtitle,
                               String pvpDisabled, String pvpEnabled, String rotation,
                               String overlapStart, String overlapEnd, String timeRemaining) {
            this.enterSafeZoneTitle = enterSafeZoneTitle;
            this.enterSafeZoneSubtitle = enterSafeZoneSubtitle;
            this.pvpDisabled = pvpDisabled;
            this.pvpEnabled = pvpEnabled;
            this.rotation = rotation;
            this.overlapStart = overlapStart;
            this.overlapEnd = overlapEnd;
            this.timeRemaining = timeRemaining;
        }
    }
    
    public static class RtpMessages {
        public String teleporting;
        public String success;
        public String noSafeLocation;
        public String zoneNotFound;
        public String availableZones;
        public String worldNotSupported;
        public String randomZone;
        public String error;
        public String noPermission;
        
        public RtpMessages(String teleporting, String success, String noSafeLocation,
                         String zoneNotFound, String availableZones, String worldNotSupported,
                         String randomZone, String error, String noPermission) {
            this.teleporting = teleporting;
            this.success = success;
            this.noSafeLocation = noSafeLocation;
            this.zoneNotFound = zoneNotFound;
            this.availableZones = availableZones;
            this.worldNotSupported = worldNotSupported;
            this.randomZone = randomZone;
            this.error = error;
            this.noPermission = noPermission;
        }
    }
    
    public static class EssenceMessages {
        public String balanceInfo;
        public String given;
        public String taken;
        public String maxSet;
        public String deposited;
        public String noFaction;
        public String notEnough;
        
        public EssenceMessages(String balanceInfo, String given, String taken, String maxSet,
                             String deposited, String noFaction, String notEnough) {
            this.balanceInfo = balanceInfo;
            this.given = given;
            this.taken = taken;
            this.maxSet = maxSet;
            this.deposited = deposited;
            this.noFaction = noFaction;
            this.notEnough = notEnough;
        }
    }
    
    public MessagesConfig(ExtractionMessages extraction, ReturnMessages returnMessages,
                        SafeZoneMessages safeZone, RtpMessages rtp, EssenceMessages essence) {
        this.extraction = extraction;
        this.returnMessages = returnMessages;
        this.safeZone = safeZone;
        this.rtp = rtp;
        this.essence = essence;
    }
    
    public static MessagesConfig load(@Nonnull Path configPath) {
        File configFile = configPath.resolve("messages.toml").toFile();
        
        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("messages.toml not found, creating default");
            MessagesConfig defaultConfig = createDefault();
            defaultConfig.save(configPath);
            return defaultConfig;
        }
        
        try {
            Toml toml = new Toml().read(configFile);
            
            Toml extractToml = toml.getTable("extraction");
            ExtractionMessages extraction = new ExtractionMessages(
                extractToml.getString("portalSpawned", "Portail d'extraction créé à {distance}m ({x}, {y}, {z})! Durée: {duration}s"),
                extractToml.getString("alreadyHasPortal", "Vous avez déjà un portail actif"),
                extractToml.getString("cooldown", "Cooldown actif. Temps restant: {remaining} secondes"),
                extractToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr"),
                extractToml.getString("teleporting", "Téléportation vers le spawn..."),
                extractToml.getString("notYourPortal", "Ce portail ne vous appartient pas"),
                extractToml.getString("error", "Erreur lors de la création du portail")
            );
            
            Toml returnToml = toml.getTable("return");
            ReturnMessages returnMsg = new ReturnMessages(
                returnToml.getString("success", "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})"),
                returnToml.getString("cooldown", "Cooldown actif. Temps restant: {remaining} secondes"),
                returnToml.getString("noDeathPoint", "Aucun point de mort enregistré"),
                returnToml.getString("expired", "Votre point de mort a expiré"),
                returnToml.getString("alreadyUsed", "Vous avez déjà utilisé votre téléportation pour cette mort"),
                returnToml.getString("teleporting", "Recherche d'un emplacement sûr près de votre point de mort..."),
                returnToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr après {attempts} tentatives"),
                returnToml.getString("error", "Erreur lors de la téléportation"),
                returnToml.getString("firstUseWarning", "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!")
            );
            
            Toml safeToml = toml.getTable("safezone");
            SafeZoneMessages safeZone = new SafeZoneMessages(
                safeToml.getString("enterTitle", "Zone non-PvP"),
                safeToml.getString("enterSubtitle", "Vous êtes en sécurité"),
                safeToml.getString("pvpDisabled", "[PvP] Vous êtes dans une zone non-PvP!"),
                safeToml.getString("pvpEnabled", "[PvP] Vous êtes dans une zone PvP!"),
                safeToml.getString("rotation", "[PvP] La zone non-PvP est maintenant au {direction}!"),
                safeToml.getString("overlapStart", "[PvP] Double zone non-PvP active pendant {minutes} minutes!"),
                safeToml.getString("overlapEnd", "[PvP] Fin de la double zone non-PvP!"),
                safeToml.getString("timeRemaining", "[PvP] Rotation dans {minutes} minutes")
            );
            
            Toml rtpToml = toml.getTable("rtp");
            RtpMessages rtp = new RtpMessages(
                rtpToml.getString("teleporting", "Téléportation en cours..."),
                rtpToml.getString("success", "Téléporté en {zone} à ({x}, {y}, {z})"),
                rtpToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr après {attempts} tentatives"),
                rtpToml.getString("zoneNotFound", "Zone '{zone}' introuvable."),
                rtpToml.getString("availableZones", "Zones disponibles"),
                rtpToml.getString("worldNotSupported", "Ce monde ne supporte pas la téléportation"),
                rtpToml.getString("randomZone", "zone aléatoire"),
                rtpToml.getString("error", "Erreur lors de la téléportation"),
                rtpToml.getString("noPermission", "Vous n'avez pas la permission pour cette zone. Permission requise: {permission}")
            );
            
            Toml essenceToml = toml.getTable("essence");
            EssenceMessages essence = new EssenceMessages(
                essenceToml.getString("balanceInfo", "Essence: {current}/{max} | Faction: {faction} | Global: {global}"),
                essenceToml.getString("given", "Donné {amount} essence à {player}"),
                essenceToml.getString("taken", "Retiré {amount} essence de {player}"),
                essenceToml.getString("maxSet", "Maximum d'essence de {player} défini à {max}"),
                essenceToml.getString("deposited", "Déposé {amount} essence pour {faction}. Balance globale: {global}"),
                essenceToml.getString("noFaction", "Vous devez rejoindre une faction d'abord (/varyon faction <nom>)"),
                essenceToml.getString("notEnough", "Vous n'avez pas assez d'essence")
            );
            
            return new MessagesConfig(extraction, returnMsg, safeZone, rtp, essence);
            
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load messages.toml, using defaults: " + e.getMessage());
            return createDefault();
        }
    }
    
    public void save(@Nonnull Path configPath) {
        try {
            File configFile = configPath.resolve("messages.toml").toFile();
            configFile.getParentFile().mkdirs();
            
            StringBuilder sb = new StringBuilder();
            sb.append("# Varyon Messages Configuration\n");
            sb.append("# All text messages used by the plugin\n\n");
            
            sb.append("[extraction]\n");
            sb.append("portalSpawned = \"").append(extraction.portalSpawned).append("\"\n");
            sb.append("alreadyHasPortal = \"").append(extraction.alreadyHasPortal).append("\"\n");
            sb.append("cooldown = \"").append(extraction.cooldown).append("\"\n");
            sb.append("noSafeLocation = \"").append(extraction.noSafeLocation).append("\"\n");
            sb.append("teleporting = \"").append(extraction.teleporting).append("\"\n");
            sb.append("notYourPortal = \"").append(extraction.notYourPortal).append("\"\n");
            sb.append("error = \"").append(extraction.error).append("\"\n\n");
            
            sb.append("[return]\n");
            sb.append("success = \"").append(returnMessages.success).append("\"\n");
            sb.append("cooldown = \"").append(returnMessages.cooldown).append("\"\n");
            sb.append("noDeathPoint = \"").append(returnMessages.noDeathPoint).append("\"\n");
            sb.append("expired = \"").append(returnMessages.expired).append("\"\n");
            sb.append("alreadyUsed = \"").append(returnMessages.alreadyUsed).append("\"\n");
            sb.append("teleporting = \"").append(returnMessages.teleporting).append("\"\n");
            sb.append("noSafeLocation = \"").append(returnMessages.noSafeLocation).append("\"\n");
            sb.append("error = \"").append(returnMessages.error).append("\"\n");
            sb.append("firstUseWarning = \"").append(returnMessages.firstUseWarning).append("\"\n\n");
            
            sb.append("[safezone]\n");
            sb.append("enterTitle = \"").append(safeZone.enterSafeZoneTitle).append("\"\n");
            sb.append("enterSubtitle = \"").append(safeZone.enterSafeZoneSubtitle).append("\"\n");
            sb.append("pvpDisabled = \"").append(safeZone.pvpDisabled).append("\"\n");
            sb.append("pvpEnabled = \"").append(safeZone.pvpEnabled).append("\"\n");
            sb.append("rotation = \"").append(safeZone.rotation).append("\"\n");
            sb.append("overlapStart = \"").append(safeZone.overlapStart).append("\"\n");
            sb.append("overlapEnd = \"").append(safeZone.overlapEnd).append("\"\n");
            sb.append("timeRemaining = \"").append(safeZone.timeRemaining).append("\"\n\n");
            
            sb.append("[rtp]\n");
            sb.append("teleporting = \"").append(rtp.teleporting).append("\"\n");
            sb.append("success = \"").append(rtp.success).append("\"\n");
            sb.append("noSafeLocation = \"").append(rtp.noSafeLocation).append("\"\n");
            sb.append("zoneNotFound = \"").append(rtp.zoneNotFound).append("\"\n");
            sb.append("availableZones = \"").append(rtp.availableZones).append("\"\n");
            sb.append("worldNotSupported = \"").append(rtp.worldNotSupported).append("\"\n");
            sb.append("randomZone = \"").append(rtp.randomZone).append("\"\n");
            sb.append("error = \"").append(rtp.error).append("\"\n");
            sb.append("noPermission = \"").append(rtp.noPermission).append("\"\n\n");
            
            sb.append("[essence]\n");
            sb.append("balanceInfo = \"").append(essence.balanceInfo).append("\"\n");
            sb.append("given = \"").append(essence.given).append("\"\n");
            sb.append("taken = \"").append(essence.taken).append("\"\n");
            sb.append("maxSet = \"").append(essence.maxSet).append("\"\n");
            sb.append("deposited = \"").append(essence.deposited).append("\"\n");
            sb.append("noFaction = \"").append(essence.noFaction).append("\"\n");
            sb.append("notEnough = \"").append(essence.notEnough).append("\"\n");
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(sb.toString());
            }
            
            LOGGER.at(Level.INFO).log("Messages configuration saved");
            
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save messages.toml: " + e.getMessage());
        }
    }
    
    public static MessagesConfig createDefault() {
        ExtractionMessages extraction = new ExtractionMessages(
            "Portail d'extraction créé à {distance}m ({x}, {y}, {z})! Durée: {duration}s",
            "Vous avez déjà un portail actif",
            "Cooldown actif. Temps restant: {remaining} secondes",
            "Impossible de trouver un emplacement sûr",
            "Téléportation vers le spawn...",
            "Ce portail ne vous appartient pas",
            "Erreur lors de la création du portail"
        );
        
        ReturnMessages returnMsg = new ReturnMessages(
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
        
        SafeZoneMessages safeZone = new SafeZoneMessages(
            "Zone non-PvP",
            "Vous êtes en sécurité",
            "[PvP] Vous êtes dans une zone non-PvP!",
            "[PvP] Vous êtes dans une zone PvP!",
            "[PvP] La zone non-PvP est maintenant au {direction}!",
            "[PvP] Double zone non-PvP active pendant {minutes} minutes!",
            "[PvP] Fin de la double zone non-PvP!",
            "[PvP] Rotation dans {minutes} minutes"
        );
        
        RtpMessages rtp = new RtpMessages(
            "Téléportation en cours...",
            "Téléporté en {zone} à ({x}, {y}, {z})",
            "Impossible de trouver un emplacement sûr après {attempts} tentatives",
            "Zone '{zone}' introuvable.",
            "Zones disponibles",
            "Ce monde ne supporte pas la téléportation",
            "zone aléatoire",
            "Erreur lors de la téléportation",
            "Vous n'avez pas la permission pour cette zone. Permission requise: {permission}"
        );
        
        EssenceMessages essence = new EssenceMessages(
            "Essence: {current}/{max} | Faction: {faction} | Global: {global}",
            "Donné {amount} essence à {player}",
            "Retiré {amount} essence de {player}",
            "Maximum d'essence de {player} défini à {max}",
            "Déposé {amount} essence pour {faction}. Balance globale: {global}",
            "Vous devez rejoindre une faction d'abord (/varyon faction <nom>)",
            "Vous n'avez pas assez d'essence"
        );
        
        return new MessagesConfig(extraction, returnMsg, safeZone, rtp, essence);
    }
    
    public ExtractionMessages getExtraction() {
        return extraction;
    }
    
    public ReturnMessages getReturn() {
        return returnMessages;
    }
    
    public SafeZoneMessages getSafeZone() {
        return safeZone;
    }
    
    public RtpMessages getRtp() {
        return rtp;
    }
    
    public EssenceMessages getEssence() {
        return essence;
    }
}
