package com.mobdistancescaling;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.mobdistancescaling.command.MdsCommand;
import com.mobdistancescaling.component.MobScalingComponent;
import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.map.ZoneWorldMapProvider;
import com.mobdistancescaling.system.MobDamageScalingSystem;
import com.mobdistancescaling.system.MobScalingRefSystem;
import com.mobdistancescaling.system.ZoneTitleTickingSystem;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class MobDistanceScalingPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ConfigManager staticConfigManager;
    private ConfigManager configManager;

    public MobDistanceScalingPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.at(Level.INFO).log("MobDistanceScaling v{0} loaded", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        try {
            // Register custom component type
            ComponentType<EntityStore, MobScalingComponent> mobScalingComponentType =
                    this.getEntityStoreRegistry().registerComponent(MobScalingComponent.class,
                            () -> new MobScalingComponent(1.0f));
            MobScalingComponent.setComponentType(mobScalingComponentType);

            // Load configuration
            configManager = new ConfigManager(this.getDataDirectory());
            configManager.load();
            staticConfigManager = configManager;

            // Register systems
            MobScalingRefSystem mobScalingRefSystem = new MobScalingRefSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(mobScalingRefSystem);

            MobDamageScalingSystem mobDamageScalingSystem = new MobDamageScalingSystem();
            this.getEntityStoreRegistry().registerSystem(mobDamageScalingSystem);

            // Register zone notification system
            ZoneTitleTickingSystem zoneTitleSystem = new ZoneTitleTickingSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(zoneTitleSystem);

            // Setup minimap overlay if enabled
            if (configManager.getZoneConfig().isMinimapEnabled()) {
                setupMinimapProvider();
            }

            // Register commands
            this.getCommandRegistry().registerCommand(new MdsCommand(this));

            LOGGER.at(Level.INFO).log("MobDistanceScaling initialized with {0} zones",
                    configManager.getZoneConfig().getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize MobDistanceScaling", e);
            throw e;
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
}
