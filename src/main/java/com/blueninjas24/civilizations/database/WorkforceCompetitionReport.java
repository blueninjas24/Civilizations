package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.BusinessOperationPolicy;
import com.blueninjas24.civilizations.population.*;
import java.sql.*;
import java.util.*;

/** Bounded read-only view of overlapping saved worker qualifications; it does not rank firms or assign people. */
public final class WorkforceCompetitionReport {
    public static final int MAX_FIRMS = 32;
    public static final int MAX_RESIDENT_CANDIDATES = 512;
    private static final int MAX_SAVED_WORKER_ASSIGNMENTS = 512;
    private static final int MAX_REQUIREMENT_ROWS = 4096;
    private static final int MAX_SKILL_ROWS = 65_536;

    public record Firm(UUID id, String industry, int workers, int vacancies, int qualifiedIdleCandidates,
                       Long businessCashHundredths, boolean available, String availabilityReason) {
        public Firm {
            Objects.requireNonNull(id); Objects.requireNonNull(industry); Objects.requireNonNull(availabilityReason);
            if (workers < 0 || vacancies < 0 || qualifiedIdleCandidates < 0
                    || (businessCashHundredths != null && businessCashHundredths < 0))
                throw new IllegalArgumentException("Invalid workforce report row");
        }
    }

    public record Snapshot(UUID settlement, boolean complete, int residentCandidatePool,
                           List<Firm> firms, String explanation) {
        public Snapshot {
            Objects.requireNonNull(settlement); firms = List.copyOf(firms); Objects.requireNonNull(explanation);
            if (residentCandidatePool < 0) throw new IllegalArgumentException("Invalid candidate pool size");
        }

        public List<String> describe() {
            var lines = new ArrayList<String>();
            if (!complete) {
                lines.add("Workforce comparison incomplete: " + explanation + "; no partial comparison is shown.");
                return List.copyOf(lines);
            }
            lines.add("Read-only workforce comparison for " + settlement + ": " + firms.size() + " firms; "
                    + residentCandidatePool + " living, unassigned residents outside active courses.");
            lines.add("Qualified candidate counts overlap and are not additive. This is not a ranking or hiring decision; "
                    + "existing automatic hiring visits firms in bounded UUID/page order. No jobs, funds, courses, wages, or closures changed.");
            for (var firm : firms) {
                lines.add(firm.id() + " [" + firm.industry() + "]: " + firm.workers() + " workers, " + firm.vacancies()
                        + " vacancies, " + firm.qualifiedIdleCandidates() + " skill/age-qualified idle residents, business cash "
                        + (firm.businessCashHundredths() == null ? "unknown (no saved business store)"
                        : firm.businessCashHundredths() + " hundredths") + "; " + (firm.available() ? "available" : "unavailable")
                        + " (" + firm.availabilityReason() + ").");
            }
            if (firms.isEmpty()) lines.add("No firms are currently registered for this settlement.");
            lines.add(explanation);
            return List.copyOf(lines);
        }
    }

    private record FirmTerms(UUID id, String industry, int capacity, long minimumAge, Long cash) {}

    private final Connection connection;

    public WorkforceCompetitionReport(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Reads at most 32 firms and 512 otherwise-idle local residents. All profiles and saved job requirements
     * are loaded in bounded batches, then qualification is evaluated in memory.
     */
    public Snapshot read(UUID town, BusinessOperationPolicy policy) throws SQLException {
        Objects.requireNonNull(town); Objects.requireNonNull(policy);
        if (!connection.getAutoCommit()) throw new SQLException("Workforce report requires an idle SQL owner");

        if (!settlementExists(town))
            return incomplete(town, "Unknown settlement; no workforce comparison is available.");

        var firms = readFirms(town);
        if (firms.size() > MAX_FIRMS)
            return incomplete(town, "firm review budget exceeded (more than " + MAX_FIRMS + ")");

        long adultFloor = firms.stream().mapToLong(FirmTerms::minimumAge).min().orElse(0);
        var residents = firms.isEmpty() ? List.<Resident>of() : readCandidates(town, adultFloor);
        if (residents.size() > MAX_RESIDENT_CANDIDATES)
            return incomplete(town, "resident candidate page exceeded " + MAX_RESIDENT_CANDIDATES);

        var profiles = readProfiles(residents);
        var workerCounts = readWorkers(firms);
        var requirementSkills = readRequirements(firms);
        if (profiles == null) return incomplete(town, "resident skill profile budget exceeded");
        if (workerCounts == null) return incomplete(town, "saved worker assignment budget exceeded");
        if (workerCounts.values().stream().mapToLong(Integer::longValue).sum() > MAX_SAVED_WORKER_ASSIGNMENTS)
            return incomplete(town, "saved worker assignment budget exceeded (more than " + MAX_SAVED_WORKER_ASSIGNMENTS + ")");
        if (requirementSkills == null) return incomplete(town, "saved worker-requirement budget exceeded");

        var businessOperations = new BusinessOperations(connection, policy);
        var result = new ArrayList<Firm>(firms.size());
        for (var firm : firms) {
            int qualified = 0;
            var requirements = new JobRequirements(firm.minimumAge(), requirementSkills.getOrDefault(firm.id(), Map.of()));
            for (var resident : residents) {
                if (WorkEligibility.evaluate(resident, profiles.get(resident.id()), requirements).eligible()) qualified++;
            }
            var availability = businessOperations.evaluate(firm.id());
            int workers = workerCounts.getOrDefault(firm.id(), 0);
            result.add(new Firm(firm.id(), firm.industry(), workers, Math.max(0, firm.capacity() - workers),
                    qualified, firm.cash(), availability.permitted(), availability.explanation()));
        }
        return new Snapshot(town, true, residents.size(), result,
                "Qualification uses each firm's saved worker age/skill terms and current living local residents; availability is checked independently.");
    }

    private boolean settlementExists(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT 1 FROM settlements WHERE id=?")) {
            s.setString(1, town.toString());
            try (var r = s.executeQuery()) { return r.next(); }
        }
    }

    private List<FirmTerms> readFirms(UUID town) throws SQLException {
        var firms = new ArrayList<FirmTerms>();
        try (var s = connection.prepareStatement("""
                SELECT b.id,b.industry,b.worker_capacity,b.worker_min_age,k.hundredths
                FROM businesses b LEFT JOIN industry_stores k ON k.business_id=b.id
                WHERE b.settlement_id=? ORDER BY b.id LIMIT ?
                """)) {
            s.setString(1, town.toString()); s.setInt(2, MAX_FIRMS + 1);
            try (var r = s.executeQuery()) {
                while (r.next()) {
                    Object savedCash=r.getObject(5);
                    Long cash=savedCash==null?null:((Number)savedCash).longValue();
                    firms.add(new FirmTerms(UUID.fromString(r.getString(1)), r.getString(2), r.getInt(3),r.getLong(4),cash));
                }
            }
        }
        return firms;
    }

    private Map<UUID, Integer> readWorkers(List<FirmTerms> firms) throws SQLException {
        if (firms.isEmpty()) return Map.of();
        var ids = firms.stream().map(f -> f.id().toString()).toList();
        var result = new HashMap<UUID, Integer>();
        try (var s = connection.prepareStatement("SELECT business_id,COUNT(*) FROM business_employment WHERE ended_step IS NULL AND business_id IN ("
                + placeholders(ids.size()) + ") GROUP BY business_id")) {
            bindStrings(s, ids, 1);
            try (var r = s.executeQuery()) { while (r.next()) result.put(UUID.fromString(r.getString(1)), r.getInt(2)); }
        }
        return result;
    }

    private List<Resident> readCandidates(UUID town, long adultFloor) throws SQLException {
        var residents = new ArrayList<Resident>();
        try (var s = connection.prepareStatement("""
                SELECT r.id,r.name,r.household_id,r.age_steps,r.status
                FROM residents r WHERE r.settlement_id=? AND r.status='ALIVE' AND r.age_steps>=?
                  AND NOT EXISTS(SELECT 1 FROM business_employment e WHERE e.resident_id=r.id AND e.ended_step IS NULL)
                  AND NOT EXISTS(SELECT 1 FROM resident_courses c WHERE c.resident_id=r.id AND c.status='ACTIVE')
                ORDER BY r.id LIMIT ?
                """)) {
            s.setString(1, town.toString()); s.setLong(2, adultFloor); s.setInt(3, MAX_RESIDENT_CANDIDATES + 1);
            try (var r = s.executeQuery()) {
                while (r.next()) {
                    String household = r.getString("household_id");
                    residents.add(new Resident(UUID.fromString(r.getString("id")), r.getString("name"), town,
                            household == null ? null : UUID.fromString(household), r.getLong("age_steps"),
                            Resident.Status.valueOf(r.getString("status"))));
                }
            }
        }
        return residents;
    }

    /** Returns null on overflow; one bounded query loads all candidate profiles. */
    private Map<UUID, WorkProfile> readProfiles(List<Resident> residents) throws SQLException {
        var profiles = new HashMap<UUID, Map<String, Long>>();
        for (var resident : residents) profiles.put(resident.id(), new TreeMap<>());
        if (residents.isEmpty()) return Map.of();
        var ids = residents.stream().map(r -> r.id().toString()).toList();
        try (var s = connection.prepareStatement("SELECT resident_id,skill,experience FROM resident_skills WHERE resident_id IN ("
                + placeholders(ids.size()) + ") ORDER BY resident_id,skill LIMIT " + (MAX_SKILL_ROWS + 1))) {
            bindStrings(s, ids, 1);
            try (var r = s.executeQuery()) {
                int count = 0;
                while (r.next()) {
                    if (++count > MAX_SKILL_ROWS) return null;
                    profiles.get(UUID.fromString(r.getString(1))).put(r.getString(2), r.getLong(3));
                }
            }
        }
        var result = new HashMap<UUID, WorkProfile>();
        profiles.forEach((id, skills) -> result.put(id, new WorkProfile(id, skills)));
        return Map.copyOf(result);
    }

    /** Returns null on overflow; one bounded query loads all firms' worker requirements. */
    private Map<UUID, Map<String, Long>> readRequirements(List<FirmTerms> firms) throws SQLException {
        if (firms.isEmpty()) return Map.of();
        var ids = firms.stream().map(f -> f.id().toString()).toList();
        var result = new HashMap<UUID, Map<String, Long>>();
        try (var s = connection.prepareStatement("SELECT business_id,skill,experience FROM business_skill_requirements WHERE role='WORKER' AND business_id IN ("
                + placeholders(ids.size()) + ") ORDER BY business_id,skill LIMIT " + (MAX_REQUIREMENT_ROWS + 1))) {
            bindStrings(s, ids, 1);
            try (var r = s.executeQuery()) {
                int count = 0;
                while (r.next()) {
                    if (++count > MAX_REQUIREMENT_ROWS) return null;
                    result.computeIfAbsent(UUID.fromString(r.getString(1)), ignored -> new TreeMap<>())
                            .put(r.getString(2), r.getLong(3));
                }
            }
        }
        return result;
    }

    private static Snapshot incomplete(UUID town, String reason) { return new Snapshot(town, false, 0, List.of(), reason); }
    private static String placeholders(int count) { return String.join(",", Collections.nCopies(count, "?")); }
    private static void bindStrings(PreparedStatement statement, List<String> values, int first) throws SQLException {
        for (int i = 0; i < values.size(); i++) statement.setString(first + i, values.get(i));
    }
}
