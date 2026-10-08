package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.Household;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.population.FamilyTie;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Uses the manager-owned connection on its owning thread; no independent scheduler or connection. */
public final class PopulationRepository {
    private final Connection connection;

    PopulationRepository(Connection connection) throws SQLException {
        this.connection = Objects.requireNonNull(connection, "Database must be initialized");
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA foreign_keys")) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new SQLException("Population persistence requires SQLite foreign key enforcement");
            }
        }
    }

    public void insertHousehold(Household household) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO households (id, settlement_id, name) VALUES (?, ?, ?)")) {
            statement.setString(1, household.id().toString());
            statement.setString(2, household.settlementId().toString());
            statement.setString(3, household.name());
            statement.executeUpdate();
        }
    }

    public void insertResident(Resident resident) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO residents (id, name, settlement_id, household_id, age_steps, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, resident.id().toString());
            statement.setString(2, resident.name());
            statement.setString(3, resident.settlementId().toString());
            bindHousehold(statement, 4, resident.householdId());
            statement.setLong(5, resident.ageSteps());
            statement.setString(6, resident.status().name());
            statement.executeUpdate();
        }
    }

    /** Update within the same settlement; transfer/migration policy is deliberately not implemented. */
    public void updateResident(Resident resident) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE residents SET name = ?, household_id = ?, age_steps = ?, status = ?
                WHERE id = ? AND settlement_id = ?
                """)) {
            statement.setString(1, resident.name());
            bindHousehold(statement, 2, resident.householdId());
            statement.setLong(3, resident.ageSteps());
            statement.setString(4, resident.status().name());
            statement.setString(5, resident.id().toString());
            statement.setString(6, resident.settlementId().toString());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Resident does not exist in the specified settlement: " + resident.id());
            }
        }
    }

    public Optional<Resident> findResident(UUID id) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM residents WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(readResident(result)) : Optional.empty();
            }
        }
    }

    /** Includes deceased residents so history is retained; ordering is stable UUID text order. */
    public List<Resident> findResidents(UUID settlementId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT * FROM residents WHERE settlement_id = ? ORDER BY id")) {
            statement.setString(1, settlementId.toString());
            List<Resident> residents = new ArrayList<>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    residents.add(readResident(result));
                }
            }
            return List.copyOf(residents);
        }
    }

    public List<Household> findHouseholds(UUID settlementId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT * FROM households WHERE settlement_id = ? ORDER BY id")) {
            statement.setString(1, settlementId.toString());
            List<Household> households = new ArrayList<>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    households.add(new Household(UUID.fromString(result.getString("id")),
                            UUID.fromString(result.getString("settlement_id")), result.getString("name")));
                }
            }
            return List.copyOf(households);
        }
    }

    public long countLivingResidents(UUID settlementId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM residents WHERE settlement_id = ? AND status = 'ALIVE'")) {
            statement.setString(1, settlementId.toString());
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Could not count living residents");
                }
                return result.getLong(1);
            }
        }
    }

    /** Atomically insert a caller-defined household and its residents; no generated family/seeding rules. */
    public void insertHouseholdWithResidents(Household household, List<Resident> residents) throws SQLException {
        List<Resident> batch = List.copyOf(residents);
        for (Resident resident : batch) {
            if (!household.id().equals(resident.householdId())
                    || !household.settlementId().equals(resident.settlementId())) {
                throw new IllegalArgumentException("Batch residents must belong to the supplied household and settlement");
            }
        }
        if (!connection.getAutoCommit()) {
            throw new SQLException("Household insertion requires no active transaction");
        }
        connection.setAutoCommit(false);
        try {
            insertHousehold(household);
            for (Resident resident : batch) {
                insertResident(resident);
            }
            connection.commit();
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
        connection.setAutoCommit(true);
    }

    public void insertFamilyTie(FamilyTie tie) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO family_ties (first_id, second_id, type) VALUES (?, ?, ?)")) {
            statement.setString(1, tie.first().toString());
            statement.setString(2, tie.second().toString());
            statement.setString(3, tie.type().name());
            statement.executeUpdate();
        }
    }

    /** Both incoming/outgoing links, including deceased relatives and relatives elsewhere. */
    public List<FamilyTie> findFamilyTies(UUID residentId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT * FROM family_ties WHERE first_id = ? OR second_id = ?
                ORDER BY type, first_id, second_id
                """)) {
            statement.setString(1, residentId.toString());
            statement.setString(2, residentId.toString());
            List<FamilyTie> ties = new ArrayList<>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    ties.add(new FamilyTie(UUID.fromString(result.getString("first_id")),
                            UUID.fromString(result.getString("second_id")),
                            FamilyTie.Type.valueOf(result.getString("type"))));
                }
            }
            return List.copyOf(ties);
        }
    }

    /** Shared recorded parent, including half-siblings; unrelated housemates are not siblings. */
    public List<UUID> findSiblings(UUID residentId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT DISTINCT sibling.second_id FROM family_ties own
                JOIN family_ties sibling ON sibling.first_id = own.first_id
                WHERE own.type = 'PARENT_CHILD' AND sibling.type = 'PARENT_CHILD'
                  AND own.second_id = ? AND sibling.second_id <> ?
                ORDER BY sibling.second_id
                """)) {
            statement.setString(1, residentId.toString());
            statement.setString(2, residentId.toString());
            List<UUID> siblings = new ArrayList<>();
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    siblings.add(UUID.fromString(result.getString(1)));
                }
            }
            return List.copyOf(siblings);
        }
    }

    static Resident readResident(ResultSet result) throws SQLException {
        String household = result.getString("household_id");
        return new Resident(UUID.fromString(result.getString("id")), result.getString("name"),
                UUID.fromString(result.getString("settlement_id")),
                household == null ? null : UUID.fromString(household), result.getLong("age_steps"),
                Resident.Status.valueOf(result.getString("status")));
    }

    private static void bindHousehold(java.sql.PreparedStatement statement, int index, UUID householdId)
            throws SQLException {
        if (householdId == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, householdId.toString());
        }
    }
}
