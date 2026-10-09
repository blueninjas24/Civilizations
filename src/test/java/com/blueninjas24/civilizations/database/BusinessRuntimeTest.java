package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class BusinessRuntimeTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),founder=UUID.randomUUID(),worker=UUID.randomUUID();
    private BusinessRepository repository;
    @BeforeEach void setup() throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("BusinessRuntimeTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Test","Test",0,64,0,1,0,0));
        var identity=db.getVillageIdentityRepository();identity.observeWorld(world,"Test");identity.bindSettlement(town,world);
        for(var id:List.of(founder,worker))db.getPopulationRepository().insertResident(new Resident(id,"Resident",town,null,300,Resident.Status.ALIVE));
        repository=db.getBusinessRepository();
    }
    @AfterEach void close(){db.close();}
    private UUID found(int x) throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,64,0,1000,"Test-only field");
        var b=new Business(new UUID(0,x+1),town,"Field "+x,founder,founder,"farming",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        repository.found(new BusinessFounding(b,site,requirements,requirements,0,"Explicit test opportunity"));return b.id();
    }
    private void kill(UUID id) throws Exception {
        db.getPopulationRepository().updateResident(new Resident(id,"Resident",town,null,300,Resident.Status.DECEASED));
    }
    @Test void activeReconciliationRetainsHistoryAndImmutablePreviousSnapshot() throws Exception {
        var b=found(0);var job=UUID.randomUUID();repository.hire(job,b,worker,0);
        var runtime=new BusinessRuntime(db.getConnection(),8,32);var before=runtime.refresh(0,Set.of(world));
        kill(worker);var after=runtime.refresh(1,Set.of(world));
        assertEquals(1,before.businesses().get(b).workers());assertEquals(0,after.businesses().get(b).workers());
        assertEquals(0,after.businesses().get(b).openAssignments());assertEquals(2,after.businesses().get(b).vacancies());
        assertEquals(1,repository.findEmployment(job).orElseThrow().endedStep());
        assertTrue(after.businesses().get(b).lastSeparation().contains("deceased"));
        assertThrows(UnsupportedOperationException.class,()->after.businesses().clear());
        runtime.refresh(2,Set.of(world));assertEquals(1,repository.employmentHistory(b).size());
        assertEquals(1,repository.findEmployment(job).orElseThrow().endedStep());
    }
    @Test void absentWorldSkipsWritesAndOwnerDeathDoesNotInventSuccession() throws Exception {
        var b=found(0);var job=UUID.randomUUID();repository.hire(job,b,worker,0);kill(worker);kill(founder);
        var runtime=new BusinessRuntime(db.getConnection(),8,32);var paused=runtime.refresh(100,Set.of()).businesses().get(b);
        assertTrue(paused.paused());assertFalse(paused.ownerAvailable());assertEquals(0,paused.workers());assertEquals(1,paused.openAssignments());
        assertNull(repository.findEmployment(job).orElseThrow().endedStep());
        assertEquals(founder,repository.findFounding(b).orElseThrow().business().ownerId());
        runtime.refresh(101,Set.of(world));assertEquals(101,repository.findEmployment(job).orElseThrow().endedStep());
    }
    @Test void boundedRotationVisitsLateBusinessesAndEvictsOldCachedDetails() throws Exception {
        for(int x=0;x<6;x++)found(x);
        var runtime=new BusinessRuntime(db.getConnection(),2,3);
        assertEquals(2,runtime.refresh(0,Set.of(world)).businesses().size());
        var second=runtime.refresh(0,Set.of(world));assertEquals(6,second.total());assertEquals(3,second.businesses().size());
        var last=runtime.refresh(0,Set.of(world));assertTrue(last.businesses().containsKey(new UUID(0,6)));
        var wrapped=runtime.refresh(0,Set.of());assertTrue(wrapped.businesses().containsKey(new UUID(0,1)));
        assertTrue(wrapped.businesses().values().stream().allMatch(BusinessRuntime.View::paused));
    }
    @Test void failedSeparationRollsBackAndNextRefreshRetriesSamePage() throws Exception {
        var b=found(0);var job=UUID.randomUUID();repository.hire(job,b,worker,0);kill(worker);
        try(var s=db.getConnection().createStatement()){s.execute("CREATE TRIGGER fail_staff BEFORE UPDATE ON business_employment BEGIN SELECT RAISE(ABORT,'injected'); END");}
        var runtime=new BusinessRuntime(db.getConnection(),1,1);
        assertThrows(java.sql.SQLException.class,()->runtime.refresh(1,Set.of(world)));
        assertNull(repository.findEmployment(job).orElseThrow().endedStep());assertTrue(db.getConnection().getAutoCommit());
        try(var s=db.getConnection().createStatement()){s.execute("DROP TRIGGER fail_staff");}
        assertEquals(0,runtime.refresh(1,Set.of(world)).businesses().get(b).openAssignments());
    }
    @Test void sessionStartupAndWorldReturnPublishAndReconcileExistingBusinesses() throws Exception {
        var b=found(0);var job=UUID.randomUUID();repository.hire(job,b,worker,0);kill(worker);db.close();
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("SessionBusinessTest"),PopulationSession.Settings.prototype(),()->1000)) {
            session.start(false,Set.of()).get(5,TimeUnit.SECONDS);
            assertEquals(1,session.businessView().businesses().get(b).openAssignments());
            session.updateLoadedWorlds(Set.of(world)).get(5,TimeUnit.SECONDS);
            assertEquals(0,session.businessView().businesses().get(b).openAssignments());
            assertTrue(BusinessDiagnostics.describe(session.businessView(),town).stream().anyMatch(s->s.contains("vacancies: 2")));
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("ReopenBusinessTest"));db.initialize();
        assertEquals(0,db.getBusinessRepository().findEmployment(job).orElseThrow().endedStep());
    }
    @Test void emptyAndNestedRefreshAreExplicitAndCreateNoBusinessOrInventory() throws Exception {
        var runtime=new BusinessRuntime(db.getConnection(),8,32);assertEquals(BusinessRuntime.Snapshot.empty(),runtime.refresh(0,Set.of(world)));
        assertTrue(BusinessDiagnostics.describe(runtime.refresh(0,Set.of(world)),town).stream().anyMatch(s->s.contains("not active yet")));
        db.getConnection().setAutoCommit(false);assertThrows(java.sql.SQLException.class,()->runtime.refresh(0,Set.of(world)));
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);
    }
}
