package com.blueninjas24.civilizations.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Read-only provenance inventory for administrative review; it does not certify current block state. */
public final class ResourceReviewReport {
    public static final int MAX_SCOPES = 32;

    public record TrackingFence(boolean initialized, boolean trusted, boolean cleanStop, long observedMillis) {
        public String describe() {
            if (!initialized) return "not initialized; trust is unknown";
            return trusted ? "trusted" : "untrusted; reviewed recovery required";
        }
    }

    public record ScopeCount(String resource, UUID world, int chunkX, int chunkZ, long coordinates) {
        public ScopeCount {
            Objects.requireNonNull(resource);
            Objects.requireNonNull(world);
            if (coordinates < 1) throw new IllegalArgumentException("Scope count must be positive");
        }
    }

    public record Counts(long mineralCoordinates, long mineralRemainingShares, long oreRetiredCoordinates,
                         long oreExcludedCoordinates, long forestryResources, long forestrySupports,
                         long forestryLostSupports, long forestryExcludedCoordinates, long forestryAvailableUnits) {}

    public record Snapshot(TrackingFence oreTracking, TrackingFence forestryTracking, Counts counts,
                           Set<UUID> loadedWorlds, boolean loadedWorldsKnown, List<ScopeCount> scopes, long moreScopes) {
        public Snapshot {
            Objects.requireNonNull(oreTracking);
            Objects.requireNonNull(forestryTracking);
            Objects.requireNonNull(counts);
            loadedWorlds = Set.copyOf(loadedWorlds);
            scopes = List.copyOf(scopes);
            if (scopes.size() > MAX_SCOPES || moreScopes < 0) throw new IllegalArgumentException("Invalid scope page");
        }

        public String describe() {
            return "Resource provenance inventory (diagnostic only; no current block truth or recovery applied)\n"
                    + "Trust is reported independently; clean stop=false can mean capture is currently active.\n"
                    + "Ore tracking: " + oreTracking.describe() + " (clean stop " + oreTracking.cleanStop()
                    + ", observed " + oreTracking.observedMillis() + ")\n"
                    + "Forest tracking: " + forestryTracking.describe() + " (clean stop " + forestryTracking.cleanStop()
                    + ", observed " + forestryTracking.observedMillis() + ")\n"
                    + "Mineral coordinates " + counts.mineralCoordinates() + ", remaining source shares "
                    + counts.mineralRemainingShares() + ", retired " + counts.oreRetiredCoordinates()
                    + ", sticky exclusions " + counts.oreExcludedCoordinates() + ".\n"
                    + "Forests " + counts.forestryResources() + ", supports " + counts.forestrySupports()
                    + ", named losses " + counts.forestryLostSupports() + ", exclusions "
                    + counts.forestryExcludedCoordinates() + ", available timber units " + counts.forestryAvailableUnits() + ".\n"
                    + "Loaded worlds supplied by caller: " + loadedWorlds.size() + "; showing " + scopes.size()
                    + " world/chunk resource scopes" + (moreScopes == 0 ? "." : "; " + moreScopes + " additional scopes omitted.")
                    + (scopes.isEmpty() ? "" : "\n" + String.join("\n", scopes.stream().map(scope ->
                        scope.resource() + " " + scope.world() + " chunk " + scope.chunkX() + "," + scope.chunkZ()
                                + ": " + scope.coordinates() + " coordinates; world "
                                + (loadedWorldsKnown ? loadedWorlds.contains(scope.world()) ? "is in supplied loaded set" : "is not in supplied loaded set" : "availability unknown"))
                        .toList()));
        }
    }

    private final Connection connection;

    public ResourceReviewReport(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /** Called on the SQL owner. Reads saved counters and bounded grouped scopes only. */
    public Snapshot snapshot(Set<UUID> loadedWorlds) throws SQLException {
        boolean worldsKnown = loadedWorlds != null;
        var loaded = worldsKnown ? Set.copyOf(loadedWorlds) : Set.<UUID>of();
        var ore = fence("ore_tracking");
        var forest = fence("forestry_tracking");
        var counts = new Counts(
                scalar("SELECT COUNT(*) FROM mineral_sources"),
                scalar("SELECT COALESCE(SUM(remaining_batches),0) FROM mineral_sources"),
                scalar("SELECT COUNT(*) FROM mineral_source_retirements"),
                scalar("SELECT COUNT(*) FROM (SELECT world_id,x,y,z FROM player_ore_placements UNION SELECT world_id,x,y,z FROM ore_source_removals)"),
                scalar("SELECT COUNT(*) FROM forestry_resources"),
                scalar("SELECT COUNT(*) FROM forestry_supports"),
                scalar("SELECT COUNT(*) FROM forestry_source_losses"),
                scalar("SELECT COUNT(*) FROM forestry_exclusions"),
                scalar("SELECT COALESCE(SUM(available),0) FROM forestry_resources"));
        long totalScopes = scalar(scopeSql(true));
        var scopes = new ArrayList<ScopeCount>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(scopeSql(false))) {
            while (rows.next() && scopes.size() < MAX_SCOPES) {
                scopes.add(new ScopeCount(rows.getString(1), UUID.fromString(rows.getString(2)),
                        rows.getInt(3), rows.getInt(4), rows.getLong(5)));
            }
        }
        long more = Math.max(0, totalScopes - scopes.size());
        return new Snapshot(ore, forest, counts, loaded, worldsKnown, scopes, more);
    }

    /** Read-only convenience name for command and diagnostic callers. */
    public Snapshot read(Set<UUID> loadedWorlds) throws SQLException {
        return snapshot(loadedWorlds);
    }

    private TrackingFence fence(String table) throws SQLException {
        // Table name is internal constant at call sites; clean_stop=false can describe active capture.
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT clean_stop,trusted,observed_millis FROM " + table + " WHERE id=1")) {
            return rows.next() ? new TrackingFence(true, rows.getInt(2) == 1, rows.getInt(1) == 1, rows.getLong(3))
                    : new TrackingFence(false, false, false, 0);
        }
    }

    private long scalar(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            if (!rows.next()) throw new SQLException("Resource review query returned no row");
            return rows.getLong(1);
        }
    }

    private static String scopeSql(boolean countOnly) {
        String rows = """
                WITH coordinate_rows(kind,world_id,x,z) AS (
                    SELECT 'mineral-source',world_id,x,z FROM mineral_sources
                    UNION ALL SELECT 'ore-exclusion',world_id,x,z FROM (
                        SELECT world_id,x,y,z FROM player_ore_placements
                        UNION SELECT world_id,x,y,z FROM ore_source_removals
                    )
                    UNION ALL SELECT 'mineral-retired',world_id,x,z FROM mineral_source_retirements
                    UNION ALL SELECT 'forest-support',world_id,x,z FROM forestry_supports
                    UNION ALL SELECT 'forest-exclusion',world_id,x,z FROM forestry_exclusions
                    UNION ALL SELECT 'forest-loss',world_id,x,z FROM forestry_source_losses
                ), grouped AS (
                    SELECT kind,world_id,
                        CASE WHEN x<0 AND x%16<>0 THEN x/16-1 ELSE x/16 END AS chunk_x,
                        CASE WHEN z<0 AND z%16<>0 THEN z/16-1 ELSE z/16 END AS chunk_z,
                        COUNT(*) AS coordinates
                    FROM coordinate_rows GROUP BY kind,world_id,chunk_x,chunk_z
                )
                """;
        return countOnly
                ? rows + " SELECT COUNT(*) FROM grouped"
                : rows + " SELECT kind,world_id,chunk_x,chunk_z,coordinates FROM grouped "
                        + "ORDER BY kind,world_id,chunk_x,chunk_z LIMIT " + (MAX_SCOPES + 1);
    }
}
