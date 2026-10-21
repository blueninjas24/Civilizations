package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.BuildingRegistry.Declined;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

/** SQL-owner occupancy history. No Paper, people edits or legacy-capacity replacement.
 * Live scheduling/capacity integration remains separate from this transactional backend. */
public final class DwellingOccupancy {
    public static final long MAX_REVIEW_AGE=30_000;
    public record Context(UUID world,boolean present,Policy policy,long now) {
        public Context{Objects.requireNonNull(world);Objects.requireNonNull(policy);if(now<0)throw new IllegalArgumentException("Negative housing time");}
    }
    public enum Outcome { RECORDED, REPLAY, UNVERIFIED, OLDER }
    public record Review(UUID operation,UUID building,UUID consent,String fingerprint,long observedMillis,long recordedMillis,boolean covered){}
    public record UnitStatus(UUID unit,UUID building,int beds,Set<Issue> issues,Review review){public UnitStatus{issues=Set.copyOf(issues);}}
    public record Assignment(UUID operation,UUID household,UUID unit,UUID review,UUID previous,long atMillis) {
        public Assignment{Objects.requireNonNull(operation);Objects.requireNonNull(household);Objects.requireNonNull(unit);Objects.requireNonNull(review);if(atMillis<0)throw new IllegalArgumentException("Negative assignment time");}
    }
    public record Departure(UUID operation,UUID assignment,String reason,UUID consent,long atMillis){}
    public record Vacancies(boolean verified,List<UnitStatus> units,String explanation){public Vacancies{units=List.copyOf(units);}}
    private final Connection connection;
    public DwellingOccupancy(Connection connection){this.connection=Objects.requireNonNull(connection);}

    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE dwelling_reviews (
                  operation_id TEXT PRIMARY KEY NOT NULL, building_id TEXT NOT NULL REFERENCES building_records(id),
                  consent_id TEXT NOT NULL REFERENCES building_consent_events(operation_id),
                  fingerprint TEXT NOT NULL CHECK(length(fingerprint)=64),
                  observed_ms INTEGER NOT NULL CHECK(typeof(observed_ms)='integer' AND observed_ms>=0),
                  recorded_ms INTEGER NOT NULL CHECK(typeof(recorded_ms)='integer' AND recorded_ms>=observed_ms),
                  require_cover INTEGER NOT NULL CHECK(require_cover IN (0,1)), UNIQUE(operation_id,building_id)
                )
                """);
            s.execute("""
                CREATE TABLE dwelling_review_units (
                  review_id TEXT NOT NULL REFERENCES dwelling_reviews(operation_id),
                  unit_id TEXT NOT NULL REFERENCES building_units(id),
                  beds INTEGER NOT NULL CHECK(typeof(beds)='integer' AND beds BETWEEN 0 AND 512),
                  issues TEXT NOT NULL CHECK(length(issues)<=512), PRIMARY KEY(review_id,unit_id)
                )
                """);
            s.execute("""
                CREATE TABLE dwelling_current_reviews (
                  building_id TEXT PRIMARY KEY NOT NULL REFERENCES building_records(id), review_id TEXT NOT NULL,
                  FOREIGN KEY(review_id,building_id) REFERENCES dwelling_reviews(operation_id,building_id)
                )
                """);
            s.execute("""
                CREATE TABLE dwelling_assignments (
                  operation_id TEXT PRIMARY KEY NOT NULL, household_id TEXT NOT NULL REFERENCES households(id),
                  unit_id TEXT NOT NULL REFERENCES building_units(id), review_id TEXT NOT NULL,
                  previous_assignment TEXT REFERENCES dwelling_assignments(operation_id),
                  at_ms INTEGER NOT NULL CHECK(typeof(at_ms)='integer' AND at_ms>=0),
                  FOREIGN KEY(review_id,unit_id) REFERENCES dwelling_review_units(review_id,unit_id),
                  UNIQUE(operation_id,household_id,unit_id)
                )
                """);
            s.execute("""
                CREATE TABLE dwelling_active_occupancy (
                  household_id TEXT PRIMARY KEY NOT NULL REFERENCES households(id),
                  unit_id TEXT NOT NULL UNIQUE REFERENCES building_units(id), assignment_id TEXT NOT NULL UNIQUE,
                  FOREIGN KEY(assignment_id,household_id,unit_id) REFERENCES dwelling_assignments(operation_id,household_id,unit_id)
                )
                """);
            s.execute("""
                CREATE TABLE dwelling_departures (
                  operation_id TEXT PRIMARY KEY NOT NULL, assignment_id TEXT NOT NULL UNIQUE REFERENCES dwelling_assignments(operation_id),
                  reason TEXT NOT NULL CHECK(reason IN ('MOVED','RECLAIMED','EMPTY')),
                  consent_id TEXT REFERENCES building_consent_events(operation_id),
                  at_ms INTEGER NOT NULL CHECK(typeof(at_ms)='integer' AND at_ms>=0)
                )
                """);
            s.execute("CREATE INDEX dwelling_assignment_history ON dwelling_assignments(household_id,at_ms,operation_id)");
        }
    }
    /** Unknown/stale/incomplete evidence never replaces a saved review or ends an occupancy. */
    public Outcome review(UUID operation,UUID building,Assessment assessment,Context context)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(assessment);
        return transaction(()->{
            var entry=entry(building,context);var layout=entry.layout();
            if(!assessment.world().equals(layout.world())||!assessment.layoutHash().equals(layout.fingerprint()))throw new Declined("Housing review does not match saved building");
            String fingerprint=fingerprint(assessment);
            var old=review(operation);
            if(old.isPresent()) {
                var r=old.get();
                if(!r.building().equals(building)||!r.fingerprint().equals(fingerprint)||r.recordedMillis()!=context.now())throw new Declined("Housing review operation reused with changed terms");
                return Outcome.REPLAY; // Never restore an older current pointer or later-revoked permission.
            }
            if(assessment.issues().stream().anyMatch(i->i!=Issue.ENTRANCE_UNSAFE)
                    ||assessment.capturedMillis()>context.now()||context.now()-assessment.capturedMillis()>MAX_REVIEW_AGE)return Outcome.UNVERIFIED;
            if(!assessment.policy().equals(context.policy()))throw new Declined("Housing cover settings changed; inspect again");
            requireConsent(entry);
            if(assessment.capturedMillis()<entry.latest().atMillis())throw new Declined("Housing inspection predates current donation; inspect again");
            var previous=currentReview(building);
            if(previous.isPresent()&&assessment.capturedMillis()<=previous.get().observedMillis())return Outcome.OLDER;
            if(assessment.issues().isEmpty()&&!assessment.units().keySet().equals(new HashSet<>(layout.units().stream().map(BuildingLayout.Unit::id).toList())))throw new Declined("Housing unit evidence is incomplete");
            try(var s=connection.prepareStatement("INSERT INTO dwelling_reviews VALUES(?,?,?,?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,building.toString());s.setString(3,entry.latest().operation().toString());s.setString(4,fingerprint);
                s.setLong(5,assessment.capturedMillis());s.setLong(6,context.now());s.setBoolean(7,context.policy().requireCoveredBeds());s.executeUpdate();
            }
            try(var s=connection.prepareStatement("INSERT INTO dwelling_review_units VALUES(?,?,?,?)")) {
                for(var unit:layout.units()) {
                    var result=assessment.units().get(unit.id());var issues=assessment.issues().isEmpty()?result.issues():assessment.issues();
                    s.setString(1,operation.toString());s.setString(2,unit.id().toString());s.setInt(3,issues.isEmpty()?result.accessibleBeds():0);s.setString(4,issueKey(issues));s.executeUpdate();
                }
            }
            try(var s=connection.prepareStatement("INSERT INTO dwelling_current_reviews VALUES(?,?) ON CONFLICT(building_id) DO UPDATE SET review_id=excluded.review_id")){
                s.setString(1,building.toString());s.setString(2,operation.toString());s.executeUpdate();
            }
            return Outcome.RECORDED;
        });
    }
    /** Assign or atomically move one whole saved household. Exact retries return history, never resurrect it. */
    public Assignment assign(Assignment request,Context context)throws SQLException {
        return transaction(()->{
            UUID building=building(request.unit());var entry=entry(building,context);
            var replay=assignment(request.operation());
            if(replay.isPresent()){if(!replay.get().equals(request))throw new Declined("Assignment operation reused with changed terms");return replay.get();}
            if(request.atMillis()!=context.now())throw new Declined("Assignment time differs from current operation");
            requireConsent(entry);
            var status=unit(request.unit()).orElseThrow(()->new Declined("Unit needs a current inspection"));
            if(!status.review().operation().equals(request.review())||!usable(status,entry,context))throw new Declined("Unit inspection, donation or cover settings changed; inspect again");
            UUID village;
            try(var s=connection.prepareStatement("SELECT settlement_id FROM households WHERE id=?")){
                s.setString(1,request.household().toString());try(var r=s.executeQuery()){if(!r.next())throw new Declined("Unknown saved household");village=UUID.fromString(r.getString(1));}
            }
            if(!village.equals(entry.layout().settlement()))throw new Declined("Household and home belong to different villages");
            long living=living(request.household());if(living<1||living>status.beds())throw new Declined("Home cannot fit this whole living household");
            var previous=forHousehold(request.household());
            if(!Objects.equals(previous.map(Assignment::operation).orElse(null),request.previous()))throw new Declined("Household occupancy changed; review again");
            if(forUnit(request.unit()).isPresent())throw new Declined("Unit already has a household");
            HousingCapacity.claim(connection,request.unit(),request.household());
            if(previous.isPresent()) {
                entry(building(previous.get().unit()),context);
                if(context.now()<previous.get().atMillis())throw new Declined("Move predates saved assignment");
                end(previous.get(),new Departure(derived("move",request.operation(),previous.get().operation()),previous.get().operation(),"MOVED",null,context.now()));
            }
            try(var s=connection.prepareStatement("INSERT INTO dwelling_assignments VALUES(?,?,?,?,?,?)")) {
                s.setString(1,request.operation().toString());s.setString(2,request.household().toString());s.setString(3,request.unit().toString());s.setString(4,request.review().toString());
                s.setString(5,request.previous()==null?null:request.previous().toString());s.setLong(6,request.atMillis());s.executeUpdate();
            }
            try(var s=connection.prepareStatement("INSERT INTO dwelling_active_occupancy VALUES(?,?,?)")) {
                s.setString(1,request.household().toString());s.setString(2,request.unit().toString());s.setString(3,request.operation().toString());s.executeUpdate();
            }
            return request;
        });
    }
    public Vacancies vacancies(UUID building,Context context)throws SQLException {
        var entry=entry(building,context);
        if(!hasConsent(entry))return new Vacancies(false,List.of(),"Village use permission is absent or excluded");
        var statuses=new ArrayList<UnitStatus>();
        for(var declared:entry.layout().units()) {
            var status=unit(declared.id());
            if(status.isEmpty()||!current(status.get(),entry,context))return new Vacancies(false,List.of(),"Current complete inspection required; no new-build bypass");
            if(status.get().beds()>0&&status.get().issues().isEmpty()&&forUnit(declared.id()).isEmpty())statuses.add(status.get());
        }
        return new Vacancies(true,statuses,"Vacancy is saved occupancy, not visible villagers");
    }
    public Optional<Assignment> forHousehold(UUID id)throws SQLException{return active("household_id",id);}
    public Optional<Assignment> forUnit(UUID id)throws SQLException{return active("unit_id",id);}
    /** A saved household with no living members can free its unit; unloaded bodies never enter this decision. */
    public Departure releaseEmpty(UUID operation,UUID assignment,Context context)throws SQLException {
        return transaction(()->{
            var a=assignment(assignment).orElseThrow(()->new Declined("Unknown saved assignment"));entry(building(a.unit()),context);
            var requested=new Departure(operation,assignment,"EMPTY",null,context.now());
            var replay=departure(operation);
            if(replay.isPresent()){if(!replay.get().equals(requested))throw new Declined("Departure operation reused with changed terms");return replay.get();}
            if(context.now()<a.atMillis()||living(a.household())!=0)throw new Declined("Household still has living members or time precedes assignment");
            if(!forHousehold(a.household()).map(Assignment::operation).filter(assignment::equals).isPresent())throw new Declined("Assignment is no longer current");
            end(a,requested);return requested;
        });
    }
    public Optional<Departure> departure(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM dwelling_departures WHERE operation_id=?")){
            s.setString(1,operation.toString());try(var r=s.executeQuery()){if(!r.next())return Optional.empty();String consent=r.getString("consent_id");
                return Optional.of(new Departure(operation,UUID.fromString(r.getString("assignment_id")),r.getString("reason"),consent==null?null:UUID.fromString(consent),r.getLong("at_ms")));}
        }
    }
    public Optional<Assignment> assignment(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM dwelling_assignments WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(readAssignment(r)):Optional.empty();}
        }
    }
    public Optional<UnitStatus> unit(UUID unit)throws SQLException {
        try(var s=connection.prepareStatement("""
            SELECT u.*,r.* FROM dwelling_review_units u JOIN dwelling_reviews r ON r.operation_id=u.review_id
            JOIN dwelling_current_reviews c ON c.review_id=r.operation_id AND c.building_id=r.building_id WHERE u.unit_id=?
            """)) {
            s.setString(1,unit.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(new UnitStatus(unit,UUID.fromString(r.getString("building_id")),r.getInt("beds"),parseIssues(r.getString("issues")),readReview(r))):Optional.empty();}
        }
    }
    /** Called inside BuildingRegistry.reclaim's transaction, so consent and displacement cannot split. */
    static void reclaimed(Connection connection,UUID building,UUID consent,long now)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Reclamation occupancy must share the consent transaction");
        var repository=new DwellingOccupancy(connection);var assignments=new ArrayList<Assignment>();
        var layout=new BuildingRegistry(connection).find(building).orElseThrow().layout();
        for(var unit:layout.units())HousingCapacity.claim(connection,unit.id(),null);
        var review=repository.currentReview(building);
        if(review.isPresent()&&review.get().recordedMillis()>now)throw new Declined("Reclamation predates the latest housing review");
        try(var s=connection.prepareStatement("SELECT a.* FROM dwelling_assignments a JOIN dwelling_active_occupancy o ON o.assignment_id=a.operation_id JOIN building_units u ON u.id=a.unit_id WHERE u.building_id=? ORDER BY a.operation_id LIMIT 65")) {
            s.setString(1,building.toString());try(var r=s.executeQuery()){while(r.next()){if(assignments.size()>=BuildingLayout.MAX_UNITS)throw new SQLException("Building occupancy exceeds unit budget");assignments.add(readAssignment(r));}}
        }
        for(var a:assignments) {
            if(now<a.atMillis())throw new Declined("Reclamation predates an active assignment");
            repository.end(a,new Departure(derived("reclaim",consent,a.operation()),a.operation(),"RECLAIMED",consent,now));
        }
    }
    private void end(Assignment a,Departure d)throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO dwelling_departures VALUES(?,?,?,?,?)")){
            s.setString(1,d.operation().toString());s.setString(2,a.operation().toString());s.setString(3,d.reason());s.setString(4,d.consent()==null?null:d.consent().toString());s.setLong(5,d.atMillis());s.executeUpdate();
        }
        try(var s=connection.prepareStatement("DELETE FROM dwelling_active_occupancy WHERE household_id=? AND assignment_id=?")){
            s.setString(1,a.household().toString());s.setString(2,a.operation().toString());if(s.executeUpdate()!=1)throw new Declined("Occupancy changed before departure");
        }
    }
    private Optional<Assignment> active(String column,UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT a.* FROM dwelling_assignments a JOIN dwelling_active_occupancy o ON o.assignment_id=a.operation_id WHERE o."+column+"=?")){
            s.setString(1,id.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(readAssignment(r)):Optional.empty();}
        }
    }
    private UUID building(UUID unit)throws SQLException {
        try(var s=connection.prepareStatement("SELECT building_id FROM building_units WHERE id=?")){
            s.setString(1,unit.toString());try(var r=s.executeQuery()){if(!r.next())throw new Declined("Unknown saved dwelling unit");return UUID.fromString(r.getString(1));}
        }
    }
    private BuildingRegistry.Entry entry(UUID id,Context context)throws SQLException {
        var entry=new BuildingRegistry(connection).find(id).orElseThrow(()->new Declined("Unknown registered home"));
        if(!context.present()||!entry.layout().world().equals(context.world()))throw new Declined("Housing world is absent or changed");
        if(entry.layout().kind()!=BuildingLayout.Kind.HOUSING)throw new Declined("Building is not declared housing");return entry;
    }
    private boolean hasConsent(BuildingRegistry.Entry e)throws SQLException{return e.latest()!=null&&e.latest().action()==BuildingRegistry.Action.DONATE&&new BuildingRegistry(connection).hasDonationPermission(e.layout().id());}
    private void requireConsent(BuildingRegistry.Entry e)throws SQLException{if(!hasConsent(e))throw new Declined("Home has no current village use permission");}
    private boolean current(UnitStatus status,BuildingRegistry.Entry e,Context c) {
        var r=status.review();return r.consent().equals(e.latest().operation())&&r.covered()==c.policy().requireCoveredBeds()
                &&r.observedMillis()<=c.now()&&c.now()-r.observedMillis()<=MAX_REVIEW_AGE;
    }
    private boolean usable(UnitStatus status,BuildingRegistry.Entry e,Context c){return current(status,e,c)&&status.beds()>0&&status.issues().isEmpty();}
    private long living(UUID household)throws SQLException {
        try(var s=connection.prepareStatement("SELECT count(*) FROM residents WHERE household_id=? AND status='ALIVE'")){
            s.setString(1,household.toString());try(var r=s.executeQuery()){return r.getLong(1);}
        }
    }
    private Optional<Review> currentReview(UUID building)throws SQLException {
        try(var s=connection.prepareStatement("SELECT r.* FROM dwelling_reviews r JOIN dwelling_current_reviews c ON c.review_id=r.operation_id WHERE c.building_id=?")){
            s.setString(1,building.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(readReview(r)):Optional.empty();}
        }
    }
    private Optional<Review> review(UUID id)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM dwelling_reviews WHERE operation_id=?")){
            s.setString(1,id.toString());try(var r=s.executeQuery()){return r.next()?Optional.of(readReview(r)):Optional.empty();}
        }
    }
    private static Review readReview(ResultSet r)throws SQLException{return new Review(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("building_id")),UUID.fromString(r.getString("consent_id")),r.getString("fingerprint"),r.getLong("observed_ms"),r.getLong("recorded_ms"),r.getBoolean("require_cover"));}
    private static Assignment readAssignment(ResultSet r)throws SQLException{String previous=r.getString("previous_assignment");return new Assignment(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("household_id")),UUID.fromString(r.getString("unit_id")),UUID.fromString(r.getString("review_id")),previous==null?null:UUID.fromString(previous),r.getLong("at_ms"));}
    private static String issueKey(Set<Issue> issues){return issues.stream().sorted().map(Enum::name).collect(java.util.stream.Collectors.joining(","));}
    private static Set<Issue> parseIssues(String text){if(text.isEmpty())return Set.of();var result=EnumSet.noneOf(Issue.class);for(var s:text.split(","))result.add(Issue.valueOf(s));return result;}
    private static String fingerprint(Assessment assessment) {
        var text=new StringBuilder("1:").append(assessment.world()).append(':').append(assessment.layoutHash()).append(':').append(assessment.capturedMillis()).append(':').append(assessment.policy().requireCoveredBeds()).append(':').append(issueKey(assessment.issues()));
        for(var u:new TreeMap<>(assessment.units()).values())text.append('|').append(u.unit()).append(':').append(u.accessibleBeds()).append(':').append(issueKey(u.issues()));
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static UUID derived(String action,UUID operation,UUID assignment){return UUID.nameUUIDFromBytes(("civilizations:dwelling:"+action+":"+operation+":"+assignment).getBytes(StandardCharsets.UTF_8));}
    @FunctionalInterface private interface Work<T>{T run()throws SQLException;}
    private <T>T transaction(Work<T> work)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Housing operation requires the SQL owner's transaction boundary");
        connection.setAutoCommit(false);
        try{T result=work.run();connection.commit();connection.setAutoCommit(true);return result;}
        catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
