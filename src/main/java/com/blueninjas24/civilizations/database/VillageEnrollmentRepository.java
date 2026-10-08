package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Owner-thread atomic positive import; no Bukkit work, implicit merge or census overwrite. */
public final class VillageEnrollmentRepository {
    public record Policy(int homeBedRadius, int residentsPerBed, long initialAdultAgeSteps,
                         long initialChildAgeSteps, PartnershipFormation.Policy partnerships) {
        public Policy {
            if (homeBedRadius < 1 || homeBedRadius > 32 || residentsPerBed < 1 || residentsPerBed > 8
                    || initialAdultAgeSteps < 0 || initialChildAgeSteps < 0 || initialChildAgeSteps > initialAdultAgeSteps)
                throw new IllegalArgumentException("Invalid editable enrollment settings");
            Objects.requireNonNull(partnerships);
        }
        public static Policy prototype() {
            return new Policy(8, 2, 240, 0, new PartnershipFormation.Policy(true, 216));
        }
    }
    public record Result(UUID settlementId, boolean createdSettlement, int importedPeople,
                         Map<UUID, Resident> observedResidents, int createdPartnerships) {
        public Result { observedResidents = Map.copyOf(observedResidents); }
    }
    private final DatabaseManager database;
    private final Connection connection;
    VillageEnrollmentRepository(DatabaseManager database) {
        this.database = database; this.connection = database.getConnection();
    }

    public Result enroll(VillageObservation observation, VillageRecognition.Candidate candidate, Policy policy) throws SQLException {
        if (candidate.beds().isEmpty() || candidate.villagers().isEmpty()
                || !observation.beds().containsAll(candidate.beds()) || !observation.villagers().containsAll(candidate.villagers())
                || candidate.complete() != observation.complete())
            throw new IllegalArgumentException("Candidate must come from this populated observation");
        if (!connection.getAutoCommit()) throw new SQLException("Enrollment requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var identity = database.getVillageIdentityRepository();
            var match = identity.match(observation.worldId(), candidate);
            if (match.type() == VillageIdentityRepository.MatchType.CONFLICT)
                throw new SQLException("Village identity conflict: " + match.settlementIds());
            identity.observeWorld(observation.worldId(), observation.worldName());
            boolean created = match.type() == VillageIdentityRepository.MatchType.NEW;
            UUID settlementId = created ? UUID.randomUUID() : match.settlementIds().getFirst();
            if (created) {
                rejectUnboundLegacy();
                var center = candidate.beds().getFirst();
                database.saveSettlement(new Settlement(settlementId, "Village " + settlementId.toString().substring(0, 8),
                        observation.worldName(), center.x(), center.y(), center.z(), observation.observedMillis(), 0, 0));
                identity.bindSettlement(settlementId, observation.worldId());
            }
            var people = database.getPopulationRepository();
            var homes = new HashMap<VillageObservation.Position, UUID>();
            var observedBedsPerHome = new HashMap<UUID, Integer>();
            for (var group : homeGroups(candidate.beds(), policy.homeBedRadius())) {
                var knownHomes = new HashSet<UUID>();
                for (var bed : group) {
                    identity.bindBed(observation.worldId(), bed, settlementId);
                    UUID home = readBedHome(observation.worldId(), bed);
                    if (home != null) knownHomes.add(home);
                }
                if (knownHomes.size() > 1) throw new SQLException("Observed home group overlaps saved households; explicit reconciliation required");
                UUID home = knownHomes.isEmpty() ? UUID.randomUUID() : knownHomes.iterator().next();
                if (knownHomes.isEmpty()) people.insertHousehold(new Household(home, settlementId, "Home " + home.toString().substring(0, 8)));
                for (var bed : group) {
                    assignBedHome(observation.worldId(), bed, home, settlementId);
                    homes.put(bed, home); observedBedsPerHome.merge(home, 1, Math::addExact);
                }
            }
            // A partial frame supplies identity evidence, never a complete housing census.
            try (var statement = connection.prepareStatement("""
                    DELETE FROM household_capacity WHERE household_id IN (SELECT id FROM households WHERE settlement_id = ?)
                    """)) { statement.setString(1, settlementId.toString()); statement.executeUpdate(); }
            if (observation.complete()) {
                for (var entry : observedBedsPerHome.entrySet()) database.getDemographicRepository().recordCapacity(
                        entry.getKey(), Math.multiplyExact(entry.getValue(), policy.residentsPerBed()));
            }
            int imported = 0;
            var bindings = new HashMap<UUID, Resident>();
            for (var observed : candidate.villagers()) {
                var known = identity.findResident(observation.worldId(), observed.entityId());
                Resident person;
                if (known.isPresent()) {
                    person = people.findResident(known.get()).orElseThrow(() -> new SQLException("Broken resident identity link"));
                } else {
                    UUID residentId = UUID.nameUUIDFromBytes(("Civilizations:resident:" + observation.worldId() + ":" + observed.entityId())
                            .getBytes(StandardCharsets.UTF_8));
                    var nearest = candidate.beds().stream().min(Comparator
                            .comparingDouble((VillageObservation.Position bed) -> bed.squaredDistance(observed.position()))
                            .thenComparing(VillageObservation.Position.ORDER)).orElseThrow();
                    person = new Resident(residentId, observed.existingName() == null ? "Resident " + residentId.toString().substring(0, 8)
                            : observed.existingName(), settlementId, homes.get(nearest),
                            observed.adult() ? policy.initialAdultAgeSteps() : policy.initialChildAgeSteps(), Resident.Status.ALIVE);
                    people.insertResident(person); identity.bindEntity(observation.worldId(), observed.entityId(), residentId); imported++;
                }
                bindings.put(observed.entityId(), person);
            }
            var formation = PartnershipFormation.form(database.getDemographicRepository().readPopulationState(), policy.partnerships());
            int added = 0;
            for (var tie : formation.addedPartnerships()) {
                // Only this observed settlement is enrolled, not unrelated unseen towns.
                var first = people.findResident(tie.first()).orElseThrow();
                if (first.settlementId().equals(settlementId)) { people.insertFamilyTie(tie); added++; }
            }
            try (var statement = connection.prepareStatement("UPDATE settlements SET population = ?, beds = CASE WHEN ? THEN ? ELSE beds END WHERE id = ?")) {
                statement.setLong(1, people.countLivingResidents(settlementId)); statement.setBoolean(2, observation.complete());
                statement.setInt(3, candidate.beds().size()); statement.setString(4, settlementId.toString()); statement.executeUpdate();
            }
            connection.commit(); connection.setAutoCommit(true);
            return new Result(settlementId, created, imported, bindings, added);
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
    }

    private void rejectUnboundLegacy() throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT s.id FROM settlements s LEFT JOIN settlement_worlds w ON w.settlement_id = s.id
                WHERE w.settlement_id IS NULL LIMIT 1
                """)) {
            try (var result = statement.executeQuery()) {
                if (result.next()) throw new SQLException("Legacy settlement requires explicit world/identity adoption before new enrollment: " + result.getString(1));
            }
        }
    }
    private UUID readBedHome(UUID world, VillageObservation.Position bed) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT household_id FROM village_beds WHERE world_id = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world.toString()); statement.setInt(2, bed.x()); statement.setInt(3, bed.y()); statement.setInt(4, bed.z());
            try (var result = statement.executeQuery()) { return result.next() && result.getString(1) != null ? UUID.fromString(result.getString(1)) : null; }
        }
    }
    private void assignBedHome(UUID world, VillageObservation.Position bed, UUID home, UUID settlement) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE village_beds SET household_id = ? WHERE world_id = ? AND x = ? AND y = ? AND z = ?
                AND settlement_id = ? AND (household_id IS NULL OR household_id = ?)
                AND EXISTS (SELECT 1 FROM households WHERE id = ? AND settlement_id = ?)
                """)) {
            statement.setString(1, home.toString()); statement.setString(2, world.toString()); statement.setInt(3, bed.x());
            statement.setInt(4, bed.y()); statement.setInt(5, bed.z()); statement.setString(6, settlement.toString());
            statement.setString(7, home.toString()); statement.setString(8, home.toString()); statement.setString(9, settlement.toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Bed household identity conflict");
        }
    }
    private static List<List<VillageObservation.Position>> homeGroups(List<VillageObservation.Position> beds, int radius) {
        var remaining = new TreeSet<>(VillageObservation.Position.ORDER); remaining.addAll(beds);
        var groups = new ArrayList<List<VillageObservation.Position>>();
        while (!remaining.isEmpty()) {
            var pending = new ArrayDeque<VillageObservation.Position>(); pending.add(remaining.pollFirst());
            var group = new ArrayList<VillageObservation.Position>();
            while (!pending.isEmpty()) {
                var current = pending.removeFirst(); group.add(current);
                var neighbors = remaining.stream().filter(other -> current.squaredDistance(other) <= (double) radius * radius).toList();
                remaining.removeAll(neighbors); pending.addAll(neighbors);
            }
            groups.add(List.copyOf(group));
        }
        return groups;
    }
}
