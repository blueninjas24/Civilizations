package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.LivestockAdoption;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AnimalOwnershipRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID world = new UUID(0, 1), player = new UUID(0, 2), animal = new UUID(0, 3), town = new UUID(0, 4);
    @BeforeEach void open() throws Exception {
        db = new DatabaseManager(directory.toFile(), Logger.getLogger("AnimalOwnershipRepositoryTest")); db.initialize();
        db.getVillageIdentityRepository().observeWorld(world, "Ownership fixture");
    }
    @AfterEach void close() { db.close(); }
    AnimalOwnershipRepository repo() { return new AnimalOwnershipRepository(db.getConnection()); }
    AnimalOwnershipRepository.Claim playerClaim() {
        return new AnimalOwnershipRepository.Claim(animal, world, LivestockAdoption.Ownership.PLAYER, player, null, 1000, "Explicit fixture protection");
    }
    void town() throws Exception {
        db.saveSettlement(new Settlement(town, "Ownership fixture", "Ownership fixture", 0, 64, 0, 1, 0, 0));
        db.getVillageIdentityRepository().bindSettlement(town, world);
    }
    long scalar(String sql) throws Exception {
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery(sql)) { assertTrue(r.next()); return r.getLong(1); }
    }
    void sql(String sql) throws Exception { try (var s = db.getConnection().createStatement()) { s.execute(sql); } }
    @Test void firstPlayerOwnershipSurvivesRestartWithoutLocationOrSignEvidence() throws Exception {
        var claim = playerClaim(); assertTrue(repo().load(animal).isEmpty()); assertEquals(claim, repo().record(claim));
        db.close(); open(); assertEquals(claim, repo().load(animal).orElseThrow());
        assertEquals(1, scalar("SELECT COUNT(*) FROM animal_ownership"));
    }
    @Test void exactRetriesReplayButOtherPlayersAndVillageClaimsCannotOverwrite() throws Exception {
        var claim = playerClaim(); repo().record(claim); assertEquals(claim, repo().record(claim)); town();
        assertThrows(SQLException.class, () -> repo().record(new AnimalOwnershipRepository.Claim(animal, world,
                LivestockAdoption.Ownership.PLAYER, town, null, 1000, claim.source())));
        assertThrows(SQLException.class, () -> repo().record(new AnimalOwnershipRepository.Claim(animal, world,
                LivestockAdoption.Ownership.VILLAGE, null, town, 1000, "Fixture automatic adoption")));
        assertEquals(claim, repo().load(animal).orElseThrow()); assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void villageRegistrationRequiresRealTownInTheObservedOriginWorld() throws Exception {
        town(); var secondWorld = new UUID(0, 5); db.getVillageIdentityRepository().observeWorld(secondWorld, "Other fixture");
        assertThrows(SQLException.class, () -> repo().record(new AnimalOwnershipRepository.Claim(animal, secondWorld,
                LivestockAdoption.Ownership.VILLAGE, null, town, 1000, "Wrong world fixture")));
        assertTrue(repo().load(animal).isEmpty());
        var claim = new AnimalOwnershipRepository.Claim(animal, world, LivestockAdoption.Ownership.VILLAGE, null, town, 1000, "Approved fixture adoption");
        assertEquals(claim, repo().record(claim)); assertEquals(claim, repo().load(animal).orElseThrow());
    }
    @Test void unknownWorldAndLateInsertFailureLeaveNoPartialOwnership() throws Exception {
        assertThrows(SQLException.class, () -> repo().record(new AnimalOwnershipRepository.Claim(animal, new UUID(0, 99),
                LivestockAdoption.Ownership.PLAYER, player, null, 1000, "Unknown world fixture")));
        sql("CREATE TRIGGER reject_animal AFTER INSERT ON animal_ownership BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class, () -> repo().record(playerClaim()));
        assertTrue(repo().load(animal).isEmpty()); assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER reject_animal"); assertEquals(playerClaim(), repo().record(playerClaim()));
    }
    @Test void composedPenTransactionCanRollBackTheEntireOwnershipBatch() throws Exception {
        var c = db.getConnection(); c.setAutoCommit(false);
        try {
            assertThrows(SQLException.class, () -> repo().record(playerClaim())); assertFalse(c.getAutoCommit());
            repo().recordRows(playerClaim());
            assertThrows(SQLException.class, () -> repo().recordRows(new AnimalOwnershipRepository.Claim(animal, world,
                    LivestockAdoption.Ownership.PLAYER, town, null, 1000, "Conflicting fixture")));
            c.rollback();
        } finally { c.setAutoCommit(true); }
        assertTrue(repo().load(animal).isEmpty());
    }
    @Test void migrationFrom73IsAdditiveAtomicAndRetryable() throws Exception {
        town(); SchemaFixtures.rewind(db.getConnection(), 73);
        sql("CREATE VIEW animal_ownership AS SELECT 1 AS fixture");
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(73, scalar("PRAGMA user_version")); assertEquals(1, scalar("SELECT COUNT(*) FROM settlements"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM observed_worlds"));
        sql("DROP VIEW animal_ownership"); new SchemaMigrator().migrate(db.getConnection());
        assertEquals(74, scalar("PRAGMA user_version")); assertEquals(0, scalar("SELECT COUNT(*) FROM animal_ownership"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM settlements"));
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA foreign_key_check")) { assertFalse(r.next()); }
    }
    @Test void unclaimedOrAmbiguousOwnersCannotBeRecordedAsOwnership() {
        assertThrows(IllegalArgumentException.class, () -> new AnimalOwnershipRepository.Claim(animal, world,
                LivestockAdoption.Ownership.UNCLAIMED, null, null, 1000, "Unclaimed"));
        assertThrows(IllegalArgumentException.class, () -> new AnimalOwnershipRepository.Claim(animal, world,
                LivestockAdoption.Ownership.PLAYER, player, town, 1000, "Ambiguous"));
    }
}
