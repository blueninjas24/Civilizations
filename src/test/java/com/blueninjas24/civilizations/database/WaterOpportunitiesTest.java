package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WaterOpportunitiesTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    @BeforeEach void open()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WaterTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,0,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
    }
    @AfterEach void close(){db.close();}
    WaterOpportunities repo(){return new WaterOpportunities(db.getConnection());}
    WaterScan.Observation frame(long time,int chunk,int depth,boolean complete,int suitable) {
        var columns=new ArrayList<WaterScan.Column>();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int index=x*16+z;var state=!complete&&index==255?FisheryHabitatObservation.State.UNKNOWN:index<suitable?FisheryHabitatObservation.State.SUITABLE:FisheryHabitatObservation.State.ABSENT;
            columns.add(new WaterScan.Column(new FisheryObservation.Column(chunk*16+x,z),state,state==FisheryHabitatObservation.State.SUITABLE?64:null));
        }
        return new WaterScan.Observation(world,chunk,0,time,-64,320,depth,columns);
    }
    WaterOpportunities.Review review(long now)throws Exception{return repo().review(town,now,100,Set.of(world),2,512);}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    @Test void durableCanonicalReplayCreatesNoAssets()throws Exception {
        var frame=frame(1000,-1,2,true,3);repo().observe(town,frame);var reverse=new ArrayList<>(frame.columns());Collections.reverse(reverse);
        repo().observe(town,new WaterScan.Observation(world,-1,0,1000,-64,320,2,reverse));assertEquals(3,review(1000).columns().size());assertEquals(-16,review(1000).columns().getFirst().position().x());
        assertEquals(1,count("water_surveys"));assertEquals(3,count("water_columns"));
        for(var table:List.of("businesses","business_sites","fishery_resources","treasury_entries","industry_stores"))assertEquals(0,count(table));
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("WaterTest"));db.initialize();assertEquals(3,review(1000).columns().size());
    }
    @Test void partialInvalidatesButDoesNotEraseAndCompleteEmptyClearsOnlyOneChunk()throws Exception {
        repo().observe(town,frame(1000,0,2,true,2));repo().observe(town,frame(1000,1,2,true,1));
        repo().observe(town,frame(1001,0,2,false,1));assertEquals(3,count("water_columns"));assertEquals(1,review(1001).columns().size());
        repo().observe(town,frame(1002,0,2,true,0));assertEquals(1,count("water_columns"));
        repo().observe(town,frame(1000,0,2,true,2));assertEquals(1,review(1002).columns().size());
    }
    @Test void freshnessDepthWorldAndBudgetRejectUnsafeCandidates()throws Exception {
        repo().observe(town,frame(1000,0,2,true,2));assertTrue(review(999).columns().isEmpty());assertTrue(review(1101).columns().isEmpty());
        assertTrue(repo().review(town,1000,100,null,2,512).columns().isEmpty());assertTrue(repo().review(town,1000,100,Set.of(),2,512).columns().isEmpty());
        assertTrue(repo().review(town,1000,100,Set.of(world),3,512).columns().isEmpty());assertTrue(repo().review(town,1000,100,Set.of(world),2,1).columns().isEmpty());
        assertThrows(IllegalArgumentException.class,()->repo().review(town,1000,100,Set.of(world),2,4097));
    }
    @Test void conflictsAndLateFailureRollbackWholeCapture()throws Exception {
        repo().observe(town,frame(1000,0,2,true,2));assertThrows(SQLException.class,()->repo().observe(town,frame(1000,0,2,true,1)));
        try(var s=db.getConnection().createStatement()){s.execute("CREATE TRIGGER reject_water BEFORE INSERT ON water_columns BEGIN SELECT RAISE(ABORT,'late fixture failure'); END");}
        assertThrows(SQLException.class,()->repo().observe(town,frame(1001,0,2,true,1)));assertEquals(2,review(1001).columns().size());assertEquals(1000,review(1001).columns().getFirst().capturedMillis());assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void overlappingTownsDeferEvenWithDifferentDepthTerms()throws Exception {
        UUID other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Other","Fixture",0,64,0,0,0,0));db.getVillageIdentityRepository().bindSettlement(other,world);
        repo().observe(town,frame(1000,0,2,true,1));repo().observe(other,frame(1000,0,3,true,1));assertTrue(review(1000).columns().isEmpty());
        assertTrue(repo().review(other,1000,100,Set.of(world),3,512).columns().isEmpty());repo().observe(other,frame(1001,0,3,true,0));assertEquals(1,review(1001).columns().size());
    }
    @Test void worldMismatchDoesNotWrite()throws Exception {
        var f=frame(1000,0,2,true,1);assertThrows(SQLException.class,()->repo().observe(town,new WaterScan.Observation(UUID.randomUUID(),0,0,1000,-64,320,2,f.columns())));assertEquals(0,count("water_surveys"));
    }
    @Test void migrationLateFailureRollsBackAndRetryPreservesIdentity()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),67);
        try(var s=db.getConnection().createStatement()){s.execute("CREATE VIEW water_columns AS SELECT 1");}
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(67,r.getInt(1));}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='water_surveys'")){assertEquals(0,r.getInt(1));}
        try(var s=db.getConnection().createStatement()){s.execute("DROP VIEW water_columns");}new SchemaMigrator().migrate(db.getConnection());assertEquals(1,count("settlements"));assertEquals(1,count("settlement_worlds"));assertEquals(0,count("water_columns"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void connectedPatchesRespectCurrentSqlQualityAndConfiguredOwnerTerms()throws Exception {
        repo().observe(town,frame(1000,0,2,true,20));assertEquals(1,repo().patches(town,1000,100,Set.of(world),2,16,16,1).size());
        assertEquals(16,repo().patches(town,1000,100,Set.of(world),2,16,16,1).getFirst().evidence().columns().size());
        repo().observe(town,frame(1001,0,2,false,20));assertTrue(repo().patches(town,1001,100,Set.of(world),2,16,16,1).isEmpty());
        db.close();var clock=new java.util.concurrent.atomic.AtomicLong(1002);
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("WaterTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get)) {
            session.configureWater(WaterObserverPolicy.prototype());session.configureFishing(FishingPolicy.prototype());session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.observeWater(town,frame(1002,0,2,true,20)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(16,session.currentFishingPatches(town,100,16,1).get(5,java.util.concurrent.TimeUnit.SECONDS).getFirst().evidence().columns().size());
            session.updateLoadedWorlds(Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(session.currentFishingPatches(town,100,16,1).get(5,java.util.concurrent.TimeUnit.SECONDS).isEmpty());
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WaterTest"));db.initialize();assertEquals(0,count("fishery_resources"));assertEquals(0,count("businesses"));
    }
    @Test void actualOwnerHonorsDepthConfigurationAndAbsentWorldPause()throws Exception {
        db.close();var clock=new java.util.concurrent.atomic.AtomicLong(1000);
        try(var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("WaterTest"),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get)) {
            session.configureWater(WaterObserverPolicy.prototype());session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.observeWater(town,frame(1000,0,2,true,2)).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(2,session.currentWaterOpportunities(town,100,512).get(5,java.util.concurrent.TimeUnit.SECONDS).columns().size());
            assertThrows(java.util.concurrent.ExecutionException.class,()->session.observeWater(town,frame(1001,0,3,true,2)).get(5,java.util.concurrent.TimeUnit.SECONDS));
            session.updateLoadedWorlds(Set.of()).get(5,java.util.concurrent.TimeUnit.SECONDS);assertTrue(session.currentWaterOpportunities(town,100,512).get(5,java.util.concurrent.TimeUnit.SECONDS).columns().isEmpty());
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WaterTest"));db.initialize();assertEquals(2,review(1000).columns().size());
    }
}
