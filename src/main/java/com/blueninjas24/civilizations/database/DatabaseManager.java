package com.blueninjas24.civilizations.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseManager {

    private final File dataFolder;
    private final Logger logger;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
    }

    // Allows isolated persistence tests without starting a Paper server.
    public DatabaseManager(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void initialize() throws SQLException {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File databaseFile = new File(dataFolder, "civilizations.db");

        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.getAbsolutePath()
        );

        try {
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            new SchemaMigrator().migrate(connection);
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            connection = null;
            throw failure;
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
                        result.getLong("discovered_at"),
                        result.getInt("population"),
                        result.getInt("beds")
                );
            }
        }
    }

    public void updatePopulation(Settlement settlement) throws SQLException {
        String sql = """
            UPDATE settlements
            SET population = ?
            WHERE id = ?;
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, settlement.getPopulation());
            statement.setString(2, settlement.getId().toString());
            statement.executeUpdate();
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
                discovered_at,
                population,
                beds
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, settlement.getId().toString());
            statement.setString(2, settlement.getName());
            statement.setString(3, settlement.getWorld());
            statement.setInt(4, settlement.getCenterX());
            statement.setInt(5, settlement.getCenterY());
            statement.setInt(6, settlement.getCenterZ());
            statement.setLong(7, settlement.getDiscoveredAt());
            statement.setInt(8, settlement.getPopulation());
            statement.setInt(9, settlement.getBeds());

            statement.executeUpdate();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public PopulationRepository getPopulationRepository() throws SQLException {
        return new PopulationRepository(connection);
    }

    public PopulationSimulationRepository getPopulationSimulationRepository() throws SQLException {
        return new PopulationSimulationRepository(connection);
    }

    public DemographicRepository getDemographicRepository() throws SQLException {
        return new DemographicRepository(connection);
    }

    public CivilizationClockRepository getCivilizationClockRepository() {
        return new CivilizationClockRepository(connection);
    }

    public VillageIdentityRepository getVillageIdentityRepository() {
        return new VillageIdentityRepository(connection);
    }

    public VillageEnrollmentRepository getVillageEnrollmentRepository() {
        return new VillageEnrollmentRepository(this);
    }

    public ResidentProjectionRepository getResidentProjectionRepository() {
        return new ResidentProjectionRepository(connection);
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            logger.warning(
                    "Failed to close Civilizations database: " + e.getMessage()
            );
        }
    }

    public void updateBeds(Settlement settlement) throws SQLException {
        String sql = """
            UPDATE settlements
            SET beds = ?
            WHERE id = ?;
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, settlement.getBeds());
            statement.setString(2, settlement.getId().toString());
            statement.executeUpdate();
        }
    }
}
