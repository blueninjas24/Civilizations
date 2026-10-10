package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class BusinessRepositoryTest {
    @TempDir Path directory;
    DatabaseManager database;
    BusinessRepository businesses;
    UUID town, world;
    Resident founder;
    static final JobRequirements FOUNDER = new JobRequirements(216,Map.of("farming",10L));
    static final JobRequirements WORKER = new JobRequirements(216,Map.of("farming",2L));

    @BeforeEach void setup() throws SQLException {
        open(); town = town(); world = UUID.randomUUID();
        var identity = database.getVillageIdentityRepository();
        identity.observeWorld(world,"Test World"); identity.bindSettlement(town,world);
        founder = person(town,300,Resident.Status.ALIVE,12);
        new TreasuryRepository(database.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test-owned opening capital");
    }

    void open() throws SQLException {
        database = new DatabaseManager(directory.toFile(),Logger.getLogger("BusinessTest"));
        database.initialize(); businesses = database.getBusinessRepository();
    }

    @AfterEach void close() { database.close(); }

    UUID town() throws SQLException {
        UUID id=UUID.randomUUID();
        database.saveSettlement(new Settlement(id,"Test Town","Test World",0,64,0,1,0,0)); return id;
    }

    Resident person(UUID settlement, long age, Resident.Status status, long experience) throws SQLException {
        var resident=new Resident(UUID.randomUUID(),"Test Resident",settlement,null,age,status);
        var population=database.getPopulationRepository(); population.insertResident(resident);
        population.recordSkillExperience(resident.id(),"farming",experience); return resident;
    }

    BusinessFounding proposal(int x, int capacity, long cost) {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,-32,0,1234,"Test adapter: existing cultivated field");
        var business=new Business(UUID.randomUUID(),town,"River Farm",founder.id(),founder.id(),"farming",site.id(),capacity,12);
        return new BusinessFounding(business,site,FOUNDER,WORKER,cost,"Explicit test field opportunity");
    }

    long balance() throws SQLException { return new TreasuryRepository(database.getConnection()).load(town).orElseThrow().hundredths(); }
    void sql(String text) throws SQLException { try(var s=database.getConnection().createStatement()) { s.execute(text); } }
    long count(String table) throws SQLException {
        try(var s=database.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)) { assertTrue(r.next()); return r.getLong(1); }
    }

    private void die(Resident person)throws SQLException {
        database.getPopulationRepository().updateResident(new Resident(person.id(),person.name(),person.settlementId(),person.householdId(),person.ageSteps(),Resident.Status.DECEASED));
    }
    private void child(Resident parent,Resident child)throws SQLException {
        database.getPopulationRepository().insertFamilyTie(new FamilyTie(parent.id(),child.id(),FamilyTie.Type.PARENT_CHILD));
    }
    @Test void qualifiedFamilyInheritsBeforeEmployeeWithoutChangingFoundingMoneyOrJobs()throws Exception {
        var request=proposal(0,2,2500);businesses.found(request);
        var heir=person(town,250,Resident.Status.ALIVE,12);child(founder,heir);
        var worker=person(town,300,Resident.Status.ALIVE,20);
        var job=businesses.hire(UUID.randomUUID(),request.business().id(),worker.id(),12);die(founder);
        var succession=new BusinessSuccession(database.getConnection());var operation=UUID.randomUUID();
        var transfer=succession.succeed(operation,request.business().id(),13).orElseThrow();
        assertEquals(heir.id(),transfer.nextOwner());assertEquals(BusinessSuccession.Kind.FAMILY,transfer.kind());
        assertEquals(request,businesses.findFounding(request.business().id()).orElseThrow());
        assertEquals(request,businesses.found(request));assertEquals(7500,balance());assertEquals(2,count("treasury_entries"));
        assertEquals(heir.id(),businesses.findBusinesses(town).getFirst().ownerId());assertEquals(List.of(job),businesses.currentWorkers(request.business().id()));
        assertEquals(transfer,succession.succeed(operation,request.business().id(),13).orElseThrow());
        assertThrows(SQLException.class,()->succession.succeed(operation,request.business().id(),14));
        database.close();open();assertEquals(heir.id(),businesses.findBusiness(request.business().id()).orElseThrow().ownerId());
        assertEquals(request,businesses.findFounding(request.business().id()).orElseThrow());
        assertEquals(transfer,new BusinessSuccession(database.getConnection()).find(operation).orElseThrow());
    }
    @Test void unqualifiedUnderageDeadAndElsewhereFamilyDoNotBlockQualifiedEmployee()throws Exception {
        var request=proposal(0,2,0);businesses.found(request);
        for(var relative:List.of(person(town,100,Resident.Status.ALIVE,20),person(town,300,Resident.Status.ALIVE,0),
                person(town,300,Resident.Status.DECEASED,20),person(town(),300,Resident.Status.ALIVE,20)))child(founder,relative);
        var worker=person(town,300,Resident.Status.ALIVE,20);businesses.hire(UUID.randomUUID(),request.business().id(),worker.id(),12);die(founder);
        var result=new BusinessSuccession(database.getConnection()).succeed(UUID.randomUUID(),request.business().id(),13).orElseThrow();
        assertEquals(worker.id(),result.nextOwner());assertEquals(BusinessSuccession.Kind.EMPLOYEE,result.kind());assertEquals(10000,balance());
    }
    @Test void successionDoesNotChangeAWorkingRelativesIndustryOrPromoteAnUnqualifiedEmployee()throws Exception {
        var request=proposal(0,2,0);businesses.found(request);var relative=person(town,300,Resident.Status.ALIVE,20);child(founder,relative);
        var other=proposal(4,2,0);var old=other.business();
        var mine=new Business(old.id(),town,old.name(),founder.id(),founder.id(),"mining",old.siteId(),2,12);
        businesses.found(new BusinessFounding(mine,other.site(),FOUNDER,WORKER,0,"Test cross-industry job"));
        var job=businesses.hire(UUID.randomUUID(),mine.id(),relative.id(),12);
        var unqualified=person(town,300,Resident.Status.ALIVE,3);businesses.hire(UUID.randomUUID(),request.business().id(),unqualified.id(),12);die(founder);
        assertTrue(new BusinessSuccession(database.getConnection()).succeed(UUID.randomUUID(),request.business().id(),13).isEmpty());
        assertEquals(founder.id(),businesses.findBusiness(request.business().id()).orElseThrow().ownerId());
        assertEquals(List.of(job),businesses.currentWorkers(mine.id()));assertEquals(0,count("business_ownership"));
    }
    @Test void ownershipAuditFailureRollsBackOwnerAndCanRetry()throws Exception {
        var request=proposal(0,2,2000);businesses.found(request);var heir=person(town,300,Resident.Status.ALIVE,20);child(founder,heir);die(founder);
        sql("CREATE TRIGGER fail_ownership BEFORE INSERT ON business_ownership BEGIN SELECT RAISE(ABORT,'test ownership failure'); END");
        var succession=new BusinessSuccession(database.getConnection());var op=UUID.randomUUID();
        assertThrows(SQLException.class,()->succession.succeed(op,request.business().id(),13));
        assertEquals(founder.id(),businesses.findBusiness(request.business().id()).orElseThrow().ownerId());assertEquals(0,count("business_ownership"));
        assertTrue(database.getConnection().getAutoCommit());assertEquals(8000,balance());
        sql("DROP TRIGGER fail_ownership");assertEquals(heir.id(),succession.succeed(op,request.business().id(),13).orElseThrow().nextOwner());
    }
    @Test void runtimeSuccessionPausesAbsentWorldAndPublishesCurrentOwner()throws Exception {
        var request=proposal(0,2,0);businesses.found(request);var heir=person(town,300,Resident.Status.ALIVE,20);child(founder,heir);die(founder);
        var runtime=new BusinessRuntime(database.getConnection(),8,64);
        runtime.refresh(13,Set.of());assertEquals(founder.id(),businesses.findBusiness(request.business().id()).orElseThrow().ownerId());
        var view=runtime.refresh(14,Set.of(world)).businesses().get(request.business().id());
        assertTrue(view.ownerAvailable());assertEquals(heir.id(),businesses.findBusiness(request.business().id()).orElseThrow().ownerId());assertEquals(1,count("business_ownership"));
        runtime.refresh(15,Set.of(world));assertEquals(1,count("business_ownership"));
    }
    @Test void schema37OwnershipUpgradeFailureRetainsExistingBusinessAndRetries()throws Exception {
        var request=proposal(0,2,2000);businesses.found(request);SchemaFixtures.rewind(database.getConnection(),37);
        sql("CREATE INDEX business_owner_history ON businesses(id)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(database.getConnection()));
        assertThrows(SQLException.class,()->count("business_ownership"));assertEquals(request,businesses.findFounding(request.business().id()).orElseThrow());assertEquals(8000,balance());
        sql("DROP INDEX business_owner_history");new SchemaMigrator().migrate(database.getConnection());assertEquals(0,count("business_ownership"));
        try(var s=database.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }

    @Test void allFoundingTermsStaffingCapitalAndSnapshotSurviveRestart() throws Exception {
        var request=proposal(0,2,2500); var saved=businesses.found(request);
        var worker=person(town,250,Resident.Status.ALIVE,3);
        UUID hire=UUID.randomUUID(); var job=businesses.hire(hire,saved.business().id(),worker.id(),13);
        assertEquals(7500,balance()); assertEquals(request,saved);
        assertEquals(List.of(saved.business()),businesses.findBusinesses(town));
        assertTrue(businesses.currentWorkers(saved.business().id()).stream().noneMatch(e->e.residentId().equals(founder.id())));
        database.close(); open();
        assertEquals(request,businesses.findFounding(request.business().id()).orElseThrow());
        assertEquals(job,businesses.findEmployment(hire).orElseThrow());
        assertEquals(List.of(job),businesses.currentWorkers(request.business().id()));
        assertEquals(7500,balance());
        Path snapshot=directory.resolve("business-snapshot.db");
        DatabaseBackup.snapshot(directory.resolve("civilizations.db"),snapshot);
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+snapshot);var s=c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON"); var restored=new BusinessRepository(c);
            assertEquals(request,restored.findFounding(request.business().id()).orElseThrow());
            assertEquals(List.of(job),restored.employmentHistory(request.business().id()));
            try(var r=s.executeQuery("PRAGMA foreign_key_check")) { assertFalse(r.next()); }
            try(var r=s.executeQuery("PRAGMA integrity_check")) { assertTrue(r.next()); assertEquals("ok",r.getString(1)); }
        }
    }

    @Test void exactRetriesPreserveHistoryAndNeverSpendOrHireTwice() throws SQLException {
        var request=proposal(0,2,2000); businesses.found(request);
        var job=businesses.hire(UUID.randomUUID(),request.business().id(),founder.id(),12);
        var ended=businesses.endEmployment(job.id(),15,"Worker left voluntarily");
        database.getPopulationRepository().updateResident(new Resident(founder.id(),founder.name(),town,null,301,Resident.Status.DECEASED));
        assertEquals(request,businesses.found(request)); // Original accepted facts are immutable.
        assertEquals(8000,balance()); assertEquals(1,count("businesses")); assertEquals(2,count("treasury_entries"));
        assertEquals(ended,businesses.hire(job.id(),job.businessId(),job.residentId(),12));
        assertEquals(ended,businesses.endEmployment(job.id(),15,"Worker left voluntarily"));
        assertTrue(businesses.currentWorkers(job.businessId()).isEmpty());
        assertThrows(SQLException.class,()->businesses.endEmployment(job.id(),16,"Different departure"));
        assertThrows(SQLException.class,()->businesses.hire(job.id(),job.businessId(),job.residentId(),13));
        assertThrows(SQLException.class,()->businesses.found(new BusinessFounding(request.business(),request.site(),FOUNDER,WORKER,2001,request.reason())));
    }

    @Test void unavailableCapitalOrLedgerFailureRollsBackEntireFoundingAndRetries() throws SQLException {
        var request=proposal(0,2,10001);
        assertThrows(SQLException.class,()->businesses.found(request));
        assertEquals(0,count("business_sites")); assertEquals(0,count("businesses")); assertEquals(10000,balance());
        var affordable=proposal(0,2,2500);
        sql("CREATE TRIGGER fail_business_debit BEFORE INSERT ON treasury_entries WHEN NEW.kind='DEBIT' BEGIN SELECT RAISE(ABORT,'test debit save failure'); END");
        assertThrows(SQLException.class,()->businesses.found(affordable));
        assertEquals(10000,balance()); assertEquals(0,count("businesses")); assertEquals(0,count("business_skill_requirements"));
        assertTrue(database.getConnection().getAutoCommit());
        sql("DROP TRIGGER fail_business_debit"); assertEquals(affordable,businesses.found(affordable)); assertEquals(7500,balance());
    }

    @Test void qualificationWorldAndDuplicatePhysicalSiteFailuresAreExplicit() throws SQLException {
        var request=proposal(0,1,0);
        for(var resident:List.of(person(town,100,Resident.Status.ALIVE,99),person(town,300,Resident.Status.DECEASED,99),
                person(town,300,Resident.Status.ALIVE,0),person(town(),300,Resident.Status.ALIVE,99))) {
            var b=request.business();
            var invalid=new Business(b.id(),town,b.name(),resident.id(),resident.id(),b.industry(),b.siteId(),1,12);
            assertThrows(SQLException.class,()->businesses.found(new BusinessFounding(invalid,request.site(),FOUNDER,WORKER,0,request.reason())));
        }
        var site=request.site();
        var wrongWorld=new BusinessSite(site.id(),town,UUID.randomUUID(),0,0,0,1234,"Wrong-world evidence");
        assertThrows(SQLException.class,()->businesses.found(new BusinessFounding(request.business(),wrongWorld,FOUNDER,WORKER,0,request.reason())));
        assertEquals(0,count("business_sites"));
        businesses.found(request);
        assertThrows(SQLException.class,()->businesses.found(proposal(0,1,0)));
        assertEquals(1,count("businesses")); assertEquals(1,count("business_sites")); assertEquals(10000,balance());
    }

    @Test void staffingRejectsUnderageUnskilledDeadElsewhereAndDoubleAssignedPeople() throws SQLException {
        var a=proposal(0,3,0); var b=proposal(1,3,0); businesses.found(a); businesses.found(b);
        for(var worker:List.of(person(town,10,Resident.Status.ALIVE,20),person(town,300,Resident.Status.ALIVE,0),
                person(town,300,Resident.Status.DECEASED,20),person(town(),300,Resident.Status.ALIVE,20))) {
            assertThrows(SQLException.class,()->businesses.hire(UUID.randomUUID(),a.business().id(),worker.id(),12));
        }
        var job=businesses.hire(UUID.randomUUID(),a.business().id(),founder.id(),12);
        assertThrows(SQLException.class,()->businesses.hire(UUID.randomUUID(),b.business().id(),founder.id(),12));
        assertThrows(SQLException.class,()->businesses.hire(UUID.randomUUID(),a.business().id(),founder.id(),11));
        assertEquals(List.of(job),businesses.currentWorkers(a.business().id()));
        assertEquals(1,count("business_employment"));
    }

    @Test void staffingCapacityAndSeparationHistoryAllowRehiringWithoutResurrection() throws SQLException {
        var request=proposal(0,1,0); businesses.found(request);
        var worker=person(town,300,Resident.Status.ALIVE,10);
        var first=businesses.hire(UUID.randomUUID(),request.business().id(),founder.id(),12);
        assertThrows(SQLException.class,()->businesses.hire(UUID.randomUUID(),request.business().id(),worker.id(),13));
        assertThrows(IllegalArgumentException.class,()->businesses.endEmployment(first.id(),11,"Departure"));
        businesses.endEmployment(first.id(),14,"Departure");
        var second=businesses.hire(UUID.randomUUID(),request.business().id(),worker.id(),14);
        assertEquals(2,businesses.employmentHistory(request.business().id()).size());
        assertEquals(List.of(second),businesses.currentWorkers(request.business().id()));
    }

    @Test void deathAndMigrationDoNotCountAsLaborAndReconciliationRetainsJobs() throws SQLException {
        var request=proposal(0,3,0); businesses.found(request);
        var dead=person(town,300,Resident.Status.ALIVE,3); var moved=person(town,300,Resident.Status.ALIVE,3);
        businesses.hire(UUID.randomUUID(),request.business().id(),dead.id(),12);
        businesses.hire(UUID.randomUUID(),request.business().id(),moved.id(),12);
        database.getPopulationRepository().updateResident(new Resident(dead.id(),dead.name(),town,null,301,Resident.Status.DECEASED));
        UUID destination=town(); database.getVillageIdentityRepository().bindSettlement(destination,world);
        database.getPopulationRepository().updateMigratedResident(new Resident(moved.id(),moved.name(),destination,null,301,Resident.Status.ALIVE),town);
        assertTrue(businesses.currentWorkers(request.business().id()).isEmpty());
        assertEquals(2,businesses.reconcileEmployment(town,13).size());
        assertTrue(businesses.reconcileEmployment(town,13).isEmpty());
        assertTrue(businesses.employmentHistory(request.business().id()).stream().allMatch(e->e.endedStep()==13));
        assertEquals(Resident.Status.DECEASED,database.getPopulationRepository().findResident(dead.id()).orElseThrow().status());
        assertEquals(destination,database.getPopulationRepository().findResident(moved.id()).orElseThrow().settlementId());
    }

    @Test void failedStaffingReconciliationAndNestedOperationsNeverPartiallyCommit() throws SQLException {
        var request=proposal(0,2,0); businesses.found(request);
        var worker=person(town,300,Resident.Status.ALIVE,3);
        for(var person:List.of(founder,worker)) {
            businesses.hire(UUID.randomUUID(),request.business().id(),person.id(),12);
            database.getPopulationRepository().updateResident(new Resident(person.id(),person.name(),town,null,301,Resident.Status.DECEASED));
        }
        sql("CREATE TRIGGER fail_job_departure BEFORE UPDATE ON business_employment BEGIN SELECT RAISE(ABORT,'test staffing save failure'); END");
        assertThrows(SQLException.class,()->businesses.reconcileEmployment(town,13));
        assertTrue(businesses.employmentHistory(request.business().id()).stream().allMatch(e->e.endedStep()==null));
        sql("DROP TRIGGER fail_job_departure"); assertEquals(2,businesses.reconcileEmployment(town,13).size());
        database.getConnection().setAutoCommit(false);
        assertThrows(SQLException.class,()->businesses.found(proposal(1,1,0)));
        database.getConnection().rollback(); database.getConnection().setAutoCommit(true);
    }

    @Test void schema26UpgradeRetainsPeopleTreasuryAndRollsBackPartialNewTables() throws SQLException {
        var connection=database.getConnection(); SchemaFixtures.rewind(connection,26);
        sql("CREATE TABLE business_employment (collision TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(connection));
        try(var s=connection.createStatement();var r=s.executeQuery("PRAGMA user_version")) { assertTrue(r.next()); assertEquals(26,r.getInt(1)); }
        assertThrows(SQLException.class,()->count("business_sites")); assertThrows(SQLException.class,()->count("businesses"));
        assertEquals(founder,database.getPopulationRepository().findResident(founder.id()).orElseThrow()); assertEquals(10000,balance());
        sql("DROP TABLE business_employment"); new SchemaMigrator().migrate(connection);
        assertEquals(0,count("businesses")); assertEquals(founder,database.getPopulationRepository().findResident(founder.id()).orElseThrow());
        var request=proposal(0,1,500); assertEquals(request,businesses.found(request)); assertEquals(9500,balance());
    }

    @Test void seededStaffingTurnoverConservesCapacityHistoryAndCapitalAcrossRestart() throws SQLException {
        var request=proposal(0,4,1000); businesses.found(request);
        var workers=new ArrayList<Resident>(); for(int i=0;i<12;i++) workers.add(person(town,300,Resident.Status.ALIVE,3));
        var random=new Random(831); int hires=0, departures=0;
        for(int step=12;step<252;step++) {
            var active=businesses.currentWorkers(request.business().id());
            for(var job:active) if(random.nextBoolean()) { businesses.endEmployment(job.id(),step,"Test turnover"); departures++; }
            var employed=new HashSet<UUID>(); businesses.currentWorkers(request.business().id()).forEach(e->employed.add(e.residentId()));
            var shuffled=new ArrayList<>(workers); Collections.shuffle(shuffled,random);
            for(var person:shuffled) {
                if(employed.size()>=4) break;
                if(employed.add(person.id())) {
                    UUID id=new UUID(step,person.id().getLeastSignificantBits());
                    var job=businesses.hire(id,request.business().id(),person.id(),step);
                    assertEquals(job,businesses.hire(id,request.business().id(),person.id(),step)); hires++;
                }
            }
            assertTrue(businesses.currentWorkers(request.business().id()).size()<=4);
            if(step==120) { database.close(); open(); assertEquals(request,businesses.found(request)); }
        }
        var history=businesses.employmentHistory(request.business().id());
        assertEquals(hires,history.size()); assertEquals(departures,history.stream().filter(e->e.endedStep()!=null).count());
        assertEquals(hires-departures,businesses.currentWorkers(request.business().id()).size()); assertEquals(9000,balance());
    }
}
