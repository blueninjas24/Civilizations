package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.database.MineralOpportunities.Status.*;

class GeneratedOpportunityEligibilityTest {
    @TempDir Path directory;
    private void scoped(QuarrySurveyTest f)throws Exception {
        var c=f.db.getConnection();var tracking=new OreTrackingRepository(c);tracking.markGap(11);tracking.begin(12);
        var epoch=new ResourceRecoveryEpochs(c).load("ore").orElseThrow();
        assertTrue(new GeneratedTerrainRepository(c).recordGenerated("ore",f.world,2,0,epoch.generation(),20));
        assertFalse(tracking.load().orElseThrow().trusted());
    }
    private List<MineralObservation> ore(QuarrySurveyTest f,long millis,Position... positions) {
        return List.of(new MineralObservation(f.world,millis,true,Commodity.COAL,List.of(positions)),
                new MineralObservation(f.world,millis,true,Commodity.IRON,List.of()));
    }
    @Test void oldMineralSightingsCannotBecomeCurrentThroughLaterGenerationEvidence()throws Exception {
        var f=new QuarrySurveyTest();f.directory=directory;f.open();
        try {
            var old=new Position(0,40,0);var fresh=new Position(32,40,0);
            f.minerals().observe(f.town,ore(f,10,old,fresh));scoped(f);
            assertEquals(TRACKING_GAP,f.minerals().review(f.town,25,100,Set.of(f.world)).status());
            assertThrows(SQLException.class,()->f.minerals().observe(f.town,ore(f,30,old,fresh)));
            f.minerals().observe(f.town,ore(f,30,fresh));
            var review=f.minerals().review(f.town,30,100,Set.of(f.world));assertEquals(CURRENT,review.status());
            assertEquals(List.of(fresh),review.candidates().stream().map(MineralOpportunities.Evidence::position).toList());
            assertEquals(2,f.minerals().read(f.town,512).size(),"Historical sightings remain recorded without becoming candidates");
            new OrePlacementRepository(f.db.getConnection()).record(f.world,List.of(fresh),31);
            assertTrue(f.minerals().review(f.town,31,100,Set.of(f.world)).candidates().isEmpty());
        } finally {f.close();}
    }
    @Test void quarryFramesFilterOldChunksAndRetainTheActualFreshCaptureTime()throws Exception {
        var f=new QuarrySurveyTest();f.directory=directory;f.open();
        try {
            var fresh=new Position(32,40,0);
            f.quarries().observeChunk(f.town,f.quarries(10,List.of(f.stoneAt),List.of()),0,0);scoped(f);
            assertThrows(SQLException.class,()->f.quarries().observeChunk(f.town,f.quarries(30,List.of(f.stoneAt),List.of()),0,0));
            f.quarries().observeChunk(f.town,f.quarries(30,List.of(fresh),List.of()),2,0);
            var review=f.quarryReview(30,100,Set.of(f.world));assertEquals(CURRENT,review.status());
            assertEquals(30,review.capturedMillis());assertEquals(List.of(fresh),review.candidates().stream().map(MineralOpportunities.Evidence::position).toList());
            f.quarries().observeChunk(f.town,f.quarries(40,List.of(),List.of()),2,0);
            assertTrue(f.quarryReview(40,100,Set.of(f.world)).candidates().isEmpty());
            new OreTrackingRepository(f.db.getConnection()).markGap(41);
            assertEquals(TRACKING_GAP,f.quarryReview(41,100,Set.of(f.world)).status());
            try(var s=f.db.getConnection().createStatement();var rows=s.executeQuery("SELECT COUNT(*) FROM businesses")) {
                assertTrue(rows.next());assertEquals(0,rows.getInt(1));
            }
        } finally {f.close();}
    }
    @Test void scopedCurrentEvidenceCanWinAnActualPaidIndustryInvestmentWithoutGlobalTrust()throws Exception {
        var scenario=new ResourceInvestmentTest();scenario.directory=directory;scenario.setup();
        try {
            var f=scenario.fixture;var c=f.db.getConnection();var tracking=new OreTrackingRepository(c);
            tracking.markGap(1100);tracking.begin(1200);
            scenario.review(2000);assertEquals(0,scenario.count("startup_grants"));assertEquals(60000,f.cash());
            var epoch=new ResourceRecoveryEpochs(c).load("ore").orElseThrow();
            assertTrue(new GeneratedTerrainRepository(c).recordGenerated("ore",f.world,2,0,epoch.generation(),1500));
            String result=scenario.review(7000);assertTrue(result.contains("mining"),result);
            assertEquals(1,f.firms("mining"));assertEquals(0,f.firms("quarry"));
            assertEquals(1,scenario.count("startup_grants"));assertEquals(57500,f.cash());
            assertFalse(tracking.load().orElseThrow().trusted());
        } finally {scenario.close();}
    }
}
