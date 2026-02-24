package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class MobFragmentsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "mob_special_rates.toml";
    private static final String SECTION  = "mob_special_rates";

    private final Map<String, Integer> fragmentsByMobId;

    public MobFragmentsConfig(@Nonnull Map<String, Integer> fragmentsByMobId) {
        this.fragmentsByMobId = new HashMap<>(fragmentsByMobId);
    }

    public int getFragments(@Nonnull String roleName) {
        return fragmentsByMobId.getOrDefault(roleName.toLowerCase(Locale.ROOT), -1);
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
            Map<String, Integer> map = new HashMap<>();
            Toml section = toml.getTable(SECTION);
            if (section != null) {
                Map<String, Object> raw = section.toMap();
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        map.put(entry.getKey().toLowerCase(Locale.ROOT),
                                ((Number) entry.getValue()).intValue());
                    }
                }
            }
            LOGGER.at(Level.INFO).log("Loaded {0} with {1} entries", FILENAME, map.size());
            return new MobFragmentsConfig(map);
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
        sb.append("# MobId = fragments_count\n");
        sb.append("# 0 = no drop  |  1 = basic hostile  |  10 = légendaire\n\n");
        sb.append("[").append(SECTION).append("]\n\n");

        sb.append("# --- 0 fragments : trivial aggressors (fish, jellyfish, Bat_Ice...) ---\n");
        appendGroup(sb, TRIVIAL, 0);

        sb.append("\n# --- Tier 1 : Skeleton/Zombie/Goblin de base, wolves, scarabs communs ---\n");
        appendGroup(sb, TIER1, 1);

        sb.append("\n# --- Tier 2 : variantes basiques, Trorks, Fen_Stalker ---\n");
        appendGroup(sb, TIER2, 2);

        sb.append("\n# --- Tier 3 : Knight, Mage, Tiger Sabertooth, Scarab fighters ---\n");
        appendGroup(sb, TIER3, 3);

        sb.append("\n# --- Tier 4 : Archmage, Bear, Trork_Chieftain, Scarab seekers ---\n");
        appendGroup(sb, TIER4, 4);

        sb.append("\n# --- Tier 5 : Wraith, Spawn_Void, Scarab defenders ---\n");
        appendGroup(sb, TIER5, 5);

        sb.append("\n# --- Tier 6 : Yeti, Scarab broodmothers ---\n");
        appendGroup(sb, TIER6, 6);

        sb.append("\n# --- Tier 8 : Werewolf, Praetorian ---\n");
        appendGroup(sb, TIER8, 8);

        sb.append("\n# --- Tier 10 : Shadow Knight, Rex Cave, Fire Queen ---\n");
        appendGroup(sb, TIER10, 10);

        sb.append("\n# --- Passifs (0 fragments) ---\n");
        appendGroup(sb, PASSIVE, 0);

        return sb.toString();
    }

    private void appendGroup(StringBuilder sb, List<String> ids, int fragments) {
        for (String id : ids) {
            sb.append(id).append(" = ").append(fragments).append("\n");
        }
    }

    @Nonnull
    public static MobFragmentsConfig createDefault() {
        Map<String, Integer> map = new HashMap<>();
        for (String id : TRIVIAL) { map.put(id.toLowerCase(Locale.ROOT), 0); }
        for (String id : TIER1)   { map.put(id.toLowerCase(Locale.ROOT), 1); }
        for (String id : TIER2)   { map.put(id.toLowerCase(Locale.ROOT), 2); }
        for (String id : TIER3)   { map.put(id.toLowerCase(Locale.ROOT), 3); }
        for (String id : TIER4)   { map.put(id.toLowerCase(Locale.ROOT), 4); }
        for (String id : TIER5)   { map.put(id.toLowerCase(Locale.ROOT), 5); }
        for (String id : TIER6)   { map.put(id.toLowerCase(Locale.ROOT), 6); }
        for (String id : TIER8)   { map.put(id.toLowerCase(Locale.ROOT), 8); }
        for (String id : TIER10)  { map.put(id.toLowerCase(Locale.ROOT), 10); }
        for (String id : PASSIVE) { map.put(id.toLowerCase(Locale.ROOT), 0); }
        return new MobFragmentsConfig(map);
    }

    // -------------------------------------------------------------------------
    // Tier definitions
    // -------------------------------------------------------------------------

    /** 0 — aggressive but no reward: fish, jellyfish, trivial beasts */
    private static final List<String> TRIVIAL = List.of(
        "Jellyfish_Red",
        "Jellyfish_Green",
        "Jellyfish_Yellow",
        "Jellyfish_Cyan",
        "Jellyfish_Blue",
        "Jellyfish_Man_Of_War",
        "Piranha",
        "Piranha_Black",
        "Pike",
        "Snapjaw",
        "Eel_Moray",
        "Shark_Hammerhead",
        "Vulture",
        "Hawk",
        "Tetrabird",
        "Bat_Ice",
        "Pig_Wild",
        "Chicken_Undead",
        "Pig_Undead",
        "Cow_Undead",
        "Spark_Living",
        "Boar"
    );

    /** 1 — Skeleton/Zombie/Goblin de base, wolves, spiders, snakes, scarabs légers */
    private static final List<String> TIER1 = List.of(
        "Zombie",
        "Zombie_Sand",
        "Zombie_Frost",
        "Zombie_Burnt",
        "Skeleton_Scout",
        "Skeleton_Soldier",
        "Skeleton_Archer",
        "Skeleton_Fighter",
        "Skeleton_Fighter_Wander",
        "Skeleton_Ranger",
        "Ghoul",
        "Spider",
        "Spider_Cave",
        "Snake_Marsh",
        "Snake_Cobra",
        "Snake_Rattle",
        "Wolf_Black",
        "Wolf_White",
        "Wolf_Trork_Hunter",
        "Wolf_Trork_Shaman",
        "Wolf_Outlander_Priest",
        "Wolf_Outlander_Sorcerer",
        "Skeleton_Archer_Patrol",
        "Skeleton_Fighter_Patrol",
        "Skeleton_Archer_Wander",
        "Dungeon_Scarak_Louse",
        "Scarak_Louse",
        "Larva_Silk",
        "Larva_Void",
        "Fox",
        "Rat"
    );

    /** 2 — variantes basiques de biome, Goblins, Trorks (sauf Chieftain), Fen_Stalker */
    private static final List<String> TIER2 = List.of(
        "Goblin_Scavenger",
        "Goblin_Scavenger_Battleaxe",
        "Goblin_Scavenger_Sword",
        "Goblin_Scrapper",
        "Goblin_Scrapper_Patrol",
        "Goblin_Thief",
        "Goblin_Thief_Patrol",
        "Goblin_Miner",
        "Goblin_Miner_Patrol",
        "Goblin_Lobber",
        "Goblin_Lobber_Patrol",
        "Goblin_Hermit",
        "Goblin_Ogre",
        "Trillodon",
        "Skeleton_Frost_Scout",
        "Skeleton_Frost_Soldier",
        "Skeleton_Frost_Ranger",
        "Skeleton_Frost_Fighter",
        "Skeleton_Frost_Archer",
        "Skeleton_Sand_Scout",
        "Skeleton_Sand_Soldier",
        "Skeleton_Sand_Archer",
        "Skeleton_Sand_Ranger",
        "Skeleton_Sand_Guard",
        "Skeleton_Pirate_Striker",
        "Skeleton_Pirate_Gunner",
        "Skeleton_Burnt_Soldier",
        "Skeleton_Burnt_Archer",
        "Skeleton_Burnt_Lancer",
        "Skeleton_Incandescent_Footman",
        "Skeleton_Incandescent_Head",
        "Hound_Bleached",
        "Archaeopteryx",
        "Fen_Stalker",
        "Trork_Brawler",
        "Trork_Doctor_Witch",
        "Trork_Guard",
        "Trork_Hunter",
        "Trork_Mauler",
        "Trork_Sentry",
        "Trork_Sentry_Patrol",
        "Trork_Shaman",
        "Trork_Unarmed",
        "Trork_Warrior",
        "Trork_Warrior_Patrol",
        "Skeleton_Burnt_Archer_Patrol",
        "Skeleton_Burnt_Lancer_Patrol",
        "Skeleton_Burnt_Soldier_Patrol",
        "Skeleton_Burnt_Archer_Wander",
        "Skeleton_Burnt_Lancer_Wander",
        "Skeleton_Burnt_Soldier_Wander",
        "Skeleton_Frost_Archer_Wander",
        "Skeleton_Frost_Fighter_Wander",
        "Skeleton_Frost_Ranger_Wander",
        "Skeleton_Frost_Scout_Wander",
        "Skeleton_Frost_Soldier_Wander"
    );

    /** 3 — Knight, Mage, Tiger Sabertooth, Scarab fighters */
    private static final List<String> TIER3 = List.of(
        "Skeleton_Mage",
        "Skeleton_Knight",
        "Skeleton_Frost_Mage",
        "Skeleton_Frost_Knight",
        "Skeleton_Sand_Mage",
        "Skeleton_Sand_Assassin",
        "Skeleton_Burnt_Wizard",
        "Skeleton_Burnt_Gunner",
        "Skeleton_Burnt_Alchemist",
        "Skeleton_Burnt_Knight",
        "Skeleton_Incandescent_Mage",
        "Skeleton_Incandescent_Fighter",
        "Crawler_Void",
        "Shellfish_Lava",
        "Cactee",
        "Hyena",
        "Pterodactyl",
        "Tiger_Sabertooth",
        "Skeleton_Burnt_Alchemist_Patrol",
        "Skeleton_Burnt_Gunner_Patrol",
        "Skeleton_Burnt_Knight_Patrol",
        "Skeleton_Burnt_Wizard_Patrol",
        "Skeleton_Burnt_Alchemist_Wander",
        "Skeleton_Burnt_Gunner_Wander",
        "Skeleton_Burnt_Knight_Wander",
        "Skeleton_Frost_Knight_Wander",
        "Skeleton_Frost_Mage_Wander",
        "Skeleton_Incandescent_Fighter_Wander",
        "Dungeon_Scarak_Fighter_Patrol",
        "Scarak_Fighter_Patrol"
    );

    /** 4 — Archmage, Outlanders, Trork_Chieftain, Bear, grands prédateurs, Scarab seekers */
    private static final List<String> TIER4 = List.of(
        "Outlander_Berserker",
        "Outlander_Cultist",
        "Outlander_Hunter",
        "Outlander_Marauder",
        "Outlander_Peon",
        "Outlander_Priest",
        "Outlander_Sorcerer",
        "Outlander_Stalker",
        "Skeleton_Archmage",
        "Skeleton_Frost_Archmage",
        "Skeleton_Sand_Archmage",
        "Skeleton_Pirate_Captain",
        "Skeleton_Archmage_Patrol",
        "Skeleton_Archmage_Wander",
        "Skeleton_Frost_Archmage_Wander",
        "Eye_Void",
        "Toad_Rhino",
        "Raptor_Cave",
        "Snapdragon",
        "Emberwulf",
        "Crocodile",
        "Leopard_Snow",
        "Bear_Polar",
        "Bear_Grizzly",
        "Trork_Chieftain",
        "Dungeon_Scarak_Seeker",
        "Scarak_Seeker",
        "Scarak_Fighter_Royal_Guard"
    );

    /** 5 — Wraith, Spawn_Void, Toad Magma, Scarab defenders */
    private static final List<String> TIER5 = List.of(
        "Toad_Rhino_Magma",
        "Wraith",
        "Spawn_Void",
        "Dungeon_Scarak_Defender",
        "Scarak_Defender",
        "Dungeon_Scarak_Defender_Patrol",
        "Scarak_Defender_Patrol"
    );

    /** 6 — Outlander_Brute, Yeti, Scarak broodmothers */
    private static final List<String> TIER6 = List.of(
        "Outlander_Brute",
        "Yeti",
        "Dungeon_Scarak_Broodmother",
        "Dungeon_Scarak_Broodmother_Young",
        "Scarak_Broodmother"
    );

    /** 8 — Werewolf, Praetorian */
    private static final List<String> TIER8 = List.of(
        "Werewolf",
        "Skeleton_Burnt_Praetorian",
        "Skeleton_Burnt_Praetorian_Wander",
        "Skeleton_Burnt_Praetorian_Patrol"
    );

    /** 10 — Shadow Knight, Rex Cave, Fire Queen */
    private static final List<String> TIER10 = List.of(
        "Shadow_Knight",
        "Rex_Cave",
        "Fire_Queen"
    );

    private static final List<String> PASSIVE = List.of(
        "Horse_Skeleton",
        "Horse_Skeleton_Armored",
        "Bison",
        "Mosshorn",
        "Warthog",
        "Ram",
        "Mouflon",
        "Moose_Bull",
        "Pig_Wild_Piglet",
        "Trilobite",
        "Trilobite_Black",
        "Whale_Humpback",
        "Frostgill",
        "Trout_Rainbow",
        "Bluegill",
        "Salmon",
        "Catfish",
        "Minnow",
        "Crab",
        "Pufferfish",
        "Tang_Sailfin",
        "Tang_Blue",
        "Lobster",
        "Clownfish",
        "Tang_Lemon_Peel",
        "Tang_Chevron",
        "Sparrow",
        "Bat",
        "Parrot",
        "Finch_Green",
        "Bluebird",
        "Owl_Snow",
        "Penguin",
        "Woodpecker",
        "Crow",
        "Raven",
        "Flamingo",
        "Owl_Brown",
        "Duck",
        "Pigeon",
        "Lizard_Sand",
        "Tortoise",
        "Horse",
        "Rabbit",
        "Camel_Calf",
        "Chicken_Desert_Chick",
        "Cow_Calf",
        "Goat_Kid",
        "Sheep_Lamb",
        "Skrill_Chick",
        "Warthog_Piglet",
        "Bison_Calf",
        "Pig_Piglet",
        "Camel",
        "Bunny",
        "Pig",
        "Boar_Piglet",
        "Horse_Foal",
        "Chicken_Desert",
        "Turkey_Chick",
        "Cow",
        "Chicken_Chick",
        "Chicken",
        "Turkey",
        "Ram_Lamb",
        "Goat",
        "Mouflon_Lamb",
        "Antelope",
        "Deer_Stag",
        "Armadillo",
        "Deer_Doe",
        "Moose_Cow",
        "Mouse",
        "Meerkat",
        "Frog_Green",
        "Gecko",
        "Frog_Blue",
        "Squirrel",
        "Snail_Magma",
        "Sheep"
    );
}
