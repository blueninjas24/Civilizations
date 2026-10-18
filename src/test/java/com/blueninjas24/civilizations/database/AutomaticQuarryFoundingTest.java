package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticQuarryFoundingTest {
    @TempDir Path directory;
    DatabaseManager db;
    UUID town=new UUID(1,1),world=new UUID(1,2),owner=new UUID(0,1),shopWorker=new UUID(0,2),warehouseWorker=new UUID(0,3),founder=new UUID(0,4),other=new UUID(0,5);
    UUID workshop=new UUID(2,1),warehouse=new UUID(2,2);
    QuarryFoundingPolicy founding=QuarryFoundingPolicy.prototype();
    boolean healthy=true;
    long capture=1999;
    EconomySettings economySettings;
    Commodity input;
    String industry;

    IndustryRuntimePolicy industryPolicy() {
        var recipe=new ProductionRecipe("prototype_cutting", "craftsman", Map.of(input,2L), Map.of(new Commodity("cut_material"),1L),1,4);
        var sizes=storageSizes(input);
        var defaults=IndustryPolicy.prototype();
        var mining=new IndustryPolicy(defaults.depositBatchesPerObservedOre(),defaults.batchesPerWorker(),defaults.siteBatchCapacity(),
                defaults.storageCapacity(),defaults.localDeliveryPeriods(),defaults.localHaulUnitsPerPeriod(),defaults.miningBundle(),sizes);
        return new IndustryRuntimePolicy(true,1000,8,mining,Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(input,100L)),new WarehouseSupplyPolicy(true,16,16));
    }
    AutomaticQuarryFounding automation() {
        return new AutomaticQuarryFounding(db.getConnection(),founding,QuarryPolicy.prototype(),industryPolicy(),
                BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),216,42,()->healthy);
    }
    String review()throws Exception{return automation().review(town,1,Math.max(2000,capture),Set.of(world));}

    @BeforeEach void setup()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticQuarryTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,40,0,5,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(owner,shopWorker,warehouseWorker,founder,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        for(var id:List.of(founder,other))db.getPopulationRepository().recordSkillExperience(id,"mining",10);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100000,"Explicit fixture funds");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var condition:Condition.values())bands.put(condition,100);
        economySettings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,100L,Commodity.STONE,20L,Commodity.TIMBER,20L),
                Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),economySettings,FarmObservation.Policy.prototype(),216,
                VillagePriorities.Policy.prototype(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype().seed());
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        new OreTrackingRepository(db.getConnection()).begin(1000);
    }
    void prepareResource(String mineral)throws Exception {
        input=new Commodity(mineral);industry=mineral.equals("stone")?"quarry":"sandpit";
        var sizes=storageSizes(input);
        business(workshop,"craftsman",0,20000,shopWorker,sizes);
        business(warehouse,"warehouse",1,10000,warehouseWorker,sizes);
        var blocks=List.of(new VillageObservation.Position(12,40,0),new VillageObservation.Position(13,40,0),new VillageObservation.Position(30,40,0));
        survey(mineral.equals("stone")?blocks:List.of(),mineral.equals("sand")?blocks:List.of());
    }
    Map<Commodity,Long> storageSizes(Commodity resource) {
        var sizes=new TreeMap<Commodity,Long>();sizes.put(resource,1L);sizes.put(new Commodity("cut_material"),1L);
        sizes.put(Commodity.COAL,1L);sizes.put(Commodity.STONE,1L);sizes.put(Commodity.SAND,1L);sizes.put(ToolUpkeepPolicy.TOOLS,1L);return sizes;
    }
    void business(UUID id,String kind,int x,long cash,UUID worker,Map<Commodity,Long> sizes)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,1,"Explicit fixture site");var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,kind,owner,owner,kind,site.id(),1,0),site,requirements,requirements,0,"Fixture"));
        db.getIndustryRepository().openStore(id,1000,sizes,cash);db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);
    }
    void survey(List<VillageObservation.Position> stone,List<VillageObservation.Position> sand)throws Exception {
        capture++;
        MineralOpportunities.quarries(db.getConnection()).observe(town,List.of(
                new MineralObservation(world,capture,true,Commodity.STONE,stone),new MineralObservation(world,capture,true,Commodity.SAND,sand)));
    }
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long scalar(String query)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    long cash()throws Exception{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long firms(String kind)throws Exception{return scalar("SELECT COUNT(*) FROM businesses WHERE industry='"+kind+"'");}
    UUID extraction(String kind)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT id FROM businesses WHERE industry='"+kind+"' ORDER BY id")){assertTrue(r.next());return UUID.fromString(r.getString(1));}}
    void unchanged()throws Exception{assertEquals(0,firms("quarry")+firms("sandpit"));assertEquals(70000,cash());assertEquals(0,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(0,scalar("SELECT COUNT(*) FROM mineral_deposits"));assertTrue(db.getConnection().getAutoCommit());}
    @AfterEach void close(){if(db!=null)db.close();}

    @ParameterizedTest @ValueSource(strings={"stone","sand"})
    void actualCustomerFundsOneFiniteEmptyFirmAndRetryCannotRefillOrGrantAgain(String mineral)throws Exception {
        prepareResource(mineral);String kind=mineral.equals("stone")?"quarry":"sandpit";
        String message=review();assertTrue(message.startsWith(mineral.equals("stone")?"Founded one stone quarry":"Founded one sand"),message);
        assertEquals(1,firms(kind));assertEquals(67500,cash());
        var saved=db.getBusinessRepository().findBusiness(extraction(kind)).orElseThrow();assertEquals(founder,saved.ownerId());
        assertEquals(founder,db.getBusinessRepository().currentWorkers(saved.id()).getFirst().residentId());
        var store=db.getIndustryRepository().store(saved.id()).orElseThrow();assertEquals(2000,store.hundredths());assertTrue(store.units().values().stream().allMatch(n->n==0));
        assertEquals(mineral,db.getIndustryRepository().deposit(scalarUuid("SELECT id FROM mineral_deposits")).orElseThrow().bundle().keySet().iterator().next().id());
        long remaining=scalar("SELECT SUM(remaining_batches) FROM mineral_deposits");assertTrue(remaining>0);
        assertEquals(500,scalar("SELECT startup_hundredths FROM startup_grants"));
        review();assertEquals(1,firms(kind));assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(67500,cash());
        assertEquals(remaining,scalar("SELECT SUM(remaining_batches) FROM mineral_deposits"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    UUID scalarUuid(String query)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return UUID.fromString(r.getString(1));}}

    @ParameterizedTest @ValueSource(strings={"stone","sand"})
    void noCustomerOrFundedStaffedWarehouseCannotClaimResources(String mineral)throws Exception {
        prepareResource(mineral);String kind=mineral.equals("stone")?"quarry":"sandpit";
        sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+workshop+"'");review();unchanged();
        sql("UPDATE industry_stores SET hundredths=20000 WHERE business_id='"+workshop+"'");
        sql("UPDATE industry_stores SET hundredths=99 WHERE business_id='"+warehouse+"'");review();unchanged();
        sql("UPDATE industry_stores SET hundredths=10000 WHERE business_id='"+warehouse+"'");
        db.getBusinessRepository().endEmployment(db.getBusinessRepository().currentWorkers(warehouse).getFirst().id(),1,"Fixture");review();unchanged();
        db.getPopulationRepository().recordSkillExperience(founder,"mining",9);db.getPopulationRepository().recordSkillExperience(other,"mining",9);review();unchanged();
        assertEquals(0,firms(kind));
    }

    com.blueninjas24.civilizations.application.PopulationSession ownerSession(java.util.concurrent.atomic.AtomicLong clock,StudyPolicy study) {
        var session=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),Logger.getLogger("AutomaticQuarryTest"),
                com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get,economySettings,FarmObservation.Policy.prototype());
        session.configureOreTracking();session.configureIndustryRuntime(industryPolicy());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        session.configureQuarries(QuarryPolicy.prototype());session.configureQuarryFounding(founding);
        session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
        session.configureCareerStudy(CareerStudyPolicy.prototype());session.configureStudy(study);return session;
    }
    void removeMiningSkills()throws Exception{sql("DELETE FROM resident_skills WHERE skill='mining'");}
    long xp(UUID person)throws Exception{return db.getPopulationRepository().findWorkProfile(person).orElseThrow().experience().getOrDefault("mining",0L);}
    long villageStock(String commodity)throws Exception{return scalar("SELECT units FROM economy_stocks WHERE commodity='"+commodity+"'");}

    @Test void sqlOwnerTrainsProspectiveQuarryFounderOnRealPracticeGoodsThenFundsFirm()throws Exception {
        prepareResource("stone");removeMiningSkills();new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
        var study=new StudyPolicy(true,1,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(10,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L))));
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=ownerSession(clock,study)) {
            session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
            for(int n=1;n<=10;n++){clock.set(2000+n*1000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);}
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticQuarryTest"));db.initialize();
        assertEquals(1,firms("quarry"));assertEquals(10,xp(founder));assertEquals(0,xp(other));
        assertEquals(1,scalar("SELECT COUNT(*) FROM quarry_founder_courses"));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'"));
        assertEquals(90,villageStock("stone"));assertEquals(90,villageStock("timber"));assertEquals(20,scalar("SELECT COUNT(*) FROM study_spending"));
        assertEquals(67500,cash());assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(founder,db.getBusinessRepository().findBusiness(extraction("quarry")).orElseThrow().ownerId());
    }

    @Test void vanishedQuarryEvidenceCancelsFounderCourseBeforeFreeProgressOrStartup()throws Exception {
        prepareResource("stone");removeMiningSkills();new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
        var study=new StudyPolicy(true,1,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(10,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L))));
        var clock=new java.util.concurrent.atomic.AtomicLong(2000);
        try(var session=ownerSession(clock,study)) {
            session.start(false,Set.of(world)).get(5,java.util.concurrent.TimeUnit.SECONDS);
            session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(3000);session.surveyQuarries(town,List.of(
                    new MineralObservation(world,3000,true,Commodity.STONE,List.of()),new MineralObservation(world,3000,true,Commodity.SAND,List.of())))
                    .get(5,java.util.concurrent.TimeUnit.SECONDS);
            clock.set(4000);session.pulse().get(5,java.util.concurrent.TimeUnit.SECONDS);
        }
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticQuarryTest"));db.initialize();
        assertEquals(0,firms("quarry"));assertEquals(0,xp(founder));assertEquals(0,scalar("SELECT COUNT(*) FROM study_spending"));
        assertEquals(1,scalar("SELECT COUNT(*) FROM quarry_founder_courses"));assertEquals(1,scalar("SELECT COUNT(*) FROM resident_courses WHERE status='CANCELLED'"));
        assertEquals(100,villageStock("stone"));assertEquals(100,villageStock("timber"));assertEquals(70000,cash());assertEquals(0,scalar("SELECT COUNT(*) FROM startup_grants"));
    }

    @ParameterizedTest @ValueSource(strings={"stone","sand"})
    void absentStaleFutureOrUntrustedCurrentSurveyCannotFound(String mineral)throws Exception {
        prepareResource(mineral);var adapter=automation();adapter.review(town,1,2000,Set.of());unchanged();
        adapter.review(town,1,62001,Set.of(world));unchanged();adapter.review(town,1,1999,Set.of(world));unchanged();
        healthy=false;assertTrue(review().toLowerCase(Locale.ROOT).contains("trust"));unchanged();healthy=true;
        new OreTrackingRepository(db.getConnection()).markGap(2000);review();unchanged();
    }

    @ParameterizedTest @ValueSource(strings={"stone","sand"})
    void grantReceiptFailureRollsBackFirmClaimDepositCashAndJobThenRetries(String mineral)throws Exception {
        prepareResource(mineral);String kind=mineral.equals("stone")?"quarry":"sandpit";
        sql("CREATE TRIGGER reject_quarry_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'injected receipt failure'); END");
        assertThrows(SQLException.class,this::review);unchanged();assertEquals(2,scalar("SELECT COUNT(*) FROM business_sites"));assertEquals(2,scalar("SELECT COUNT(*) FROM business_employment"));
        sql("DROP TRIGGER reject_quarry_grant");review();assertEquals(1,firms(kind));assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));
    }
}
