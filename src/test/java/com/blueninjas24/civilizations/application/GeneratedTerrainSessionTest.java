package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.database.GeneratedTerrainRepository;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedTerrainSessionTest {
    @TempDir Path directory;
    private final UUID world=new UUID(0,1);
    private PopulationSession session() {
        var session=new PopulationSession(directory.toFile(),Logger.getLogger("GeneratedTerrainSessionTest"),PopulationSession.Settings.prototype(),()->1000);
        session.configureOreTracking();session.configureForestTracking();return session;
    }
    private int save(PopulationSession session,UUID id,int x,int z)throws Exception {
        return session.recordGeneratedTerrain(id,"Generated fixture",x,z,1000).get(5,TimeUnit.SECONDS);
    }
    private boolean covered(String kind,int x,int z)throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"))) {
            return new GeneratedTerrainRepository(connection).covers(kind,world,List.of(new Position(x,64,z)));
        }
    }
    @Test void currentGenerationPersistsAcrossCleanRestartWithoutBackfillingOldChunks()throws Exception {
        try(var session=session()) {
            assertEquals(0,save(session,world,-1,-1));
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            assertEquals(2,save(session,world,-1,-1));
            assertEquals(2,save(session,world,-1,-1));
            assertEquals(0,save(session,new UUID(0,2),0,0));
        }
        assertTrue(covered("ore",-1,-1));assertTrue(covered("forest",-16,-16));
        assertFalse(covered("ore",0,0));
        try(var reopened=session()) {
            reopened.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            assertTrue(covered("ore",-1,-1));
            assertEquals(2,save(reopened,world,0,0));
        }
        try(var connection=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("civilizations.db"));
                var statement=connection.createStatement();var rows=statement.executeQuery("SELECT count(*),min(observed_ms),max(observed_ms) FROM generated_resource_chunks")) {
            assertTrue(rows.next());assertEquals(4,rows.getInt(1));assertEquals(1000,rows.getLong(2));assertEquals(1000,rows.getLong(3));
        }
    }
    @Test void pendingWritesAndGapsCannotCertifyGenerationOrRecertifyOldTerrain()throws Exception {
        try(var session=session()) {
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            assertEquals(2,save(session,world,-1,-1));
            try(var pending=session.beginExternalResourceMutation()) {
                assertEquals(0,save(session,world,0,0));pending.unchanged();
            }
            session.invalidateOreTracking("Fixture capture gap");
            assertEquals(1,save(session,world,1,1)); // Independent healthy forest capture can still record.
            session.invalidateForestTracking("Fixture capture gap");
            assertEquals(0,save(session,world,2,2));
        }
        assertFalse(covered("ore",-1,-1));assertFalse(covered("forest",-1,-1));
        try(var reopened=session()) {
            reopened.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            assertFalse(reopened.oreDiscoveryReady());assertFalse(reopened.forestDiscoveryReady());
            assertEquals(0,save(reopened,world,-1,-1));
            assertEquals(2,save(reopened,world,3,3));
            assertFalse(reopened.oreDiscoveryReady()); // Scoped evidence never resets global historical trust.
            assertTrue(reopened.canSurveyResourceChunk("ore",world,3,3,1000).get(5,TimeUnit.SECONDS));
            assertFalse(reopened.canSurveyResourceChunk("ore",world,3,3,999).get(5,TimeUnit.SECONDS));
            assertFalse(reopened.canSurveyResourceChunk("ore",world,2,2,1000).get(5,TimeUnit.SECONDS));
            assertTrue(reopened.canSurveyResourceChunk("forest",world,3,3,1000).get(5,TimeUnit.SECONDS));
            assertTrue(reopened.oreTrackingDiagnostic().contains("certified new terrain"));
            assertTrue(reopened.forestTrackingDiagnostic().contains("certified new terrain"));
        }
        assertFalse(covered("ore",-1,-1));assertTrue(covered("ore",48,48));
        assertTrue(covered("forest",63,63));assertFalse(covered("forest",32,32));
    }
}
