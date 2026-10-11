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

class FarmLearningTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository runtime;
    final UUID town=new UUID(0,100),world=new UUID(0,200),first=new UUID(0,1),second=new UUID(0,2),idle=new UUID(0,3);
    EconomySettings settings;
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Farm learning","World",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var person:List.of(first,second,idle))db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Real fixture capital");
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        create(WorkLearningPolicy.prototype(),true);observe(1000,32,true);runtime.startOnline(1000,Set.of(world));
    }
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("FarmLearningTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    void create(WorkLearningPolicy policy,boolean businesses)throws Exception {
        runtime=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        runtime.configureFarmBusinesses(new FarmBusinessPolicy(businesses,4,2500,216,0),BusinessOperationPolicy.prototype());runtime.configureWorkLearning(policy);
    }
    void observe(long now,long crops,boolean complete)throws Exception{runtime.observe(new FarmObservation(town,world,now,complete,crops,crops==0?null:new VillageObservation.Position(3,64,5)),now);}
    long xp(UUID resident)throws Exception{return db.getPopulationRepository().findWorkProfile(resident).orElseThrow().experience().getOrDefault("farming",0L);}
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    @Test void productiveEmployeesLearnOnceWithFoodAndKeepSkillsThroughAbsenceAndRestart()throws Exception {
        runtime.pulse(2000,false);assertEquals(1,xp(first));assertEquals(1,xp(second));assertEquals(0,xp(idle));assertEquals(2,count("farm_learning_entries"));
        assertEquals(8,runtime.views().get(town).latest().reports().getFirst().flow().production());
        assertTrue(runtime.views().get(town).farmBusiness().contains("employees learned farming"));runtime.pulse(2000,false);assertEquals(1,xp(first));
        runtime.updateLoadedWorlds(2000,Set.of());runtime.pulse(100000,false);assertEquals(1,xp(first));
        db.close();open();create(WorkLearningPolicy.prototype(),true);runtime.startOnline(1000000,Set.of(world));assertEquals(1,xp(first));
        runtime.pulse(1001000,false);assertEquals(2,xp(first));assertEquals(2,xp(second));assertEquals(4,count("farm_learning_entries"));
        assertEquals(7500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
    }
    @Test void shrinkingFieldsRotateOnlyActualProductiveSlotsWithoutFreeExperience()throws Exception {
        runtime.pulse(2000,false);observe(2000,16,true);runtime.pulse(3000,false);
        assertEquals(1,xp(first));assertEquals(2,xp(second));assertEquals(3,count("farm_learning_entries"));
        runtime.pulse(4000,false);assertEquals(2,xp(first));assertEquals(2,xp(second));
        observe(4000,0,true);runtime.pulse(5000,false);assertEquals(4,count("farm_learning_entries"));
        assertTrue(new FarmLearning(db.getConnection(),WorkLearningPolicy.prototype()).explanation(town,4).orElseThrow().contains("No actual farm production"));
    }
    @Test void missingDisabledMappingsAndLegacyAdultLaborNeverAwardFarmSkills()throws Exception {
        create(new WorkLearningPolicy(true,1,100,64,Map.of("mining","mining")),true);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);assertEquals(0,xp(first));
        create(WorkLearningPolicy.disabled(),true);runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);assertEquals(0,xp(first));
        // Legacy adult accounting runs in its own database, with no firm or worker awards.
        var legacyDb=new DatabaseManager(directory.resolve("legacy").toFile(),Logger.getLogger("LegacyLearningTest"));
        try {
            legacyDb.initialize();legacyDb.saveSettlement(new Settlement(town,"Legacy","World",0,64,0,1,0,0));
            legacyDb.getVillageIdentityRepository().observeWorld(world,"World");legacyDb.getVillageIdentityRepository().bindSettlement(town,world);
            legacyDb.getPopulationRepository().insertResident(new Resident(first,"Legacy",town,null,300,Resident.Status.ALIVE));
            var old=new EconomyRuntimeRepository(legacyDb.getConnection(),settings,FarmObservation.Policy.prototype(),216);old.configureWorkLearning(WorkLearningPolicy.prototype());
            old.observe(new FarmObservation(town,world,1000,true,32),1000);old.startOnline(1000,Set.of(world));old.pulse(2000,false);
            assertEquals(0,legacyDb.getPopulationRepository().findWorkProfile(first).orElseThrow().experience().getOrDefault("farming",0L));
            try(var q=legacyDb.getConnection().createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM farm_learning_entries")){assertEquals(0,r.getLong(1));}
        }finally{legacyDb.close();}
    }
    @Test void workerBudgetDefersAllAwardsAndRetunedCeilingsNeverRemoveSkills()throws Exception {
        create(new WorkLearningPolicy(true,1,100,1,Map.of("farming","farming")),true);runtime.startOnline(1000,Set.of(world));runtime.pulse(2000,false);assertEquals(0,count("farm_learning_entries"));
        assertTrue(new FarmLearning(db.getConnection(),WorkLearningPolicy.prototype()).explanation(town,1).orElseThrow().contains("budget exceeded"));
        db.getPopulationRepository().recordSkillExperience(first,"farming",200);
        create(new WorkLearningPolicy(true,10,2,64,Map.of("farming","farming")),true);runtime.startOnline(2000,Set.of(world));runtime.pulse(3000,false);
        assertEquals(200,xp(first));assertEquals(2,xp(second));assertEquals(1,count("farm_learning_entries"));
    }
    @Test void failedAwardAndLaterPeriodFailureRollBackFoodSkillsAndCheckpointThenRetry()throws Exception {
        sql("CREATE TRIGGER reject_learning BEFORE INSERT ON farm_learning_entries BEGIN SELECT RAISE(ABORT,'fixture award failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(0,xp(first));assertEquals(0,count("farm_learning_reviews"));assertEquals(0,count("economy_periods"));
        sql("DROP TRIGGER reject_learning");sql("CREATE TRIGGER reject_needs BEFORE INSERT ON village_priorities BEGIN SELECT RAISE(ABORT,'fixture later failure'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,false));assertEquals(0,xp(first));assertEquals(0,count("farm_learning_entries"));assertEquals(0,count("economy_periods"));
        sql("DROP TRIGGER reject_needs");runtime.pulse(2000,false);assertEquals(1,xp(first));assertEquals(2,count("farm_learning_entries"));
    }
    @Test void deceasedEmployeesAndClosedFarmCannotKeepLearningOrReturnAfterRestart()throws Exception {
        runtime.pulse(2000,false);var owner=db.getBusinessRepository().findBusiness(FarmEnterprises.businessId(town)).orElseThrow().ownerId();
        db.getPopulationRepository().updateResident(new Resident(owner,"Fixture",town,null,300,Resident.Status.DECEASED));
        runtime.pulse(3000,false);assertEquals(1,xp(owner));assertEquals(2,xp(second));assertEquals(0,xp(idle));
        // Qualified inheritance would take priority; raise founder requirements for the explicit closure fixture.
        sql("UPDATE business_skill_requirements SET experience=100 WHERE business_id='"+FarmEnterprises.businessId(town)+"' AND role='FOUNDER'");
        new BusinessSaleListings(db.getConnection()).open(FarmEnterprises.businessId(town),100,1000,3000,0);
        new BusinessSaleListings(db.getConnection()).observe(4000,Set.of(world));new BusinessClosure(db.getConnection()).closeIfExpired(FarmEnterprises.businessId(town),0);
        runtime.pulse(4000,false);assertEquals(2,xp(second));assertEquals(3,count("farm_learning_entries"));
    }
    @Test void incompleteEvidenceDoesNotProduceOrLearnAndConfigurationMustPrecedeUse()throws Exception {
        observe(1500,32,false);runtime.pulse(5000,false);assertEquals(0,count("farm_learning_entries"));assertEquals(0,count("economy_periods"));
        assertThrows(IllegalStateException.class,()->runtime.configureWorkLearning(WorkLearningPolicy.prototype()));
        assertThrows(SQLException.class,()->new FarmLearning(db.getConnection(),WorkLearningPolicy.prototype()).reviewRows(town,1,2));
    }
    @Test void actualFarmWorkCanQualifyAnEmployeeForSuccessionWithoutWaivingFounderRequirements()throws Exception {
        runtime.pulse(2000,false);
        sql("UPDATE business_skill_requirements SET experience=2 WHERE business_id='"+FarmEnterprises.businessId(town)+"' AND role='FOUNDER'");
        db.getPopulationRepository().updateResident(new Resident(first,"Fixture",town,null,300,Resident.Status.DECEASED));
        var succession=new BusinessSuccession(db.getConnection());assertTrue(succession.candidate(FarmEnterprises.businessId(town)).isEmpty());
        runtime.pulse(3000,false);assertEquals(2,xp(second));assertEquals(second,succession.candidate(FarmEnterprises.businessId(town)).orElseThrow().resident());
        new BusinessRuntime(db.getConnection(),8,1024).refresh(0,Set.of(world));
        assertEquals(second,db.getBusinessRepository().findBusiness(FarmEnterprises.businessId(town)).orElseThrow().ownerId());
        assertEquals(first,db.getBusinessRepository().findFounding(FarmEnterprises.businessId(town)).orElseThrow().business().founderId());
    }
    @Test void schema44FailureRollsBackAndSuccessfulMigrationKeepsFoodMoneyAndSkills()throws Exception {
        runtime.pulse(2000,false);SchemaFixtures.rewind(db.getConnection(),44);sql("CREATE TABLE farm_learning_entries(collision TEXT)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(1,xp(first));assertEquals(7500,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        sql("DROP TABLE farm_learning_entries");new SchemaMigrator().migrate(db.getConnection());runtime.pulse(3000,false);assertEquals(2,xp(first));assertEquals(2,count("farm_learning_entries"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
