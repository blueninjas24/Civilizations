package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdWellbeingRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private EconomyRuntimeRepository runtime;
    private final UUID town=UUID.randomUUID(), world=UUID.randomUUID(), home=UUID.randomUUID();
    private HouseholdWellbeing.Policy policy=HouseholdWellbeing.Policy.prototype();
    @BeforeEach void setup() throws SQLException {
        open();db.saveSettlement(new Settlement(town,"Wellbeing fixture","world",0,64,0,1,0,2));
        db.getVillageIdentityRepository().observeWorld(world,"world");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertHousehold(new Household(home,town,"Fixture family"));
        for(int i=0;i<2;i++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture",town,home,300,Resident.Status.ALIVE));
        sql("INSERT INTO household_capacity VALUES ('"+home+"',2)");
        runtime.observe(new FarmObservation(town,world,1000,true,0),1000);
    }
    private void open() throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("WellbeingTest"));db.initialize();
        var prices=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var band:CommodityAccounting.Condition.values())prices.put(band,100);
        var settings=new EconomySettings(1,0,2,2,1,0,0,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,0L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),prices);
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype(),0,OpeningFunds.prototype(),policy);
    }
    @AfterEach void close(){db.close();}
    private void sql(String sql) throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    private HouseholdWellbeing.Snapshot current() throws SQLException {return runtime.views().get(town).wellbeing().getFirst();}
    @Test void foodAndWelfareSaveTogetherAndRestartNeverReinitializesTheScore() throws SQLException {
        runtime.pulse(2000,false);assertEquals(47,current().after());var saved=current();
        db.close();open();runtime.start(2000);assertEquals(saved,current());
        runtime.pulse(3000,false);assertEquals(47,current().before());assertEquals(44,current().after());
        runtime.pulse(4000,false);assertEquals(HouseholdWellbeing.Condition.FOOD_CRISIS,current().condition());assertEquals(39,current().after());
    }
    @Test void recoveryReflectsAllocatedSuppliesAndKeepsOldCrisisHistory() throws SQLException {
        for(int p=1;p<=3;p++)runtime.pulse(1000+p*1000L,false);var old=current();
        runtime.recordDeliveredFood(UUID.randomUUID(),town,4,"Fixture aid",4000);runtime.pulse(5000,false);
        assertEquals(HouseholdWellbeing.Condition.SECURE,current().condition());assertEquals(40,current().after());
        assertEquals(old,new HouseholdWellbeingRepository(db.getConnection()).load(town,3).getFirst());
        assertEquals(4800,runtime.views().get(town).treasury().hundredths());
    }
    @Test void failedWelfareSaveRollsBackFoodClockNeedsAndPendingDeliveryThenRetries() throws SQLException {
        var op=UUID.randomUUID();runtime.recordDeliveredFood(op,town,4,"Fixture aid",1000);
        sql("CREATE TRIGGER reject_welfare BEFORE INSERT ON household_wellbeing BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));var view=runtime.views().get(town);
        assertEquals(0,view.account().state().completedPeriods());assertNull(view.needs());assertTrue(view.wellbeing().isEmpty());
        assertNull(new FoodDeliveryRepository(db.getConnection()).find(op).orElseThrow().appliedPeriod());
        sql("DROP TRIGGER reject_welfare");runtime.pulse(2000,false);assertEquals(51,current().after());
        runtime.pulse(2000,false);assertEquals(51,current().after());
    }
    @Test void disabledAssessmentLeavesHistoryAndReenableContinuesRatherThanResetting() throws SQLException {
        runtime.pulse(2000,false);var old=current();db.close();policy=HouseholdWellbeing.Policy.disabled();open();runtime.start(2000);
        runtime.pulse(3000,false);assertTrue(runtime.views().get(town).wellbeing().isEmpty());
        assertEquals(old,new HouseholdWellbeingRepository(db.getConnection()).load(town,1).getFirst());
        db.close();policy=new HouseholdWellbeing.Policy(true,99,2,1,4,0);open();runtime.start(3000);runtime.pulse(4000,false);
        assertEquals(47,current().before());assertEquals(46,current().after());assertEquals(policy,current().policy());
        assertEquals(2,current().foodStreak()); // Disabled periods do not invent unrecorded personal history.
    }
    @Test void householdMoveCarriesItsScoreWithoutRewritingItsOldTownHistory() throws SQLException {
        runtime.pulse(2000,false);var old=current();var destination=UUID.randomUUID();
        db.saveSettlement(new Settlement(destination,"Destination","world",100,64,0,1,0,2));
        db.getVillageIdentityRepository().bindSettlement(destination,world);
        var nextHome=UUID.randomUUID();db.getPopulationRepository().insertHousehold(new Household(nextHome,destination,"Destination home"));
        sql("INSERT INTO household_capacity VALUES ('"+nextHome+"',2)");
        for(var person : db.getPopulationRepository().findResidents(town))
            db.getPopulationRepository().updateMigratedResident(new Resident(person.id(),person.name(),destination,nextHome,person.ageSteps(),person.status()),town);
        runtime.observe(new FarmObservation(destination,world,2000,true,0),2000);runtime.pulse(3000,false);
        var moved=runtime.views().get(destination).wellbeing().getFirst();assertEquals(47,moved.before());assertEquals(44,moved.after());
        assertEquals(old,new HouseholdWellbeingRepository(db.getConnection()).load(town,1).getFirst());
    }
    @Test void schema21UpgradePreservesNeedsAndBudgetWithoutInventingWelfareHistory() throws SQLException {
        runtime.pulse(2000,false);var needs=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),21);
        new SchemaMigrator().migrate(db.getConnection());assertTrue(runtime.views().get(town).wellbeing().isEmpty());
        assertEquals(needs,runtime.views().get(town).needs());assertEquals(4800,runtime.views().get(town).treasury().hundredths());
        runtime.pulse(3000,false);assertEquals(50,current().before());
    }
    @Test void newOccupantsDoNotInheritThePreviousFamiliesWelfareOrCrisis() throws SQLException {
        for(int p=1;p<=3;p++)runtime.pulse(1000+p*1000L,false);var former=current();
        sql("UPDATE residents SET household_id=NULL WHERE household_id='"+home+"'");
        for(int i=0;i<2;i++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New fixture",town,home,300,Resident.Status.ALIVE));
        runtime.pulse(5000,false);assertEquals(50,current().before());assertEquals(47,current().after());
        assertEquals(1,current().foodStreak());assertEquals(HouseholdWellbeing.Condition.PRESSURED,current().condition());
        assertEquals(former,new HouseholdWellbeingRepository(db.getConnection()).load(town,3).getFirst());
    }
    @Test void residentMembershipFailureRollsBackTheWholeEconomicPeriod() throws SQLException {
        sql("CREATE TRIGGER reject_member BEFORE INSERT ON wellbeing_members BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(0,runtime.views().get(town).account().state().completedPeriods());
        assertTrue(runtime.views().get(town).wellbeing().isEmpty());
        sql("DROP TRIGGER reject_member");runtime.pulse(2000,false);assertEquals(47,current().after());
    }
    @Test void extendedFaminePurchasesAndRepeatedRestartsConserveFundsFoodAndWelfare() throws SQLException {
        var purchases=FoodPurchases.Policy.prototype();var foods=FoodAidPreview.Policy.prototype();
        var random=new SplittableRandom(872613);int completed=0,cancelled=0;
        for(int period=1;period<=240;period++) {
            long now=1000+period*1000L;runtime.pulse(now,false);var view=runtime.views().get(town);
            assertTrue(current().after()>=0&&current().after()<=100);
            assertEquals(4800-completed*800,view.treasury().hundredths());
            if(view.needs().demand()>view.needs().fulfilled() && view.priority().selected()==VillagePriorities.Action.SEEK_FOOD
                    && view.treasury().hundredths()>=800) {
                var terms=new FoodPurchases.Terms(new UUID(random.nextLong(),random.nextLong()),new UUID(random.nextLong(),random.nextLong()),
                        town,4,"WHEAT",4,1,800);
                runtime.reserveFoodPurchase(terms,purchases,foods,now);
                if(random.nextInt(3)==0) {runtime.cancelFoodPurchase(terms);cancelled++;}
                else {assertTrue(runtime.completeFoodPurchase(terms,now));assertFalse(runtime.completeFoodPurchase(terms,now));completed++;}
            }
            if(period%40==0) {
                var prior=current();db.close();open();runtime.start(now);assertEquals(prior,current());
                assertEquals(4800-completed*800,runtime.views().get(town).treasury().hundredths());
            }
        }
        assertEquals(6,completed);assertTrue(cancelled>0);assertEquals(0,current().after());assertEquals(HouseholdWellbeing.Condition.FOOD_CRISIS,current().condition());
        assertEquals(0,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(2,db.getPopulationRepository().findResidents(town).size()); // Welfare does not silently kill residents.
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT sum(units),count(*) FROM food_deliveries WHERE applied_period IS NOT NULL")) {
            assertEquals(24,r.getLong(1));assertEquals(6,r.getLong(2));
        }
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void failedSchema22MigrationKeepsVersionAndAllPriorHistoryForRetry() throws SQLException {
        runtime.pulse(2000,false);var needs=runtime.views().get(town).needs();SchemaFixtures.rewind(db.getConnection(),21);
        sql("CREATE TABLE household_wellbeing(bad_column TEXT)");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(21,r.getInt(1));}
        assertEquals(needs,new HouseholdNeedsRepository(db.getConnection()).load(town,1).orElseThrow());
        sql("DROP TABLE household_wellbeing");new SchemaMigrator().migrate(db.getConnection());runtime.pulse(3000,false);assertEquals(47,current().after());
    }
}
