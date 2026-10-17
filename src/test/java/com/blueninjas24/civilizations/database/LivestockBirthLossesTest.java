package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockBirthLossesTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    void open()throws Exception{f.openHerd();f.feed(100);f.advance(new UUID(9,1),1);}
    List<LivestockBirth> children()throws Exception{return new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,256);}
    LivestockBirthProjections projections(){return new LivestockBirthProjections(f.db.getConnection());}
    LivestockLosses losses(){return new LivestockLosses(f.db.getConnection());}
    Claim claim(LivestockBirth birth,long n)throws Exception {
        var c=new Claim(birth.id(),new UUID(20,n),f.world,LivestockHerd.Species.COW,0,64,0,1000+n);
        projections().claim(c,Set.of(f.world));return c;
    }
    LivestockLosses.Observation death(long n){return new LivestockLosses.Observation(new UUID(30,n),new UUID(20,n),f.world,2000+n,"Explicit offspring fixture death");}
    LivestockHerd.State state()throws Exception{return f.repo().load(f.business.id()).orElseThrow().state();}
    Map<String,List<List<Object>>> snapshot()throws Exception{var s=new LivestockLossesTest();s.f=f;return s.snapshot();}
    @Test void confirmedYoungDeathRemovesOnlyThatChildWithoutGoodsRevenueOrRepeatedLoss()throws Exception {
        open();var birth=children().getFirst();var c=claim(birth,1);projections().activate(birth.id(),c.entity());var store=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();
        var receipt=losses().record(death(1)).orElseThrow();assertEquals(1,receipt.afterPeriod());assertEquals(3,state().heads());assertEquals(2,state().adults());assertEquals(List.of(new LivestockHerd.Young(2,1)),state().young());
        assertEquals(store,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());assertEquals(State.DECEASED,projections().find(birth.id()).orElseThrow().state());
        var before=snapshot();assertEquals(receipt,losses().record(death(1)).orElseThrow());assertEquals(before,snapshot());assertEquals(1,f.count("livestock_births"));
    }
    @Test void oneDeadSiblingCannotEraseTheOtherOrLetItMatureTwice()throws Exception {
        var roster=new HashMap<>(f.roster);for(int n=40;n<42;n++){var id=new UUID(0,n);roster.put(id,0);new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,f.world,LivestockAdoption.Ownership.VILLAGE,null,f.town,1,"Explicit sibling fixture adult"));}
        f.repo().open(f.business.id(),LivestockHerd.Species.COW,f.grain,8,roster);f.feed(100);var p=LivestockPolicy.prototype();var twins=new LivestockPolicy(true,12,6,4,2,2,p.cow(),p.chicken());
        f.repo().advance(new UUID(9,1),f.business.id(),1,10,10,twins,BusinessOperationPolicy.prototype(),true);var siblings=children();assertEquals(2,siblings.size());
        claim(siblings.getFirst(),1);claim(siblings.getLast(),2);losses().record(death(1));assertEquals(6,state().heads());
        for(int period=2;period<=13;period++)f.repo().advance(new UUID(9,period),f.business.id(),period,10,10,twins,BusinessOperationPolicy.prototype(),true);
        assertEquals(6,state().adults());assertEquals(State.CLAIMED,projections().find(siblings.getLast().id()).orElseThrow().state());assertEquals(1,f.count("livestock_birth_losses"));
    }
    @Test void adultOffspringDeathUsesItsSavedMaturityAndNeverDeletesAnotherYoungCohort()throws Exception {
        open();var birth=children().getFirst();claim(birth,1);
        for(int period=2;period<=13;period++)f.advance(new UUID(9,period),period);var before=state();assertEquals(4,before.adults());
        losses().record(death(1));assertEquals(3,state().adults());assertEquals(before.young(),state().young());assertEquals(before.heads()-1,state().heads());
    }
    @Test void aChildSharingTheOriginalYoungCohortMaturityDoesNotEraseThatOriginalAnimal()throws Exception {
        f.openHerd();f.feed(100);var p=LivestockPolicy.prototype();var fast=new LivestockPolicy(true,1,6,4,2,1,p.cow(),p.chicken());
        f.repo().advance(new UUID(9,1),f.business.id(),1,10,10,fast,BusinessOperationPolicy.prototype(),true);assertEquals(List.of(new LivestockHerd.Young(2,2)),state().young());
        claim(children().getFirst(),1);losses().record(death(1));assertEquals(List.of(new LivestockHerd.Young(2,1)),state().young());
        var next=f.repo().advance(new UUID(9,2),f.business.id(),2,10,10,fast,BusinessOperationPolicy.prototype(),true);assertEquals(1,next.matured());assertEquals(3,next.after().adults());
    }
    @Test void pendingLossAndAppliedCheckpointSurviveRestartAndReplayWithoutASecondDecrement()throws Exception {
        open();claim(children().getFirst(),1);var receipt=losses().record(death(1)).orElseThrow();f.db.close();f.reopen();assertEquals(3,state().heads());
        assertEquals(receipt,losses().record(death(1)).orElseThrow());var plan=f.advance(new UUID(9,2),2);assertEquals(3,plan.after().adults());assertEquals(3,state().heads());
        f.db.close();f.reopen();var before=snapshot();assertEquals(receipt,losses().record(death(1)).orElseThrow());assertEquals(plan,f.advance(new UUID(9,2),2));assertEquals(before,snapshot());
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT applied_period FROM livestock_birth_losses")){assertTrue(r.next());assertEquals(2,r.getLong(1));}
    }
    @Test void deathCanCommitBeforeActivationButAProvenUnspawnedAliasIsNeverADeath()throws Exception {
        open();var birth=children().getFirst();var first=claim(birth,1);assertTrue(projections().failedUnspawned(birth.id(),first.entity()));var second=claim(birth,2);
        var before=snapshot();assertTrue(losses().record(death(1)).isEmpty());assertEquals(before,snapshot());
        assertTrue(losses().record(death(2)).isPresent());assertFalse(projections().activate(birth.id(),second.entity()));assertEquals(3,state().heads());
    }
    @Test void operationIdsCannotBeReusedAcrossInitialAndOffspringDeathLedgersOrChangedEvidence()throws Exception {
        open();claim(children().getFirst(),1);var d=death(1);losses().record(d);var before=snapshot();
        var initial=new LivestockLosses.Observation(d.operation(),new UUID(0,10),f.world,2100,"Explicit original fixture death");
        assertThrows(SQLException.class,()->losses().record(initial));assertThrows(SQLException.class,()->losses().record(new LivestockLosses.Observation(d.operation(),d.animal(),d.observedWorld(),d.observedMillis()+1,d.reason())));assertEquals(before,snapshot());
        var initialSaved=new LivestockLosses.Observation(new UUID(30,99),new UUID(0,10),f.world,2200,"Explicit initial loss");losses().record(initialSaved);before=snapshot();
        assertThrows(SQLException.class,()->losses().record(new LivestockLosses.Observation(initialSaved.operation(),d.animal(),d.observedWorld(),d.observedMillis(),d.reason())));assertEquals(before,snapshot());
    }
    @Test void lossStateWriteFailureRollsBackReceiptAndHeadChangeForTheExactRetry()throws Exception {
        open();claim(children().getFirst(),1);var before=snapshot();
        f.sql("CREATE TRIGGER refuse_child_death BEFORE UPDATE ON livestock_birth_projections WHEN NEW.state='DECEASED' BEGIN SELECT RAISE(ABORT,'explicit death checkpoint failure'); END");
        assertThrows(SQLException.class,()->losses().record(death(1)));assertEquals(before,snapshot());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER refuse_child_death");assertTrue(losses().record(death(1)).isPresent());assertEquals(3,state().heads());
    }
    @Test void productionCheckpointFailureKeepsBothInitialAndChildLossesPendingUntilOneSuccessfulApplication()throws Exception {
        open();claim(children().getFirst(),1);losses().record(death(1));losses().record(new LivestockLosses.Observation(new UUID(30,99),new UUID(0,10),f.world,2200,"Explicit original fixture loss"));var before=snapshot();
        f.sql("CREATE TRIGGER refuse_child_application BEFORE UPDATE ON livestock_birth_losses WHEN NEW.applied_period IS NOT NULL BEGIN SELECT RAISE(ABORT,'explicit child application failure'); END");
        assertThrows(SQLException.class,()->f.advance(new UUID(9,2),2));assertEquals(before,snapshot());
        f.sql("DROP TRIGGER refuse_child_application");var after=f.advance(new UUID(9,2),2);assertEquals(2,after.after().adults());assertEquals(2,state().heads());assertEquals(4,f.units(f.milk));
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT applied_period FROM livestock_animal_losses UNION ALL SELECT applied_period FROM livestock_birth_losses")){while(r.next())assertEquals(2,r.getLong(1));}
    }
    @Test void deathInAnotherWorldIsConfirmedEvidenceNotElapsedTimeOrAnOwnershipTransfer()throws Exception {
        open();var birth=children().getFirst();var c=claim(birth,1);var d=death(1);var observed=new LivestockLosses.Observation(d.operation(),d.animal(),new UUID(99,1),d.observedMillis(),d.reason());
        losses().record(observed);assertEquals(1,state().period());assertEquals(3,state().heads());assertEquals(f.world,new AnimalOwnershipRepository(f.db.getConnection()).load(c.entity()).orElseThrow().originWorld());
        var before=snapshot();f.repo().advance(new UUID(9,2),f.business.id(),2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);assertEquals(before,snapshot());
    }
}
