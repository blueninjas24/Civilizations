package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedResourceEligibilityTest {
    @TempDir Path directory;
    private void beginScoped(Connection c,String kind)throws Exception {
        if(kind.equals("ore")) {
            var tracker=new OreTrackingRepository(c);tracker.begin(1);tracker.markGap(2);tracker.begin(3);
            assertFalse(tracker.load().orElseThrow().trusted());
        } else {
            var tracker=new ForestryTrackingRepository(c);tracker.begin(1);tracker.markGap(2);tracker.begin(3);
            assertFalse(tracker.load().orElseThrow().trusted());
        }
    }
    private void certify(Connection c,String kind,UUID world,int x,int z)throws Exception {
        var generation=new ResourceRecoveryEpochs(c).load(kind).orElseThrow().generation();
        assertTrue(new GeneratedTerrainRepository(c).recordGenerated(kind,world,x,z,generation,4));
    }
    @Test void allOreChunksRequiredForPaidOpeningAndWorkWithoutGlobalTrustResetOrRefill()throws Exception {
        var f=new MiningStartupRepositoryTest();f.directory=directory;f.prepare();
        try {
            var c=f.db.getConnection();beginScoped(c,"ore");
            var old=f.request;
            f.request=new MiningStartupRepository.Request(old.founding(),new MineralObservation(f.world,10,true,Commodity.COAL,
                    List.of(new Position(15,40,0),new Position(16,40,0))),old.batchesPerOre(),old.bundle(),old.storageCapacity(),old.spacePerUnit(),old.workingCapital(),old.worker());
            certify(c,"ore",f.world,0,0);
            assertThrows(SQLException.class,()->f.startup.start(f.request));f.unchanged();
            certify(c,"ore",f.world,1,0);
            var opened=f.startup.start(f.request);var id=opened.discovery().deposit().id();
            var industry=f.db.getIndustryRepository();
            assertTrue(industry.sourceHistoryTrusted(id));
            industry.mine(UUID.randomUUID(),f.request.founding().business().id(),id,2,0,8,8);
            assertEquals(6,industry.deposit(id).orElseThrow().remainingBatches());
            assertEquals(6,f.startup.start(f.request).discovery().deposit().remainingBatches());
            assertEquals(8500,f.treasury());assertFalse(new OreTrackingRepository(c).load().orElseThrow().trusted());
            new OreTrackingRepository(c).markGap(20);
            assertFalse(industry.sourceHistoryTrusted(id));
            assertThrows(SQLException.class,()->industry.mine(UUID.randomUUID(),f.request.founding().business().id(),id,1,1,8,8));
            assertEquals(6,industry.deposit(id).orElseThrow().remainingBatches());
            assertEquals(2,f.count("generated_resource_chunks"));
        } finally {f.close();}
    }
    @Test void generatedEvidenceNeverOverridesPlacedOreOrAddsRecoveryApproval()throws Exception {
        var f=new MiningStartupRepositoryTest();f.directory=directory;f.prepare();
        try {
            var c=f.db.getConnection();beginScoped(c,"ore");certify(c,"ore",f.world,0,0);
            new OrePlacementRepository(c).record(f.world,List.of(f.request.evidence().ore().getFirst()),5);
            assertThrows(SQLException.class,()->f.startup.start(f.request));f.unchanged();
            assertEquals(0,f.count("mineral_recoveries"));assertEquals(1,f.count("player_ore_placements"));
        } finally {f.close();}
    }
    @Test void laterGenerationCannotUnlockAnOlderSavedDepositWithoutExplicitRecovery()throws Exception {
        var f=new MiningStartupRepositoryTest();f.directory=directory;f.prepare();
        try {
            var opened=f.startup.start(f.request);var deposit=opened.discovery().deposit().id();
            var c=f.db.getConnection();beginScoped(c,"ore");
            var epoch=new ResourceRecoveryEpochs(c).load("ore").orElseThrow();
            assertTrue(new GeneratedTerrainRepository(c).recordGenerated("ore",f.world,0,0,epoch.generation(),20));
            var industry=f.db.getIndustryRepository();assertFalse(industry.sourceHistoryTrusted(deposit));
            // A new observation can retain identity, but cannot rewrite the original discovery time or grant recovery.
            var evidence=f.request.evidence();
            new MineralDiscoveryRepository(c).discover(f.request.founding().site().id(),
                    new MineralObservation(f.world,30,true,evidence.mineral(),evidence.ore()),f.request.batchesPerOre(),f.request.bundle());
            assertFalse(industry.sourceHistoryTrusted(deposit));
            assertEquals(8,industry.deposit(deposit).orElseThrow().remainingBatches());
            assertEquals(0,f.count("mineral_recoveries"));assertEquals(8500,f.treasury());
        } finally {f.close();}
    }
    @Test void forestOpeningAndWorkUseCompleteSavedSupportsAndRemainRevocable()throws Exception {
        var f=new ForestryPersistenceTest();f.directory=directory;f.setup();
        try {
            var c=f.data.db.getConnection();beginScoped(c,"forest");
            assertThrows(SQLException.class,f::start);assertEquals(0,f.data.count("forestry_resources"));
            certify(c,"forest",f.data.world,1,0);f.start();
            var industry=f.data.db.getIndustryRepository();assertTrue(industry.forestryHistoryTrusted(f.site()));
            industry.log(UUID.randomUUID(),f.firm(),4,1,ForestryPolicy.prototype());
            var resource=f.forests().find(f.site()).orElseThrow();assertEquals(92,resource.stock().available());
            assertEquals(resource,f.start().resource());assertEquals(8500,f.data.cash());
            assertFalse(new ForestryTrackingRepository(c).load().orElseThrow().trusted());
            new ForestryTrackingRepository(c).markGap(20);assertFalse(industry.forestryHistoryTrusted(f.site()));
            assertThrows(SQLException.class,()->industry.log(UUID.randomUUID(),f.firm(),4,2,ForestryPolicy.prototype()));
            assertEquals(resource,f.forests().find(f.site()).orElseThrow());
            assertEquals(0,f.data.count("forest_recoveries"));assertEquals(1,f.data.count("generated_resource_chunks"));
        } finally {f.close();}
    }
}
