package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockLossesTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);}
    @AfterEach void close(){f.close();}
    LivestockLosses losses(){return new LivestockLosses(f.db.getConnection());}
    LivestockLosses.Observation observed(long id){return new LivestockLosses.Observation(new UUID(1,id),new UUID(0,id),f.world,1000,"minecraft:player_attack");}
    LivestockHerd.State state()throws Exception{return f.repo().load(f.business.id()).orElseThrow().state();}
    LivestockHerd.Plan advance(long period)throws Exception {
        var p=LivestockPolicy.prototype();var noBirths=new LivestockPolicy(true,p.maturityPeriods(),p.breedingDelayPeriods(),p.feedPerBirth(),p.workPerBirth(),0,p.cow(),p.chicken());
        return f.repo().advance(new UUID(2,period),f.business.id(),period,10,10,noBirths,BusinessOperationPolicy.prototype(),true);
    }
    Map<String,List<List<Object>>> snapshot()throws Exception {
        var result=new TreeMap<String,List<List<Object>>>();try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while(r.next()){String name=r.getString(1);var rows=new ArrayList<List<Object>>();try(var q=f.db.getConnection().createStatement();var data=q.executeQuery("SELECT * FROM \""+name+"\" ORDER BY rowid")){while(data.next()){var row=new ArrayList<Object>();for(int n=1;n<=data.getMetaData().getColumnCount();n++)row.add(data.getObject(n));rows.add(row);}}result.put(name,rows);}
        }return result;
    }
    @Test void confirmedAdultLossImmediatelyReducesCurrentHerdAndFutureOutputExactlyOnce()throws Exception {
        var before=state();var receipt=losses().record(observed(10)).orElseThrow();assertEquals(0,receipt.afterPeriod());assertEquals(before.adults()-1,state().adults());assertEquals(100,f.units(f.grain));assertEquals(0,f.units(f.milk));
        var first=advance(1);assertEquals(1,first.renewableUnits());assertEquals(1,first.feedUsed());assertEquals(99,f.units(f.grain));
        assertEquals(receipt,losses().record(observed(10)).orElseThrow());assertEquals(first.after(),state());
        var second=advance(2);assertEquals(1,second.matured());assertEquals(2,second.after().adults());
        var oldFirst=advance(1);assertEquals(first,oldFirst,"Old work receipt remains immutable after later deaths/periods");
        f.db.close();f.reopen();var saved=snapshot();assertEquals(receipt,losses().record(observed(10)).orElseThrow());assertEquals(saved,snapshot());assertEquals(second.after(),state());
    }
    @Test void youngLossRemovesOnlyItsSavedCohortAndNeverMaturesLater()throws Exception {
        losses().record(observed(12));assertTrue(state().young().isEmpty());assertEquals(2,state().heads());
        assertEquals(0,advance(1).matured());assertEquals(0,advance(2).matured());assertEquals(2,state().adults());assertEquals(4,f.units(f.milk));
    }
    @Test void initialYoungAlreadyMaturedAtDeathLosesOneAdultWithoutChangingPriorProduction()throws Exception {
        advance(1);var prior=advance(2);assertEquals(3,prior.after().adults());long milk=f.units(f.milk);
        losses().record(observed(12));assertEquals(2,state().adults());assertEquals(prior,advance(2));assertEquals(milk,f.units(f.milk));assertEquals(2,advance(3).renewableUnits());
    }
    @Test void unknownAndPlayerAnimalsStayUnclaimedAndChangedEvidenceOrOperationCannotBeReused()throws Exception {
        var before=snapshot();assertTrue(losses().record(observed(90)).isEmpty());assertEquals(before,snapshot());
        new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(new UUID(0,90),f.world,LivestockAdoption.Ownership.PLAYER,f.owner,null,1,"Explicit player fixture"));before=snapshot();assertTrue(losses().record(observed(90)).isEmpty());assertEquals(before,snapshot());
        losses().record(observed(10));before=snapshot();var o=observed(10);
        assertThrows(java.sql.SQLException.class,()->losses().record(new LivestockLosses.Observation(o.operation(),o.animal(),o.observedWorld(),1001,o.reason())));assertEquals(before,snapshot());
        assertThrows(java.sql.SQLException.class,()->losses().record(new LivestockLosses.Observation(o.operation(),new UUID(0,11),o.observedWorld(),o.observedMillis(),o.reason())));assertEquals(before,snapshot());
    }
    @Test void actualDeathInAnotherWorldIsEvidenceNotMigrationOrOfflineTime()throws Exception {
        var o=observed(10);var actual=new LivestockLosses.Observation(o.operation(),o.animal(),UUID.randomUUID(),o.observedMillis(),"minecraft:lava");
        losses().record(actual);assertEquals(0,state().period());assertEquals(2,state().heads());assertEquals(f.world,new AnimalOwnershipRepository(f.db.getConnection()).load(o.animal()).orElseThrow().originWorld());
        var before=snapshot();f.repo().advance(UUID.randomUUID(),f.business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);assertEquals(before,snapshot());
    }
    @Test void failedLossAndFailedProductionApplicationRollbackThenRetryWithoutDoubleLoss()throws Exception {
        f.sql("CREATE TRIGGER refuse_loss BEFORE INSERT ON livestock_animal_losses BEGIN SELECT RAISE(ABORT,'fixture loss'); END");var before=snapshot();assertThrows(java.sql.SQLException.class,()->losses().record(observed(10)));assertEquals(before,snapshot());f.sql("DROP TRIGGER refuse_loss");losses().record(observed(10));
        f.sql("CREATE TRIGGER refuse_loss_application BEFORE UPDATE ON livestock_animal_losses BEGIN SELECT RAISE(ABORT,'fixture apply'); END");before=snapshot();assertThrows(java.sql.SQLException.class,()->advance(1));assertEquals(before,snapshot());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER refuse_loss_application");assertEquals(1,advance(1).after().adults());assertEquals(1,f.count("livestock_animal_losses"));assertEquals(2,f.count("livestock_periods"));
    }
    @Test void schema81UpgradeIsEmptyTransactionalAndRetainsAllExistingHerdAssets()throws Exception {
        SchemaFixtures.rewind(f.db.getConnection(),81);var before=snapshot();f.sql("CREATE VIEW livestock_animal_losses AS SELECT 1");
        assertThrows(java.sql.SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(81,r.getInt(1));}
        f.sql("DROP VIEW livestock_animal_losses");assertEquals(before,snapshot());new SchemaMigrator().migrate(f.db.getConnection());var after=snapshot();assertEquals(List.of(),after.remove("livestock_learning_reviews"));assertEquals(List.of(),after.remove("livestock_learning_entries"));assertEquals(List.of(),after.remove("livestock_slaughter"));assertEquals(List.of(),after.remove("quarry_founder_courses"));assertEquals(List.of(),after.remove("quarry_founder_reviews"));assertEquals(List.of(),after.remove("quarry_survey_members"));assertEquals(List.of(),after.remove("quarry_surveys"));assertEquals(List.of(),after.remove("quarry_opportunities"));assertEquals(List.of(),after.remove("forestry_work"));assertEquals(List.of(),after.remove("forestry_supports"));assertEquals(List.of(),after.remove("forestry_habitat_changes"));assertEquals(List.of(),after.remove("forestry_resources"));assertEquals(List.of(),after.remove("active_pen_claims"));assertEquals(List.of(),after.remove("pen_releases"));assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(List.of(),after.remove("livestock_product_sales"));assertEquals(List.of(),after.remove("livestock_capture"));assertEquals(List.of(),after.remove("livestock_animal_losses"));assertEquals(before,after);
        losses().record(observed(10));assertEquals(2,state().heads());try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
