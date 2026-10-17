package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSlaughterTest {
    @TempDir Path directory;LivestockRepositoryTest f;IndustryRuntime runtime;final UUID animal=new UUID(0,10);
    @BeforeEach void setup()throws Exception {
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.roster=Map.of(animal,0,new UUID(0,11),0,new UUID(0,12),0);f.openHerd();f.feed(100);
        runtime=new IndustryRuntime(f.db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of()),BusinessOperationPolicy.prototype());
        runtime.configureLivestock(LivestockPolicy.prototype(),new LivestockRuntimePolicy(true,10,10),LivestockWorkingArea.prototype(),()->true);runtime.start(1000);runtime.pulse(2000,Set.of(f.world));
    }
    @AfterEach void close(){f.close();}
    LivestockSlaughter repo(){return new LivestockSlaughter(f.db.getConnection());}
    LivestockPresence.Sight sight(UUID id,long ms,Position p){return new LivestockPresence.Sight(id,LivestockHerd.Species.COW,f.world,p,ms);}
    Optional<LivestockSlaughter.Permit> prepare()throws Exception{return repo().prepare(sight(animal,2000,new Position(0,64,0)),2001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true);}
    long scalar(String sql)throws Exception {try(var s=f.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    LivestockLosses.Observation death(LivestockSlaughter.Permit permit){return new LivestockLosses.Observation(new UUID(9,10),animal,f.world,2002,LivestockSlaughter.REASON_PREFIX+permit.id());}
    @Test void actualDeathCreditsExactlyOnceAndPendingPermitPausesOnlyThisHerdAndReservesStorage()throws Exception {
        var before=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();var permit=prepare().orElseThrow();
        assertEquals(4,permit.units());assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().adults());assertEquals(0,f.units(Commodity.FOOD));
        assertEquals(before.freeSpace()-4,f.db.getIndustryRepository().store(f.business.id()).orElseThrow().freeSpace());
        runtime.pulse(3000,Set.of(f.world));assertEquals(1,f.repo().load(f.business.id()).orElseThrow().state().period());
        var loss=new LivestockLosses(f.db.getConnection());var receipt=loss.record(death(permit));assertTrue(receipt.isPresent());assertEquals(4,f.units(Commodity.FOOD));
        assertEquals(2,f.repo().load(f.business.id()).orElseThrow().state().adults());assertFalse(repo().pending(f.business.id()));
        assertEquals(receipt,loss.record(death(permit)));assertEquals(4,f.units(Commodity.FOOD));
        f.db.close();f.reopen();assertEquals(receipt,new LivestockLosses(f.db.getConnection()).record(death(permit)));assertEquals(4,f.units(Commodity.FOOD));
        assertEquals(1,scalar("SELECT COUNT(*) FROM livestock_slaughter WHERE state='CONFIRMED'"));
    }
    @Test void ordinaryDeathReleasesPermitAndNeverAlsoCreditsBusinessProducts()throws Exception {
        prepare().orElseThrow();new LivestockLosses(f.db.getConnection()).record(new LivestockLosses.Observation(new UUID(9,11),animal,f.world,2002,"minecraft:player_attack"));
        assertEquals(0,f.units(Commodity.FOOD));assertEquals(2,f.repo().load(f.business.id()).orElseThrow().state().adults());assertFalse(repo().pending(f.business.id()));
        assertEquals(1,scalar("SELECT COUNT(*) FROM livestock_slaughter WHERE state='CANCELLED'"));
    }
    @Test void breedingReserveStaleUnknownFarAwayAndAbsentBodiesCannotAuthorizeSlaughter()throws Exception {
        assertTrue(repo().prepare(sight(animal,0,new Position(0,64,0)),10000,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        assertTrue(repo().prepare(sight(animal,2000,new Position(33,64,0)),2001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        assertTrue(repo().prepare(sight(animal,2000,new Position(0,64,0)),2001,Set.of(),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        assertTrue(repo().prepare(sight(new UUID(1,99),2000,new Position(0,64,0)),2001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        new LivestockLosses(f.db.getConnection()).record(new LivestockLosses.Observation(new UUID(9,12),new UUID(0,11),f.world,2000,"minecraft:fall"));
        assertTrue(prepare().isEmpty());assertEquals(0,f.count("livestock_slaughter"));
    }
    @Test void workAndStorageAreRealAndNoRecipeOrCapitalIsCreated()throws Exception {
        f.sql("UPDATE livestock_periods SET available_work=work_used WHERE period=1");assertTrue(prepare().isEmpty());
        f.sql("UPDATE livestock_periods SET available_work=10 WHERE period=1");
        var store=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();f.sql("UPDATE industry_stores SET capacity="+store.usedSpace()+" WHERE business_id='"+f.business.id()+"'");
        assertTrue(prepare().isEmpty());assertFalse(f.db.getIndustryRepository().store(f.business.id()).orElseThrow().units().containsKey(Commodity.FOOD));assertEquals(0,f.count("livestock_slaughter"));
    }
    @Test void latePreparationAndDeathFailuresRollBackAndRetryWithoutLosingHeadsOrMintingGoods()throws Exception {
        f.sql("CREATE TRIGGER fail_permit AFTER INSERT ON livestock_slaughter BEGIN SELECT RAISE(ABORT,'fixture permit failure'); END");
        assertThrows(SQLException.class,this::prepare);assertEquals(0,f.count("livestock_slaughter"));assertFalse(f.db.getIndustryRepository().store(f.business.id()).orElseThrow().units().containsKey(Commodity.FOOD));
        f.sql("DROP TRIGGER fail_permit");var permit=prepare().orElseThrow();
        f.sql("CREATE TRIGGER fail_meat BEFORE UPDATE OF units ON industry_stock WHEN NEW.commodity='food' BEGIN SELECT RAISE(ABORT,'fixture meat failure'); END");
        assertThrows(SQLException.class,()->new LivestockLosses(f.db.getConnection()).record(death(permit)));
        assertEquals(0,f.count("livestock_animal_losses"));assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().adults());assertTrue(repo().pending(f.business.id()));assertEquals(0,f.units(Commodity.FOOD));
        f.sql("DROP TRIGGER fail_meat");new LivestockLosses(f.db.getConnection()).record(death(permit));assertEquals(4,f.units(Commodity.FOOD));
    }
    @Test void cancellationRetainsAnimalAndReleasesReservationWhileChangedPermitCannotCredit()throws Exception {
        var permit=prepare().orElseThrow();repo().cancel(permit.id(),"Fixture native death cancelled");assertFalse(repo().pending(f.business.id()));assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().adults());
        assertThrows(SQLException.class,()->new LivestockLosses(f.db.getConnection()).record(death(permit)));assertEquals(0,f.count("livestock_animal_losses"));assertEquals(0,f.units(Commodity.FOOD));
    }
    @Test void populated89MigrationPreservesEverythingAndFailureRollsBackVersionAndTable()throws Exception {
        var herd=f.repo().load(f.business.id()).orElseThrow();var stock=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();SchemaFixtures.rewind(f.db.getConnection(),89);
        f.sql("CREATE VIEW livestock_slaughter AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));assertEquals(89,scalar("PRAGMA user_version"));
        assertEquals(herd,f.repo().load(f.business.id()).orElseThrow());assertEquals(stock,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());
        f.sql("DROP VIEW livestock_slaughter");new SchemaMigrator().migrate(f.db.getConnection());assertEquals(90,scalar("PRAGMA user_version"));assertEquals(0,f.count("livestock_slaughter"));
        assertEquals(herd,f.repo().load(f.business.id()).orElseThrow());assertEquals(stock,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());
    }
    @Test void activeMatureOffspringUsesItsExistingAliasAndCannotBeReplacedOrCreditedTwice()throws Exception {
        var birth=new LivestockBirths(f.db.getConnection()).page(f.business.id(),0,-1,1).getFirst();var body=new UUID(10,100);
        var projections=new LivestockBirthProjections(f.db.getConnection());
        projections.claim(new LivestockBirthProjections.Claim(birth.id(),body,f.world,LivestockHerd.Species.COW,0,64,0,2000),Set.of(f.world));projections.activate(birth.id(),body);
        assertTrue(repo().prepare(sight(body,2000,new Position(0,64,0)),2001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        for(int n=3;n<=14;n++)runtime.pulse(n*1000L,Set.of(f.world));
        var before=f.repo().load(f.business.id()).orElseThrow().state();
        var permit=repo().prepare(sight(body,14000,new Position(0,64,0)),14001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).orElseThrow();
        var loss=new LivestockLosses.Observation(new UUID(9,100),body,f.world,14002,LivestockSlaughter.REASON_PREFIX+permit.id());
        new LivestockLosses(f.db.getConnection()).record(loss);new LivestockLosses(f.db.getConnection()).record(loss);
        assertEquals(before.heads()-1,f.repo().load(f.business.id()).orElseThrow().state().heads());assertEquals(4,f.units(Commodity.FOOD));
        assertEquals(LivestockBirthProjections.State.DECEASED,projections.find(birth.id()).orElseThrow().state());assertEquals(1,f.count("livestock_birth_losses"));
    }
    @Test void noCurrentWorkerOrUntrustedCaptureCannotAuthorizeRetirement()throws Exception {
        assertTrue(repo().prepare(sight(animal,2000,new Position(0,64,0)),2001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->false).isEmpty());
        f.db.getBusinessRepository().endEmployment(new UUID(0,7),0,"Fixture no remaining staff");assertTrue(prepare().isEmpty());assertEquals(0,f.count("livestock_slaughter"));
    }
    @Test void unresolvedPermitSurvivesRestartWithoutInferringDeathGoodsOrElapsedWork()throws Exception {
        var permit=prepare().orElseThrow();var before=f.repo().load(f.business.id()).orElseThrow();var store=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();
        f.db.close();f.reopen();assertTrue(repo().pending(f.business.id()));assertEquals(store,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());
        var live=new IndustryRuntime(f.db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of()),BusinessOperationPolicy.prototype());
        live.configureLivestock(LivestockPolicy.prototype(),new LivestockRuntimePolicy(true,10,10),LivestockWorkingArea.prototype(),()->true);live.start(100000);live.pulse(200000,Set.of(f.world));
        assertEquals(before,f.repo().load(f.business.id()).orElseThrow());assertEquals(0,f.count("livestock_animal_losses"));assertEquals(0,f.units(Commodity.FOOD));
        repo().cancel(permit.id(),"Trusted adapter observed same body alive and did not dispatch");live.pulse(201000,Set.of(f.world));
        assertEquals(2,f.repo().load(f.business.id()).orElseThrow().state().period());assertEquals(0,f.units(Commodity.FOOD));
    }
    @Test void enrollingOldManualProductionCannotTurnHistoricalUnusedWorkIntoNativeSlaughter()throws Exception {
        UUID business=new UUID(2,1);var site=new BusinessSite(new UUID(2,2),f.town,f.world,8,64,0,1,"Explicit manual legacy fixture");
        var req=new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of());
        var firm=new Business(business,f.town,"Legacy manual herd",f.owner,f.owner,"livestock",site.id(),1,0);
        f.db.getBusinessRepository().found(new BusinessFounding(firm,site,req,req,0,"Explicit manual fixture"));f.db.getBusinessRepository().hire(new UUID(2,3),business,f.owner,0);
        f.db.getIndustryRepository().openStore(business,128,Map.of(f.grain,1L,f.milk,1L),0);
        var animals=Map.of(new UUID(2,10),0,new UUID(2,11),0,new UUID(2,12),0);
        for(var id:animals.keySet())new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,f.world,LivestockAdoption.Ownership.VILLAGE,null,f.town,1,"Manual fixture animal"));
        new LivestockRepository(f.db.getConnection()).open(business,LivestockHerd.Species.COW,f.grain,8,animals);
        f.sql("UPDATE industry_stock SET units=20 WHERE business_id='"+business+"' AND commodity='grain'");
        new LivestockRepository(f.db.getConnection()).advance(new UUID(2,20),business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
        runtime.start(5000);
        assertTrue(repo().prepare(sight(new UUID(2,10),5000,new Position(8,64,0)),5001,Set.of(f.world),LivestockPolicy.prototype(),LivestockSlaughterPolicy.prototype(),BusinessOperationPolicy.prototype(),LivestockWorkingArea.prototype(),()->true).isEmpty());
        assertEquals(0,f.count("livestock_slaughter"));
    }
}
