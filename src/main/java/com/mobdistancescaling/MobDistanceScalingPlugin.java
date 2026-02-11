package com.mobdistancescaling;

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
import com.mobdistancescaling.command.EssenceCommand;
import com.mobdistancescaling.command.ExtractCommand;
import com.mobdistancescaling.command.GiveEssenceCommand;
import com.mobdistancescaling.command.MdsCommand;
import com.mobdistancescaling.command.RtpzCommand;
import com.mobdistancescaling.command.RtpvCommand;
import com.mobdistancescaling.component.MobScalingComponent;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.essence.EssenceKillSystem;
import com.mobdistancescaling.essence.EssenceManager;
import com.mobdistancescaling.extraction.ExtractionPortalManager;
import com.mobdistancescaling.system.ExtractionPortalTickSystem;
import com.mobdistancescaling.essence.EssenceMiningSystem;
import com.mobdistancescaling.faction.FactionManager;
import com.mobdistancescaling.hud.ZoneHUDManager;
import com.mobdistancescaling.map.ZoneWorldMapProvider;
import com.mobdistancescaling.safezone.SafeZoneManager;
import com.mobdistancescaling.safezone.SafeZoneNotificationSystem;
import com.mobdistancescaling.safezone.SafeZonePvpSystem;
import com.mobdistancescaling.system.MobDamageScalingSystem;
import com.mobdistancescaling.system.MobLootScalingSystem;
import com.mobdistancescaling.system.MobScalingRefSystem;
import com.mobdistancescaling.system.ZoneTitleTickingSystem;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class MobDistanceScalingPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ConfigManager staticConfigManager;
    private static EssenceManager staticEssenceManager;
    private static FactionManager staticFactionManager;
    private static SafeZoneManager staticSafeZoneManager;
    private static SafeZoneNotificationSystem staticSafeZoneNotificationSystem;
    private static MobDistanceScalingPlugin staticInstance;
    private ConfigManager configManager;
    private EssenceManager essenceManager;
    private FactionManager factionManager;
    private SafeZoneManager safeZoneManager;
    private SafeZoneNotificationSystem safeZoneNotificationSystem;
    private ZoneHUDManager hudManager;
    private ExtractionPortalManager extractionPortalManager;

    public MobDistanceScalingPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.at(Level.INFO).log("MobDistanceScaling v{0} loaded", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        try {
            staticInstance = this;
            ComponentType<EntityStore, MobScalingComponent> mobScalingComponentType =
                    this.getEntityStoreRegistry().registerComponent(MobScalingComponent.class,
                            () -> new MobScalingComponent(1.0f, 1.0f, 1.0f));
            MobScalingComponent.setComponentType(mobScalingComponentType);

            configManager = new ConfigManager(this.getDataDirectory());
            configManager.load();
            staticConfigManager = configManager;

            // Initialiser le système d'essence
            essenceManager = new EssenceManager(this.getDataDirectory().toFile());
            staticEssenceManager = essenceManager;
            LOGGER.at(Level.INFO).log("Essence system initialized");

            // Initialiser le système de factions
            factionManager = new FactionManager();
            staticFactionManager = factionManager;
            LOGGER.at(Level.INFO).log("Faction system initialized");

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

            EssenceKillSystem essenceKillSystem = new EssenceKillSystem(essenceManager, configManager);
            this.getEntityStoreRegistry().registerSystem(essenceKillSystem);
            LOGGER.at(Level.INFO).log("Registered EssenceKillSystem");
            
            EssenceMiningSystem essenceMiningSystem = new EssenceMiningSystem(essenceManager, configManager);
            this.getEntityStoreRegistry().registerSystem(essenceMiningSystem);
            LOGGER.at(Level.INFO).log("Registered EssenceMiningSystem");

            // Initialiser le système de safe zone
            if (configManager.getSafeZoneConfig().isEnabled()) {
                safeZoneManager = new SafeZoneManager(configManager.getSafeZoneConfig(), this.getDataDirectory());
                staticSafeZoneManager = safeZoneManager;
                
                SafeZonePvpSystem safeZonePvpSystem = new SafeZonePvpSystem();
                SafeZonePvpSystem.setSafeZoneManager(safeZoneManager);
                this.getEntityStoreRegistry().registerSystem(safeZonePvpSystem);
                
                safeZoneNotificationSystem = new SafeZoneNotificationSystem(configManager.getSafeZoneConfig());
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

            this.getCommandRegistry().registerCommand(new MdsCommand(this, factionManager));
            this.getCommandRegistry().registerCommand(new RtpzCommand());
            this.getCommandRegistry().registerCommand(new RtpvCommand());
            this.getCommandRegistry().registerCommand(new EssenceCommand(essenceManager, factionManager));
            this.getCommandRegistry().registerCommand(new ExtractCommand());
            LOGGER.at(Level.INFO).log("Registered /extract command");

            LOGGER.at(Level.INFO).log("MobDistanceScaling initialized with {0} zones",
                    configManager.getZoneConfig().getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize MobDistanceScaling", e);
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
        LOGGER.at(Level.INFO).log("Set MobDistanceScaling minimap for world: {0}", world.getName());
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

    public ZoneHUDManager getHudManager() {
        return hudManager;
    }

    @Nullable
    public static MobDistanceScalingPlugin getInstance() {
        return staticInstance;
    }
}
