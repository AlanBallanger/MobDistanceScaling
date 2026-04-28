package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MobFragmentsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME         = "key_fragment_rates.toml";
    private static final String LEGACY_FILENAME  = "mob_special_rates.toml";
    private static final String SECTION_MOBS     = "mob_special_rates";
    private static final String SECTION_MINING   = "mining";

    private final Map<String, Integer> fragmentsByMobId;
    private final Map<String, Double> miningFragments;

    public MobFragmentsConfig(@Nonnull Map<String, Integer> fragmentsByMobId,
                              @Nonnull Map<String, Double> miningFragments) {
        this.fragmentsByMobId = new HashMap<>(fragmentsByMobId);
        this.miningFragments  = new LinkedHashMap<>(miningFragments);
    }

    public int getFragments(@Nonnull String roleName) {
        String id = roleName.toLowerCase(Locale.ROOT);
        Integer exact = fragmentsByMobId.get(id);
        if (exact != null) return exact;
        String bestKey = null;
        for (String key : fragmentsByMobId.keySet()) {
            if (id.startsWith(key + "_")) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey != null ? fragmentsByMobId.get(bestKey) : -1;
    }

    /**
     * Expected fragments per break for this block (config value; can be fractional).
     */
    public double getMiningFragmentWeight(@Nonnull String blockId) {
        Double w = resolveMiningWeight(blockId.toLowerCase(Locale.ROOT));
        return w != null && w > 0 ? w : 0.0;
    }

    /**
     * Stochastic drop count: integer part always drops; fractional part is an extra +1 with that probability
     * (e.g. 0.5 → 50% one fragment, 0.2 → 20% one fragment, 1.5 → one + 50% second).
     */
    public int rollMiningFragmentDrops(@Nonnull String blockId) {
        Double wObj = resolveMiningWeight(blockId.toLowerCase(Locale.ROOT));
        if (wObj == null || wObj <= 0) {
            return 0;
        }
        double w = wObj;
        int base = (int) Math.floor(w);
        double frac = w - base;
        int extra = (frac > 0.0 && ThreadLocalRandom.current().nextDouble() < frac) ? 1 : 0;
        return base + extra;
    }

    @Nullable
    private Double resolveMiningWeight(@Nonnull String id) {
        Double exact = miningFragments.get(id);
        if (exact != null) {
            return exact;
        }
        String bestKey = null;
        for (String key : miningFragments.keySet()) {
            if (id.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey != null ? miningFragments.get(bestKey) : null;
    }

    @Nonnull
    public static MobFragmentsConfig load(@Nonnull Path dataFolder) {
        java.nio.file.Path newPath = dataFolder.resolve(FILENAME);
        java.nio.file.Path legacyPath = dataFolder.resolve(LEGACY_FILENAME);
        if (!java.nio.file.Files.exists(newPath) && java.nio.file.Files.exists(legacyPath)) {
            try {
                java.nio.file.Files.move(legacyPath, newPath);
                LOGGER.at(Level.INFO).log("Renamed {0} to {1}", LEGACY_FILENAME, FILENAME);
            } catch (java.io.IOException e) {
                LOGGER.at(Level.WARNING).log("Could not rename " + LEGACY_FILENAME + " to " + FILENAME + ": " + e.getMessage());
            }
        }
        File file = newPath.toFile();
        boolean loadedFromLegacy = false;
        if (!file.exists() && legacyPath.toFile().exists()) {
            file = legacyPath.toFile();
            loadedFromLegacy = true;
        }
        if (!file.exists()) {
            MobFragmentsConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);

            // Mobs
            Map<String, Integer> mobs = new HashMap<>();
            Toml mobSection = toml.getTable(SECTION_MOBS);
            if (mobSection != null) {
                for (Map.Entry<String, Object> e : mobSection.toMap().entrySet()) {
                    if (e.getValue() instanceof Number n)
                        mobs.put(e.getKey().toLowerCase(Locale.ROOT), n.intValue());
                }
            }

            // Mining (values may be fractional, e.g. 0.5 = 50% chance of 1 fragment)
            Map<String, Double> mining = new LinkedHashMap<>();
            Toml mineSection = toml.getTable(SECTION_MINING);
            if (mineSection != null) {
                for (Map.Entry<String, Object> e : mineSection.toMap().entrySet()) {
                    if (e.getValue() instanceof Number n) {
                        mining.put(e.getKey().toLowerCase(Locale.ROOT), n.doubleValue());
                    }
                }
            }

            LOGGER.at(Level.INFO).log("Loaded {0}: {1} mobs, {2} mining entries", file.getName(), mobs.size(), mining.size());
            MobFragmentsConfig cfg = new MobFragmentsConfig(mobs, mining);
            if (loadedFromLegacy) {
                cfg.save(dataFolder);
                try {
                    java.nio.file.Files.deleteIfExists(legacyPath);
                } catch (java.io.IOException ignored) {
                }
            }
            return cfg;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + file.getName() + ", using defaults", e);
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
        sb.append("# Mob Key Fragment Drops\n");
        sb.append("# Prefix matching : Wolf = Wolf_Black, Wolf_White... (plus long gagne)\n");
        sb.append("# 0 = passif  |  paliers hostiles : 1,1,2,4,5,8,12,20,25,50\n\n");
        sb.append("[").append(SECTION_MOBS).append("]\n\n");

        sb.append("# --- Passifs (0 frags) ---\n");
        appendGroup(sb, PASSIVE, 0);

        sb.append("\n# --- 1 frag : XP 8-20 (Rat, Scarab, Vulture...) ---\n");
        appendGroup(sb, F1, 1);

        sb.append("\n# --- 1 frag : XP 35 (Snake, Snail, Cactee...) ---\n");
        appendGroup(sb, F2, 1);

        sb.append("\n# --- 2 frags : XP 45 (Fen_Stalker, Chicken_Undead, Crawler_Void) ---\n");
        appendGroup(sb, F3, 2);

        sb.append("\n# --- 4 frags : XP 55 (Goblin, Hyena, Spider, Scarak...) ---\n");
        appendGroup(sb, F5, 4);

        sb.append("\n# --- 5 frags : XP 65 (Zombie, Bear, Tiger...) ---\n");
        appendGroup(sb, F7, 5);

        sb.append("\n# --- 8 frags : XP 85 (Raptor, Outlander... + surcharges) ---\n");
        appendGroup(sb, F10, 8);

        sb.append("\n# --- 12 frags : XP 130 (Yeti, Emberwulf, Golem, Wraith...) ---\n");
        appendGroup(sb, F15, 12);

        sb.append("\n# --- 20 frags : XP 175 (Werewolf, Hedera, Rex_Cave) ---\n");
        appendGroup(sb, F22, 20);

        sb.append("\n# --- 25 frags : XP 250 (Void, Shadow_Knight, Zombie_Aberrant) ---\n");
        appendGroup(sb, F30, 25);

        sb.append("\n# --- 50 frags : XP 350-500 (Dragon, Goblin_Duke, Golem_Guardian...) ---\n");
        appendGroup(sb, F50, 50);

        sb.append("\n# ============================================================\n");
        sb.append("# Mining fragments : block_id = expected fragments per break (can be decimal)\n");
        sb.append("# Decimal part = probability of one extra fragment (0.5 → 50% × 1 frag, 0.2 → 20% × 1)\n");
        sb.append("# Prefix matching : ore_adamantite_magma → ore_adamantite\n");
        sb.append("# ============================================================\n");
        sb.append("[").append(SECTION_MINING).append("]\n\n");
        sb.append("# --- Tier 1 (1 fragment) ---\n");
        sb.append("ore_copper = 1\n");
        sb.append("ore_iron   = 1\n");
        sb.append("ore_silver = 1\n");
        sb.append("ore_gold   = 1\n");
        sb.append("\n# --- Tier 2 (1 fragment) ---\n");
        sb.append("ore_cobalt  = 1\n");
        sb.append("ore_thorium = 1\n");
        sb.append("\n# --- Tier 3 (1 fragment) ---\n");
        sb.append("ore_adamantite = 1\n");
        sb.append("ore_mithril    = 1\n");
        sb.append("ore_onyxium    = 1\n");
        sb.append("ore_prisma     = 1\n");
        sb.append("\n# --- Rock crystals ---\n");
        sb.append("rock_crystal_blue   = 0.3\n");
        sb.append("rock_crystal_yellow = 0.3\n");
        sb.append("rock_crystal_red    = 0.3\n");
        sb.append("rock_crystal_cyan   = 0.3\n");
        sb.append("rock_crystal_purple = 0.5\n");
        sb.append("rock_crystal_white  = 1\n");
        sb.append("rock_crystal_green  = 0.5\n");
        sb.append("rock_crystal_pink   = 0.3\n");
        sb.append("\n# --- Gemmes ---\n");
        sb.append("rock_gem_diamond   = 20\n");
        sb.append("rock_gem_emerald   = 8\n");
        sb.append("rock_gem_ruby      = 12\n");
        sb.append("rock_gem_sapphire  = 12\n");
        sb.append("rock_gem_topaz     = 25\n");
        sb.append("rock_gem_voidstone = 4\n");
        sb.append("rock_gem_zephyr    = 25\n");

        return sb.toString();
    }

    private void appendGroup(StringBuilder sb, List<String> ids, int fragments) {
        for (String id : ids) {
            sb.append(id).append(" = ").append(fragments).append("\n");
        }
    }

    @Nonnull
    public static MobFragmentsConfig createDefault() {
        Map<String, Integer> mobs = new HashMap<>();
        for (String id : PASSIVE) { mobs.put(id.toLowerCase(Locale.ROOT),  0); }
        for (String id : F1)      { mobs.put(id.toLowerCase(Locale.ROOT),  1); }
        for (String id : F2)      { mobs.put(id.toLowerCase(Locale.ROOT),  1); }
        for (String id : F3)      { mobs.put(id.toLowerCase(Locale.ROOT),  2); }
        for (String id : F5)      { mobs.put(id.toLowerCase(Locale.ROOT),  4); }
        for (String id : F7)      { mobs.put(id.toLowerCase(Locale.ROOT),  5); }
        for (String id : F10)     { mobs.put(id.toLowerCase(Locale.ROOT),  8); }
        for (String id : F15)     { mobs.put(id.toLowerCase(Locale.ROOT), 12); }
        for (String id : F22)     { mobs.put(id.toLowerCase(Locale.ROOT), 20); }
        for (String id : F30)     { mobs.put(id.toLowerCase(Locale.ROOT), 25); }
        for (String id : F50)     { mobs.put(id.toLowerCase(Locale.ROOT), 50); }

        Map<String, Double> mining = new LinkedHashMap<>();
        mining.put("ore_copper",          1.0);
        mining.put("ore_iron",            1.0);
        mining.put("ore_silver",          1.0);
        mining.put("ore_gold",            1.0);
        mining.put("ore_cobalt",          1.0);
        mining.put("ore_thorium",         1.0);
        mining.put("ore_adamantite",      1.0);
        mining.put("ore_mithril",         1.0);
        mining.put("ore_onyxium",         1.0);
        mining.put("ore_prisma",          1.0);
        mining.put("rock_crystal_blue",   0.3);
        mining.put("rock_crystal_yellow", 0.3);
        mining.put("rock_crystal_red",    0.3);
        mining.put("rock_crystal_cyan",   0.3);
        mining.put("rock_crystal_purple", 0.5);
        mining.put("rock_crystal_white",  1.0);
        mining.put("rock_crystal_green",  0.5);
        mining.put("rock_crystal_pink",   0.3);
        mining.put("rock_gem_diamond",   20.0);
        mining.put("rock_gem_emerald",    8.0);
        mining.put("rock_gem_ruby",      12.0);
        mining.put("rock_gem_sapphire",  12.0);
        mining.put("rock_gem_topaz",     25.0);
        mining.put("rock_gem_voidstone",  4.0);
        mining.put("rock_gem_zephyr",    25.0);

        return new MobFragmentsConfig(mobs, mining);
    }

    // -------------------------------------------------------------------------
    // Fragment values per mob  (prefix matching — plus long gagne sur le nom de base)
    // -------------------------------------------------------------------------

    /** 0 frags — passifs + overrides passifs (Moose_Cow/Bull > Moose=5) */
    private static final List<String> PASSIVE = List.of(
        "Squirrel", "Frog", "Gecko", "Mouse", "Bat", "Hatworm",
        "Sparrow", "Bluebird", "Finch", "Woodpecker", "Pigeon",
        "Meerkat", "Turkey", "Chicken", "Crow", "Duck", "Bunny", "Skrill", "Rabbit",
        "Pig", "Owl", "Parrot", "Flamingo", "Penguin", "Fox", "Hawk", "Tetrabird",
        "Goat", "Sheep", "Raven",
        "Cow", "Deer", "Antelope", "Camel", "Warthog", "Horse", "Ram",
        "Bison", "Boar", "Mouflon", "Feran", "Kweebec",
        "Archaeopteryx", "Pterodactyl",
        "Mannequin",
        "Moose_Cow", "Moose_Bull"
    );

    /** 1 frag — XP 8-20 */
    private static final List<String> F1 = List.of(
        "Rat", "Larva_Silk", "Larva_Void", "Molerat", "Scarab", "Vulture"
    );

    /** 1 frag — XP 35 */
    private static final List<String> F2 = List.of(
        "Snail", "Snake", "Cactee", "Spark_Living", "Tortoise"
    );

    /** 2 frags — XP 45 */
    private static final List<String> F3 = List.of(
        "Fen_Stalker", "Chicken_Undead", "Crawler_Void",
        "Pig_Undead"
    );

    /** 4 frags — XP 55 */
    private static final List<String> F5 = List.of(
        "Goblin", "Hyena", "Leopard", "Snapdragon", "Moose",
        "Klops", "Slug_Magma", "Armadillo", "Lizard_Sand", "Eye_Void",
        "Tuluk_Fisherman", "Tuluk_King", "Tuluk_Merchant", "Tuluk_Pink",
        "Grung_Hopling", "Grung_Hopling_Archer", "Grung_Hopling_Settled",
        "Slothian_Monk", "Slothian_Scout", "Slothian_Warrior",
        "Bramblekin", "Bramblekin_Berserker", "Bramblekin_Fighter", "Bramblekin_Shaman",
        "Saurian_Hunter", "Saurian_Rogue", "Saurian_Warrior",
        "Scarak", "Spider",
        "Skeleton", "Wolf", "Trork",
        "Mosshorn", "Cow_Undead"
    );

    /** 5 frags — XP 65 */
    private static final List<String> F7 = List.of(
        "Zombie", "Spirit", "Spectre_Void",
        "Bear", "Tiger"
    );

    /** 8 frags — XP 85 + surcharges cross-prefix */
    private static final List<String> F10 = List.of(
        "Raptor", "Crocodile", "Scorpion",
        "Toad_Rhino", "Hound_Bleached",
        "Trillodon", "Outlander", "Spawn_Void", "Grung_Elder",
        "Endgame_Saurian_Hunter", "Endgame_Saurian_Rogue", "Endgame_Saurian_Warrior",
        "Goblin_Ogre",       // goblin prefix = 5
        "Skeleton_Burnt",    // skeleton prefix = 7
        "Zombie_Burnt",      // zombie prefix = 7
        "Scarak_Broodmother" // scarak prefix = 5 (hors Broodmother)
    );

    /** 12 frags — XP 130 */
    private static final List<String> F15 = List.of(
        "Yeti",
        "Emberwulf", "Golem", "Slothian", "Ghoul", "Wraith",
        "Spirit_Thunder"     // spirit prefix = 7
    );

    /** 20 frags — XP 175 */
    private static final List<String> F22 = List.of(
        "Rex_Cave", "Werewolf", "Hedera"
    );

    /** 25 frags — XP 250 */
    private static final List<String> F30 = List.of(
        "Void", "Shadow_Knight",
        "Zombie_Aberrant"    // zombie prefix = 7
    );

    /** 50 frags — XP 350-500 */
    private static final List<String> F50 = List.of(
        "Dragon", "Boss", "Elite",
        "Goblin_Duke",       // goblin prefix = 5
        "Golem_Guardian"     // golem prefix = 15
    );
}
