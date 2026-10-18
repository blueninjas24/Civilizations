package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Durable positive evidence, not a reserve, a land claim or permission to found a mine. */
public final class MineralOpportunities {
    public record Evidence(UUID world, Position position, Commodity mineral, long firstSeenMillis,
                           long lastSeenMillis, boolean excluded, UUID savedDeposit) {}
    public enum Status { MISSING, INACTIVE_WORLD, TRACKING_GAP, FUTURE, STALE, CURRENT }
    /** Current candidates are visible, unexcluded and unclaimed, not yet suitable funded businesses. */
    public record Review(Status status, UUID world, long capturedMillis, List<Evidence> candidates, String explanation) {
        public Review { candidates=List.copyOf(candidates); }
    }
    private record Survey(UUID world,long captured,String fingerprint) {}
    private final Connection connection;
    private final boolean quarry;
    public MineralOpportunities(Connection connection) {this(connection,false);}
    private MineralOpportunities(Connection connection,boolean quarry){this.connection=Objects.requireNonNull(connection);this.quarry=quarry;}
    /** Independent current frames: common rock must never replace a coal/iron survey. */
    public static MineralOpportunities quarries(Connection connection){return new MineralOpportunities(connection,true);}
    private PreparedStatement prepare(String sql)throws SQLException {
        return connection.prepareStatement(quarry?sql.replace("mineral_opportunities","quarry_opportunities")
                .replace("mineral_surveys","quarry_surveys").replace("mineral_survey_members","quarry_survey_members"):sql);
    }

    /** Validate and save one bounded survey atomically. Missing observations never erase saved evidence. */
    public List<MineralSurveyReader.Patch> observe(UUID town, List<MineralObservation> observations) throws SQLException {
        if(quarry)return new QuarryOpportunities(connection).observe(town,observations,Integer.MIN_VALUE,Integer.MIN_VALUE);
        observations = List.copyOf(observations);
        if (!connection.getAutoCommit()) throw new SQLException("Opportunity observation requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            var patches = (quarry?MineralSurveyReader.quarries(connection):new MineralSurveyReader(connection)).read(town, observations);
            var first=observations.getFirst();var fingerprint=fingerprint(observations);var previous=survey(town);
            if(previous!=null && previous.captured()==first.observedMillis() && !previous.fingerprint().equals(fingerprint))
                throw new SQLException("Same survey capture time has conflicting evidence");
            try (var insert = prepare("""
                    INSERT INTO mineral_opportunities(settlement_id,world_id,x,y,z,mineral,first_seen_millis,last_seen_millis)
                    VALUES(?,?,?,?,?,?,?,?)
                    ON CONFLICT(settlement_id,world_id,x,y,z) DO UPDATE SET
                    last_seen_millis=MAX(last_seen_millis,excluded.last_seen_millis)
                    WHERE mineral=excluded.mineral
                    """)) {
                for (var patch : patches) for (var p : patch.evidence().ore()) {
                    var evidence = patch.evidence();
                    insert.setString(1, town.toString()); insert.setString(2, evidence.world().toString());
                    insert.setInt(3, p.x()); insert.setInt(4, p.y()); insert.setInt(5, p.z());
                    insert.setString(6, evidence.mineral().id());
                    insert.setLong(7, evidence.observedMillis()); insert.setLong(8, evidence.observedMillis());
                    if (insert.executeUpdate() != 1) throw new SQLException("Observed mineral conflicts with retained opportunity evidence");
                }
            }
            if(previous==null || first.observedMillis()>previous.captured())saveCurrent(town,first,fingerprint,patches);
            connection.commit(); connection.setAutoCommit(true); return patches;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    /** Complete single-chunk frame. Empty observations only replace this chunk's membership. */
    public List<MineralSurveyReader.Patch> observeChunk(UUID town,List<MineralObservation> observations,int chunkX,int chunkZ)throws SQLException {
        if(!quarry)throw new IllegalStateException("Chunk-scoped observations are a quarry contract");
        return new QuarryOpportunities(connection).observe(town,observations,chunkX,chunkZ);
    }
    /** No historical union: missing/stale/absent-world evidence cannot become an automatic startup candidate. */
    public Review review(UUID town,long now,long maximumAgeMillis,Set<UUID> activeWorlds)throws SQLException {
        if(quarry)return new QuarryOpportunities(connection).review(town,now,maximumAgeMillis,activeWorlds);
        Objects.requireNonNull(town);activeWorlds=activeWorlds==null?Set.of():Set.copyOf(activeWorlds);
        if(now<0 || maximumAgeMillis<1)throw new IllegalArgumentException("Nonnegative time and positive survey lifetime required");
        var survey=survey(town);
        if(survey==null)return new Review(Status.MISSING,null,-1,List.of(),"No complete current survey; historical sightings are insufficient");
        if(!activeWorlds.contains(survey.world()))return unavailable(Status.INACTIVE_WORLD,survey,"World absent or availability unknown; candidates paused");
        if(new OreTrackingRepository(connection).load().map(s->!s.trusted()).orElse(true))
            return unavailable(Status.TRACKING_GAP,survey,"Ore mutation history is unavailable or requires reviewed recovery");
        if(survey.captured()>now)return unavailable(Status.FUTURE,survey,"Capture lies ahead of the current clock; candidates deferred");
        if(now-survey.captured()>maximumAgeMillis)return unavailable(Status.STALE,survey,"Complete survey expired; new loaded-chunk evidence required");
        var rows=new ArrayList<Evidence>();
        try(var s=prepare("""
            SELECT o.*,
              EXISTS(SELECT 1 FROM player_ore_placements p WHERE p.world_id=o.world_id AND p.x=o.x AND p.y=o.y AND p.z=o.z)
              OR EXISTS(SELECT 1 FROM ore_source_removals r WHERE r.world_id=o.world_id AND r.x=o.x AND r.y=o.y AND r.z=o.z) AS excluded,
              (SELECT e.deposit_id FROM mineral_evidence e WHERE e.world_id=o.world_id AND e.x=o.x AND e.y=o.y AND e.z=o.z) AS deposit
            FROM mineral_survey_members m JOIN mineral_opportunities o
              USING(settlement_id,world_id,x,y,z)
            WHERE m.settlement_id=? ORDER BY o.world_id,o.x,o.y,o.z LIMIT 513
            """)) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                int count=0;while(r.next()) {
                    if(++count>512)throw new SQLException("Current survey exceeds coordinate budget");
                    if(!r.getBoolean("excluded") && r.getString("deposit")==null)
                        rows.add(new Evidence(survey.world(),new Position(r.getInt("x"),r.getInt("y"),r.getInt("z")),
                            new Commodity(r.getString("mineral")),r.getLong("first_seen_millis"),r.getLong("last_seen_millis"),false,null));
                }
            }
        }
        return new Review(Status.CURRENT,survey.world(),survey.captured(),rows,
            "Current complete survey: "+rows.size()+" visible unclaimed coordinates; demand, site, skills and funding still required");
    }
    private Review unavailable(Status status,Survey survey,String reason) {
        return new Review(status,survey.world(),survey.captured(),List.of(),reason);
    }
    private Survey survey(UUID town)throws SQLException {
        try(var s=prepare("SELECT world_id,captured_millis,fingerprint FROM mineral_surveys WHERE settlement_id=?")) {
            s.setString(1,town.toString());try(var r=s.executeQuery()) {
                return r.next()?new Survey(UUID.fromString(r.getString(1)),r.getLong(2),r.getString(3)):null;
            }
        }
    }
    private void saveCurrent(UUID town,MineralObservation first,String fingerprint,List<MineralSurveyReader.Patch> patches)throws SQLException {
        try(var s=prepare("DELETE FROM mineral_survey_members WHERE settlement_id=?")) {
            s.setString(1,town.toString());s.executeUpdate();
        }
        try(var s=prepare("""
            INSERT INTO mineral_surveys VALUES(?,?,?,?) ON CONFLICT(settlement_id) DO UPDATE SET
            world_id=excluded.world_id,captured_millis=excluded.captured_millis,fingerprint=excluded.fingerprint
            """)) {
            s.setString(1,town.toString());s.setString(2,first.world().toString());s.setLong(3,first.observedMillis());s.setString(4,fingerprint);s.executeUpdate();
        }
        try(var s=prepare("INSERT INTO mineral_survey_members VALUES(?,?,?,?,?)")) {
            for(var patch:patches)for(var p:patch.evidence().ore()) {
                s.setString(1,town.toString());s.setString(2,first.world().toString());s.setInt(3,p.x());s.setInt(4,p.y());s.setInt(5,p.z());s.executeUpdate();
            }
        }
    }
    static String fingerprint(List<MineralObservation> observations) {
        var text=new StringBuilder("mineral-survey-v1|");
        for(var o:observations) {
            text.append(o.world()).append('|').append(o.observedMillis()).append('|').append(o.mineral().id()).append('|');
            for(var p:o.ore())text.append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(';');
            text.append('|');
        }
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("JDK SHA-256 unavailable",impossible);}
    }

    /** Bounded diagnostic page, ordered by coordinate; exclusions/claims are derived from current saved truth. */
    public List<Evidence> read(UUID town, int limit) throws SQLException {
        Objects.requireNonNull(town);
        if (limit < 1 || limit > 512) throw new IllegalArgumentException("Opportunity page limit must be 1..512");
        var result = new ArrayList<Evidence>();
        try (var query = prepare("""
                SELECT o.*,
                  EXISTS(SELECT 1 FROM player_ore_placements p WHERE p.world_id=o.world_id AND p.x=o.x AND p.y=o.y AND p.z=o.z)
                  OR EXISTS(SELECT 1 FROM ore_source_removals r WHERE r.world_id=o.world_id AND r.x=o.x AND r.y=o.y AND r.z=o.z) AS excluded,
                  (SELECT e.deposit_id FROM mineral_evidence e WHERE e.world_id=o.world_id AND e.x=o.x AND e.y=o.y AND e.z=o.z) AS deposit
                FROM mineral_opportunities o WHERE settlement_id=? ORDER BY world_id,x,y,z LIMIT ?
                """)) {
            query.setString(1, town.toString()); query.setInt(2, limit);
            try (var row = query.executeQuery()) {
                while (row.next()) result.add(new Evidence(UUID.fromString(row.getString("world_id")),
                        new Position(row.getInt("x"), row.getInt("y"), row.getInt("z")),
                        new Commodity(row.getString("mineral")),
                        row.getLong("first_seen_millis"), row.getLong("last_seen_millis"), row.getBoolean("excluded"),
                        row.getString("deposit") == null ? null : UUID.fromString(row.getString("deposit"))));
            }
        }
        return List.copyOf(result);
    }
}
