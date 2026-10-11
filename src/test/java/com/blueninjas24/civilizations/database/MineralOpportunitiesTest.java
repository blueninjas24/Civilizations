package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.MineralObservation;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MineralOpportunitiesTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    List<MineralObservation> pair(long time, int... xs) {
        return List.of(new MineralObservation(world,time,true,Commodity.COAL,Arrays.stream(xs)
                .mapToObj(x->new VillageObservation.Position(x,40,0)).toList()),
                new MineralObservation(world,time,true,Commodity.IRON,List.of()));
    }
    MineralOpportunities opportunities(){return new MineralOpportunities(db.getConnection());}
    @BeforeEach void open()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("OpportunityTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
    }
    @AfterEach void close(){db.close();}
    void sql(String sql)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    @Test void revisitRestartAndOlderEvidenceKeepCoordinatesAndMonotonicCaptureTimes()throws Exception {
        opportunities().observe(town,pair(10,1,0));opportunities().observe(town,pair(20,0,1));opportunities().observe(town,pair(5,0));
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("OpportunityTest"));db.initialize();
        var rows=opportunities().read(town,512);assertEquals(2,rows.size());assertEquals(0,rows.getFirst().position().x());
        assertTrue(rows.stream().allMatch(e->e.firstSeenMillis()==10&&e.lastSeenMillis()==20&&e.savedDeposit()==null&&!e.excluded()));
        assertThrows(UnsupportedOperationException.class,rows::clear);
        assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());assertTrue(new TreasuryRepository(db.getConnection()).load(town).isEmpty());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM mineral_deposits")){assertEquals(0,r.getInt(1));}
    }
    @Test void partialOrEmptySurveyCannotErasePositiveEvidenceOrMakeUnknownZero()throws Exception {
        opportunities().observe(town,pair(10,0,1));opportunities().observe(town,pair(20,0));opportunities().observe(town,pair(30));
        var rows=opportunities().read(town,512);assertEquals(20,rows.get(0).lastSeenMillis());assertEquals(10,rows.get(1).lastSeenMillis());
        var partial=new MineralObservation(world,40,false,Commodity.COAL,pair(10,0).getFirst().ore());
        assertThrows(SQLException.class,()->opportunities().observe(town,List.of(partial,pair(40).getLast())));assertEquals(rows,opportunities().read(town,512));
    }
    @Test void stickyRemovalAndPlacementRemainVisibleWithoutRefreshingExcludedEvidence()throws Exception {
        opportunities().observe(town,pair(10,0,1));var coordinates=pair(10,0,1).getFirst().ore();
        new OrePlacementRepository(db.getConnection()).record(world,List.of(coordinates.getFirst()),11);
        new MineralSources(db.getConnection()).remove(world,List.of(coordinates.getLast()),12,"Fixture break");
        assertTrue(opportunities().observe(town,pair(20,0,1)).isEmpty());
        var rows=opportunities().read(town,512);assertTrue(rows.stream().allMatch(MineralOpportunities.Evidence::excluded));
        assertTrue(rows.stream().allMatch(e->e.lastSeenMillis()==10));
    }
    @Test void conflictingMineralRollsBackEarlierCoordinatesInSameSurvey()throws Exception {
        opportunities().observe(town,pair(10,1));
        var iron=new MineralObservation(world,20,true,Commodity.IRON,pair(20,1).getFirst().ore());
        assertThrows(SQLException.class,()->opportunities().observe(town,List.of(pair(20,0).getFirst(),iron)));
        assertEquals(1,opportunities().read(town,512).size());assertEquals(10,opportunities().read(town,512).getFirst().lastSeenMillis());assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void failedWriteRollsBackEntireSurveyThenRetriesOnce()throws Exception {
        sql("CREATE TRIGGER reject_second BEFORE INSERT ON mineral_opportunities WHEN NEW.x=1 BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->opportunities().observe(town,pair(10,0,1)));assertTrue(opportunities().read(town,512).isEmpty());
        sql("DROP TRIGGER reject_second");opportunities().observe(town,pair(10,0,1));opportunities().observe(town,pair(10,0,1));assertEquals(2,opportunities().read(town,512).size());
    }
    @Test void wrongTownWorldAndCaptureGapDoNotPersistEvidence()throws Exception {
        assertThrows(SQLException.class,()->opportunities().observe(UUID.randomUUID(),pair(10,0)));
        var wrong=new MineralObservation(UUID.randomUUID(),10,true,Commodity.IRON,List.of());
        assertThrows(SQLException.class,()->opportunities().observe(town,List.of(pair(10,0).getFirst(),wrong)));
        var tracking=new OreTrackingRepository(db.getConnection());tracking.begin(10);tracking.markGap(11);
        assertThrows(SQLException.class,()->opportunities().observe(town,pair(20,0)));assertTrue(opportunities().read(town,512).isEmpty());
    }
    @Test void diagnosticPagesAreBoundedAndWorldsAndTownsRemainSeparate()throws Exception {
        opportunities().observe(town,pair(10,2,0,1));assertEquals(2,opportunities().read(town,2).size());
        assertTrue(opportunities().read(UUID.randomUUID(),512).isEmpty());
        assertThrows(IllegalArgumentException.class,()->opportunities().read(town,0));assertThrows(IllegalArgumentException.class,()->opportunities().read(town,513));
        assertThrows(SQLException.class,()->opportunities().observe(town,List.of(pair(10,0).getFirst(),pair(11).getLast())));
    }
    @Test void upgradeIsAdditiveAndFailureRetainsVersionThenRetries()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),47);sql("CREATE TABLE mineral_opportunities(dummy INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(47,r.getInt(1));}
        sql("DROP TABLE mineral_opportunities");new SchemaMigrator().migrate(db.getConnection());new SchemaMigrator().migrate(db.getConnection());
        assertTrue(opportunities().read(town,512).isEmpty());opportunities().observe(town,pair(10,0));assertEquals(1,opportunities().read(town,512).size());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
