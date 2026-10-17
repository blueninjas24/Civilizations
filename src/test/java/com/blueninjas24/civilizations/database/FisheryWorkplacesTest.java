package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FisheryWorkplacesTest {
    @TempDir Path directory;FisheryStartupTest fixture;
    ShorelineScan.Position initial=new ShorelineScan.Position(-1,65,0),replacement=new ShorelineScan.Position(0,65,-1);
    WaterPatch.Patch patch;UUID site,business;
    @BeforeEach void setup()throws Exception {
        fixture=new FisheryStartupTest();fixture.directory=directory;fixture.setup();var request=fixture.request;var old=request.founding();var p=old.site();site=p.id();business=old.business().id();
        var location=new BusinessSite(p.id(),p.settlementId(),p.worldId(),initial.x(),initial.y(),initial.z(),p.observedMillis(),p.evidence());
        fixture.request=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(old.business(),location,old.founderRequirements(),old.workerRequirements(),old.startupHundredths(),old.reason()),request.capacity(),request.space(),request.capital(),request.worker());
        patch=new WaterPatch.Patch(fixture.habitat,64);
    }
    @AfterEach void close(){fixture.close();}
    BusinessStartupGrantsTest data(){return fixture.fixture;}
    FisheryWorkplaces banks(){return new FisheryWorkplaces(data().db.getConnection());}
    ShorelineEvidence frame(long time,ShorelineScan.Position origin,Set<ShorelineScan.Position> safe,boolean unknown) {
        var scan=new ShorelineScan(p->unknown&&p.equals(new ShorelineScan.Position(origin.x(),origin.y()-1,origin.z()))?ShorelineScan.Cell.UNKNOWN:
                p.y()==64&&safe.contains(new ShorelineScan.Position(p.x(),65,p.z()))?ShorelineScan.Cell.SAFE_FOOTING:ShorelineScan.Cell.AIR,patch,0);
        while(!scan.complete())scan.advance(4096);return new ShorelineEvidence(data().world,origin,time,scan.assessments());
    }
    void start()throws Exception{data().grants().startShoreFishery(fixture.request,patch,2,0,16,frame(20,initial,Set.of(initial),false),20,100,FishingPolicy.prototype(),StartupGrantPolicy.prototype());}
    @Test void fundedOpeningAndRestartReplayPreserveBankResourceAndRealCash()throws Exception {
        start();assertEquals(initial,banks().find(site).orElseThrow().point());assertFalse(banks().find(site).orElseThrow().unsafe());assertEquals(8500,data().cash());
        data().db.close();data().open();start();assertEquals(1,data().count("fishery_workplaces"));assertEquals(1,data().count("fishery_workplace_history"));assertEquals(8500,data().cash());
    }
    @Test void relocationKeepsFoundingIdentityStockCashAndReplayEvenAfterLaterFrames()throws Exception {
        start();data().db.getIndustryRepository().fish(UUID.randomUUID(),business,4,1,FishingPolicy.prototype());var resource=new FisheryRepository(data().db.getConnection()).find(site).orElseThrow();var founding=data().db.getBusinessRepository().findFounding(business).orElseThrow();
        var frame=frame(30,initial,Set.of(replacement),false);var moved=banks().reconcile(site,frame,30,100);assertEquals(ShorelineRecovery.Action.RELOCATE,moved.decision().action());assertEquals(replacement,banks().find(site).orElseThrow().point());
        banks().reconcile(site,frame(40,replacement,Set.of(replacement),false),40,100);data().db.close();data().open();assertEquals(moved,banks().reconcile(site,frame,1,0));start();
        assertEquals(replacement,banks().find(site).orElseThrow().point());assertEquals(resource,new FisheryRepository(data().db.getConnection()).find(site).orElseThrow());assertEquals(founding,data().db.getBusinessRepository().findFounding(business).orElseThrow());assertEquals(8500,data().cash());
    }
    @Test void confirmedNoBankPausesCatchWithoutDeletingBusinessAndUnknownPreservesLastState()throws Exception {
        start();var unknown=frame(30,initial,Set.of(),true);banks().reconcile(site,unknown,30,100);assertFalse(banks().find(site).orElseThrow().unsafe());
        banks().reconcile(site,frame(40,initial,Set.of(),false),40,100);assertTrue(banks().find(site).orElseThrow().unsafe());
        assertEquals(0,data().db.getIndustryRepository().fish(UUID.randomUUID(),business,4,1,FishingPolicy.prototype()).quantity());assertEquals(1,data().count("businesses"));assertEquals(0,data().count("business_closures"));assertEquals(1,data().count("business_employment"));
        banks().reconcile(site,frame(50,initial,Set.of(),true),50,100);assertTrue(banks().find(site).orElseThrow().unsafe());
        banks().reconcile(site,frame(60,initial,Set.of(replacement),false),60,100);assertFalse(banks().find(site).orElseThrow().unsafe());assertEquals(4,data().db.getIndustryRepository().fish(UUID.randomUUID(),business,4,2,FishingPolicy.prototype()).quantity());
    }
    @Test void lateAuditFailureRollsBackMovementAndOpeningAllPaidRows()throws Exception {
        data().sql("CREATE TRIGGER reject_bank_audit BEFORE INSERT ON fishery_workplace_history BEGIN SELECT RAISE(ABORT,'fixture late failure'); END");
        assertThrows(SQLException.class,this::start);data().unchanged();assertEquals(0,data().count("fishery_resources"));assertEquals(0,data().count("fishery_workplaces"));
        data().sql("DROP TRIGGER reject_bank_audit");start();var before=data().snapshot();data().sql("CREATE TRIGGER reject_bank_audit BEFORE INSERT ON fishery_workplace_history BEGIN SELECT RAISE(ABORT,'fixture late failure'); END");
        assertThrows(SQLException.class,()->banks().reconcile(site,frame(30,initial,Set.of(replacement),false),30,100));assertEquals(before,data().snapshot());
        data().sql("DROP TRIGGER reject_bank_audit");banks().reconcile(site,frame(30,initial,Set.of(replacement),false),30,100);
    }
    @Test void wrongWorldFuturePartialConflictAndChangedOriginCannotMoveSavedBanks()throws Exception {
        start();var good=frame(30,initial,Set.of(replacement),false);var before=data().snapshot();
        assertThrows(SQLException.class,()->banks().reconcile(site,new ShorelineEvidence(UUID.randomUUID(),initial,30,good.assessments()),30,100));assertThrows(SQLException.class,()->banks().reconcile(site,good,29,100));
        assertThrows(SQLException.class,()->banks().reconcile(site,new ShorelineEvidence(data().world,initial,30,good.assessments().stream().filter(a->a.position().equals(initial)).toList()),30,100));assertEquals(before,data().snapshot());
        banks().reconcile(site,good,30,100);assertThrows(SQLException.class,()->banks().reconcile(site,frame(30,initial,Set.of(initial),false),30,100));assertThrows(SQLException.class,()->banks().reconcile(site,frame(40,initial,Set.of(initial),false),40,100));
    }
    @Test void migration69FailureIsAtomicAndLegacyResourcesRemainUnbound()throws Exception {
        fixture.start();SchemaFixtures.rewind(data().db.getConnection(),68);data().sql("PRAGMA user_version=68");var before=data().snapshot();
        data().sql("CREATE VIEW fishery_workplace_history AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(data().db.getConnection()));assertEquals(before,data().snapshot());
        data().sql("DROP VIEW fishery_workplace_history");new SchemaMigrator().migrate(data().db.getConnection());var after=data().snapshot();for(var added:List.of("livestock_learning_reviews","livestock_learning_entries","livestock_slaughter","active_pen_claims","pen_releases","livestock_presence","livestock_birth_losses","livestock_birth_projections","livestock_birth_entities","livestock_births","livestock_product_sales","livestock_capture","livestock_animal_losses","craftsman_founder_courses","craftsman_founder_reviews","business_storage_upgrades","bakery_founder_courses","bakery_founder_reviews","bakery_private_starters","livestock_herds","livestock_initial_animals","livestock_periods","livestock_young"))assertEquals(List.of(),after.remove(added));assertEquals(List.of(),after.remove("protected_pen_animals"));assertEquals(List.of(),after.remove("protected_pen_columns"));assertEquals(List.of(),after.remove("protected_pens"));assertEquals(List.of(),after.remove("animal_ownership"));assertEquals(List.of(),after.remove("farm_market_harvests"));assertEquals(List.of(),after.remove("farm_market_stores"));assertEquals(List.of(),after.remove("grain_harvests"));assertEquals(List.of(),after.remove("grain_stocks"));assertEquals(List.of(),after.remove("farm_grain_evidence"));assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(List.of(),after.remove("fishery_workplace_history"));assertEquals(List.of(),after.remove("fishery_workplaces"));assertEquals(before,after);
        assertThrows(BusinessStartupGrants.Declined.class,this::start);assertEquals(4,data().db.getIndustryRepository().fish(UUID.randomUUID(),business,4,1,FishingPolicy.prototype()).quantity());
    }
    FisheryHabitatObservation habitat(long time,int suitable) {
        return new FisheryHabitatObservation(data().world,time,true,patch.evidence().columns().stream().map(c->new FisheryHabitatObservation.Support(c,c.x()<suitable?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT)).toList());
    }
    @Test void stableFramesRefreshWithoutAnAuditPerTickAndExactSupportCoverageStillMatters()throws Exception {
        start();var review=new FisheryWorksiteReview(data().db.getConnection());
        for(int time=30;time<80;time++)review.reconcile(site,frame(time,initial,Set.of(initial),false),habitat(time,16),time,100,Set.of(data().world));
        assertEquals(1,data().count("fishery_workplace_history"));assertEquals(0,data().count("fishery_habitat_changes"));
        var invalid=new FisheryHabitatObservation(data().world,80,true,List.of(new FisheryHabitatObservation.Support(patch.evidence().columns().getFirst(),FisheryHabitatObservation.State.SUITABLE)));
        var before=data().snapshot();assertThrows(SQLException.class,()->review.reconcile(site,frame(80,initial,Set.of(initial),false),invalid,80,100,Set.of(data().world)));assertEquals(before,data().snapshot());
    }
    @Test void waterLossAndBankRelocationCommitTogetherAndRestorationCannotRefill()throws Exception {
        start();var review=new FisheryWorksiteReview(data().db.getConnection());var bank=frame(30,initial,Set.of(replacement),false);var water=habitat(30,8);
        var moved=review.reconcile(site,bank,water,30,100,Set.of(data().world));var resource=new FisheryRepository(data().db.getConnection()).find(site).orElseThrow();assertEquals(64,resource.stock().capacity());assertEquals(64,resource.stock().available());
        review.reconcile(site,frame(40,replacement,Set.of(replacement),false),habitat(40,16),40,100,Set.of(data().world));assertEquals(moved,review.reconcile(site,bank,water,40,100,Set.of(data().world)));
        resource=new FisheryRepository(data().db.getConnection()).find(site).orElseThrow();assertEquals(128,resource.stock().capacity());assertEquals(64,resource.stock().available());assertEquals(2,data().count("fishery_habitat_changes"));assertEquals(8500,data().cash());
    }
    @Test void lateHabitatFailureRollsBackItsBankMovementAndHistoryToo()throws Exception {
        start();var before=data().snapshot();data().sql("CREATE TRIGGER reject_live_habitat BEFORE INSERT ON fishery_habitat_changes BEGIN SELECT RAISE(ABORT,'fixture late habitat failure'); END");
        var review=new FisheryWorksiteReview(data().db.getConnection());assertThrows(SQLException.class,()->review.reconcile(site,frame(30,initial,Set.of(replacement),false),habitat(30,8),30,100,Set.of(data().world)));assertEquals(before,data().snapshot());
        data().sql("DROP TRIGGER reject_live_habitat");review.reconcile(site,frame(30,initial,Set.of(replacement),false),habitat(30,8),30,100,Set.of(data().world));assertEquals(replacement,banks().find(site).orElseThrow().point());
    }

}
