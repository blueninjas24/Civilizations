package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OrePlacementRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;
    OrePlacementRepository placements;
    final UUID world = UUID.randomUUID();
    void open() throws Exception {
        db = new DatabaseManager(directory.toFile(),Logger.getLogger("OrePlacementTest")); db.initialize();
        placements = new OrePlacementRepository(db.getConnection());
    }
    @BeforeEach void setup() throws Exception { open(); db.getVillageIdentityRepository().observeWorld(world,"Test"); }
    @AfterEach void close() { db.close(); }
    void sql(String text) throws Exception { try (var s = db.getConnection().createStatement()) { s.execute(text); } }

    @Test void repeatedPlacementsStayExcludedAfterRestartRenameAndWorldAbsence() throws Exception {
        var position = new Position(-1,-64,15);
        assertEquals(1,placements.record(world,List.of(position),10));
        assertEquals(0,placements.record(world,List.of(position),20));
        db.close(); open(); db.getVillageIdentityRepository().observeWorld(world,"Renamed");
        assertTrue(placements.excludes(world,position));
        assertFalse(placements.excludes(UUID.randomUUID(),position));
        try (var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT observed_millis FROM player_ore_placements")) {
            assertEquals(10,r.getLong(1));
        }
    }

    @Test void negativeChunkReadsAreBoundedImmutableAndWorldSpecific() throws Exception {
        var a=new Position(-16,30,-16); var b=new Position(-1,-64,-1); var outside=new Position(0,40,0);
        assertEquals(3,placements.record(world,List.of(a,b,outside),10));
        var result=placements.inChunk(world,-1,-1,2); assertEquals(Set.of(a,b),result);
        assertThrows(UnsupportedOperationException.class,result::clear);
        assertTrue(placements.inChunk(UUID.randomUUID(),-1,-1,2).isEmpty());
        assertThrows(SQLException.class,()->placements.inChunk(world,-1,-1,1));
        assertThrows(IllegalArgumentException.class,()->placements.inChunk(world,0,0,0));
        assertThrows(ArithmeticException.class,()->placements.inChunk(world,Integer.MAX_VALUE,0,1));
    }

    @Test void failedBatchRollsBackEveryCoordinateThenRetries() throws Exception {
        var a=new Position(1,40,0); var b=new Position(2,40,0);
        sql("CREATE TRIGGER fail_placement BEFORE INSERT ON player_ore_placements WHEN NEW.x=2 BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->placements.record(world,List.of(a,b),10));
        assertFalse(placements.excludes(world,a)); assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER fail_placement"); assertEquals(2,placements.record(world,List.of(a,b),10));
    }

    @Test void invalidUnknownWorldAndNestedWritesCannotCreateExclusions() throws Exception {
        var position=new Position(0,40,0);
        assertThrows(IllegalArgumentException.class,()->placements.record(world,List.of(),10));
        assertThrows(IllegalArgumentException.class,()->placements.record(world,List.of(position,position),10));
        assertThrows(IllegalArgumentException.class,()->placements.record(world,List.of(position),-1));
        assertThrows(IllegalArgumentException.class,()->placements.record(world,
                java.util.stream.IntStream.range(0,513).mapToObj(x->new Position(x,40,0)).toList(),10));
        assertThrows(SQLException.class,()->placements.record(UUID.randomUUID(),List.of(position),10));
        db.getConnection().setAutoCommit(false);
        assertThrows(SQLException.class,()->placements.record(world,List.of(position),10));
        db.getConnection().rollback(); db.getConnection().setAutoCommit(true);
        assertFalse(placements.excludes(world,position));
    }

    @Test void schema32FailurePreservesVersionAndWorldThenRecoversWithoutInventedPlacements() throws Exception {
        SchemaFixtures.rewind(db.getConnection(),32); sql("CREATE TABLE player_ore_placements(bad TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try (var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")) { assertEquals(32,r.getInt(1)); }
        sql("DROP TABLE player_ore_placements"); new SchemaMigrator().migrate(db.getConnection());
        assertTrue(placements.inChunk(world,0,0,512).isEmpty());
        assertEquals(1,placements.record(world,List.of(new Position(0,40,0)),10));
    }
}
