package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.database.MineralOpportunities.Status.*;

class CurrentMineralSurveyTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    List<MineralObservation> pair(long time,int...xs) {
        return List.of(new MineralObservation(world,time,true,Commodity.COAL,Arrays.stream(xs)
            .mapToObj(x->new VillageObservation.Position(x,40,0)).toList()),
            new MineralObservation(world,time,true,Commodity.IRON,List.of()));
    }
    MineralOpportunities opportunities(){return new MineralOpportunities(db.getConnection());}
    MineralOpportunities.Review review(long now,long age)throws SQLException{return opportunities().review(town,now,age,Set.of(world));}
    void reopen()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("CurrentSurveyTest"));db.initialize();}
    @BeforeEach void open()throws Exception {
        reopen();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        new OreTrackingRepository(db.getConnection()).begin(0);
    }
    @AfterEach void close(){db.close();}
    void sql(String sql)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    @Test void smallerAndEmptyFramesReplaceCurrentCandidatesWithoutErasingHistory()throws Exception {
        assertEquals(MISSING,review(10,10).status());opportunities().observe(town,pair(10,0,1));
        assertEquals(2,review(10,10).candidates().size());opportunities().observe(town,pair(20,1));
        assertEquals(1,review(20,10).candidates().size());assertEquals(1,review(20,10).candidates().getFirst().position().x());
        opportunities().observe(town,pair(30));assertEquals(CURRENT,review(30,10).status());assertTrue(review(30,10).candidates().isEmpty());
        assertEquals(2,opportunities().read(town,512).size());
        assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());assertTrue(new TreasuryRepository(db.getConnection()).load(town).isEmpty());
    }
    @Test void olderFramesCannotRestoreCandidatesAfterEmptySurveyEvenAcrossRestart()throws Exception {
        opportunities().observe(town,pair(10,0));opportunities().observe(town,pair(30));opportunities().observe(town,pair(20,1));
        db.close();reopen();assertEquals(30,review(30,10).capturedMillis());assertTrue(review(30,10).candidates().isEmpty());
        assertEquals(2,opportunities().read(town,512).size());
    }
    @Test void identicalCaptureReplayIsStableButDifferentEvidenceAtSameTimeRollsBack()throws Exception {
        opportunities().observe(town,pair(10,1,0));var before=review(10,10);opportunities().observe(town,pair(10,0,1));
        assertEquals(before,review(10,10));assertThrows(SQLException.class,()->opportunities().observe(town,pair(10,0,2)));
        assertEquals(before,review(10,10));assertEquals(2,opportunities().read(town,512).size());assertTrue(db.getConnection().getAutoCommit());
        assertThrows(UnsupportedOperationException.class,()->before.candidates().clear());
    }
    @Test void freshnessBoundariesClockRollbackAndOverflowAreConservative()throws Exception {
        opportunities().observe(town,pair(10,0));assertEquals(CURRENT,review(20,10).status());
        assertEquals(STALE,review(21,10).status());assertEquals(FUTURE,review(9,10).status());
        assertTrue(review(21,10).candidates().isEmpty());assertEquals(STALE,review(Long.MAX_VALUE,1).status());
        assertEquals(CURRENT,review(Long.MAX_VALUE,Long.MAX_VALUE).status());
        assertThrows(IllegalArgumentException.class,()->review(-1,10));assertThrows(IllegalArgumentException.class,()->review(10,0));
    }
    @Test void worldAbsenceUnknownAvailabilityAndTrackingGapsHideCandidatesButKeepHistory()throws Exception {
        opportunities().observe(town,pair(10,0));
        assertEquals(INACTIVE_WORLD,opportunities().review(town,10,10,Set.of()).status());
        assertEquals(INACTIVE_WORLD,opportunities().review(town,10,10,null).status());
        assertEquals(CURRENT,review(10,10).status());new OreTrackingRepository(db.getConnection()).markGap(11);
        assertEquals(TRACKING_GAP,review(11,10).status());assertTrue(review(11,10).candidates().isEmpty());
        assertEquals(1,opportunities().read(town,512).size());
    }
    @Test void missingMutationHistoryIsNotCertifiedBySavedSurvey()throws Exception {
        opportunities().observe(town,pair(10,0));sql("DELETE FROM ore_tracking");assertEquals(TRACKING_GAP,review(10,10).status());
    }
    @Test void laterPlacementAndRemovalFilterSavedFrameWithoutNeedingAnotherSurvey()throws Exception {
        opportunities().observe(town,pair(10,0,1));var coordinates=pair(10,0,1).getFirst().ore();
        new OrePlacementRepository(db.getConnection()).record(world,List.of(coordinates.getFirst()),11);
        assertEquals(1,review(11,10).candidates().size());
        new MineralSources(db.getConnection()).remove(world,List.of(coordinates.getLast()),12,"Fixture break");
        assertTrue(review(12,10).candidates().isEmpty());assertEquals(10,review(12,10).capturedMillis());
    }
    @Test void anotherVillagesClaimNeverCreatesASecondCandidateOrReserve()throws Exception {
        opportunities().observe(town,pair(10,0));
        var other=UUID.randomUUID();db.saveSettlement(new Settlement(other,"Other","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().bindSettlement(other,world);var owner=UUID.randomUUID();
        db.getPopulationRepository().insertResident(new Resident(owner,"Owner",other,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),other,world,0,40,0,10,"Fixture site");
        var mine=new Business(UUID.randomUUID(),other,"Mine",owner,owner,"mining",site.id(),4,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(mine,site,requirements,requirements,0,"Fixture"));
        var saved=new MineralDiscoveryRepository(db.getConnection()).discover(site.id(),pair(10,0).getFirst(),2,Map.of(Commodity.COAL,1L));
        assertTrue(review(10,10).candidates().isEmpty());
        assertEquals(2,db.getIndustryRepository().deposit(saved.deposit().id()).orElseThrow().remainingBatches());
    }
    @Test void failedLatestFrameWriteRollsBackHistoricalUpdatesAndReplacementThenRetries()throws Exception {
        opportunities().observe(town,pair(10,0));var before=review(10,100);
        sql("CREATE TRIGGER reject_member BEFORE INSERT ON mineral_survey_members WHEN NEW.x=1 BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->opportunities().observe(town,pair(20,0,1)));
        assertEquals(before,review(10,100));assertEquals(1,opportunities().read(town,512).size());
        assertEquals(10,opportunities().read(town,512).getFirst().lastSeenMillis());
        sql("DROP TRIGGER reject_member");opportunities().observe(town,pair(20,0,1));assertEquals(2,review(20,100).candidates().size());
    }
    @Test void partialFrameCannotReplaceCompleteSurveyOrRefreshItsExpiry()throws Exception {
        opportunities().observe(town,pair(10,0));
        var partial=new MineralObservation(world,20,false,Commodity.COAL,pair(20,1).getFirst().ore());
        assertThrows(SQLException.class,()->opportunities().observe(town,List.of(partial,pair(20).getLast())));
        assertEquals(10,review(20,10).capturedMillis());assertEquals(1,opportunities().read(town,512).size());
    }
    @Test void completeBoundedPageIncludesAll512MembersAndSeparatesTowns()throws Exception {
        opportunities().observe(town,pair(10,java.util.stream.IntStream.range(0,512).toArray()));
        assertEquals(512,review(10,10).candidates().size());assertEquals(MISSING,opportunities().review(UUID.randomUUID(),10,10,Set.of(world)).status());
    }
    @Test void upgradeDoesNotInventCurrentEvidenceAndFailureRestoresPreviousSchema()throws Exception {
        opportunities().observe(town,pair(10,0));SchemaFixtures.rewind(db.getConnection(),49);
        sql("CREATE TABLE mineral_survey_members(dummy INTEGER)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(49,r.getInt(1));}
        sql("DROP TABLE mineral_survey_members");new SchemaMigrator().migrate(db.getConnection());new SchemaMigrator().migrate(db.getConnection());
        assertEquals(MISSING,review(10,10).status());assertEquals(1,opportunities().read(town,512).size());
        opportunities().observe(town,pair(20,0));assertEquals(CURRENT,review(20,10).status());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
