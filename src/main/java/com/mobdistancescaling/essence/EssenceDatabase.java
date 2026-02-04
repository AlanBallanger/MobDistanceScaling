package com.mobdistancescaling.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceDatabase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Connection connection;
    private final File dbFile;

    public EssenceDatabase(File pluginFolder) {
        this.dbFile = new File(pluginFolder, "essence.db");
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            LOGGER.at(Level.INFO).log("SQLite JDBC driver loaded successfully");
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            LOGGER.at(Level.INFO).log("Essence database initialized at: " + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.SEVERE).log("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize essence database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_essence (
                player_uuid TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                essence INTEGER NOT NULL DEFAULT 0,
                last_updated INTEGER NOT NULL
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_essence ON player_essence(essence DESC)");
        }
    }

    public int getEssence(UUID playerUuid) {
        String query = "SELECT essence FROM player_essence WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("essence");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get essence for " + playerUuid + ": " + e.getMessage());
        }
        return 0;
    }

    public void setEssence(UUID playerUuid, String playerName, int essence) {
        String upsert = """
            INSERT INTO player_essence (player_uuid, player_name, essence, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                essence = excluded.essence,
                last_updated = excluded.last_updated
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, essence);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to set essence for " + playerUuid + ": " + e.getMessage());
        }
    }

    public void addEssence(UUID playerUuid, String playerName, int amount) {
        int current = getEssence(playerUuid);
        setEssence(playerUuid, playerName, current + amount);
    }

    public List<PlayerEssenceData> getTopPlayers(int limit) {
        List<PlayerEssenceData> topPlayers = new ArrayList<>();
        String query = "SELECT player_uuid, player_name, essence FROM player_essence ORDER BY essence DESC LIMIT ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                topPlayers.add(new PlayerEssenceData(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getInt("essence")
                ));
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get top players: " + e.getMessage());
        }
        
        return topPlayers;
    }

    public int getPlayerRank(UUID playerUuid) {
        String query = """
            SELECT COUNT(*) + 1 as rank FROM player_essence
            WHERE essence > (SELECT essence FROM player_essence WHERE player_uuid = ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get player rank: " + e.getMessage());
        }
        return -1;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.at(Level.INFO).log("Essence database closed");
            } catch (SQLException e) {
                LOGGER.at(Level.WARNING).log("Failed to close database: " + e.getMessage());
            }
        }
    }
}
