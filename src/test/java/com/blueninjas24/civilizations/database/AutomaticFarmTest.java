package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticFarmTest {
    @TempDir Path directory;
    private DatabaseManager db;
    private final UUID town=new UUID(0,100),world=new UUID(0,200);
    private EconomySettings settings;
    private EconomyRuntimeRepository runtime;
    private final FarmBusinessPolicy policy=new FarmBusinessPolicy(true,4,2500,216,0);
    @BeforeEach void setup() throws Exception {
        open();db.saveSettlement(new Settlement(town,"Farm","world",0,64,0,1,0,0));
        var identity=db.getVillageIdentityRepository();identity.observeWorld(world,"world");identity.bindSettlement(town,world);
        for(int n=1;n<=3;n++)person(new UUID(0,n),300);
        person(new UUID(0,4),10);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test-owned starting capital");
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        runtime=create(policy);
    }
    private void open() throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticFarmTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    private void person(UUID id,long age) throws Exception{db.getPopulationRepository().insertResident(new Resident(id,"Person",town,null,age,Resident.Status.ALIVE));}
    private EconomyRuntimeRepository create(FarmBusinessPolicy p) throws Exception {
        var value=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);value.configureFarmBusinesses(p);return value;
    }
    private void observe(long now,boolean complete,long crops,boolean site) throws Exception {
        runtime.observe(new FarmObservation(town,world,now,complete,crops,site?new VillageObservation.Position(3,64,5):null),now);
    }
    private long funds() throws Exception{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    private long output() throws Exception{return runtime.views().get(town).latest().reports().getFirst().flow().production();}
    @Test void reliableFieldsFoundAndHireOnceAndReplaceRatherThanDoubleFoodOutput() throws Exception {
        observe(1000,true,32,true);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);
        var business=FarmEnterprises.businessId(town);var saved=db.getBusinessRepository().findFounding(business).orElseThrow();
        assertEquals(new UUID(0,1),saved.business().founderId());assertEquals(2,db.getBusinessRepository().currentWorkers(business).size());
        assertEquals(7500,funds());assertEquals(8,output());assertEquals(7,runtime.views().get(town).latest().reports().getFirst().flow().demand());
        assertEquals(new VillageObservation.Position(3,64,5),runtime.views().get(town).farm().representativeCrop());
        runtime.pulse(2000,false);assertEquals(7500,funds());assertEquals(2,db.getBusinessRepository().employmentHistory(business).size());
        runtime.pulse(3000,false);assertEquals(8,output());assertEquals(7500,funds());
    }
    @Test void approvedStaffedFarmContinuesAfterDeathWithoutHiringAndCurrentOwnerCanResumeHiring()throws Exception {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(policy,BusinessOperationPolicy.prototype());observe(1000,true,32,true);runtime.pulse(2000,false);
        var id=FarmEnterprises.businessId(town);var original=db.getBusinessRepository().findFounding(id).orElseThrow();
        db.getPopulationRepository().updateResident(new Resident(new UUID(0,1),"Person",town,null,300,Resident.Status.DECEASED));
        person(new UUID(0,5),300);observe(2001,true,48,true);runtime.pulse(3001,false);
        assertEquals(4,output());assertEquals(1,db.getBusinessRepository().currentWorkers(id).size());assertEquals(7500,funds());
        assertTrue(runtime.views().get(town).farmBusiness().contains("existing workers"));
        new BusinessSuccession(db.getConnection()).succeed(UUID.randomUUID(),id,0).orElseThrow();
        runtime.pulse(4001,false);assertEquals(12,output());assertEquals(3,db.getBusinessRepository().currentWorkers(id).size());
        assertEquals(original,db.getBusinessRepository().findFounding(id).orElseThrow());assertEquals(7500,funds());
    }
    @Test void configurablePauseStopsFarmProductionAfterDeathWithoutDiscardingJobs()throws Exception {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(policy,new BusinessOperationPolicy(true,false));observe(1000,true,32,true);runtime.pulse(2000,false);
        db.getPopulationRepository().updateResident(new Resident(new UUID(0,1),"Person",town,null,300,Resident.Status.DECEASED));
        runtime.pulse(3000,false);assertEquals(0,output());assertEquals(1,db.getBusinessRepository().currentWorkers(FarmEnterprises.businessId(town)).size());
        assertEquals(2,db.getBusinessRepository().employmentHistory(FarmEnterprises.businessId(town)).size());assertEquals(7500,funds());
    }
    @Test void unknownPartialOrSmallFieldsCannotCreateFarmOrSpendFunds() throws Exception {
        observe(1000,true,32,false);runtime.pulse(2000,false);assertEquals(0,output());
        assertTrue(runtime.views().get(town).farmBusiness().contains("crop position required"));assertEquals(10000,funds());
        observe(2001,false,32,true);runtime.pulse(3001,false);assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());
        observe(3002,true,8,true);runtime.pulse(4002,false);assertEquals(0,output());assertEquals(10000,funds());
    }
    @Test void absentWorldDoesNotFoundSpendHireOrProduceAndReturnsWithoutCatchUp() throws Exception {
        observe(1000,true,32,true);runtime.startOnline(1000,Set.of());runtime.pulse(1_000_000,false);
        assertEquals(10000,funds());assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());
        runtime.updateLoadedWorlds(1_000_000,Set.of(world));runtime.pulse(1_001_000,false);
        assertEquals(8,output());assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
    }
    @Test void capitalAndQualificationBlockImpossibleFoundingWithReasons() throws Exception {
        runtime=create(new FarmBusinessPolicy(true,4,20000,216,0));observe(1000,true,32,true);runtime.pulse(2000,false);
        assertEquals(0,output());assertTrue(runtime.views().get(town).farmBusiness().contains("startup funds"));assertEquals(10000,funds());
        runtime=create(new FarmBusinessPolicy(true,4,0,216,10));runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);
        assertEquals(0,output());assertTrue(runtime.views().get(town).farmBusiness().contains("qualified adult founder"));
    }
    @Test void failedStartupDebitRollsBackAndRetryDoesNotDoubleChargeOrHire() throws Exception {
        observe(1000,true,32,true);
        try(var s=db.getConnection().createStatement()){s.execute("CREATE TRIGGER fail_farm BEFORE INSERT ON treasury_entries WHEN NEW.kind='DEBIT' BEGIN SELECT RAISE(ABORT,'injected'); END");}
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(10000,funds());assertTrue(db.getBusinessRepository().findBusinesses(town).isEmpty());
        try(var s=db.getConnection().createStatement()){s.execute("DROP TRIGGER fail_farm");}
        runtime.pulse(2000,false);assertEquals(7500,funds());assertEquals(8,output());
    }
    @Test void restartPreservesFarmJobsFieldsFundsAndDecreasedFieldsLimitOutput() throws Exception {
        observe(1000,true,32,true);runtime.pulse(2000,false);var saved=db.getBusinessRepository().findFounding(FarmEnterprises.businessId(town));
        db.close();open();runtime=create(policy);runtime.startOnline(1_000_000,Set.of(world));
        assertEquals(saved,db.getBusinessRepository().findFounding(FarmEnterprises.businessId(town)));assertEquals(7500,funds());
        observe(1_000_001,true,16,true);runtime.pulse(1_001_001,false);assertEquals(4,output());
        observe(1_001_002,true,0,false);runtime.pulse(1_002_002,false);assertEquals(0,output());assertEquals(7500,funds());
    }
    @Test void foodWriteFailureRetainsFoundedBusinessButRetryAppliesOneFoodPeriod() throws Exception {
        observe(1000,true,32,true);
        try(var s=db.getConnection().createStatement()){s.execute("CREATE TRIGGER fail_food BEFORE INSERT ON economy_periods BEGIN SELECT RAISE(ABORT,'injected'); END");}
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(7500,funds());assertEquals(0,runtime.views().get(town).account().state().completedPeriods());
        try(var s=db.getConnection().createStatement()){s.execute("DROP TRIGGER fail_food");}
        runtime.pulse(2000,false);assertEquals(8,output());assertEquals(1,runtime.views().get(town).account().state().completedPeriods());assertEquals(7500,funds());
    }
    @Test void workersWithOtherJobsAreNotCountedAndDeathAllowsAQualifiedReplacement() throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,20,64,20,1000,"Explicit test workshop");
        var business=new Business(UUID.randomUUID(),town,"Workshop",new UUID(0,1),new UUID(0,1),"carpentry",site.id(),1,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Test existing job"));
        db.getBusinessRepository().hire(UUID.randomUUID(),business.id(),new UUID(0,1),0);
        observe(1000,true,48,true);runtime.pulse(2000,false);assertEquals(8,output());
        assertEquals(2,db.getBusinessRepository().currentWorkers(FarmEnterprises.businessId(town)).size());
        db.getPopulationRepository().updateResident(new Resident(new UUID(0,2),"Person",town,null,300,Resident.Status.DECEASED));
        person(new UUID(0,5),300);runtime.pulse(3000,false);assertEquals(8,output());
        assertEquals(3,db.getBusinessRepository().employmentHistory(FarmEnterprises.businessId(town)).size());assertEquals(7500,funds());
    }
    @Test void longRunFieldsConserveFoodAndNoRepeatedStartupSpending() throws Exception {
        observe(1000,true,32,true);
        for(int period=1;period<=240;period++)runtime.pulse(1000L+period*1000,false);
        assertEquals(240,runtime.views().get(town).account().state().completedPeriods());
        assertEquals(288,runtime.views().get(town).account().state().stocks().get(Commodity.FOOD).units());
        assertEquals(7500,funds());assertEquals(2,db.getBusinessRepository().employmentHistory(FarmEnterprises.businessId(town)).size());
    }
    @Test void schema29FailureRollsBackNewCoordinatesAndHistoryThenRetries() throws Exception {
        observe(1000,true,32,true);runtime.pulse(2000,false);SchemaFixtures.rewind(db.getConnection(),29);
        try(var s=db.getConnection().createStatement()){s.execute("ALTER TABLE farm_evidence ADD COLUMN site_y INTEGER");}
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(29,r.getInt(1));}
        try(var s=db.getConnection().createStatement()){s.execute("ALTER TABLE farm_evidence DROP COLUMN site_y");}
        new SchemaMigrator().migrate(db.getConnection());assertEquals(7500,funds());assertEquals(1,runtime.views().get(town).account().state().completedPeriods());
        assertNull(runtime.views().get(town).farm().representativeCrop());
    }
}
