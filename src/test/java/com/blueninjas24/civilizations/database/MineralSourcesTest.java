package com.blueninjas24.civilizations.database;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class MineralSourcesTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),resident=UUID.randomUUID(),site=UUID.randomUUID(),mine=UUID.randomUUID();
    final List<VillageObservation.Position> coordinates=List.of(new VillageObservation.Position(0,40,0),new VillageObservation.Position(1,40,0),new VillageObservation.Position(2,40,0),new VillageObservation.Position(3,40,0));
    UUID deposit;
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Sources","World",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(resident,"Miner",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Real fixture capital");
        var place=new BusinessSite(site,town,world,0,40,0,1,"Saved explicit fixture site");var business=new Business(mine,town,"Mine",resident,resident,"mining",site,1,0);
        db.getBusinessRepository().found(new BusinessFounding(business,place,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Actual fixture opportunity"));
        db.getBusinessRepository().hire(UUID.randomUUID(),mine,resident,0);db.getIndustryRepository().openStore(mine,10000,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),1000);
        deposit=new MineralDiscoveryRepository(db.getConnection()).discover(site,new MineralObservation(world,1,true,Commodity.COAL,coordinates),25,Map.of(Commodity.COAL,1L,Commodity.STONE,2L)).deposit().id();
    }
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("MineralSourcesTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    MineralSources sources(){return new MineralSources(db.getConnection());}
    List<Long> balances()throws Exception{return sources().shares(deposit).stream().map(MineralSources.Share::remaining).toList();}
    long reserve()throws Exception{return db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches();}
    long count(String table)throws Exception{try(var q=db.getConnection().createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws Exception{try(var q=db.getConnection().createStatement()){q.execute(query);}}
    IndustryRepository.Receipt mine(UUID operation,long amount,long period)throws Exception{return db.getIndustryRepository().mine(operation,mine,deposit,amount,period,100,100);}
    @Test void newDiscoveryAttributesEachOriginalBlockAndMiningDrainsEvenlyOnce()throws Exception {
        assertEquals(List.of(25L,25L,25L,25L),balances());var operation=UUID.randomUUID();assertEquals(7,mine(operation,7,1).quantity());
        assertEquals(List.of(23L,23L,23L,24L),balances());assertEquals(93,reserve());assertEquals(4,count("mineral_source_extraction"));
        assertEquals(7,mine(operation,7,1).quantity());assertEquals(93,reserve());assertEquals(4,count("mineral_source_extraction"));
        assertEquals(7,db.getIndustryRepository().store(mine).orElseThrow().units().get(Commodity.COAL));assertEquals(14,db.getIndustryRepository().store(mine).orElseThrow().units().get(Commodity.STONE));
    }
    @Test void singleBlockRetirementPreservesOtherSharesExtractedGoodsAndHistoricReceiptsThroughRestart()throws Exception {
        var operation=UUID.randomUUID();mine(operation,7,1);assertEquals(23,sources().remove(world,List.of(coordinates.getFirst()),2,"Player removed source"));
        assertEquals(List.of(0L,23L,23L,24L),balances());assertEquals(70,reserve());assertEquals(0,sources().remove(world,List.of(coordinates.getFirst()),3,"Repeat"));
        db.close();open();assertEquals(70,reserve());mine(operation,7,1);assertEquals(70,reserve());assertEquals(7,db.getIndustryRepository().store(mine).orElseThrow().units().get(Commodity.COAL));
        mine(UUID.randomUUID(),10,2);assertEquals(List.of(0L,19L,20L,21L),balances());assertEquals(60,reserve());
        assertEquals(1,count("ore_source_removals"));assertEquals(1,count("mineral_source_retirements"));
    }
    @Test void removalOfUnknownOrLinkedZeroReserveSourceCannotInventOrDestroyOriginalShares()throws Exception {
        var unknown=new VillageObservation.Position(8,40,0);assertEquals(0,sources().remove(world,List.of(unknown),2,"Pre-discovery break"));assertEquals(100,reserve());
        assertTrue(new OrePlacementRepository(db.getConnection()).excludes(world,unknown));
        assertThrows(SQLException.class,()->new MineralDiscoveryRepository(db.getConnection()).discover(site,new MineralObservation(world,3,true,Commodity.COAL,List.of(coordinates.getLast(),unknown)),25,Map.of(Commodity.COAL,1L,Commodity.STONE,2L)));
        var linked=new VillageObservation.Position(4,40,0);new MineralDiscoveryRepository(db.getConnection()).discover(site,new MineralObservation(world,3,true,Commodity.COAL,List.of(coordinates.getLast(),linked)),999,Map.of(Commodity.COAL,1L,Commodity.STONE,2L));
        assertEquals(List.of(25L,25L,25L,25L,0L),balances());assertEquals(0,sources().remove(world,List.of(linked),4,"Linked evidence only"));assertEquals(100,reserve());
    }
    @Test void replacingOrResurveyingARecordedRemovedSourceCannotRefillDeposit()throws Exception {
        sources().remove(world,List.of(coordinates.get(1)),2,"Player break");assertEquals(75,reserve());
        new OrePlacementRepository(db.getConnection()).record(world,List.of(coordinates.get(1)),3);
        assertThrows(SQLException.class,()->new MineralDiscoveryRepository(db.getConnection()).discover(site,new MineralObservation(world,4,true,Commodity.COAL,coordinates),25,Map.of(Commodity.COAL,1L,Commodity.STONE,2L)));
        assertEquals(75,reserve());assertEquals(1,count("mineral_source_retirements"));
        assertEquals(Set.of(coordinates.get(1)),new OrePlacementRepository(db.getConnection()).inChunk(world,0,0,1));
        sources().remove(world,List.of(coordinates.get(2)),5,"Another source");assertThrows(SQLException.class,()->new OrePlacementRepository(db.getConnection()).inChunk(world,0,0,1));
    }
    @Test void failedSourceExtractionRollsBackOutputDepositReceiptsAndCanRetryOnce()throws Exception {
        sql("CREATE TRIGGER reject_source BEFORE INSERT ON mineral_source_extraction BEGIN SELECT RAISE(ABORT,'source failure'); END");
        var operation=UUID.randomUUID();assertThrows(SQLException.class,()->mine(operation,7,1));assertEquals(100,reserve());assertEquals(List.of(25L,25L,25L,25L),balances());assertEquals(0,count("industry_production"));assertEquals(0,db.getIndustryRepository().store(mine).orElseThrow().units().get(Commodity.COAL));
        sql("DROP TRIGGER reject_source");mine(operation,7,1);assertEquals(93,reserve());assertEquals(1,count("industry_production"));
    }
    @Test void failedRetirementRollsBackTombstoneSourceAndAggregateThenRetries()throws Exception {
        sql("CREATE TRIGGER reject_retirement BEFORE INSERT ON mineral_source_retirements BEGIN SELECT RAISE(ABORT,'retirement failure'); END");
        assertThrows(SQLException.class,()->sources().remove(world,List.of(coordinates.getFirst()),2,"Player break"));assertEquals(100,reserve());assertEquals(0,count("ore_source_removals"));assertEquals(List.of(25L,25L,25L,25L),balances());
        sql("DROP TRIGGER reject_retirement");assertEquals(25,sources().remove(world,List.of(coordinates.getFirst()),2,"Player break"));assertEquals(75,reserve());
    }
    @Test void boundedBatchRetiresOnlyRequestedSharesAndExhaustionNeverRefills()throws Exception {
        assertEquals(50,sources().remove(world,List.of(coordinates.get(0),coordinates.get(2)),2,"Explicit fixture batch"));assertEquals(50,reserve());
        assertEquals(50,mine(UUID.randomUUID(),100,1).quantity());assertEquals(List.of(0L,0L,0L,0L),balances());assertEquals(0,reserve());
        assertEquals(0,sources().remove(world,List.of(coordinates.get(1)),3,"Exhausted source"));assertEquals(0,mine(UUID.randomUUID(),100,2).quantity());
        assertThrows(IllegalArgumentException.class,()->sources().remove(world,List.of(coordinates.get(3),coordinates.get(3)),4,"Duplicate input"));
    }
    @Test void schema45ConversionKeepsUnevenRemainingTotalGoodsAndAddsOnlyAdoptionNotHistoricalExtraction()throws Exception {
        var operation=UUID.randomUUID();mine(operation,7,1);SchemaFixtures.rewind(db.getConnection(),45);new SchemaMigrator().migrate(db.getConnection());
        assertEquals(93,reserve());assertEquals(List.of(24L,23L,23L,23L),balances());assertEquals(0,count("mineral_source_extraction"));assertEquals(1,count("mineral_source_adoptions"));
        assertEquals(7,db.getIndustryRepository().store(mine).orElseThrow().units().get(Commodity.COAL));mine(operation,7,1);assertEquals(93,reserve());
        try(var q=db.getConnection().createStatement();var r=q.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void failedMigrationRetainsOldVersionResourcesAndHistoryThenRetryConvertsOnce()throws Exception {
        mine(UUID.randomUUID(),7,1);SchemaFixtures.rewind(db.getConnection(),45);sql("CREATE INDEX mineral_sources_by_deposit ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(93,reserve());assertEquals(1,count("industry_production"));
        sql("DROP INDEX mineral_sources_by_deposit");new SchemaMigrator().migrate(db.getConnection());assertEquals(93,reserve());assertEquals(1,count("mineral_source_adoptions"));
        new SchemaMigrator().migrate(db.getConnection());assertEquals(List.of(24L,23L,23L,23L),balances());
    }
    @Test void failureOnSecondCoordinateRollsBackEntireRemovalBatch()throws Exception {
        sql("CREATE TRIGGER reject_second BEFORE INSERT ON mineral_source_retirements WHEN NEW.x=2 BEGIN SELECT RAISE(ABORT,'second failure'); END");
        assertThrows(SQLException.class,()->sources().remove(world,List.of(coordinates.get(0),coordinates.get(2)),2,"Batch break"));
        assertEquals(100,reserve());assertEquals(List.of(25L,25L,25L,25L),balances());assertEquals(0,count("ore_source_removals"));assertEquals(0,count("mineral_source_retirements"));
    }
    @Test void inconsistentSharesCannotGenerateOutputOrRetireAnyResource()throws Exception {
        sql("UPDATE mineral_sources SET remaining_batches=24 WHERE x=0");
        assertThrows(SQLException.class,()->mine(UUID.randomUUID(),7,1));assertEquals(100,reserve());assertEquals(0,count("industry_production"));
        assertThrows(SQLException.class,()->sources().remove(world,List.of(coordinates.get(1)),2,"Break"));assertEquals(100,reserve());assertEquals(0,count("ore_source_removals"));
    }
    @Test void otherWorldRemovalDoesNotAffectThisWorldAndMigrationHandlesTinyRemainingReserve()throws Exception {
        var other=UUID.randomUUID();db.getVillageIdentityRepository().observeWorld(other,"Other");
        assertEquals(0,sources().remove(other,List.of(coordinates.getFirst()),2,"Other world"));assertEquals(100,reserve());
        mine(UUID.randomUUID(),98,1);SchemaFixtures.rewind(db.getConnection(),45);new SchemaMigrator().migrate(db.getConnection());
        assertEquals(List.of(1L,1L,0L,0L),balances());assertEquals(2,reserve());
    }
}
