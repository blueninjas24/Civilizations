package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TreasuryRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private TreasuryRepository treasury;
    private final UUID town=UUID.randomUUID();
    @BeforeEach void open() throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("TreasuryTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Town","world",0,64,0,1,0,0));treasury=new TreasuryRepository(db.getConnection());
    }
    @AfterEach void close(){db.close();}
    private void sql(String text) throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void absentBudgetIsUnknownAndExplicitOpeningCreditsDebitsSurviveRestart() throws SQLException {
        assertTrue(treasury.load(town).isEmpty());
        treasury.initialize(UUID.randomUUID(),town,101,"Approved test capital");treasury.post(UUID.randomUUID(),town,250,"Test aid");
        var payment=treasury.post(UUID.randomUUID(),town,-125,"Test spending");assertEquals(226,payment.balanceAfter());
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("TreasuryTest"));db.initialize();treasury=new TreasuryRepository(db.getConnection());
        assertEquals(new BigDecimal("2.26"),treasury.load(town).orElseThrow().tradeValue());assertEquals(payment,treasury.entry(payment.operation()).orElseThrow());
    }
    @Test void retriesAreIdempotentAndConflictingReuseOrRepeatedSeedingFails() throws SQLException {
        var id=UUID.randomUUID(); var opening=treasury.initialize(id,town,100,"Test capital");
        assertEquals(opening,treasury.initialize(id,town,100,"Test capital"));
        var credit=UUID.randomUUID(); var receipt=treasury.post(credit,town,50,"Aid");treasury.post(UUID.randomUUID(),town,-20,"Cost");
        assertEquals(receipt,treasury.post(credit,town,50,"Aid"));assertEquals(130,treasury.load(town).orElseThrow().hundredths());
        assertThrows(SQLException.class,()->treasury.post(credit,town,51,"Aid"));
        assertThrows(SQLException.class,()->treasury.initialize(UUID.randomUUID(),town,900,"Reseed"));
    }
    @Test void insufficientFundsInvalidAmountsAndOverflowNeverChangeBudget() throws SQLException {
        assertThrows(SQLException.class,()->treasury.post(UUID.randomUUID(),town,1,"No account"));
        treasury.initialize(UUID.randomUUID(),town,Long.MAX_VALUE,"Boundary test");
        assertThrows(ArithmeticException.class,()->treasury.post(UUID.randomUUID(),town,1,"Overflow"));
        assertThrows(IllegalArgumentException.class,()->treasury.post(UUID.randomUUID(),town,Long.MIN_VALUE,"Invalid"));
        assertEquals(Long.MAX_VALUE,treasury.load(town).orElseThrow().hundredths());
        treasury.post(UUID.randomUUID(),town,-Long.MAX_VALUE,"Empty");
        assertThrows(SQLException.class,()->treasury.post(UUID.randomUUID(),town,-1,"Debt"));assertEquals(0,treasury.load(town).orElseThrow().hundredths());
    }
    @Test void ledgerWriteFailureRollsBackBalanceAndSameOperationCanRetry() throws SQLException {
        treasury.initialize(UUID.randomUUID(),town,100,"Capital");var id=UUID.randomUUID();
        sql("CREATE TRIGGER reject_ledger BEFORE INSERT ON treasury_entries BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->treasury.post(id,town,-40,"Cost"));assertEquals(100,treasury.load(town).orElseThrow().hundredths());assertTrue(treasury.entry(id).isEmpty());
        sql("DROP TRIGGER reject_ledger");assertEquals(60,treasury.post(id,town,-40,"Cost").balanceAfter());
    }
    @Test void schema15MigrationFailureRollsBackAndCanRetryWithoutResettingSettlement() throws SQLException {
        SchemaFixtures.rewind(db.getConnection(),15);sql("CREATE TABLE needs_periods(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(15,r.getInt(1));}
        sql("DROP TABLE needs_periods");new SchemaMigrator().migrate(db.getConnection());
        assertTrue(db.getPopulationRepository().findResidents(town).isEmpty());
        assertTrue(treasury.load(town).isEmpty());treasury.initialize(UUID.randomUUID(),town,0,"Approved zero budget");
        assertEquals(0,treasury.load(town).orElseThrow().hundredths());
    }
    @Test void failedOpeningDoesNotLeaveAccountAndUnknownSettlementCannotGainMoney() throws SQLException {
        sql("CREATE TRIGGER reject_ledger BEFORE INSERT ON treasury_entries BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->treasury.initialize(UUID.randomUUID(),town,100,"Capital"));assertTrue(treasury.load(town).isEmpty());
        sql("DROP TRIGGER reject_ledger");assertThrows(SQLException.class,()->treasury.initialize(UUID.randomUUID(),UUID.randomUUID(),100,"Unknown"));
        assertThrows(IllegalArgumentException.class,()->treasury.initialize(UUID.randomUUID(),town,0," "));
    }
}
