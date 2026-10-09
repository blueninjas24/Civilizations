package com.blueninjas24.civilizations.database;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.population.Resident;
import com.blueninjas24.civilizations.settlement.Settlement;
import static org.junit.jupiter.api.Assertions.*;

class TreasuryTransactionTest {
    @TempDir Path directory;
    private DatabaseManager db;private TreasuryRepository treasury;private final UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    @BeforeEach void open() throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("TreasuryTransactionTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","world",0,64,0,1,0,0));treasury=new TreasuryRepository(db.getConnection());
    }
    @AfterEach void close(){db.close();}
    private void sql(String text) throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(text);}}
    @Test void rowOperationsRequireAnExplicitOwnerTransactionAndStandaloneCallsStillRejectNesting() throws SQLException {
        assertThrows(SQLException.class,()->treasury.initializeRows(UUID.randomUUID(),town,0,"Fixture"));
        assertThrows(SQLException.class,()->treasury.postRows(UUID.randomUUID(),town,1,"Fixture"));
        db.getConnection().setAutoCommit(false);var id=UUID.randomUUID();treasury.initializeRows(id,town,100,"Fixture");
        assertThrows(SQLException.class,()->treasury.post(UUID.randomUUID(),town,-1,"Nested standalone"));
        assertFalse(db.getConnection().getAutoCommit());assertTrue(treasury.entry(id).isPresent());
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);assertTrue(treasury.load(town).isEmpty());
    }
    @Test void openingCreditAndDebitShareCallerRollbackInsteadOfCommittingSeparately() throws SQLException {
        var opening=UUID.randomUUID();var credit=UUID.randomUUID();var debit=UUID.randomUUID();
        db.getConnection().setAutoCommit(false);treasury.initializeRows(opening,town,0,"Fixture opening");
        treasury.postRows(credit,town,100,"Fixture funds");treasury.postRows(debit,town,-40,"Fixture cost");
        assertEquals(60,treasury.load(town).orElseThrow().hundredths());assertFalse(db.getConnection().getAutoCommit());
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertTrue(treasury.load(town).isEmpty());for(var id:List.of(opening,credit,debit))assertTrue(treasury.entry(id).isEmpty());
    }
    @Test void callerCommitSurvivesRestartAndDuplicateRowsDoNotSpendTwice() throws SQLException {
        treasury.initialize(UUID.randomUUID(),town,100,"Fixture funds");var id=UUID.randomUUID();
        db.getConnection().setAutoCommit(false);var payment=treasury.postRows(id,town,-40,"Fixture cost");
        assertEquals(payment,treasury.postRows(id,town,-40,"Fixture cost"));
        db.getConnection().commit();db.getConnection().setAutoCommit(true);
        db.close();db=new DatabaseManager(directory.toFile(),Logger.getLogger("TreasuryTransactionTest"));db.initialize();treasury=new TreasuryRepository(db.getConnection());
        assertEquals(60,treasury.load(town).orElseThrow().hundredths());assertEquals(payment,treasury.entry(id).orElseThrow());
    }
    @Test void insufficientFundsAndConflictingRetriesStayInsideCallerTransaction() throws SQLException {
        treasury.initialize(UUID.randomUUID(),town,100,"Fixture funds");var id=UUID.randomUUID();
        db.getConnection().setAutoCommit(false);treasury.postRows(id,town,-40,"Fixture cost");
        assertThrows(SQLException.class,()->treasury.postRows(id,town,-41,"Fixture cost"));
        assertThrows(SQLException.class,()->treasury.postRows(UUID.randomUUID(),town,-61,"Debt"));
        assertFalse(db.getConnection().getAutoCommit());db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertEquals(100,treasury.load(town).orElseThrow().hundredths());assertTrue(treasury.entry(id).isEmpty());
    }
    @Test void failedFoodReceiptRollsBackSpendingAndSameOperationRetriesWithBothLedgers() throws SQLException {
        var identities=db.getVillageIdentityRepository();identities.observeWorld(world,"world");identities.bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",town,null,300,Resident.Status.ALIVE));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.TWO),bands);
        var runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.observe(new FarmObservation(town,world,1000,true,0),1000);treasury.initialize(UUID.randomUUID(),town,100,"Fixture funds");
        var id=UUID.randomUUID();var deliveries=new FoodDeliveryRepository(db.getConnection());
        sql("CREATE TRIGGER reject_receipt BEFORE INSERT ON food_deliveries BEGIN SELECT RAISE(ABORT,'injected'); END");
        db.getConnection().setAutoCommit(false);treasury.postRows(id,town,-40,"Fixture verified goods cost");
        assertThrows(SQLException.class,()->deliveries.record(id,town,20,"Fixture verified goods",0));
        db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertEquals(100,treasury.load(town).orElseThrow().hundredths());assertTrue(treasury.entry(id).isEmpty());assertTrue(deliveries.find(id).isEmpty());
        sql("DROP TRIGGER reject_receipt");db.getConnection().setAutoCommit(false);
        treasury.postRows(id,town,-40,"Fixture verified goods cost");deliveries.record(id,town,20,"Fixture verified goods",0);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);
        runtime.pulse(2000,false);assertEquals(30,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(60,treasury.load(town).orElseThrow().hundredths());
        db.getConnection().setAutoCommit(false);treasury.postRows(id,town,-40,"Fixture verified goods cost");db.getConnection().commit();db.getConnection().setAutoCommit(true);
        assertEquals(60,treasury.load(town).orElseThrow().hundredths());
    }
}
