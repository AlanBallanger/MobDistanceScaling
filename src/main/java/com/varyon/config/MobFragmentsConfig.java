package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.logging.Level;

public class MobFragmentsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME         = "mob_special_rates.toml";
    private static final String SECTION_MOBS     = "mob_special_rates";
    private static final String SECTION_MINING   = "mining";

    private final Map<String, Integer> fragmentsByMobId;
    private final Map<String, Integer> miningFragments;

    public MobFragmentsConfig(@Nonnull Map<String, Integer> fragmentsByMobId,
                              @Nonnull Map<String, Integer> miningFragments) {
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
     * Returns mining fragment count for a block ID.
     * Tries exact match first, then prefix match for biome variants
     * (e.g. "ore_adamantite_magma" → "ore_adamantite").
     */
    public int getMiningFragments(@Nonnull String blockId) {
        String id = blockId.toLowerCase();
        Integer exact = miningFragments.get(id);
        if (exact != null) return exact;
        for (Map.Entry<String, Integer> entry : miningFragments.entrySet()) {
            if (id.startsWith(entry.getKey())) return entry.getValue();
        }
        return 0;
    }

    @Nonnull
    public static MobFragmentsConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
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

            // Mining
            Map<String, Integer> mining = new LinkedHashMap<>();
            Toml mineSection = toml.getTable(SECTION_MINING);
            if (mineSection != null) {
                for (Map.Entry<String, Object> e : mineSection.toMap().entrySet()) {
                    if (e.getValue() instanceof Number n)
                        mining.put(e.getKey().toLowerCase(Locale.ROOT), n.intValue());
                }
            }

            LOGGER.at(Level.INFO).log("Loaded {0}: {1} mobs, {2} mining entries", FILENAME, mobs.size(), mining.size());
            return new MobFragmentsConfig(mobs, mining);
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
        sb.append("# Mob Key Fragment Drops\n");
        sb.append("# Prefix matching : Wolf = Wolf_Black, Wolf_White... (plus long gagne)\n");
        sb.append("# 0 = passif  |  1-50 = hostile selon XP\n\n");
        sb.append("[").append(SECTION_MOBS).append("]\n\n");

        sb.append("# --- Passifs (0 frags) ---\n");
        appendGroup(sb, PASSIVE, 0);

        sb.append("\n# --- 1 frag : XP 8-20 (Rat, Scarab, Vulture...) ---\n");
        appendGroup(sb, F1, 1);

        sb.append("\n# --- 2 frags : XP 35 (Snake, Snail, Cactee...) ---\n");
        appendGroup(sb, F2, 2);

        sb.append("\n# --- 3 frags : XP 45 (Spider, Fen_Stalker, Chicken_Undead) ---\n");
        appendGroup(sb, F3, 3);

        sb.append("\n# --- 5 frags : XP 55 (Goblin, Hyena, Leopard...) ---\n");
        appendGroup(sb, F5, 5);

        sb.append("\n# --- 7 frags : XP 65 (Zombie, Skeleton, Wolf...) ---\n");
        appendGroup(sb, F7, 7);

        sb.append("\n# --- 10 frags : XP 85 (Bear, Tiger, Outlander... + surcharges) ---\n");
        appendGroup(sb, F10, 10);

        sb.append("\n# --- 15 frags : XP 130 (Emberwulf, Golem, Wraith...) ---\n");
        appendGroup(sb, F15, 15);

        sb.append("\n# --- 22 frags : XP 175 (Yeti, Werewolf, Hedera, Rex_Cave) ---\n");
        appendGroup(sb, F22, 22);

        sb.append("\n# --- 30 frags : XP 250 (Void, Shadow_Knight, Zombie_Aberrant) ---\n");
        appendGroup(sb, F30, 30);

        sb.append("\n# --- 50 frags : XP 350-500 (Dragon, Goblin_Duke, Golem_Guardian...) ---\n");
        appendGroup(sb, F50, 50);

        sb.append("\n# ============================================================\n");
        sb.append("# Mining fragments : block_id = fragments\n");
        sb.append("# Prefix matching : ore_adamantite_magma → ore_adamantite\n");
        sb.append("# ============================================================\n");
        sb.append("[").append(SECTION_MINING).append("]\n\n");
        sb.append("# --- Tier 1 (1 fragment) ---\n");
        sb.append("ore_copper = 1\n");
        sb.append("ore_iron   = 1\n");
        sb.append("ore_silver = 1\n");
        sb.append("ore_gold   = 1\n");
        sb.append("\n# --- Tier 2 (2 fragments) ---\n");
        sb.append("ore_cobalt  = 2\n");
        sb.append("ore_thorium = 2\n");
        sb.append("\n# --- Tier 3 (2 fragments) ---\n");
        sb.append("ore_adamantite = 2\n");
        sb.append("ore_mithril    = 2\n");
        sb.append("ore_onyxium    = 2\n");
        sb.append("ore_prisma     = 2\n");
        sb.append("rock_crystal   = 2\n");
        sb.append("\n# --- Gemmes ---\n");
        sb.append("rock_gem_diamond   = 20\n");
        sb.append("rock_gem_emerald   = 10\n");
        sb.append("rock_gem_ruby      = 15\n");
        sb.append("rock_gem_sapphire  = 15\n");
        sb.append("rock_gem_topaz     = 25\n");
        sb.append("rock_gem_voidstone = 5\n");
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
        for (String id : F2)      { mobs.put(id.toLowerCase(Locale.ROOT),  2); }
        for (String id : F3)      { mobs.put(id.toLowerCase(Locale.ROOT),  3); }
        for (String id : F5)      { mobs.put(id.toLowerCase(Locale.ROOT),  5); }
        for (String id : F7)      { mobs.put(id.toLowerCase(Locale.ROOT),  7); }
        for (String id : F10)     { mobs.put(id.toLowerCase(Locale.ROOT), 10); }
        for (String id : F15)     { mobs.put(id.toLowerCase(Locale.ROOT), 15); }
        for (String id : F22)     { mobs.put(id.toLowerCase(Locale.ROOT), 22); }
        for (String id : F30)     { mobs.put(id.toLowerCase(Locale.ROOT), 30); }
        for (String id : F50)     { mobs.put(id.toLowerCase(Locale.ROOT), 50); }

        Map<String, Integer> mining = new LinkedHashMap<>();
        mining.put("ore_copper",          1);
        mining.put("ore_iron",            1);
        mining.put("ore_silver",          1);
        mining.put("ore_gold",            1);
        mining.put("ore_cobalt",          2);
        mining.put("ore_thorium",         2);
        mining.put("ore_adamantite",      2);
        mining.put("ore_mithril",         2);
        mining.put("ore_onyxium",         2);
        mining.put("ore_prisma",          2);
        mining.put("rock_crystal",        2);
        mining.put("rock_gem_diamond",   20);
        mining.put("rock_gem_emerald",   10);
        mining.put("rock_gem_ruby",      15);
        mining.put("rock_gem_sapphire",  15);
        mining.put("rock_gem_topaz",     25);
        mining.put("rock_gem_voidstone",  5);
        mining.put("rock_gem_zephyr",    25);

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
        "Rat", "Larva_Silk", "Molerat", "Scarab", "Vulture"
    );

    /** 2 frags — XP 35 */
    private static final List<String> F2 = List.of(
        "Snail", "Snake", "Cactee", "Spark_Living", "Tortoise"
    );

    /** 3 frags — XP 45 */
    private static final List<String> F3 = List.of(
        "Spider", "Fen_Stalker", "Chicken_Undead", "Crawler_Void"
    );

    /** 5 frags — XP 55 */
    private static final List<String> F5 = List.of(
        "Goblin", "Hyena", "Leopard", "Snapdragon", "Moose",
        "Klops", "Slug_Magma", "Armadillo", "Lizard_Sand", "Eye_Void"
    );

    /** 7 frags — XP 65 */
    private static final List<String> F7 = List.of(
        "Zombie", "Skeleton", "Wolf", "Trork", "Scarak",
        "Spirit", "Pig_Undead", "Bramblekin", "Spectre_Void"
    );

    /** 10 frags — XP 85 + surcharges cross-prefix */
    private static final List<String> F10 = List.of(
        "Bear", "Raptor", "Tiger", "Crocodile", "Scorpion",
        "Toad_Rhino", "Mosshorn", "Hound_Bleached", "Cow_Undead",
        "Trillodon", "Outlander", "Spawn_Void",
        "Goblin_Ogre",       // goblin prefix = 5
        "Skeleton_Burnt",    // skeleton prefix = 7
        "Zombie_Burnt",      // zombie prefix = 7
        "Scarak_Broodmother" // scarak prefix = 7
    );

    /** 15 frags — XP 130 */
    private static final List<String> F15 = List.of(
        "Emberwulf", "Golem", "Slothian", "Ghoul", "Wraith",
        "Spirit_Thunder"     // spirit prefix = 7
    );

    /** 22 frags — XP 175 */
    private static final List<String> F22 = List.of(
        "Rex_Cave", "Yeti", "Werewolf", "Hedera"
    );

    /** 30 frags — XP 250 */
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
