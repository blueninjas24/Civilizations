package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockCaptureTest {
    @TempDir Path directory;DatabaseManager db;
    @BeforeEach void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("Capture fixture"));db.initialize();}
    @AfterEach void close(){db.close();}
    LivestockCapture capture(){return new LivestockCapture(db.getConnection());}
    void sql(String text)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void newEmptyHistoryIsDirtyBeforeCaptureAndCleanRestartPreservesTrust()throws Exception {
        assertTrue(capture().load().isEmpty());assertTrue(capture().begin(1).trusted());assertFalse(capture().load().orElseThrow().cleanStop());assertTrue(capture().finish(2,true).cleanStop());
        db.close();open();assertTrue(capture().begin(3).trusted());assertTrue(capture().finish(4,true).trusted());
    }
    @Test void uncleanStopAndExplicitGapRemainUntrustedAcrossHealthyShutdowns()throws Exception {
        capture().begin(1);db.close();open();assertFalse(capture().begin(2).trusted());assertFalse(capture().finish(3,true).trusted());assertFalse(capture().begin(4).trusted());
        assertFalse(capture().markGap(5).trusted());assertFalse(capture().finish(6,true).trusted());
    }
    @Test void unknownLegacyHerdNeverAcquiresAssumedCaptureHistory()throws Exception {
        var f=new LivestockRepositoryTest();f.directory=directory.resolve("legacy");try{f.setup();f.openHerd();var capture=new LivestockCapture(f.db.getConnection());
            var before=f.repo().load(f.business.id()).orElseThrow();assertFalse(capture.begin(1).trusted());assertFalse(capture.finish(2,true).trusted());assertEquals(before,f.repo().load(f.business.id()).orElseThrow());
        }finally{f.close();}
    }
    @Test void failedFenceRollsBackAndRetryRetainsIdleOwnerAndTimestampValidation()throws Exception {
        sql("CREATE TRIGGER refuse_capture BEFORE INSERT ON livestock_capture BEGIN SELECT RAISE(ABORT,'fixture capture'); END");assertThrows(SQLException.class,()->capture().begin(1));assertTrue(capture().load().isEmpty());assertTrue(db.getConnection().getAutoCommit());sql("DROP TRIGGER refuse_capture");assertTrue(capture().begin(2).trusted());
        assertThrows(IllegalArgumentException.class,()->capture().finish(-1,true));db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->capture().finish(3,true));db.getConnection().rollback();db.getConnection().setAutoCommit(true);
    }
    @Test void schema82MigrationAddsOnlyEmptyCaptureStateAndPreservesPriorHerdLosses()throws Exception {
        var f=new LivestockLossesTest();f.directory=directory.resolve("migration");try{f.setup();f.losses().record(f.observed(10));SchemaFixtures.rewind(f.f.db.getConnection(),82);var before=f.snapshot();f.f.sql("CREATE VIEW livestock_capture AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.f.db.getConnection()));
            try(var s=f.f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(82,r.getInt(1));}
            f.f.sql("DROP VIEW livestock_capture");assertEquals(before,f.snapshot());new SchemaMigrator().migrate(f.f.db.getConnection());var after=f.snapshot();assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(),after.remove("livestock_births"));assertEquals(List.of(),after.remove("livestock_product_sales"));assertEquals(List.of(),after.remove("livestock_capture"));assertEquals(before,after);assertEquals(2,f.state().heads());
        }finally{f.close();}
    }
    @Test void ownerFailurePausesReadinessUntilExactRetryCommitsAndShutdownDrainsCleanly()throws Exception {
        var f=new LivestockLossesTest();f.directory=directory.resolve("owner");PopulationSession session=null;try {
            f.setup();f.f.sql("INSERT INTO livestock_capture VALUES(1,1,1,2)"); // Explicit fixture history: prior fully captured clean stop.
            f.f.sql("CREATE TRIGGER refuse_owner_loss BEFORE INSERT ON livestock_animal_losses BEGIN SELECT RAISE(ABORT,'fixture owner'); END");var url=f.f.db.getConnection().getMetaData().getURL();f.f.db.close();
            session=new PopulationSession(f.directory.toFile(),Logger.getLogger("Capture owner"),PopulationSession.Settings.prototype(),()->1000);session.start(false,Set.of(f.f.world)).join();assertTrue(session.livestockCaptureReady());
            var running=session;var observation=f.observed(10);assertThrows(java.util.concurrent.CompletionException.class,()->running.recordLivestockLoss(observation).join());assertFalse(running.livestockCaptureReady());assertTrue(running.livestockCaptureDiagnostic().contains("waiting"));
            try(var connection=DriverManager.getConnection(url);var statement=connection.createStatement()){statement.execute("DROP TRIGGER refuse_owner_loss");}
            running.recordLivestockLoss(observation).join();assertTrue(running.livestockCaptureReady());running.close();session=null;f.f.reopen();assertTrue(new LivestockCapture(f.f.db.getConnection()).load().orElseThrow().cleanStop());assertTrue(new LivestockCapture(f.f.db.getConnection()).load().orElseThrow().trusted());assertEquals(2,f.state().heads());
        }finally{if(session!=null)session.close();f.close();}
    }
    @Test void unresolvedOwnerWriteMakesEvenGracefulShutdownUntrusted()throws Exception {
        var f=new LivestockLossesTest();f.directory=directory.resolve("failed-close");PopulationSession session=null;try {
            f.setup();f.f.sql("INSERT INTO livestock_capture VALUES(1,1,1,2)");f.f.sql("CREATE TRIGGER refuse_shutdown_loss BEFORE INSERT ON livestock_animal_losses BEGIN SELECT RAISE(ABORT,'fixture unresolved'); END");f.f.db.close();
            session=new PopulationSession(f.directory.toFile(),Logger.getLogger("Capture shutdown"),PopulationSession.Settings.prototype(),()->1000);session.start(false,Set.of(f.f.world)).join();var running=session;assertThrows(java.util.concurrent.CompletionException.class,()->running.recordLivestockLoss(f.observed(10)).join());running.close();session=null;
            f.f.reopen();var state=new LivestockCapture(f.f.db.getConnection()).load().orElseThrow();assertTrue(state.cleanStop());assertFalse(state.trusted());assertFalse(new LivestockCapture(f.f.db.getConnection()).begin(2000).trusted());assertEquals(3,f.state().heads());
        }finally{if(session!=null)session.close();f.close();}
    }
    @Test void gapBeforeReadinessIsDurableAndCleanRestartCannotClearIt()throws Exception {
        db.close();var session=new PopulationSession(directory.toFile(),Logger.getLogger("Early gap"),PopulationSession.Settings.prototype(),()->1000);
        try{session.markLivestockCaptureGap();session.start(false,Set.of()).join();assertFalse(session.livestockCaptureReady());assertTrue(session.livestockCaptureDiagnostic().contains("review"));}finally{session.close();}
        open();var saved=capture().load().orElseThrow();assertTrue(saved.cleanStop());assertFalse(saved.trusted());assertFalse(capture().begin(2000).trusted());
    }
    @Test void allStagedLossesMustCommitBeforeReadinessCanResume()throws Exception {
        var f=new LivestockLossesTest();f.directory=directory.resolve("staged");PopulationSession session=null;try {
            f.setup();f.f.sql("INSERT INTO livestock_capture VALUES(1,1,1,2)");f.f.db.close();session=new PopulationSession(f.directory.toFile(),Logger.getLogger("Staged capture"),PopulationSession.Settings.prototype(),()->1000);session.start(false,Set.of(f.f.world)).join();assertTrue(session.livestockCaptureReady());
            session.stageLivestockLoss(f.observed(10));session.stageLivestockLoss(f.observed(11));assertFalse(session.livestockCaptureReady());session.recordLivestockLoss(f.observed(10)).join();assertFalse(session.livestockCaptureReady());session.recordLivestockLoss(f.observed(11)).join();assertTrue(session.livestockCaptureReady());
        }finally{if(session!=null)session.close();f.close();}
    }
}
