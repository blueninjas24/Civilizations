package com.blueninjas24.civilizations.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File databaseFile = new File(plugin.getDataFolder(), "civilizations.db");

        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.getAbsolutePath()
        );

        createTables();
    }

    private void createTables() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS settlements (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    center_x INTEGER NOT NULL,
                    center_y INTEGER NOT NULL,
                    center_z INTEGER NOT NULL,
                    discovered_at INTEGER NOT NULL
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public Settlement findNearbySettlement(
            String world,
            int x,
            int z,
            int radius
    ) throws SQLException {

        String sql = """
            SELECT *
            FROM settlements
            WHERE world = ?
              AND ((center_x - ?) * (center_x - ?)
              +  (center_z - ?) * (center_z - ?)) <= ?;
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, x);
            statement.setInt(4, z);
            statement.setInt(5, z);
            statement.setInt(6, radius * radius);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new Settlement(
                        UUID.fromString(result.getString("id")),
                        result.getString("name"),
                        result.getString("world"),
                        result.getInt("center_x"),
                        result.getInt("center_y"),
                        result.getInt("center_z"),
                        result.getLong("discovered_at")
                );
            }
        }
    }

    public void saveSettlement(Settlement settlement) throws SQLException {
        String sql = """
            INSERT INTO settlements (
                id,
                name,
                world,
                center_x,
                center_y,
                center_z,
                discovered_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, settlement.getId().toString());
            statement.setString(2, settlement.getName());
            statement.setString(3, settlement.getWorld());
            statement.setInt(4, settlement.getCenterX());
            statement.setInt(5, settlement.getCenterY());
            statement.setInt(6, settlement.getCenterZ());
            statement.setLong(7, settlement.getDiscoveredAt());

            statement.executeUpdate();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning(
                    "Failed to close Civilizations database: " + e.getMessage()
            );
        }
    }
}