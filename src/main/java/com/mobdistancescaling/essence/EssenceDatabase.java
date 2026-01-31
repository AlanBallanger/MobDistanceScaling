package com.mobdistancescaling.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceDatabase {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final String dbPath;
    private Connection connection;

    public EssenceDatabase(@Nonnull File dataFolder) {
        this.dbPath = new File(dataFolder, "essence.db").getAbsolutePath();
        initialize();
    }

    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            createTables();
            
            LOGGER.at(Level.INFO).log("Database SQLite initialisée: " + dbPath);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors de l'initialisation de la base de données", e);
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_essence (" +
                "uuid TEXT PRIMARY KEY," +
                "essence INTEGER NOT NULL DEFAULT 0," +
                "last_modified INTEGER NOT NULL" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            
            // Index pour les requêtes rapides
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_essence ON player_essence(essence DESC)");
            
            LOGGER.at(Level.INFO).log("Tables créées avec succès");
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors de la création des tables", e);
        }
    }

    @Nullable
    public PlayerEssenceData load(@Nonnull UUID playerUuid) {
        String sql = "SELECT essence, last_modified FROM player_essence WHERE uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int essence = rs.getInt("essence");
                return new PlayerEssenceData(playerUuid, essence);
            }
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors du chargement de l'essence pour " + playerUuid, e);
        }
        
        // Nouveau joueur
        return new PlayerEssenceData(playerUuid, 0);
    }

    public void save(@Nonnull PlayerEssenceData data) {
        String sql = "INSERT OR REPLACE INTO player_essence (uuid, essence, last_modified) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, data.getPlayerUuid().toString());
            pstmt.setInt(2, data.getEssence());
            pstmt.setLong(3, data.getLastModified());
            
            pstmt.executeUpdate();
            data.markClean();
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors de la sauvegarde de l'essence pour " + data.getPlayerUuid(), e);
        }
    }

    public void saveBatch(@Nonnull List<PlayerEssenceData> dataList) {
        if (dataList.isEmpty()) return;
        
        String sql = "INSERT OR REPLACE INTO player_essence (uuid, essence, last_modified) VALUES (?, ?, ?)";
        
        try {
            connection.setAutoCommit(false);
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (PlayerEssenceData data : dataList) {
                    if (!data.isDirty()) continue;
                    
                    pstmt.setString(1, data.getPlayerUuid().toString());
                    pstmt.setInt(2, data.getEssence());
                    pstmt.setLong(3, data.getLastModified());
                    pstmt.addBatch();
                    
                    data.markClean();
                }
                
                pstmt.executeBatch();
            }
            
            connection.commit();
            connection.setAutoCommit(true);
            
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors de la sauvegarde batch", e);
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.at(Level.SEVERE).log("Erreur lors du rollback", ex);
            }
        }
    }

    @Nonnull
    public List<PlayerEssenceData> loadAll() {
        List<PlayerEssenceData> list = new ArrayList<>();
        String sql = "SELECT uuid, essence FROM player_essence";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int essence = rs.getInt("essence");
                list.add(new PlayerEssenceData(uuid, essence));
            }
            
            LOGGER.at(Level.INFO).log("Chargé " + list.size() + " joueurs depuis la base de données");
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors du chargement de toutes les données", e);
        }
        
        return list;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.at(Level.INFO).log("Base de données fermée");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Erreur lors de la fermeture de la base de données", e);
        }
    }
}
