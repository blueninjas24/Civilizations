package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Current, transient canopy-backed evidence plus actual funded timber demand; no world writes or land claims. */
public final class AutomaticForestryFounding {
    private static final int MAX_CACHED_FRAMES = 1024;
    private record Frame(UUID town, int chunkX, int chunkZ, ForestObservation evidence) {}
    private final Connection connection;
    private final ForestryFoundingPolicy policy;
    private final ForestryPolicy resources;
    private final IndustryRuntimePolicy industry;
    private final BusinessOperationPolicy operations;
    private final StartupGrantPolicy grants;
    private final long adulthood, seed;
    private final java.util.function.BooleanSupplier provenanceHealthy;
    private final Map<String, Frame> frames = new HashMap<>();
    private VillageSupplyPolicy publicSupply = VillageSupplyPolicy.disabled();
    private StudyPolicy study = StudyPolicy.disabled();
    private FounderTraining training;
    private String townCursor = "";

    public AutomaticForestryFounding(Connection connection, ForestryFoundingPolicy policy, ForestryPolicy resources,
            IndustryRuntimePolicy industry, BusinessOperationPolicy operations, StartupGrantPolicy grants,
            long adulthood, long seed, java.util.function.BooleanSupplier provenanceHealthy) {
        this.connection = Objects.requireNonNull(connection);
        this.policy = Objects.requireNonNull(policy);
        this.resources = Objects.requireNonNull(resources);
        this.industry = Objects.requireNonNull(industry);
        this.operations = Objects.requireNonNull(operations);
        this.grants = Objects.requireNonNull(grants);
        this.adulthood = adulthood;
        this.seed = seed;
        this.provenanceHealthy = Objects.requireNonNull(provenanceHealthy);
    }

    public AutomaticForestryFounding withPublicSupplies(VillageSupplyPolicy supplies, StudyPolicy study) {
        this.publicSupply = Objects.requireNonNull(supplies);
        this.study = Objects.requireNonNull(study);
        return this;
    }

    /** Current complete frame replaces its scope, including an empty frame; refuses overflow rather than evicting evidence. */
    public synchronized boolean observe(UUID town, int chunkX, int chunkZ, ForestObservation evidence) {
        Objects.requireNonNull(town);
        Objects.requireNonNull(evidence);
        if (!evidence.complete()) return false;
        if (evidence.trunks().stream().anyMatch(p -> Math.floorDiv(p.x(), 16) != chunkX || Math.floorDiv(p.z(), 16) != chunkZ))
            return false;
        String key = frameKey(town, chunkX, chunkZ);
        Frame old = frames.get(key);
        if (old == null && frames.size() >= MAX_CACHED_FRAMES) return false;
        if (old != null && evidence.observedMillis() < old.evidence().observedMillis()) return false;
        if (old != null && evidence.observedMillis() == old.evidence().observedMillis()) {
            if (!old.evidence().complete()) return false; // Keep a conflict tombstone until a newer capture.
            if (!evidence.equals(old.evidence())) {
                frames.put(key, new Frame(town, chunkX, chunkZ,
                        new ForestObservation(evidence.world(), evidence.observedMillis(), false, List.of())));
                return false;
            }
        }
        frames.put(key, new Frame(town, chunkX, chunkZ, evidence));
        return true;
    }

    public record Opportunity(UUID town, ForestObservation observation) {
        public Opportunity {
            Objects.requireNonNull(town);
            Objects.requireNonNull(observation);
            if (!observation.complete() || observation.trunks().isEmpty())
                throw new IllegalArgumentException("A current complete forest frame with trunks is required");
        }
        public Position origin() { return observation.trunks().getFirst(); }
        /** Compatibility name for study adapters that treat an opportunity as its evidence patch. */
        public ForestObservation patch() { return observation; }
        public String key() {
            Position p = origin();
            return town + ":" + observation.world() + ":" + p.x() + ":" + p.y() + ":" + p.z();
        }
    }

    record Selection(Opportunity opportunity, String explanation) {}
    @FunctionalInterface public interface FounderTraining { String review(Opportunity opportunity, long now) throws SQLException; }
    public void configureTraining(FounderTraining training) { this.training = Objects.requireNonNull(training); }

    public String pulse(long step, long now, Set<UUID> worlds) throws SQLException {
        if (now < 0) throw new IllegalArgumentException("Invalid forestry founding observation time");
        pruneFrames(now);
        if (!enabled()) return "Automatic forestry founding disabled";
        var towns = townPage();
        if (towns.isEmpty() && !townCursor.isEmpty()) { townCursor = ""; towns = townPage(); }
        String latest = "No initialized villages to review";
        for (var town : towns) { latest = review(town, step, now, worlds); townCursor = town.toString(); }
        return latest;
    }

    private List<UUID> townPage() throws SQLException {
        var result = new ArrayList<UUID>();
        try (var s = connection.prepareStatement("SELECT settlement_id FROM economy_accounts WHERE settlement_id>? ORDER BY settlement_id LIMIT ?")) {
            s.setString(1, townCursor); s.setInt(2, policy.settlementsPerPulse());
            try (var r = s.executeQuery()) { while (r.next()) result.add(UUID.fromString(r.getString(1))); }
        }
        return result;
    }

    public String review(UUID town, long step, long now, Set<UUID> worlds) throws SQLException {
        if (step < 0 || now < 0) throw new IllegalArgumentException("Invalid forestry founding observation time");
        pruneFrames(now);
        if (!enabled()) return "Automatic forestry founding disabled";
        if (!provenanceHealthy.getAsBoolean()) return "Forestry founding paused: live provenance trust guard";
        if (!connection.getAutoCommit()) throw new SQLException("Forestry founding requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            String result = reviewRows(town, step, now, worlds == null ? Set.of() : Set.copyOf(worlds));
            if (!provenanceHealthy.getAsBoolean()) throw new BusinessStartupGrants.Declined("Forestry provenance trust changed during startup; all assets rolled back");
            connection.commit(); connection.setAutoCommit(true); return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); throw failure; }
            if (failure instanceof BusinessStartupGrants.Declined) return "Forestry founding waits: " + failure.getMessage();
            throw failure;
        }
    }

    private String reviewRows(UUID town, long step, long now, Set<UUID> worlds) throws SQLException {
        var choice = opportunityRows(town, now, worlds, null);
        if (choice.opportunity() == null) return choice.explanation();
        if (!provenanceHealthy.getAsBoolean()) return "Forestry founding paused: live provenance trust guard";
        UUID founder = founder(town);
        if (founder == null) return "Forestry founding waits: no qualified unemployed founder with forestry experience; no free skills"
                + (training == null ? "" : "; " + training.review(choice.opportunity(), now));

        var opportunity = choice.opportunity();
        var evidence = opportunity.observation();
        Position origin = opportunity.origin();
        UUID siteId = id("civilizations:automatic-forestry-site-v1:" + opportunity.key());
        UUID businessId = id("civilizations:automatic-forestry-business-v1:" + opportunity.key());
        String reason = "Automatic complete canopy-backed standing-tree evidence for actual funded timber demand; capture "
                + evidence.observedMillis() + "; provisional workplace anchor, no felling or land rights";
        var site = new BusinessSite(siteId, town, evidence.world(), origin.x(), origin.y(), origin.z(), evidence.observedMillis(), reason);
        var business = new Business(businessId, town, "Logging company", founder, founder, "logging", siteId,
                policy.workerCapacity(), step);
        long minimumAge = Math.max(adulthood, policy.minimumAgeSteps());
        var owner = new JobRequirements(minimumAge, Map.of("forestry", policy.founderExperience()));
        var worker = new JobRequirements(minimumAge, Map.of());
        var founding = new BusinessFounding(business, site, owner, worker, policy.startupHundredths(), reason);
        var request = new BusinessStartupGrants.WorkshopRequest(founding, industry.mining().storageCapacity(),
                industry.mining().storageSpacePerUnit(), policy.capitalHundredths(), founder);
        if (!stillCurrent(opportunity, now)) return "Forestry founding waits: observed forest frame changed before startup";
        if (!provenanceHealthy.getAsBoolean()) return "Forestry founding paused: live provenance trust guard";
        new BusinessStartupGrants(connection).startForestryRows(request, evidence, resources, grants);
        if (!provenanceHealthy.getAsBoolean()) throw new BusinessStartupGrants.Declined("Forestry provenance trust changed during startup; all assets rolled back");
        return "Founded one qualified logging business for actual funded timber demand; real grant, empty stock, current positive canopy evidence; no tree changes";
    }

    Selection opportunityRows(UUID town, long now, Set<UUID> worlds, Opportunity only) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Forestry opportunity requires owner transaction");
        if (now < 0) throw new IllegalArgumentException("Invalid forestry founding observation time");
        pruneFrames(now);
        if (!enabled() || !provenanceTrusted()) return new Selection(null, "Forestry founding disabled or provenance trust unavailable");
        var pressure = new LocalIndustryDemand(connection, industry, operations, provenanceHealthy).review(town, worlds, 64);
        if (!pressure.complete()) return new Selection(null, "Forestry founding waits: " + pressure.explanation());
        var demand = new TreeMap<>(pressure.uncovered());
        VillageSupplyPurchases.Demand publicDemand;
        try {
            publicDemand = new VillageSupplyPurchases(connection, publicSupply, study, operations)
                    .demandAfterSpending(town, worlds, Math.addExact(policy.startupHundredths(), policy.capitalHundredths()));
        } catch (VillageSupplyPurchases.Declined unavailable) {
            return new Selection(null, "Forestry founding waits: " + unavailable.getMessage());
        }
        long timberNeed = Math.addExact(demand.getOrDefault(Commodity.TIMBER, 0L),
                publicDemand.fundedUnits().getOrDefault(Commodity.TIMBER, 0L));
        if (timberNeed < 1) return new Selection(null, "No current funded timber customer");
        if (existingCapacity(town, timberNeed)) return new Selection(null, "Existing staffed logging stock and work capacity cover current timber demand");

        Position center = center(town);
        var candidates = currentFrames(town, now, worlds);
        if (candidates.isEmpty()) return new Selection(null, "No current complete loaded-world forest evidence");
        long period = 0;
        try (var s = connection.prepareStatement("SELECT completed_periods FROM economy_accounts WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { if (r.next()) period = r.getLong(1); }
        }
        Collections.shuffle(candidates, new Random(seed ^ town.getMostSignificantBits() ^ town.getLeastSignificantBits() ^ period));
        int examined = 0;
        for (var frame : candidates) {
            var evidence = frame.evidence();
            if (evidence.trunks().isEmpty()) continue;
            if (only != null && !only.key().equals(new Opportunity(town, evidence).key())) continue;
            if (++examined > policy.patchesPerReview()) break;
            // Never discard protected or previously claimed coordinates and reuse the remainder of a trunk set.
            if (!allSupportsAvailable(evidence)) continue;
            if (!within(center, new Opportunity(town, evidence).origin()) || occupied(evidence.world(), new Opportunity(town, evidence).origin())) continue;
            if (evidence.trunks().size() < resources.minimumTrunkCoordinates()) continue;
            long needSpace = industry.mining().storageSpacePerUnit().getOrDefault(Commodity.TIMBER, 0L);
            if (needSpace < 1 || needSpace > industry.mining().storageCapacity())
                return new Selection(null, "Forestry founding waits: configured store cannot fit timber");
            var funding = new BusinessStartupGrants(connection).previewRows(town, "logging", policy.startupHundredths(), policy.capitalHundredths(), grants);
            if (!funding.eligible()) return new Selection(null, "Forestry founding waits: " + funding.reason());
            var opportunity = new Opportunity(town, evidence);
            if (new BusinessRepository(connection).findBusiness(id("civilizations:automatic-forestry-business-v1:" + opportunity.key())).isPresent()) continue;
            if (!stillCurrent(opportunity, now)) continue;
            return new Selection(opportunity, "Current fundable forestry opportunity with real customers and current complete forest evidence");
        }
        return new Selection(null, "No viable new forest in bounded frame review: insufficient trunks, existing capacity, out-of-range anchor, occupied site, or no funding");
    }

    private synchronized List<Frame> currentFrames(UUID town, long now, Set<UUID> worlds) throws SQLException {
        var result = new ArrayList<Frame>();
        for (var frame : frames.values()) {
            if (!frame.town().equals(town)) continue;
            if (!frame.evidence().complete()) continue;
            long captured = frame.evidence().observedMillis();
            if (captured > now || now - captured > policy.surveyLifetimeMillis()) continue;
            if (!worlds.contains(frame.evidence().world())) continue;
            result.add(frame);
        }
        if (!result.isEmpty()) {
            UUID world = result.getFirst().evidence().world();
            if (result.stream().anyMatch(frame -> !frame.evidence().world().equals(world)))
                throw new BusinessStartupGrants.Declined("Current forest frames disagree on permanent world identity");
            try (var s = connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
                s.setString(1, town.toString());
                try (var r = s.executeQuery()) {
                    if (!r.next() || !world.toString().equals(r.getString(1))) return List.of();
                }
            }
        }
        result.sort(Comparator.comparingLong((Frame f) -> f.evidence().observedMillis()).reversed()
                .thenComparingInt(Frame::chunkX).thenComparingInt(Frame::chunkZ));
        return result;
    }

    private synchronized boolean stillCurrent(Opportunity opportunity, long now) {
        return frames.values().stream().anyMatch(frame -> frame.town().equals(opportunity.town())
                && frame.evidence().equals(opportunity.observation())
                && frame.evidence().observedMillis() <= now
                && now - frame.evidence().observedMillis() <= policy.surveyLifetimeMillis());
    }

    enum EvidenceState {UNKNOWN,PRESENT,ABSENT}
    synchronized EvidenceState evidenceState(Opportunity saved,long now) {
        var origin=saved.origin();var frame=frames.get(frameKey(saved.town(),Math.floorDiv(origin.x(),16),Math.floorDiv(origin.z(),16)));
        if(frame==null||!frame.evidence().complete()||!frame.evidence().world().equals(saved.patch().world())
                ||frame.evidence().observedMillis()>now||now-frame.evidence().observedMillis()>policy.surveyLifetimeMillis())return EvidenceState.UNKNOWN;
        return frame.evidence().trunks().contains(origin)?EvidenceState.PRESENT:EvidenceState.ABSENT;
    }
    boolean provenanceTrusted()throws SQLException {
        return provenanceHealthy.getAsBoolean()&&new ForestryTrackingRepository(connection).load().map(ForestryTrackingRepository.State::trusted).orElse(true);
    }

    private synchronized void pruneFrames(long now) {
        frames.values().removeIf(frame -> {
            long captured = frame.evidence().observedMillis();
            return captured > now || now - captured > policy.surveyLifetimeMillis();
        });
    }

    /** A single claimed, retired, or excluded support invalidates the whole observation; columns are never truncated. */
    private boolean allSupportsAvailable(ForestObservation evidence) throws SQLException {
        int minimumX=evidence.trunks().stream().mapToInt(Position::x).min().orElseThrow();
        int maximumX=evidence.trunks().stream().mapToInt(Position::x).max().orElseThrow();
        int minimumZ=evidence.trunks().stream().mapToInt(Position::z).min().orElseThrow();
        int maximumZ=evidence.trunks().stream().mapToInt(Position::z).max().orElseThrow();
        // A post-snapshot canopy change invalidates old positive evidence even when no trunk was removed.
        try(var s=connection.prepareStatement("SELECT 1 FROM forestry_exclusions WHERE world_id=? AND observed_ms>=? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ? LIMIT 1")) {
            s.setString(1,evidence.world().toString());s.setLong(2,evidence.observedMillis());
            s.setInt(3,Math.subtractExact(minimumX,8));s.setInt(4,Math.addExact(maximumX,8));
            s.setInt(5,Math.subtractExact(minimumZ,8));s.setInt(6,Math.addExact(maximumZ,8));
            try(var r=s.executeQuery()){if(r.next())return false;}
        }
        var supports = evidence.trunks();
        if (supports.isEmpty()) return false;
        return !hasAnyCoordinate("forestry_exclusions", evidence.world(), supports)
                && !hasAnyCoordinate("forestry_source_losses", evidence.world(), supports)
                && !hasAnyCoordinate("forestry_supports", evidence.world(), supports);
    }

    private boolean hasAnyCoordinate(String table, UUID world, List<Position> supports) throws SQLException {
        // The table name is selected only by the three private call sites above. Chunked binding stays below
        // SQLite's conservative parameter ceiling while handling the full 512-coordinate forest bound.
        for (int offset = 0; offset < supports.size(); offset += 300) {
            var batch = supports.subList(offset, Math.min(supports.size(), offset + 300));
            var sql = new StringBuilder("SELECT 1 FROM ").append(table).append(" WHERE world_id=? AND (x,y,z) IN (");
            sql.append(String.join(",", Collections.nCopies(batch.size(), "(?,?,?)"))).append(") LIMIT 1");
            try (var s = connection.prepareStatement(sql.toString())) {
                s.setString(1, world.toString());
                int parameter = 2;
                for (var p : batch) {
                    s.setInt(parameter++, p.x()); s.setInt(parameter++, p.y()); s.setInt(parameter++, p.z());
                }
                try (var r = s.executeQuery()) { if (r.next()) return true; }
            }
        }
        return false;
    }

    private boolean existingCapacity(UUID town, long needed) throws SQLException {
        var firms = new ArrayList<UUID>();
        try (var s = connection.prepareStatement("SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id JOIN forestry_resources f ON f.site_id=b.site_id WHERE b.settlement_id=? AND b.industry='logging' ORDER BY b.id LIMIT 65")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { while (r.next()) firms.add(UUID.fromString(r.getString(1))); }
        }
        if (firms.size() > 64) throw new BusinessStartupGrants.Declined("Existing logging capacity review budget exceeded");
        var repos = new IndustryRepository(connection);
        var forests = new ForestryRepository(connection);
        var availability = new BusinessOperations(connection, operations);
        long covered = 0;
        for (var firm : firms) {
            if (!availability.evaluate(firm).permitted()) continue;
            var store = repos.store(firm).orElseThrow();
            covered = Math.addExact(covered, store.units().getOrDefault(Commodity.TIMBER, 0L));
            if (covered >= needed) return true;
            var forest = forests.find(new BusinessRepository(connection).findFounding(firm).orElseThrow().site().id()).orElseThrow();
            int workers = forests.qualifiedWorkers(firm, operations);
            if (workers < 1 || forest.stock().available() < 1) continue;
            long periods = industry.supply().inputPeriods();
            long workerCapacity = Math.multiplyExact(Math.multiplyExact((long) workers, resources.unitsPerWorker()), periods);
            long siteCapacity = Math.multiplyExact(resources.siteWorkCapacity(), periods);
            long possible = Math.min(forest.stock().available(), Math.min(workerCapacity, siteCapacity));
            covered = Math.addExact(covered, possible);
            if (covered >= needed) return true;
        }
        return false;
    }

    private UUID founder(UUID town) throws SQLException {
        var candidates = new ArrayList<UUID>();
        try (var s = connection.prepareStatement("""
            SELECT r.id FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
              AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
              AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
              AND EXISTS(SELECT 1 FROM resident_skills k WHERE k.resident_id=r.id AND k.skill='forestry' AND k.experience>=?)
            ORDER BY r.id LIMIT ?
            """)) {
            s.setString(1, town.toString()); s.setLong(2, Math.max(adulthood, policy.minimumAgeSteps()));
            s.setLong(3, policy.founderExperience()); s.setInt(4, policy.foundersPerReview());
            try (var r = s.executeQuery()) { while (r.next()) candidates.add(UUID.fromString(r.getString(1))); }
        }
        for (var candidate : candidates)
            if (new PopulationRepository(connection).findWorkProfile(candidate).orElseThrow().experience().getOrDefault("forestry", 0L) >= policy.founderExperience()) return candidate;
        return null;
    }

    private Position center(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT center_x,center_y,center_z FROM settlements WHERE id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Unknown forestry settlement");
                return new Position(r.getInt(1), r.getInt(2), r.getInt(3));
            }
        }
    }

    private boolean within(Position center, Position p) {
        long dx = (long) p.x() - center.x(), dz = (long) p.z() - center.z();
        return Math.abs(dx) <= policy.horizontalRange() && Math.abs(dz) <= policy.horizontalRange()
                && dx * dx + dz * dz <= (long) policy.horizontalRange() * policy.horizontalRange()
                && Math.abs((long) p.y() - center.y()) <= policy.verticalRange();
    }

    private boolean occupied(UUID world, Position p) throws SQLException {
        try (var s = connection.prepareStatement("SELECT 1 FROM business_sites WHERE world_id=? AND x=? AND y=? AND z=? UNION ALL SELECT 1 FROM village_beds WHERE world_id=? AND x=? AND y=? AND z=? LIMIT 1")) {
            for (int start : new int[]{1, 5}) {
                s.setString(start, world.toString()); s.setInt(start + 1, p.x()); s.setInt(start + 2, p.y()); s.setInt(start + 3, p.z());
            }
            try (var r = s.executeQuery()) { return r.next(); }
        }
    }

    private boolean enabled() { return policy.enabled() && resources.enabled() && industry.enabled(); }
    private static String frameKey(UUID town, int chunkX, int chunkZ) { return town + ":" + chunkX + ":" + chunkZ; }
    private static UUID id(String key) { return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)); }
}
