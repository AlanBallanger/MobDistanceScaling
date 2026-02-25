package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ZonePermissionsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "zone_permissions.toml";
    private static final String SECTION  = "zone_permissions";

    private final Map<Integer, String> permissionByZone;
    private final int maxZone;

    public ZonePermissionsConfig(@Nonnull Map<Integer, String> permissionByZone) {
        this.permissionByZone = new HashMap<>(permissionByZone);
        this.maxZone = permissionByZone.keySet().stream().mapToInt(i -> i).max().orElse(10);
    }

    @Nonnull
    public String getPermissionForZone(int zoneId) {
        return permissionByZone.getOrDefault(zoneId, "varyon.zone." + zoneId);
    }

    /**
     * Returns the highest zone ID the player can access,
     * based on which zone permissions they hold.
     * Having varyon.zone.5 grants access to zones 1-5.
     */
    public int getMaxAccessibleZone(@Nonnull Player player) {
        for (int z = maxZone; z >= 1; z--) {
            if (player.hasPermission(getPermissionForZone(z))) {
                return z;
            }
        }
        return 1;
    }

    /**
     * Returns true if the player may access the given zone.
     * A player with varyon.zone.5 can access zones 1 to 5 but not 6+.
     */
    public boolean canAccessZone(@Nonnull Player player, int zoneId) {
        return getMaxAccessibleZone(player) >= zoneId;
    }

    @Nonnull
    public static ZonePermissionsConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ZonePermissionsConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            Map<Integer, String> map = new HashMap<>();
            Toml section = toml.getTable(SECTION);
            if (section != null) {
                Map<String, Object> raw = section.toMap();
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    try {
                        int zoneId = Integer.parseInt(entry.getKey());
                        if (entry.getValue() instanceof String perm && !perm.isBlank()) {
                            map.put(zoneId, perm);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            LOGGER.at(Level.INFO).log("Loaded {0} with {1} zones", FILENAME, map.size());
            return new ZonePermissionsConfig(map);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + FILENAME + ", using defaults", e);
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateToml());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save " + FILENAME, e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Zone Access Permissions\n");
        sb.append("# zoneId = \"permission.node\"\n");
        sb.append("# Having varyon.zone.5 grants access to zones 1-5 (not 6+).\n\n");
        sb.append("[").append(SECTION).append("]\n");
        for (int z = 1; z <= 10; z++) {
            sb.append(z).append(" = \"").append(permissionByZone.getOrDefault(z, "varyon.zone." + z)).append("\"\n");
        }
        return sb.toString();
    }

    @Nonnull
    public static ZonePermissionsConfig createDefault() {
        Map<Integer, String> map = new HashMap<>();
        for (int z = 1; z <= 10; z++) {
            map.put(z, "varyon.zone." + z);
        }
        return new ZonePermissionsConfig(map);
    }
}
