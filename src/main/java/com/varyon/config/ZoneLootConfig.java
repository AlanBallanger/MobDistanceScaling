package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ZoneLootConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "zone_loot.toml";
    private static final String SECTION  = "zone_loot";

    private final Map<Integer, String> itemByZone;

    public ZoneLootConfig(@Nonnull Map<Integer, String> itemByZone) {
        this.itemByZone = new HashMap<>(itemByZone);
    }

    @Nullable
    public String getItemForZone(int zoneId) {
        return itemByZone.get(zoneId);
    }

    @Nonnull
    public static ZoneLootConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ZoneLootConfig def = createDefault();
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
                        if (entry.getValue() instanceof String itemId && !itemId.isBlank()) {
                            map.put(zoneId, itemId);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            LOGGER.at(Level.INFO).log("Loaded {0} with {1} zones", FILENAME, map.size());
            return new ZoneLootConfig(map);
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
        sb.append("# Zone Loot — item de fragment droppé par zone\n");
        sb.append("# La quantité vient de mob_special_rates.toml\n");
        sb.append("# zoneId = \"itemId\"\n\n");
        sb.append("[").append(SECTION).append("]\n");
        for (int z = 1; z <= 10; z++) {
            String item = itemByZone.getOrDefault(z, "Key_Fragment_" + z);
            sb.append(z).append(" = \"").append(item).append("\"\n");
        }
        return sb.toString();
    }

    @Nonnull
    public static ZoneLootConfig createDefault() {
        Map<Integer, String> map = new HashMap<>();
        for (int z = 1; z <= 10; z++) {
            map.put(z, "Key_Fragment" + z);
        }
        return new ZoneLootConfig(map);
    }
}
