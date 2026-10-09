package com.blueninjas24.civilizations.database;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OreTrackingRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;OreTrackingRepository tracking;
    void open() throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("TrackingTest"));db.initialize();
        tracking=new OreTrackingRepository(db.getConnection());
    }
    @BeforeEach void setup() throws Exception {open();}
    @AfterEach void close(){db.close();}
    void sql(String text) throws Exception {try(var statement=db.getConnection().createStatement()){statement.execute(text);}}
    @Test void dirtyFencePrecedesCaptureAndCleanRestartsRemainTrusted() throws Exception {
        assertTrue(tracking.load().isEmpty());assertTrue(tracking.begin(10).trusted());
        assertFalse(tracking.load().orElseThrow().cleanStop());
        assertTrue(tracking.finish(20,true).cleanStop());db.close();open();
        assertTrue(tracking.begin(30).trusted());assertTrue(tracking.finish(40,true).trusted());
    }
    @Test void uncleanStopPersistsGapAndHealthyRestartCannotSilentlyRepairIt() throws Exception {
        tracking.begin(10);db.close();open();
        assertFalse(tracking.begin(20).trusted());assertFalse(tracking.finish(30,true).trusted());
        db.close();open();assertFalse(tracking.begin(40).trusted());
    }
    @Test void failedCaptureRemainsUntrustedAfterCleanStopAndRestart() throws Exception {
        tracking.begin(10);assertFalse(tracking.finish(20,false).trusted());
        db.close();open();assertFalse(tracking.begin(30).trusted());
    }
    @Test void writeFailureRollsBackFenceAndExplicitRetrySucceeds() throws Exception {
        sql("CREATE TRIGGER fail_fence BEFORE INSERT ON ore_tracking BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->tracking.begin(10));assertTrue(tracking.load().isEmpty());
        assertTrue(db.getConnection().getAutoCommit());sql("DROP TRIGGER fail_fence");assertTrue(tracking.begin(20).trusted());
        assertThrows(IllegalArgumentException.class,()->tracking.finish(-1,true));
        db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->tracking.finish(30,true));
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);assertFalse(tracking.load().orElseThrow().cleanStop());
    }
    @Test void schema33UpgradeFailurePreservesExclusionsAndThenRecovers() throws Exception {
        var world=java.util.UUID.randomUUID();db.getVillageIdentityRepository().observeWorld(world,"Fixture");
        var point=new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,40,0);
        new OrePlacementRepository(db.getConnection()).record(world,java.util.List.of(point),10);
        SchemaFixtures.rewind(db.getConnection(),33);sql("CREATE TABLE ore_tracking(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(33,r.getInt(1));}
        sql("DROP TABLE ore_tracking");new SchemaMigrator().migrate(db.getConnection());
        assertTrue(tracking.load().isEmpty());assertTrue(new OrePlacementRepository(db.getConnection()).excludes(world,point));
    }
}
