package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.economy.Commodity.*;

class CommodityRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private final UUID town = UUID.randomUUID();
    private final Policy policy = new Policy(5000, 12500, 20000, 3);
    @BeforeEach void open() throws SQLException {
        db = new DatabaseManager(directory.toFile(), Logger.getLogger("CommodityTest")); db.initialize();
        db.saveSettlement(new Settlement(town, "Test town", "test", 0, 64, 0, 1, 2, 2));
    }
    @AfterEach void close() { db.close(); }
    private Map<com.blueninjas24.civilizations.economy.Commodity, Flow> flows() {
        return Map.of(FOOD, new Flow(2, 3, 10, 4, 5, 3, 2), TIMBER, new Flow(1, 0, 0, 0, 10, 0, 0));
    }
    private void initialize() throws SQLException { db.getCommodityRepository().initialize(town, Map.of(FOOD, 20L, TIMBER, 1L), policy); }
    private void sql(String sql) throws SQLException { try (var s = db.getConnection().createStatement()) { s.execute(sql); } }

    @Test void unknownInventoryIsNotZeroAndInitializationNeverReplacesExistingReserves() throws SQLException {
        var repo = db.getCommodityRepository(); assertTrue(repo.load(town).isEmpty()); initialize();
        assertThrows(SQLException.class, () -> repo.initialize(town, Map.of(FOOD, 999L), policy));
        assertEquals(new Stock(20, 0), repo.load(town).orElseThrow().state().stocks().get(FOOD));
        assertThrows(SQLException.class, () -> repo.initialize(UUID.randomUUID(), Map.of(FOOD, 1L), policy));
    }
    @Test void allFlowFieldsHistoryPolicyAndCheckpointSurviveRestart() throws SQLException {
        initialize(); var result = db.getCommodityRepository().advance(town, 1, flows(), policy);
        db.close(); db = new DatabaseManager(directory.toFile(), Logger.getLogger("CommodityTest")); db.initialize();
        var repo = db.getCommodityRepository(); assertEquals(result.state(), repo.load(town).orElseThrow().state());
        assertEquals(policy, repo.load(town).orElseThrow().policy()); assertEquals(result, repo.history(town, 1).orElseThrow());
        assertEquals(8, result.reports().stream().filter(r -> r.commodity().equals(TIMBER)).findFirst().orElseThrow().unmet());
        assertTrue(repo.history(town, 2).isEmpty());
    }
    @Test void duplicateSkippedAndChangedPolicyPeriodsCannotDoubleConsumeGoods() throws SQLException {
        initialize(); var repo = db.getCommodityRepository(); var saved = repo.advance(town, 1, flows(), policy);
        assertThrows(SQLException.class, () -> repo.advance(town, 1, flows(), policy));
        assertThrows(SQLException.class, () -> repo.advance(town, 3, flows(), policy));
        assertThrows(SQLException.class, () -> repo.advance(town, 2, flows(), new Policy(4000, 12500, 20000, 3)));
        assertEquals(saved.state(), repo.load(town).orElseThrow().state()); assertTrue(repo.history(town, 2).isEmpty());
    }
    @Test void finalCheckpointWriteFailureRollsBackAllStocksAndHistoryAndRetryWorks() throws SQLException {
        initialize(); var repo = db.getCommodityRepository(); var before = repo.load(town).orElseThrow();
        sql("CREATE TRIGGER fail_economy BEFORE UPDATE ON economy_accounts BEGIN SELECT RAISE(ABORT, 'injected failure'); END");
        assertThrows(SQLException.class, () -> repo.advance(town, 1, flows(), policy));
        assertTrue(db.getConnection().getAutoCommit()); assertEquals(before, repo.load(town).orElseThrow()); assertTrue(repo.history(town, 1).isEmpty());
        sql("DROP TRIGGER fail_economy"); assertEquals(1, repo.advance(town, 1, flows(), policy).state().completedPeriods());
    }
    @Test void failedInitializationRollsBackAccountAndPartialStocks() throws SQLException {
        sql("CREATE TRIGGER fail_stock BEFORE INSERT ON economy_stocks WHEN NEW.commodity = 'timber' BEGIN SELECT RAISE(ABORT, 'injected failure'); END");
        assertThrows(SQLException.class, this::initialize); assertTrue(db.getCommodityRepository().load(town).isEmpty());
        sql("DROP TRIGGER fail_stock"); initialize(); assertEquals(2, db.getCommodityRepository().load(town).orElseThrow().state().stocks().size());
    }
    @Test void invalidFlowsAndOverflowPreserveSavedStateAndExistingTransactions() throws SQLException {
        initialize(); var repo = db.getCommodityRepository(); var before = repo.load(town);
        assertThrows(IllegalArgumentException.class, () -> repo.advance(town, 1, Map.of(FOOD, Flow.idle()), policy));
        assertThrows(ArithmeticException.class, () -> repo.advance(town, 1, Map.of(FOOD, new Flow(Long.MAX_VALUE, 0, 0, 0, 0, 0, 0), TIMBER, Flow.idle()), policy));
        assertEquals(before, repo.load(town));
        db.getConnection().setAutoCommit(false);
        assertThrows(SQLException.class, () -> repo.load(town)); assertFalse(db.getConnection().getAutoCommit());
        db.getConnection().rollback(); db.getConnection().setAutoCommit(true);
    }
    @Test void schema13UpgradePreservesSettlementAndFailureRollsBackNewTables() throws SQLException {
        SchemaFixtures.rewind(db.getConnection(), 13);
        sql("CREATE TABLE economy_periods (collision INTEGER)");
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(db.getConnection()));
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA user_version")) { assertEquals(13, r.getInt(1)); }
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("SELECT count(*) FROM sqlite_master WHERE name = 'economy_accounts'")) { assertEquals(0, r.getInt(1)); }
        sql("DROP TABLE economy_periods"); new SchemaMigrator().migrate(db.getConnection());
        assertEquals(town, db.findNearbySettlement("test", 0, 0, 1).getId()); assertTrue(db.getCommodityRepository().load(town).isEmpty());
    }
    @Test void sqliteRejectsNegativeAndNonIntegerStocks() throws SQLException {
        initialize();
        assertThrows(SQLException.class, () -> sql("UPDATE economy_stocks SET units = -1"));
        assertThrows(SQLException.class, () -> sql("UPDATE economy_stocks SET units = 0.25"));
        assertEquals(20, db.getCommodityRepository().load(town).orElseThrow().state().stocks().get(FOOD).units());
    }
    @Test void currentVersionWithMissingEconomyColumnsFailsExplicitly() throws SQLException {
        sql("ALTER TABLE economy_periods DROP COLUMN imports");
        var failure = assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(db.getConnection()));
        assertTrue(failure.getMessage().contains("missing columns")); assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void centuryLedgerResumesMidCrisisAndReconstructsOriginalExplanations() throws SQLException {
        var repo = db.getCommodityRepository(); repo.initialize(town, Map.of(FOOD, 30L), policy);
        Result originalCrisis = null;
        for (int period = 1; period <= 1200; period++) {
            if (period == 110) {
                db.close(); db = new DatabaseManager(directory.toFile(), Logger.getLogger("CommodityTest")); db.initialize();
                repo = db.getCommodityRepository(); assertEquals(7, repo.load(town).orElseThrow().state().stocks().get(FOOD).shortagePeriods());
            }
            var flow = new Flow(period >= 100 && period <= 120 ? 0 : 10, period == 121 ? 30 : 0, 10, 0, 0, 0, 0);
            var result = repo.advance(town, period, Map.of(FOOD, flow), policy);
            if (period == 105) originalCrisis = result;
        }
        assertEquals(originalCrisis, repo.history(town, 105).orElseThrow());
        assertEquals(new Stock(30, 0), repo.load(town).orElseThrow().state().stocks().get(FOOD));
        assertEquals(1200, repo.load(town).orElseThrow().state().completedPeriods());
        try (var s = db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA foreign_key_check")) { assertFalse(r.next()); }
    }
}
