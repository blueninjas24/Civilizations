package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.ForestryPolicy;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForestRecoveryTest {
    @TempDir Path directory;ForestryPersistenceTest fixture;
    @BeforeEach void setup()throws Exception {
        fixture=new ForestryPersistenceTest();fixture.directory=directory;fixture.setup();fixture.start();
        fixture.data.db.getIndustryRepository().log(UUID.randomUUID(),fixture.firm(),4,1,ForestryPolicy.prototype());
        new ForestryTrackingRepository(fixture.data.db.getConnection()).begin(20);
        assertFalse(new ForestryTrackingRepository(fixture.data.db.getConnection()).load().orElseThrow().trusted());
    }
    @AfterEach void close(){if(fixture!=null)fixture.close();}
    ForestRecovery recovery(){return new ForestRecovery(fixture.data.db.getConnection());}
    Map<Position,ForestRecovery.Verdict> evidence(ForestRecovery.Plan plan){
        var result=new HashMap<Position,ForestRecovery.Verdict>();plan.supports().forEach(p->result.put(p,new ForestRecovery.Verdict(true,true)));return result;
    }
    ForestRecovery.Receipt approve(ForestRecovery.Plan plan,Map<Position,ForestRecovery.Verdict> current)throws SQLException {
        return recovery().approve(UUID.randomUUID(),"operator-test",plan,current,30,30,()->true);
    }
    @Test void existingTimberAndIdentitySurviveScopedReviewAndCleanRestartWhileNewGapRevokesWork()throws Exception {
        var before=fixture.forests().find(fixture.site()).orElseThrow();var plan=recovery().prepare(fixture.site());
        var receipt=approve(plan,evidence(plan));assertEquals(before.stock().available(),receipt.available());
        assertEquals(0,receipt.lost());assertTrue(recovery().permitted(fixture.site()));assertFalse(recovery().permitted(UUID.randomUUID()));
        var tracking=new ForestryTrackingRepository(fixture.data.db.getConnection());tracking.finish(31,true);tracking.begin(32);
        assertFalse(tracking.load().orElseThrow().trusted());assertTrue(recovery().permitted(fixture.site()));
        assertEquals(4,fixture.data.db.getIndustryRepository().log(UUID.randomUUID(),fixture.firm(),4,2,ForestryPolicy.prototype()).quantity());
        var earned=fixture.data.db.getIndustryRepository().store(fixture.firm()).orElseThrow().units();tracking.markGap(33);
        assertFalse(recovery().permitted(fixture.site()));
        assertThrows(SQLException.class,()->fixture.data.db.getIndustryRepository().log(UUID.randomUUID(),fixture.firm(),4,3,ForestryPolicy.prototype()));
        assertEquals(earned,fixture.data.db.getIndustryRepository().store(fixture.firm()).orElseThrow().units());
    }
    @Test void confirmedMissingTrunkRetiresOnlyItsCoordinateAndNeverDestroysEarnedTimber()throws Exception {
        var plan=recovery().prepare(fixture.site());var current=evidence(plan);var missing=plan.supports().getFirst();
        current.put(missing,new ForestRecovery.Verdict(false,false));var receipt=approve(plan,current);
        assertEquals(80,receipt.capacity());assertEquals(80,receipt.available());assertEquals(12,receipt.lost());
        assertEquals(6,fixture.data.count("forestry_supports"));assertEquals(1,fixture.data.count("forestry_source_losses"));
        assertEquals(4,fixture.data.db.getIndustryRepository().store(fixture.firm()).orElseThrow().units().values().stream().mapToLong(Long::longValue).sum());
        var again=recovery().prepare(fixture.site());assertTrue(again.excluded().contains(missing));assertTrue(again.retired().contains(missing));
        assertEquals(80,approve(again,evidence(again)).capacity());assertEquals(1,fixture.data.count("forestry_source_losses"));
    }
    @Test void canopyLossReducesHabitatWithoutInventingTrunkDeathsOrRefillingOnReapproval()throws Exception {
        var plan=recovery().prepare(fixture.site());var current=evidence(plan);
        plan.supports().stream().limit(3).forEach(p->current.put(p,new ForestRecovery.Verdict(true,false)));
        assertEquals(48,approve(plan,current).available());assertEquals(0,fixture.data.count("forestry_source_losses"));
        var next=recovery().prepare(fixture.site());assertEquals(48,approve(next,evidence(next)).available());
        assertEquals(0,fixture.data.count("forestry_exclusions"));
    }
    @Test void incompleteStaleOrCaptureFailureCannotApplyApproval()throws Exception {
        var plan=recovery().prepare(fixture.site());var incomplete=evidence(plan);incomplete.remove(plan.supports().getFirst());
        assertThrows(IllegalArgumentException.class,()->approve(plan,incomplete));
        assertThrows(SQLException.class,()->recovery().approve(UUID.randomUUID(),"operator-test",plan,evidence(plan),30,30,()->false));
        assertThrows(IllegalArgumentException.class,()->recovery().approve(UUID.randomUUID(),"operator-test",plan,evidence(plan),30,30031,()->true));
        new ForestryProvenance(fixture.data.db.getConnection()).record(fixture.data.world,List.of(plan.supports().getFirst()),List.of(),25,"New protection");
        assertThrows(SQLException.class,()->approve(plan,evidence(plan)));assertEquals(0,fixture.data.count("forest_recoveries"));
    }
    @Test void failedAuditRollsBackSourceLossHabitatAndCertification()throws Exception {
        var plan=recovery().prepare(fixture.site());var current=evidence(plan);current.put(plan.supports().getFirst(),new ForestRecovery.Verdict(false,false));
        fixture.data.sql("CREATE TRIGGER reject_forest_recovery BEFORE INSERT ON forest_recoveries BEGIN SELECT RAISE(ABORT,'audit failed'); END");
        assertThrows(SQLException.class,()->approve(plan,current));assertEquals(plan.resource(),fixture.forests().find(fixture.site()).orElseThrow());
        assertEquals(0,fixture.data.count("forestry_source_losses"));assertEquals(0,fixture.data.count("forestry_exclusions"));
        assertEquals(0,fixture.data.count("forestry_habitat_changes"));assertFalse(recovery().permitted(fixture.site()));
        fixture.data.sql("DROP TRIGGER reject_forest_recovery");assertEquals(80,approve(plan,current).available());
    }
}
