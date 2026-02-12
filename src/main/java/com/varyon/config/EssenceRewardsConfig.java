package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EssenceRewardsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "essence_rewards.toml";

    private final Map<String, Double> oreRewards = new ConcurrentHashMap<>();
    private final Map<String, Double> mobRewards = new ConcurrentHashMap<>();
    private double globalMultiplier = 0.1;
    private double defaultMobReward = 3.0;
    private double defaultOreReward = 0.0;

    public void load(@Nonnull Path dataDir) {
        loadDefaults();
        File file = dataDir.resolve(FILE_NAME).toFile();
        if (!file.exists()) {
            save(file);
            LOGGER.at(Level.INFO).log("Created default essence_rewards.toml");
            return;
        }
        try {
            Toml toml = new Toml().read(file);

            Toml general = toml.getTable("general");
            if (general != null) {
                globalMultiplier = general.getDouble("globalMultiplier", globalMultiplier);
                defaultMobReward = general.getDouble("defaultMobReward", defaultMobReward);
                defaultOreReward = general.getDouble("defaultOreReward", defaultOreReward);
            }

            Toml ores = toml.getTable("ores");
            if (ores != null) {
                for (Map.Entry<String, Object> entry : ores.entrySet()) {
                    if (entry.getValue() instanceof Number num) {
                        oreRewards.put(entry.getKey().toLowerCase(), num.doubleValue());
                    }
                }
            }

            Toml mobs = toml.getTable("mobs");
            if (mobs != null) {
                for (Map.Entry<String, Object> entry : mobs.entrySet()) {
                    if (entry.getValue() instanceof Number num) {
                        mobRewards.put(entry.getKey().toLowerCase(), num.doubleValue());
                    }
                }
            }

            LOGGER.at(Level.INFO).log("Loaded essence rewards: " + oreRewards.size() + " ores, " + mobRewards.size() + " mobs, multiplier=" + globalMultiplier);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to load essence_rewards.toml: " + e.getMessage());
        }
    }

    public double getOreReward(@Nonnull String blockId) {
        String id = blockId.toLowerCase();
        Double exact = oreRewards.get(id);
        if (exact != null) return exact * globalMultiplier;
        for (Map.Entry<String, Double> entry : oreRewards.entrySet()) {
            if (id.contains(entry.getKey())) return entry.getValue() * globalMultiplier;
        }
        return defaultOreReward * globalMultiplier;
    }

    public double getMobReward(@Nonnull String mobId) {
        String id = mobId.toLowerCase();
        Double exact = mobRewards.get(id);
        if (exact != null) return exact * globalMultiplier;
        for (Map.Entry<String, Double> entry : mobRewards.entrySet()) {
            if (id.contains(entry.getKey())) return entry.getValue() * globalMultiplier;
        }
        return defaultMobReward * globalMultiplier;
    }

    private void loadDefaults() {
        oreRewards.clear();
        mobRewards.clear();

        globalMultiplier = 0.1;
        defaultMobReward = 0.0;
        defaultOreReward = 0.0;

        oreRewards.put("ore_copper", 2.0);
        oreRewards.put("ore_iron", 3.0);
        oreRewards.put("ore_silver", 4.0);
        oreRewards.put("ore_gold", 5.0);
        oreRewards.put("ore_cobalt", 5.0);
        oreRewards.put("ore_mithril", 7.0);
        oreRewards.put("ore_adamantite", 10.0);
        oreRewards.put("ore_thorium", 13.0);
        oreRewards.put("ore_onyxium", 15.0);
        oreRewards.put("ore_prisma", 15.0);

        oreRewards.put("rock_gem_topaz", 18.0);
        oreRewards.put("rock_gem_emerald", 20.0);
        oreRewards.put("rock_gem_ruby", 20.0);
        oreRewards.put("rock_gem_sapphire", 20.0);
        oreRewards.put("rock_gem_diamond", 25.0);
        oreRewards.put("rock_gem_zephyr", 25.0);
        oreRewards.put("rock_gem_voidstone", 30.0);
        oreRewards.put("rock_crystal", 4.0);

        // Critters (5 XP) -> 1 point
        mobRewards.put("frog", 1.0);
        mobRewards.put("mouse", 1.0);
        mobRewards.put("squirrel", 1.0);
        mobRewards.put("gecko", 1.0);
        mobRewards.put("hatworm", 1.0);
        mobRewards.put("bat", 1.0);
        mobRewards.put("sparrow", 1.0);
        mobRewards.put("bluebird", 1.0);
        mobRewards.put("finch", 1.0);
        mobRewards.put("woodpecker", 1.0);
        mobRewards.put("pigeon", 1.0);

        // Petits (8 XP) -> 2 points
        mobRewards.put("meerkat", 2.0);
        mobRewards.put("larva_silk", 2.0);
        mobRewards.put("chicken", 2.0);
        mobRewards.put("turkey", 2.0);
        mobRewards.put("skrill", 2.0);
        mobRewards.put("bunny", 2.0);
        mobRewards.put("crow", 2.0);
        mobRewards.put("duck", 2.0);
        mobRewards.put("rat", 2.0);

        // Moyens (10-12 XP) -> 2 points
        mobRewards.put("owl", 2.0);
        mobRewards.put("pig", 2.0);
        mobRewards.put("rabbit", 2.0);
        mobRewards.put("salmon", 2.0);
        mobRewards.put("fox", 2.0);

        // Notables (12-20 XP) -> 3 points
        mobRewards.put("kweebec", 3.0);
        mobRewards.put("feran", 3.0);
        mobRewards.put("mouflon", 3.0);
        mobRewards.put("boar", 3.0);
        mobRewards.put("cow", 3.0);
        mobRewards.put("penguin", 3.0);
        mobRewards.put("flamingo", 3.0);
        mobRewards.put("crab", 3.0);
        mobRewards.put("hawk", 3.0);
        mobRewards.put("parrot", 3.0);

        // Hostiles faibles (20-35 XP) -> 4 points
        mobRewards.put("snake", 4.0);
        mobRewards.put("snail", 4.0);
        mobRewards.put("cactee", 4.0);
        mobRewards.put("spark_living", 4.0);
        mobRewards.put("tortoise", 4.0);
        mobRewards.put("scarab", 4.0);
        mobRewards.put("molerat", 4.0);

        // Hostiles moyens (40-55 XP) -> 6 points
        mobRewards.put("spider", 6.0);
        mobRewards.put("fen_stalker", 6.0);
        mobRewards.put("chicken_undead", 6.0);
        mobRewards.put("frostgill", 6.0);
        mobRewards.put("snapjaw", 6.0);
        mobRewards.put("trilobite", 6.0);

        // Hostiles (55-65 XP) -> 8 points
        mobRewards.put("wolf", 8.0);
        mobRewards.put("hyena", 8.0);
        mobRewards.put("leopard", 8.0);
        mobRewards.put("snapdragon", 8.0);
        mobRewards.put("moose", 8.0);
        mobRewards.put("goblin", 8.0);
        mobRewards.put("klops", 8.0);
        mobRewards.put("slug_magma", 8.0);
        mobRewards.put("armadillo", 8.0);

        // Hostiles forts (65-85 XP) -> 11 points
        mobRewards.put("trork", 11.0);
        mobRewards.put("skeleton", 11.0);
        mobRewards.put("zombie", 11.0);
        mobRewards.put("scarak", 11.0);
        mobRewards.put("bramblekin", 11.0);
        mobRewards.put("spirit", 11.0);
        mobRewards.put("outlander", 11.0);

        // Elites (85 XP) -> 13 points
        mobRewards.put("bear", 13.0);
        mobRewards.put("raptor", 13.0);
        mobRewards.put("tiger", 13.0);
        mobRewards.put("crocodile", 13.0);
        mobRewards.put("scorpion", 13.0);
        mobRewards.put("toad_rhino", 13.0);
        mobRewards.put("mosshorn", 13.0);
        mobRewards.put("goblin_ogre", 13.0);
        mobRewards.put("skeleton_burnt", 13.0);
        mobRewards.put("zombie_burnt", 13.0);
        mobRewards.put("hound_bleached", 13.0);
        mobRewards.put("cow_undead", 13.0);
        mobRewards.put("pig_undead", 13.0);
        mobRewards.put("shark", 13.0);
        mobRewards.put("whale", 13.0);
        mobRewards.put("scarak_broodmother", 13.0);
        mobRewards.put("trillodon", 13.0);

        // Mini-boss (130 XP) -> 20 points
        mobRewards.put("emberwulf", 20.0);
        mobRewards.put("golem", 20.0);
        mobRewards.put("ghoul", 20.0);
        mobRewards.put("wraith", 20.0);
        mobRewards.put("slothian", 20.0);
        mobRewards.put("spirit_thunder", 20.0);
        mobRewards.put("rex_cave", 20.0);

        // Boss (175 XP) -> 30 points
        mobRewards.put("yeti", 30.0);
        mobRewards.put("werewolf", 30.0);
        mobRewards.put("hedera", 30.0);

        // Boss haut (250 XP) -> 40 points
        mobRewards.put("void", 40.0);
        mobRewards.put("shadow_knight", 40.0);
        mobRewards.put("zombie_aberrant", 40.0);

        // Boss elite (350-400 XP) -> 60 points
        mobRewards.put("goblin_duke", 60.0);
        mobRewards.put("golem_guardian", 60.0);
        mobRewards.put("elite", 60.0);

        // Boss final (500 XP) -> 80 points
        mobRewards.put("dragon", 80.0);
        mobRewards.put("boss", 80.0);
    }

    private void save(@Nonnull File file) {
        try {
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);

            writer.write("[general]\n");
            writer.write("globalMultiplier = " + globalMultiplier + "\n");
            writer.write("defaultMobReward = " + formatVal(defaultMobReward) + "\n");
            writer.write("defaultOreReward = " + formatVal(defaultOreReward) + "\n\n");

            writer.write("[ores]\n");
            oreRewards.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    try { writer.write(e.getKey() + " = " + formatVal(e.getValue()) + "\n"); } catch (Exception ex) {}
                });

            writer.write("\n[mobs]\n");
            mobRewards.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    try { writer.write(e.getKey() + " = " + formatVal(e.getValue()) + "\n"); } catch (Exception ex) {}
                });

            writer.flush();
            writer.close();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to save essence_rewards.toml: " + e.getMessage());
        }
    }

    @Nonnull
    private String formatVal(double val) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.valueOf(val);
    }
}
