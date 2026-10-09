package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.HouseholdNeeds.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Condition;
import java.sql.*;
import java.util.*;
import com.blueninjas24.civilizations.population.PopulationState;

/** Historical household evidence commits inside the same transaction as its food period. */
final class HouseholdNeedsRepository {
    private final Connection connection;
    HouseholdNeedsRepository(Connection connection) { this.connection = connection; }
    PopulationState population(UUID town) throws SQLException {
        var people = new PopulationRepository(connection);
        var capacities = new HashMap<UUID, Integer>();
        try (var s = connection.prepareStatement("SELECT c.* FROM household_capacity c JOIN households h ON h.id=c.household_id WHERE h.settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                while (r.next()) capacities.put(UUID.fromString(r.getString("household_id")), r.getInt("capacity"));
            }
        }
        return new PopulationState(people.findResidents(town), people.findHouseholds(town), List.of(), capacities);
    }
    void save(Assessment needs) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Needs must share the food transaction");
        try (var s = connection.prepareStatement("INSERT INTO needs_periods (settlement_id,period,food_condition,shortage_periods,fulfilled,demand,unassigned_residents,unassigned_demand,unassigned_supplied) VALUES (?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, needs.settlement().toString()); s.setLong(2, needs.period()); s.setString(3, needs.sharedFood().name());
            s.setLong(4, needs.shortagePeriods()); s.setLong(5, needs.fulfilled()); s.setLong(6, needs.demand()); s.setLong(7, needs.unassignedResidents()); nullable(s,8,needs.unassignedDemand()); nullable(s,9,needs.unassignedSupplied()); s.executeUpdate();
        }
        try (var s = connection.prepareStatement("INSERT INTO household_needs (settlement_id,period,household_id,adults,children,food_demand,capacity,supplied,unmet_periods) VALUES (?,?,?,?,?,?,?,?,?)")) {
            for (var home : needs.homes()) {
                s.setString(1, needs.settlement().toString()); s.setLong(2, needs.period()); s.setString(3, home.household().toString());
                s.setLong(4, home.adults()); s.setLong(5, home.children()); s.setLong(6, home.foodDemand());
                if (home.capacity() == null) s.setNull(7, Types.INTEGER); else s.setInt(7, home.capacity());
                nullable(s,8,home.supplied()); nullable(s,9,home.unmetPeriods());
                s.executeUpdate();
            }
        }
    }
    Optional<Assessment> load(UUID town, long period) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM needs_periods WHERE settlement_id=? AND period=?")) {
            s.setString(1, town.toString()); s.setLong(2, period);
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                var homes = new ArrayList<Home>();
                try (var h = connection.prepareStatement("SELECT * FROM household_needs WHERE settlement_id=? AND period=? ORDER BY household_id")) {
                    h.setString(1, town.toString()); h.setLong(2, period);
                    try (var row = h.executeQuery()) {
                        while (row.next()) {
                            int capacity = row.getInt("capacity"); boolean unknown = row.wasNull();
                            homes.add(new Home(UUID.fromString(row.getString("household_id")), row.getLong("adults"), row.getLong("children"), row.getLong("food_demand"), unknown ? null : capacity, nullable(row,"supplied"), nullable(row,"unmet_periods")));
                        }
                    }
                }
                return Optional.of(new Assessment(town, period, Condition.valueOf(r.getString("food_condition")), r.getLong("shortage_periods"),
                        r.getLong("fulfilled"), r.getLong("demand"), r.getLong("unassigned_residents"), homes, nullable(r,"unassigned_demand"), nullable(r,"unassigned_supplied")));
            }
        }
    }
    private static void nullable(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT); else statement.setLong(index,value);
    }
    private static Long nullable(ResultSet row, String column) throws SQLException {
        long value = row.getLong(column); return row.wasNull() ? null : value;
    }
}
