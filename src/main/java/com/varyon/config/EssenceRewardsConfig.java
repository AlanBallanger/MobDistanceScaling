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
    private double pvpEssenceMultiplier = 2.0;
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
                pvpEssenceMultiplier = general.getDouble("pvpEssenceMultiplier", pvpEssenceMultiplier);
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

    public double getPvpEssenceMultiplier() {
        return pvpEssenceMultiplier;
    }

    public double getOreReward(@Nonnull String blockId) {
        String id = blockId.toLowerCase();
        Double exact = oreRewards.get(id);
        if (exact != null) return exact * globalMultiplier;
        String bestKey = null;
        double bestVal = 0;
        for (Map.Entry<String, Double> entry : oreRewards.entrySet()) {
            if (id.contains(entry.getKey())) {
                if (bestKey == null || entry.getKey().length() > bestKey.length()) {
                    bestKey = entry.getKey();
                    bestVal = entry.getValue();
                }
            }
        }
        if (bestKey != null) return bestVal * globalMultiplier;
        return defaultOreReward * globalMultiplier;
    }

    public double getMobReward(@Nonnull String mobId) {
        String id = mobId.toLowerCase();
        Double exact = mobRewards.get(id);
        if (exact != null) return exact * globalMultiplier;
        String bestKey = null;
        double bestVal = 0;
        for (Map.Entry<String, Double> entry : mobRewards.entrySet()) {
            if (id.contains(entry.getKey())) {
                if (bestKey == null || entry.getKey().length() > bestKey.length()) {
                    bestKey = entry.getKey();
                    bestVal = entry.getValue();
                }
            }
        }
        if (bestKey != null) return bestVal * globalMultiplier;
        return defaultMobReward * globalMultiplier;
    }

    private void loadDefaults() {
        oreRewards.clear();
        mobRewards.clear();

        globalMultiplier = 0.1;
        pvpEssenceMultiplier = 2.0;
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

        // Critters (5 XP) -> 1 essence
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

        // Petits (8 XP) -> 1 essence
        mobRewards.put("meerkat", 1.5);
        mobRewards.put("larva_silk", 1.5);
        mobRewards.put("chicken", 1.5);
        mobRewards.put("turkey", 1.5);
        mobRewards.put("skrill", 1.5);
        mobRewards.put("bunny", 1.5);
        mobRewards.put("crow", 1.5);
        mobRewards.put("duck", 1.5);
        mobRewards.put("rat", 1.5);

        // Moyens (10-12 XP) -> 1 essence
        mobRewards.put("owl", 2.5);
        mobRewards.put("pig", 2.5);
        mobRewards.put("rabbit", 2.5);
        mobRewards.put("salmon", 2.5);
        mobRewards.put("fox", 2.5);

        // Notables (12-20 XP) -> 1 essence
        mobRewards.put("kweebec", 3.5);
        mobRewards.put("feran", 3.5);
        mobRewards.put("mouflon", 3.5);
        mobRewards.put("boar", 3.5);
        mobRewards.put("cow", 3.5);
        mobRewards.put("penguin", 3.5);
        mobRewards.put("flamingo", 3.5);
        mobRewards.put("crab", 3.5);
        mobRewards.put("hawk", 3.5);
        mobRewards.put("parrot", 3.5);
        mobRewards.put("goat", 2.5);
        mobRewards.put("sheep", 2.5);
        mobRewards.put("tetrabird", 2.5);
        mobRewards.put("raven", 3.5);
        mobRewards.put("deer", 3.5);
        mobRewards.put("antelope", 3.5);
        mobRewards.put("camel", 3.5);
        mobRewards.put("warthog", 3.5);
        mobRewards.put("horse", 3.5);
        mobRewards.put("ram", 3.5);
        mobRewards.put("bison", 4.0);
        mobRewards.put("archaeopteryx", 4.0);
        mobRewards.put("pterodactyl", 4.0);

        // Hostiles faibles (20-35 XP) -> 1 essence
        mobRewards.put("snake", 6.5);
        mobRewards.put("snail", 6.5);
        mobRewards.put("cactee", 6.5);
        mobRewards.put("spark_living", 6.5);
        mobRewards.put("tortoise", 6.5);
        mobRewards.put("scarab", 6.5);
        mobRewards.put("molerat", 6.5);
        mobRewards.put("vulture", 6.5);

        // Hostiles moyens (40-55 XP) -> 2 essence
        mobRewards.put("spider", 12.5);
        mobRewards.put("fen_stalker", 12.5);
        mobRewards.put("chicken_undead", 12.5);
        mobRewards.put("frostgill", 12.5);
        mobRewards.put("snapjaw", 12.5);
        mobRewards.put("trilobite", 12.5);

        // Hostiles (55-65 XP) -> 2 essence
        mobRewards.put("wolf", 17.5);
        mobRewards.put("hyena", 17.5);
        mobRewards.put("leopard", 17.5);
        mobRewards.put("snapdragon", 17.5);
        mobRewards.put("moose", 17.5);
        mobRewards.put("goblin", 17.5);
        mobRewards.put("klops", 17.5);
        mobRewards.put("slug_magma", 17.5);
        mobRewards.put("armadillo", 17.5);
        mobRewards.put("lizard_sand", 17.5);
        mobRewards.put("pig_undead", 25.0);

        // Hostiles forts (65-85 XP) -> 3 essence
        mobRewards.put("trork", 25.0);
        mobRewards.put("skeleton", 25.0);
        mobRewards.put("zombie", 25.0);
        mobRewards.put("scarak", 25.0);
        mobRewards.put("bramblekin", 25.0);
        mobRewards.put("spirit", 25.0);
        mobRewards.put("outlander", 25.0);

        // Elites (85 XP) -> 4 essence
        mobRewards.put("bear", 37.5);
        mobRewards.put("raptor", 37.5);
        mobRewards.put("tiger", 37.5);
        mobRewards.put("crocodile", 37.5);
        mobRewards.put("scorpion", 37.5);
        mobRewards.put("toad_rhino", 37.5);
        mobRewards.put("mosshorn", 37.5);
        mobRewards.put("goblin_ogre", 37.5);
        mobRewards.put("skeleton_burnt", 37.5);
        mobRewards.put("zombie_burnt", 37.5);
        mobRewards.put("hound_bleached", 37.5);
        mobRewards.put("cow_undead", 37.5);
        mobRewards.put("pig_undead", 37.5);
        mobRewards.put("shark", 37.5);
        mobRewards.put("whale", 37.5);
        mobRewards.put("scarak_broodmother", 37.5);
        mobRewards.put("trillodon", 37.5);

        // Mini-boss (130 XP) -> 7 essence
        mobRewards.put("emberwulf", 70.0);
        mobRewards.put("golem", 70.0);
        mobRewards.put("ghoul", 70.0);
        mobRewards.put("wraith", 70.0);
        mobRewards.put("slothian", 70.0);
        mobRewards.put("spirit_thunder", 70.0);

        // Boss (175 XP) -> 12 essence
        mobRewards.put("rex_cave", 120.0);
        mobRewards.put("yeti", 120.0);
        mobRewards.put("werewolf", 120.0);
        mobRewards.put("hedera", 120.0);

        // Boss haut (250 XP) -> 5 essence
        mobRewards.put("void", 50.0);
        mobRewards.put("shadow_knight", 50.0);
        mobRewards.put("zombie_aberrant", 50.0);
        mobRewards.put("crawler_void", 15.0);
        mobRewards.put("eye_void", 25.0);
        mobRewards.put("spawn_void", 35.0);
        mobRewards.put("spectre_void", 30.0);

        // Boss elite (350-400 XP) -> 35 essence
        mobRewards.put("goblin_duke", 350.0);
        mobRewards.put("golem_guardian", 350.0);
        mobRewards.put("elite", 350.0);

        // Boss final (500 XP) -> 50 essence
        mobRewards.put("dragon", 500.0);
        mobRewards.put("boss", 500.0);
    }

    private void save(@Nonnull File file) {
        try {
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);

            writer.write("[general]\n");
            writer.write("globalMultiplier = " + globalMultiplier + "\n");
            writer.write("pvpEssenceMultiplier = " + formatVal(pvpEssenceMultiplier) + "\n");
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
