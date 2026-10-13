package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class QuarryPersistenceTest {
    @TempDir Path directory;
    BusinessStartupGrantsTest fixture;
    final List<Position> blocks=List.of(new Position(20,40,0),new Position(21,40,0));
    @BeforeEach void setup()throws Exception {fixture=new BusinessStartupGrantsTest();fixture.directory=directory;fixture.setup();}
    @AfterEach void close(){fixture.close();}
    Commodity resource(String kind){return kind.equals("quarry")?Commodity.STONE:Commodity.SAND;}
    MiningStartupRepository.Request request(String kind) {
        var source=fixture.mine(fixture.person,20).founding();var b=source.business();
        var business=new Business(b.id(),b.settlementId(),"Finite fixture",b.founderId(),b.ownerId(),kind,b.siteId(),b.workerCapacity(),b.foundedStep());
        var founding=new BusinessFounding(business,source.site(),source.founderRequirements(),source.workerRequirements(),500,"Explicit finite customer/site fixture");
        var good=resource(kind);var terms=QuarryPolicy.prototype().terms(good);
        return new MiningStartupRepository.Request(founding,new MineralObservation(fixture.world,10,true,good,blocks),
            terms.reserveBatchesPerBlock(),QuarryPolicy.prototype().bundle(good),1024,Map.of(good,1L),1000,fixture.person);
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void realGrantFiniteNonOneToOneProductionRemovalAndRestart(String kind)throws Exception {
        var request=request(kind);var started=fixture.grants().startMine(request,StartupGrantPolicy.prototype());
        var id=request.founding().business().id();var deposit=started.startup().discovery().deposit().id();var good=resource(kind);
        assertEquals(8500,fixture.cash());assertEquals(128,started.startup().discovery().deposit().remainingBatches());
        var operation=UUID.randomUUID();var terms=QuarryPolicy.prototype().terms(good);
        var receipt=fixture.db.getIndustryRepository().mine(operation,id,deposit,100,1,terms.batchesPerWorker(),terms.siteCapacity());
        assertEquals(terms.batchesPerWorker(),receipt.quantity());
        var before=fixture.db.getIndustryRepository().store(id).orElseThrow();
        assertEquals(receipt.quantity()*terms.unitsPerBatch(),before.units().get(good));
        var shares=new MineralSources(fixture.db.getConnection()).shares(deposit);long lost=shares.getFirst().remaining();
        assertEquals(lost,new MineralSources(fixture.db.getConnection()).remove(fixture.world,List.of(blocks.getFirst()),20,"Fixture player removed support"));
        long remaining=128-receipt.quantity()-lost;
        fixture.db.close();fixture.open();
        assertEquals(remaining,fixture.db.getIndustryRepository().deposit(deposit).orElseThrow().remainingBatches());
        assertEquals(receipt,fixture.db.getIndustryRepository().mine(operation,id,deposit,100,1,terms.batchesPerWorker(),terms.siteCapacity()));
        assertEquals(before,fixture.db.getIndustryRepository().store(id).orElseThrow());
        var discovery=new MineralDiscoveryRepository(fixture.db.getConnection()).discover(request.founding().site().id(),
            new MineralObservation(fixture.world,30,true,good,List.of(blocks.getLast())),999,Map.of(good,999L));
        assertFalse(discovery.created());assertEquals(remaining,discovery.deposit().remainingBatches());assertEquals(request.bundle(),discovery.deposit().bundle());
        assertEquals(0,new MineralSources(fixture.db.getConnection()).remove(fixture.world,List.of(blocks.getFirst()),40,"Repeated removal"));
        assertEquals(8500,fixture.cash());assertEquals(1,fixture.count("startup_grants"));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void placedRemovedIncompleteAndConflictingResourcesCannotStart(String kind)throws Exception {
        var request=request(kind);var before=fixture.snapshot();
        var partial=new MiningStartupRepository.Request(request.founding(),new MineralObservation(fixture.world,10,false,resource(kind),blocks),
            request.batchesPerOre(),request.bundle(),request.storageCapacity(),request.spacePerUnit(),request.workingCapital(),request.worker());
        assertThrows(SQLException.class,()->fixture.grants().startMine(partial,StartupGrantPolicy.prototype()));assertEquals(before,fixture.snapshot());
        new OrePlacementRepository(fixture.db.getConnection()).record(fixture.world,blocks,10);before=fixture.snapshot();
        assertThrows(SQLException.class,()->fixture.grants().startMine(request,StartupGrantPolicy.prototype()));assertEquals(before,fixture.snapshot());
        assertThrows(IllegalArgumentException.class,()->new MiningStartupRepository.Request(request.founding(),
            new MineralObservation(fixture.world,10,true,Commodity.COAL,blocks),1,Map.of(Commodity.COAL,1L),100,Map.of(Commodity.COAL,1L),0,fixture.person));
    }
    @ParameterizedTest @ValueSource(strings={"quarry","sandpit"})
    void lateSourceFailureRollsBackAllPaymentsAndAssetsThenRetries(String kind)throws Exception {
        var request=request(kind);var before=fixture.snapshot();
        fixture.sql("CREATE TRIGGER fail_quarry BEFORE INSERT ON mineral_sources BEGIN SELECT RAISE(ABORT,'fixture source failure'); END");
        assertThrows(SQLException.class,()->fixture.grants().startMine(request,StartupGrantPolicy.prototype()));assertEquals(before,fixture.snapshot());
        fixture.sql("DROP TRIGGER fail_quarry");fixture.grants().startMine(request,StartupGrantPolicy.prototype());
        assertEquals(8500,fixture.cash());assertEquals(2,fixture.count("mineral_sources"));
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    void oldSchema()throws Exception {
        fixture.sql("PRAGMA foreign_keys=OFF");
        SchemaFixtures.rewind(fixture.db.getConnection(),63);
        String ddl;try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE name='mineral_discovery'")){ddl=r.getString(1);}
        fixture.sql(ddl.replace("mineral_discovery", "mineral_discovery_old").replace("'coal','iron','stone','sand'","'coal','iron'"));
        fixture.sql("INSERT INTO mineral_discovery_old SELECT * FROM mineral_discovery");fixture.sql("DROP TABLE mineral_discovery");
        fixture.sql("ALTER TABLE mineral_discovery_old RENAME TO mineral_discovery");fixture.sql("PRAGMA user_version=63");fixture.sql("PRAGMA foreign_keys=ON");
    }
    long scalar(String sql)throws Exception{try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    @Test void upgradeRetainsAllExistingMineAssetsHistoryAndForeignKeyEnforcement()throws Exception {
        var started=fixture.grants().startMine(fixture.request,StartupGrantPolicy.prototype());
        fixture.db.getIndustryRepository().mine(UUID.randomUUID(),fixture.request.founding().business().id(),started.startup().discovery().deposit().id(),2,1,8,8);
        oldSchema();var before=fixture.snapshot();new SchemaMigrator().migrate(fixture.db.getConnection());
        var after=fixture.snapshot();assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(List.of(),after.remove("fishery_workplace_history"));assertEquals(List.of(),after.remove("fishery_workplaces"));for(var table:List.of("village_supply_orders","fishery_resources","fishery_columns","fishery_work","fishery_habitat_changes","water_surveys","water_columns"))assertEquals(List.of(),after.remove(table));assertEquals(before,after);
        assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(1,scalar("PRAGMA foreign_keys"));
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void failedUpgradeRestoresOriginalConstraintRowsAndForeignKeysThenRetries()throws Exception {
        fixture.grants().startMine(fixture.request,StartupGrantPolicy.prototype());oldSchema();var before=fixture.snapshot();
        fixture.sql("CREATE VIEW mineral_discovery_v64 AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));assertEquals(before,fixture.snapshot());
        assertEquals(63,scalar("PRAGMA user_version"));assertEquals(1,scalar("PRAGMA foreign_keys"));assertTrue(fixture.db.getConnection().getAutoCommit());
        fixture.sql("DROP VIEW mineral_discovery_v64");new SchemaMigrator().migrate(fixture.db.getConnection());
        var after=fixture.snapshot();assertEquals(List.of(),after.remove("fishery_founder_courses"));assertEquals(List.of(),after.remove("fishery_workplace_history"));assertEquals(List.of(),after.remove("fishery_workplaces"));for(var table:List.of("village_supply_orders","fishery_resources","fishery_columns","fishery_work","fishery_habitat_changes","water_surveys","water_columns"))assertEquals(List.of(),after.remove(table));assertEquals(before,after);
    }
    @Test void postRebuildForeignKeyFailureRollsBackDdlAndRestoresEnforcement()throws Exception {
        fixture.grants().startMine(fixture.request,StartupGrantPolicy.prototype());oldSchema();
        fixture.sql("PRAGMA foreign_keys=OFF");fixture.sql("UPDATE mineral_discovery SET world_id='missing-world'");fixture.sql("PRAGMA foreign_keys=ON");
        var before=fixture.snapshot();var failure=assertThrows(SQLException.class,()->new SchemaMigrator().migrate(fixture.db.getConnection()));
        assertTrue(failure.getMessage().contains("foreign keys"));assertEquals(before,fixture.snapshot());
        assertEquals(63,scalar("PRAGMA user_version"));assertEquals(1,scalar("PRAGMA foreign_keys"));
        try(var s=fixture.db.getConnection().createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE name='mineral_discovery'")){assertFalse(r.getString(1).contains("'stone'"));}
        fixture.sql("PRAGMA foreign_keys=OFF");fixture.sql("UPDATE mineral_discovery SET world_id='"+fixture.world+"'");fixture.sql("PRAGMA foreign_keys=ON");
        new SchemaMigrator().migrate(fixture.db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));
    }
}
