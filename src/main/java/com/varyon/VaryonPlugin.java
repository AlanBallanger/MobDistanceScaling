package com.varyon;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.varyon.command.EssenceCommand;
import com.varyon.command.ExtractCommand;
import com.varyon.command.VaryonCommand;
import com.varyon.command.RtpzCommand;
import com.varyon.command.RtpvCommand;
import com.varyon.command.ReturnCommand;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.config.GlobalRewardsConfig;
import com.varyon.death.DeathDetectionSystem;
import com.varyon.death.DeathPointManager;
import com.varyon.deposit.DepositBlockInteractionSystem;
import com.varyon.deposit.DepositBlockManager;
import com.varyon.deposit.DepositUIManager;
import com.varyon.essence.EssenceKillSystem;
import com.varyon.essence.EssenceManager;
import com.varyon.essence.GlobalRewardsManager;
import com.varyon.extraction.ExtractionPortalManager;
import com.varyon.system.ExtractionPortalTickSystem;
import com.varyon.essence.EssenceMiningSystem;
import com.varyon.faction.FactionManager;
import com.varyon.hud.ZoneHUDManager;
import com.varyon.map.ZoneWorldMapProvider;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.safezone.SafeZoneNotificationSystem;
import com.varyon.safezone.SafeZonePvpSystem;
import com.varyon.system.MobDamageScalingSystem;
import com.varyon.system.MobLootScalingSystem;
import com.varyon.system.MobScalingRefSystem;
import com.varyon.system.ZoneTitleTickingSystem;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class VaryonPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ConfigManager staticConfigManager;
    private static EssenceManager staticEssenceManager;
    private static FactionManager staticFactionManager;
    private static SafeZoneManager staticSafeZoneManager;
    private static SafeZoneNotificationSystem staticSafeZoneNotificationSystem;
    private static GlobalRewardsManager staticGlobalRewardsManager;
    private static VaryonPlugin staticInstance;
    private ConfigManager configManager;
    private EssenceManager essenceManager;
    private FactionManager factionManager;
    private SafeZoneManager safeZoneManager;
    private SafeZoneNotificationSystem safeZoneNotificationSystem;
    private ZoneHUDManager hudManager;
    private ExtractionPortalManager extractionPortalManager;
    private EssenceRewardsConfig essenceRewardsConfig;
    private GlobalRewardsConfig globalRewardsConfig;
    private GlobalRewardsManager globalRewardsManager;
    private DepositBlockManager depositBlockManager;
    private DepositUIManager depositUIManager;
    private DeathPointManager deathPointManager;

    public VaryonPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.at(Level.INFO).log("Varyon v{0} loaded", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        try {
            staticInstance = this;
            ComponentType<EntityStore, MobScalingComponent> mobScalingComponentType =
                    this.getEntityStoreRegistry().registerComponent(MobScalingComponent.class,
                            () -> new MobScalingComponent(0, 1.0f, 1.0f, 1.0f, 1.0f));
            MobScalingComponent.setComponentType(mobScalingComponentType);

            configManager = new ConfigManager(this.getDataDirectory());
            configManager.load();
            staticConfigManager = configManager;

            // Initialiser le système d'essence
            essenceManager = new EssenceManager(this.getDataDirectory().toFile());
            staticEssenceManager = essenceManager;

            essenceRewardsConfig = new EssenceRewardsConfig();
            essenceRewardsConfig.load(this.getDataDirectory());
            LOGGER.at(Level.INFO).log("Essence system initialized");

            // Initialiser le système de factions
            factionManager = new FactionManager();
            staticFactionManager = factionManager;
            LOGGER.at(Level.INFO).log("Faction system initialized");

            // Initialiser le système de récompenses globales
            globalRewardsConfig = configManager.getGlobalRewardsConfig();
            globalRewardsManager = new GlobalRewardsManager(globalRewardsConfig, essenceManager, factionManager);
            staticGlobalRewardsManager = globalRewardsManager;
            
            // Lier le rewards manager à l'essence manager
            essenceManager.setRewardsManager(globalRewardsManager);
            
            LOGGER.at(Level.INFO).log("Global rewards system initialized");
            
            // Vérifier les récompenses au démarrage
            globalRewardsManager.checkAndDistributeRewards();

            extractionPortalManager = new ExtractionPortalManager(configManager.getExtractionConfig());
            ExtractionPortalTickSystem extractionTickSystem = new ExtractionPortalTickSystem();
            this.getEntityStoreRegistry().registerSystem(extractionTickSystem);
            LOGGER.at(Level.INFO).log("Extraction portal system initialized (distance: " +
                configManager.getExtractionConfig().getMinDistance() + "-" +
                configManager.getExtractionConfig().getMaxDistance() + ", duration: " +
                configManager.getExtractionConfig().getPortalDurationSeconds() + "s, cooldown: " +
                configManager.getExtractionConfig().getCooldownSeconds() + "s)");

            MobScalingRefSystem mobScalingRefSystem = new MobScalingRefSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(mobScalingRefSystem);

            MobDamageScalingSystem mobDamageScalingSystem = new MobDamageScalingSystem();
            this.getEntityStoreRegistry().registerSystem(mobDamageScalingSystem);

            MobLootScalingSystem mobLootScalingSystem = new MobLootScalingSystem();
            this.getEntityStoreRegistry().registerSystem(mobLootScalingSystem);

            EssenceKillSystem essenceKillSystem = new EssenceKillSystem(essenceManager, configManager, essenceRewardsConfig);
            this.getEntityStoreRegistry().registerSystem(essenceKillSystem);

            EssenceMiningSystem essenceMiningSystem = new EssenceMiningSystem(essenceManager, configManager, essenceRewardsConfig);
            this.getEntityStoreRegistry().registerSystem(essenceMiningSystem);
            LOGGER.at(Level.INFO).log("Essence reward systems registered");

            // Initialiser le système de dépôt d'essence
            depositBlockManager = new DepositBlockManager(this.getDataDirectory());
            depositUIManager = new DepositUIManager(essenceManager);
            
            DepositBlockInteractionSystem depositInteractionSystem = new DepositBlockInteractionSystem(depositBlockManager, depositUIManager);
            this.getEntityStoreRegistry().registerSystem(depositInteractionSystem);
            LOGGER.at(Level.INFO).log("Deposit block system initialized");

            // Initialiser le système de retour au point de mort
            deathPointManager = new DeathPointManager(this.getDataDirectory());
            
            DeathDetectionSystem deathDetectionSystem = new DeathDetectionSystem(deathPointManager);
            this.getEntityStoreRegistry().registerSystem(deathDetectionSystem);
            LOGGER.at(Level.INFO).log("Death point system initialized");

            // Initialiser le système de safe zone
            if (configManager.getSafeZoneConfig().isEnabled()) {
                safeZoneManager = new SafeZoneManager(configManager.getSafeZoneConfig(), configManager.getZoneConfig(), this.getDataDirectory());
                staticSafeZoneManager = safeZoneManager;
                
                SafeZonePvpSystem safeZonePvpSystem = new SafeZonePvpSystem();
                SafeZonePvpSystem.setSafeZoneManager(safeZoneManager);
                this.getEntityStoreRegistry().registerSystem(safeZonePvpSystem);
                
                safeZoneNotificationSystem = new SafeZoneNotificationSystem(configManager.getSafeZoneConfig(), configManager.getZoneConfig());
                SafeZoneNotificationSystem.setSafeZoneManager(safeZoneManager);
                this.getEntityStoreRegistry().registerSystem(safeZoneNotificationSystem);
                staticSafeZoneNotificationSystem = safeZoneNotificationSystem;
                
                // Activer le PvP dans tous les mondes pour que le système de SafeZone fonctionne
                this.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
                    World world = event.getWorld();
                    if (!world.getWorldConfig().isDeleteOnRemove()) {
                        world.getWorldConfig().setPvpEnabled(true);
                        world.getWorldConfig().markChanged();
                        LOGGER.at(Level.INFO).log("PvP enabled for world: {0} (controlled by SafeZone system)", world.getName());
                    }
                });
                
                // Activer aussi pour les mondes déjà chargés
                for (World world : Universe.get().getWorlds().values()) {
                    if (!world.getWorldConfig().isDeleteOnRemove()) {
                        world.getWorldConfig().setPvpEnabled(true);
                        world.getWorldConfig().markChanged();
                        LOGGER.at(Level.INFO).log("PvP enabled for existing world: {0} (controlled by SafeZone system)", world.getName());
                    }
                }
                
                LOGGER.at(Level.INFO).log("Safe zone rotation system enabled");
            }

            ZoneTitleTickingSystem zoneTitleSystem = new ZoneTitleTickingSystem(configManager, essenceManager);
            this.getEntityStoreRegistry().registerSystem(zoneTitleSystem);

            if (configManager.getZoneConfig().isMinimapEnabled()) {
                setupMinimapProvider();
            }

            hudManager = new ZoneHUDManager(configManager.getZoneConfig());
            if (hudManager.isAvailable()) {
                LOGGER.at(Level.INFO).log("Zone HUD initialized with Objective system");
                
                this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
                    try {
                        PlayerRef playerRef = event.getPlayerRef();
                        // Charger l'essence du joueur depuis la base de données
                        essenceManager.loadPlayer(playerRef.getUuid());
                    } catch (Exception e) {
                        LOGGER.at(Level.WARNING).log("Failed to load player essence: " + e.getMessage());
                    }
                });
                
                this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
                    Player player = event.getPlayer();
                    Ref ref = event.getPlayerRef();
                    Store store = ref.getStore();
                    World world = ((EntityStore)store.getExternalData()).getWorld();

                    world.execute(() -> {
                        try {
                            PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                            if (playerRef == null) {
                                return;
                            }
                            hudManager.registerPlayer(player, playerRef);
                            LOGGER.at(Level.INFO).log("Registered HUD for player: " + playerRef.getUuid());
                        } catch (Exception e) {
                            LOGGER.at(Level.WARNING).log("Failed to register HUD for player: " + e.getMessage());
                        }
                    });
                });

                this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
                    PlayerRef playerRef = event.getPlayerRef();
                    hudManager.removePlayer(playerRef.getUuid());
                    essenceManager.savePlayer(playerRef.getUuid());
                    if (safeZoneNotificationSystem != null) {
                        safeZoneNotificationSystem.removePlayer(playerRef.getUuid());
                    }
                    if (extractionPortalManager != null) {
                        extractionPortalManager.removePlayerPortal(playerRef.getUuid());
                    }
                });
            } else {
                LOGGER.at(Level.WARNING).log("Zone HUD could not be initialized");
            }

            this.getCommandRegistry().registerCommand(new VaryonCommand(this, factionManager, depositBlockManager));
            this.getCommandRegistry().registerCommand(new ExtractCommand("extract"));
            this.getCommandRegistry().registerCommand(new ExtractCommand("ex"));
            this.getCommandRegistry().registerCommand(new ReturnCommand());
            this.getCommandRegistry().registerCommand(new EssenceCommand(essenceManager, factionManager));
            this.getCommandRegistry().registerCommand(new RtpzCommand());
            this.getCommandRegistry().registerCommand(new RtpvCommand());
            LOGGER.at(Level.INFO).log("Commands registered");

            LOGGER.at(Level.INFO).log("Varyon initialized with {0} zones",
                    configManager.getZoneConfig().getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize Varyon", e);
            throw e;
        }
    }

    protected void onDisable() {
        if (essenceManager != null) {
            essenceManager.shutdown();
        }
        if (hudManager != null) {
            hudManager.shutdown();
        }
        if (safeZoneManager != null) {
            safeZoneManager.shutdown();
        }
        if (extractionPortalManager != null) {
            extractionPortalManager.shutdown();
        }
        if (deathPointManager != null) {
            deathPointManager.shutdown();
        }
    }

    private void setupMinimapProvider() {
        // Register the world map provider codec
        IWorldMapProvider.CODEC.register(ZoneWorldMapProvider.ID, ZoneWorldMapProvider.class, ZoneWorldMapProvider.CODEC);

        // Listen for world creation events to apply the provider
        this.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
            applyMinimapToWorld(event.getWorld());
        });

        // Also apply to any worlds that are already loaded
        for (World world : Universe.get().getWorlds().values()) {
            applyMinimapToWorld(world);
        }

        LOGGER.at(Level.INFO).log("Minimap zone overlay enabled");
    }

    private void applyMinimapToWorld(World world) {
        // Skip temporary/instance worlds
        if (world.getWorldConfig().isDeleteOnRemove()) {
            return;
        }

        // Check if this world is in our enabled worlds list
        if (!configManager.getZoneConfig().isWorldEnabled(world.getName())) {
            LOGGER.at(Level.INFO).log("Minimap not enabled for world: {0}", world.getName());
            return;
        }

        // Set our world map provider
        world.getWorldConfig().setWorldMapProvider(new ZoneWorldMapProvider());
        
        // Register extraction portal marker provider
        world.getWorldMapManager().addMarkerProvider("extraction_portal", new com.varyon.extraction.ExtractionPortalMarkerProvider());
        
        LOGGER.at(Level.INFO).log("Set Varyon minimap for world: {0}", world.getName());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Nullable
    public static ConfigManager getStaticConfigManager() {
        return staticConfigManager;
    }

    @Nullable
    public static EssenceManager getStaticEssenceManager() {
        return staticEssenceManager;
    }

    @Nullable
    public static FactionManager getStaticFactionManager() {
        return staticFactionManager;
    }

    @Nullable
    public static SafeZoneManager getStaticSafeZoneManager() {
        return staticSafeZoneManager;
    }

    @Nullable
    public static GlobalRewardsManager getStaticGlobalRewardsManager() {
        return staticGlobalRewardsManager;
    }

    public ZoneHUDManager getHudManager() {
        return hudManager;
    }
    
    public DeathPointManager getDeathPointManager() {
        return deathPointManager;
    }

    @Nullable
    public static VaryonPlugin getInstance() {
        return staticInstance;
    }
}
