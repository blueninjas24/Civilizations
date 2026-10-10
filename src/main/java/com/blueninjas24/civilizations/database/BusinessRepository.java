package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Explicit trusted-owner operations. No scheduler, automatic opportunities, world writes or production. */
public final class BusinessRepository {
    private final Connection connection;
    private final PopulationRepository population;

    public BusinessRepository(Connection connection) throws SQLException {
        this.connection = Objects.requireNonNull(connection);
        this.population = new PopulationRepository(connection);
    }

    /** Registration, qualification requirements and real startup debit commit together. ID is retry-safe. */
    public BusinessFounding found(BusinessFounding request) throws SQLException {
        Objects.requireNonNull(request);
        return transaction(() -> foundRows(request));
    }

    /** Package-local composition only; caller owns the complete startup transaction. */
    BusinessFounding foundRows(BusinessFounding request) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Founding rows require an owner transaction");
        Objects.requireNonNull(request);
        var saved = findFounding(request.business().id());
        if (saved.isPresent()) {
            if (!saved.orElseThrow().equals(request)) throw new SQLException("Business identity reused with different founding terms");
            return saved.orElseThrow();
        }
        var business = request.business();
        requireQualified(business.founderId(), business.settlementId(), request.founderRequirements());
        requireWorld(request.site());
        insertSite(request.site());
        try (var s = connection.prepareStatement("""
                INSERT INTO businesses(id,settlement_id,name,founder_id,owner_id,industry,site_id,
                    worker_capacity,founded_step,startup_hundredths,reason,founder_min_age,worker_min_age)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            s.setString(1,business.id().toString()); s.setString(2,business.settlementId().toString());
            s.setString(3,business.name()); s.setString(4,business.founderId().toString());
            s.setString(5,business.ownerId().toString()); s.setString(6,business.industry());
            s.setString(7,business.siteId().toString()); s.setInt(8,business.workerCapacity());
            s.setLong(9,business.foundedStep()); s.setLong(10,request.startupHundredths());
            s.setString(11,request.reason()); s.setLong(12,request.founderRequirements().minimumAgeSteps());
            s.setLong(13,request.workerRequirements().minimumAgeSteps()); s.executeUpdate();
        }
        saveRequirements(business.id(),"FOUNDER",request.founderRequirements());
        saveRequirements(business.id(),"WORKER",request.workerRequirements());
        if (request.startupHundredths()>0) {
            UUID operation = UUID.nameUUIDFromBytes(("civilizations:business-founding:"+business.id()).getBytes(StandardCharsets.UTF_8));
            new TreasuryRepository(connection).postRows(operation,business.settlementId(),-request.startupHundredths(),request.reason());
        }
        return request;
    }

    /** Terms remain historical inputs even after a resident moves, dies or improves their skills. */
    public Optional<BusinessFounding> findFounding(UUID id) throws SQLException {
        try (var s = connection.prepareStatement("""
                SELECT b.*, s.world_id,s.x,s.y,s.z,s.observed_millis,s.evidence
                FROM businesses b JOIN business_sites s ON s.id=b.site_id WHERE b.id=?
                """)) {
            s.setString(1,id.toString());
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                UUID town = UUID.fromString(r.getString("settlement_id"));
                UUID siteId = UUID.fromString(r.getString("site_id"));
                var business = new Business(id,town,r.getString("name"),UUID.fromString(r.getString("founder_id")),
                        UUID.fromString(r.getString("founder_id")),r.getString("industry"),siteId,r.getInt("worker_capacity"),r.getLong("founded_step"));
                var site = new BusinessSite(siteId,town,UUID.fromString(r.getString("world_id")),r.getInt("x"),r.getInt("y"),
                        r.getInt("z"),r.getLong("observed_millis"),r.getString("evidence"));
                return Optional.of(new BusinessFounding(business,site,requirements(id,"FOUNDER",r.getLong("founder_min_age")),
                        requirements(id,"WORKER",r.getLong("worker_min_age")),r.getLong("startup_hundredths"),r.getString("reason")));
            }
        }
    }

    /** Current ownership is separate from the immutable accepted founding terms. */
    public Optional<Business> findBusiness(UUID id) throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM businesses WHERE id=?")) {
            s.setString(1,id.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();
                return Optional.of(new Business(id,UUID.fromString(r.getString("settlement_id")),r.getString("name"),
                        UUID.fromString(r.getString("founder_id")),UUID.fromString(r.getString("owner_id")),
                        r.getString("industry"),UUID.fromString(r.getString("site_id")),r.getInt("worker_capacity"),r.getLong("founded_step")));
            }
        }
    }

    public List<Business> findBusinesses(UUID town) throws SQLException {
        List<Business> result = new ArrayList<>();
        try (var s = connection.prepareStatement("SELECT id FROM businesses WHERE settlement_id=? ORDER BY id")) {
            s.setString(1,town.toString());
            try (var r = s.executeQuery()) {
                while (r.next()) result.add(findBusiness(UUID.fromString(r.getString(1))).orElseThrow());
            }
        }
        return List.copyOf(result);
    }

    /** One primary current job per resident, finite capacity, actual saved age/skills and local living labor. */
    public Employment hire(UUID operation, UUID businessId, UUID residentId, long step) throws SQLException {
        var proposed=new Employment(operation,businessId,residentId,step,null,null);
        return transaction(() -> hireRows(proposed.id(),proposed.businessId(),proposed.residentId(),proposed.startedStep()));
    }
    Employment hireRows(UUID operation, UUID businessId, UUID residentId, long step) throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Employment rows require an owner transaction");
        var proposed = new Employment(operation,businessId,residentId,step,null,null);
        var old = findEmployment(operation);
        if (old.isPresent()) {
            var saved = old.orElseThrow();
            if (!saved.businessId().equals(businessId) || !saved.residentId().equals(residentId) || saved.startedStep()!=step)
                throw new SQLException("Employment identity reused with different inputs");
            return saved; // Replaying an ended episode never resurrects it.
        }
        var founding = findFounding(businessId).orElseThrow(() -> new SQLException("Unknown business"));
        if (step < founding.business().foundedStep()) throw new SQLException("Employment predates business");
        requireQualified(residentId,founding.business().settlementId(),founding.workerRequirements());
        try (var s = connection.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND ended_step IS NULL")) {
            s.setString(1,businessId.toString());
            try (var r = s.executeQuery()) {
                if (!r.next() || r.getInt(1)>=founding.business().workerCapacity()) throw new SQLException("Business has no vacant worker capacity");
            }
        }
        try (var s = connection.prepareStatement("INSERT INTO business_employment(id,business_id,resident_id,started_step) VALUES(?,?,?,?)")) {
            s.setString(1,operation.toString()); s.setString(2,businessId.toString());
            s.setString(3,residentId.toString()); s.setLong(4,step); s.executeUpdate();
        }
        return proposed;
    }

    public Optional<Employment> findEmployment(UUID id) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM business_employment WHERE id=?")) {
            s.setString(1,id.toString());
            try (var r = s.executeQuery()) { return r.next() ? Optional.of(readEmployment(r)) : Optional.empty(); }
        }
    }

    public List<Employment> employmentHistory(UUID businessId) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM business_employment WHERE business_id=? ORDER BY started_step,id")) {
            s.setString(1,businessId.toString()); List<Employment> result = new ArrayList<>();
            try (var r = s.executeQuery()) { while (r.next()) result.add(readEmployment(r)); }
            return List.copyOf(result);
        }
    }

    /** Historical open assignments never count deceased or emigrated people as productive workers. */
    public List<Employment> currentWorkers(UUID businessId) throws SQLException {
        try (var s = connection.prepareStatement("""
                SELECT e.* FROM business_employment e JOIN businesses b ON b.id=e.business_id
                JOIN residents r ON r.id=e.resident_id
                WHERE e.business_id=? AND e.ended_step IS NULL AND r.status='ALIVE' AND r.settlement_id=b.settlement_id
                ORDER BY e.resident_id
                """)) {
            s.setString(1,businessId.toString()); List<Employment> result = new ArrayList<>();
            try (var r = s.executeQuery()) { while (r.next()) result.add(readEmployment(r)); }
            return List.copyOf(result);
        }
    }

    public Employment endEmployment(UUID id, long step, String reason) throws SQLException {
        Business.text(reason,2048);
        return transaction(() -> endRows(id,step,reason));
    }

    /** Future owner must call before hiring/production. Saves observed separation, not an invented past date. */
    public List<Employment> reconcileEmployment(UUID town, long step) throws SQLException {
        return reconcile("b.settlement_id", town, step);
    }

    /** Bounded owner runtime visits individual businesses; a town with many firms cannot monopolize a pulse. */
    public List<Employment> reconcileBusiness(UUID business, long step) throws SQLException {
        return reconcile("b.id", business, step);
    }

    private List<Employment> reconcile(String scope, UUID id, long step) throws SQLException {
        if (step < 0) throw new IllegalArgumentException("Negative reconciliation step");
        return transaction(() -> {
            Map<UUID,String> invalid = new LinkedHashMap<>();
            try (var s = connection.prepareStatement("""
                    SELECT e.id,r.status FROM business_employment e JOIN businesses b ON b.id=e.business_id
                    JOIN residents r ON r.id=e.resident_id
                    WHERE %s=? AND e.ended_step IS NULL AND (r.status<>'ALIVE' OR r.settlement_id<>b.settlement_id)
                    ORDER BY e.id
                    """.formatted(scope))) {
                s.setString(1,id.toString());
                try (var r = s.executeQuery()) {
                    while (r.next()) invalid.put(UUID.fromString(r.getString(1)),r.getString(2).equals("DECEASED")
                            ? "Resident is deceased at staffing reconciliation" : "Resident left the business settlement at staffing reconciliation");
                }
            }
            List<Employment> ended = new ArrayList<>();
            for (var entry : invalid.entrySet()) ended.add(endRows(entry.getKey(),step,entry.getValue()));
            return List.copyOf(ended);
        });
    }

    private Employment endRows(UUID id, long step, String reason) throws SQLException {
        var old = findEmployment(id).orElseThrow(() -> new SQLException("Unknown employment"));
        var ended = new Employment(id,old.businessId(),old.residentId(),old.startedStep(),step,reason);
        if (old.endedStep()!=null) {
            if (!old.equals(ended)) throw new SQLException("Employment already ended with different terms");
            return old;
        }
        try (var s = connection.prepareStatement("UPDATE business_employment SET ended_step=?,end_reason=? WHERE id=? AND ended_step IS NULL")) {
            s.setLong(1,step); s.setString(2,reason); s.setString(3,id.toString());
            if (s.executeUpdate()!=1) throw new SQLException("Stale employment separation");
        }
        return ended;
    }

    private void requireQualified(UUID residentId, UUID town, JobRequirements requirements) throws SQLException {
        var person = population.findResident(residentId).orElseThrow(() -> new SQLException("Unknown resident"));
        if (!person.settlementId().equals(town)) throw new SQLException("Resident belongs to another settlement");
        var result = WorkEligibility.evaluate(person,population.findWorkProfile(residentId).orElseThrow(),requirements);
        if (!result.eligible()) throw new SQLException("Work qualifications unmet: "+String.join("; ",result.unmetRequirements()));
    }

    private void requireWorld(BusinessSite site) throws SQLException {
        try (var s = connection.prepareStatement("SELECT world_id FROM settlement_worlds WHERE settlement_id=?")) {
            s.setString(1,site.settlementId().toString());
            try (var r = s.executeQuery()) {
                if (!r.next() || !r.getString(1).equals(site.worldId().toString()))
                    throw new SQLException("Business site requires the settlement's permanent world identity");
            }
        }
    }

    private void insertSite(BusinessSite site) throws SQLException {
        try (var s = connection.prepareStatement("INSERT INTO business_sites(id,settlement_id,world_id,x,y,z,observed_millis,evidence) VALUES(?,?,?,?,?,?,?,?)")) {
            s.setString(1,site.id().toString()); s.setString(2,site.settlementId().toString()); s.setString(3,site.worldId().toString());
            s.setInt(4,site.x()); s.setInt(5,site.y()); s.setInt(6,site.z()); s.setLong(7,site.observedMillis()); s.setString(8,site.evidence()); s.executeUpdate();
        }
    }

    private void saveRequirements(UUID business, String role, JobRequirements requirements) throws SQLException {
        for (var entry : new TreeMap<>(requirements.minimumExperience()).entrySet()) {
            try (var s = connection.prepareStatement("INSERT INTO business_skill_requirements(business_id,role,skill,experience) VALUES(?,?,?,?)")) {
                s.setString(1,business.toString()); s.setString(2,role); s.setString(3,entry.getKey()); s.setLong(4,entry.getValue()); s.executeUpdate();
            }
        }
    }

    private JobRequirements requirements(UUID business, String role, long age) throws SQLException {
        Map<String,Long> skills = new TreeMap<>();
        try (var s = connection.prepareStatement("SELECT skill,experience FROM business_skill_requirements WHERE business_id=? AND role=? ORDER BY skill")) {
            s.setString(1,business.toString()); s.setString(2,role);
            try (var r = s.executeQuery()) { while (r.next()) skills.put(r.getString(1),r.getLong(2)); }
        }
        return new JobRequirements(age,skills);
    }

    private static Employment readEmployment(ResultSet r) throws SQLException {
        long ended = r.getLong("ended_step"); Long end = r.wasNull() ? null : ended;
        return new Employment(UUID.fromString(r.getString("id")),UUID.fromString(r.getString("business_id")),
                UUID.fromString(r.getString("resident_id")),r.getLong("started_step"),end,r.getString("end_reason"));
    }

    @FunctionalInterface private interface Operation<T> { T run() throws SQLException; }
    private <T> T transaction(Operation<T> operation) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Business operation requires no active transaction");
        connection.setAutoCommit(false);
        try {
            T result = operation.run(); connection.commit(); connection.setAutoCommit(true); return result;
        } catch (SQLException | RuntimeException failure) {
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
}
