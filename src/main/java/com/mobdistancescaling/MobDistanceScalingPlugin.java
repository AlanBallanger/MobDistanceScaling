package com.mobdistancescaling;

import com.mobdistancescaling.config.ConfigManager;
import com.mobdistancescaling.system.MobScalingRefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public class MobDistanceScalingPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ConfigManager configManager;

    public MobDistanceScalingPlugin(JavaPluginInit init) {
        super(init);
        System.out.println("========================================");
        System.out.println("MobDistanceScaling CONSTRUCTOR CALLED!");
        System.out.println("========================================");
        LOGGER.at(Level.INFO).log("MobDistanceScaling v{0} loaded", this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        System.out.println("========================================");
        System.out.println("MobDistanceScaling SETUP CALLED!");
        System.out.println("========================================");

        try {
            configManager = new ConfigManager(this.getDataDirectory());
            System.out.println("ConfigManager created");

            configManager.load();
            System.out.println("Config loaded with " + configManager.getZoneConfig().getZones().size() + " zones");

            MobScalingRefSystem mobScalingRefSystem = new MobScalingRefSystem(configManager);
            System.out.println("MobScalingRefSystem created");

            this.getEntityStoreRegistry().registerSystem(mobScalingRefSystem);
            System.out.println("MobScalingRefSystem registered in EntityStore");

            LOGGER.at(Level.INFO).log("MobDistanceScaling initialized with {0} zones",
                    configManager.getZoneConfig().getZones().size());

            System.out.println("========================================");
            System.out.println("MobDistanceScaling SETUP COMPLETE!");
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("ERROR IN SETUP:");
            e.printStackTrace();
            System.err.println("========================================");
            throw e;
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
