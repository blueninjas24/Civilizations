package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BusinessStartupGrantsTest {
    @TempDir Path directory;DatabaseManager db;EconomyRuntimeRepository economy;
    UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID();
    MiningStartupRepository.Request request;EconomySettings settings;
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("GrantTest"));db.initialize();}
    BusinessStartupGrants grants(){return new BusinessStartupGrants(db.getConnection());}
    BusinessStartupGrants.WorkshopRequest producer(String industry,UUID founder,int x,long capital) {
        var source=mine(founder,x).founding();var b=source.business();
        var business=new Business(b.id(),town,"Fixture producer",founder,founder,industry,b.siteId(),2,0);
        var founding=new BusinessFounding(business,source.site(),source.founderRequirements(),source.workerRequirements(),500,"Explicit tested producer opportunity");
        return new BusinessStartupGrants.WorkshopRequest(founding,100,Map.of(Commodity.TIMBER,1L),capital,founder);
    }
    @ParameterizedTest @ValueSource(strings={"logging","fishing","quarry","sandpit"})
    void broaderProducerSharesMinePeriodLifetimeAndRealCashAfterRestart(String industry)throws Exception {
        var other=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(other,"Other",town,null,300,Resident.Status.ALIVE));economy.pulse(3000,false);
        grants().startMine(request,StartupGrantPolicy.prototype());var producer=producer(industry,other,20,4000);
        assertTrue(assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWorkshop(producer,StartupGrantPolicy.prototype())).getMessage().contains("period"));
        assertEquals(8500,cash());assertEquals(1,count("startup_grants"));
        economy.pulse(4000,false);
        var limited=new StartupGrantPolicy(true,5000,5000,5000,2500,2,200,StartupGrantPolicy.prototype().industries());
        assertTrue(assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWorkshop(producer,limited)).getMessage().contains("lifetime"));
        var saved=grants().startWorkshop(producer,StartupGrantPolicy.prototype());
        assertEquals(4000,cash());assertEquals(4000,saved.store().hundredths());assertTrue(saved.store().units().values().stream().allMatch(n->n==0));
        assertEquals(2,count("startup_grants"));assertEquals(2,count("business_employment"));
        assertTrue(grants().preview(town,"mining",501,0,StartupGrantPolicy.prototype()).reason().contains("period"));
        assertTrue(grants().preview(town,"mining",1,0,limited).reason().contains("lifetime"));
        db.close();open();assertEquals(saved,grants().startWorkshop(producer,StartupGrantPolicy.disabled()));
        assertEquals(4000,cash());assertEquals(2,count("startup_grants"));
    }
    @ParameterizedTest @ValueSource(strings={"logging","fishing","quarry","sandpit"})
    void broaderProducerCannotBypassFoodOrPartialTransactionFailure(String industry)throws Exception {
        var producer=producer(industry,person,20,1000);
        sql("UPDATE needs_periods SET fulfilled=0");assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWorkshop(producer,StartupGrantPolicy.prototype()));unchanged();
        sql("UPDATE needs_periods SET fulfilled=demand");
        sql("CREATE TRIGGER reject_producer_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->grants().startWorkshop(producer,StartupGrantPolicy.prototype()));unchanged();
        sql("DROP TRIGGER reject_producer_grant");grants().startWorkshop(producer,StartupGrantPolicy.prototype());assertEquals(8500,cash());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Grant fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture funds");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,BigDecimal.ONE),bands);
        economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        request=mine(person,0);
    }
    MiningStartupRepository.Request mine(UUID owner,int x) {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,10,"Explicit complete fixture evidence");
        var business=new Business(UUID.randomUUID(),town,"Mine",owner,owner,"mining",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        return new MiningStartupRepository.Request(new BusinessFounding(business,site,requirements,requirements,500,"Fixture startup grant"),
                new MineralObservation(world,10,true,Commodity.COAL,List.of(new VillageObservation.Position(x,40,0))),
                10,Map.of(Commodity.COAL,1L,Commodity.STONE,2L),100,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),1000,owner);
    }
    StartupGrantPolicy policy(long one,long period,long lifetime,long keep){return new StartupGrantPolicy(true,one,period,lifetime,keep,2,200,Set.of("mining","blacksmith"));}
    BusinessStartupGrants.WarehouseRequest warehouse(UUID founder,int x) {
        var source=mine(founder,x).founding();var b=source.business();
        var business=new Business(b.id(),town,"Warehouse",founder,founder,"warehouse",b.siteId(),2,0);
        var founding=new BusinessFounding(business,source.site(),source.founderRequirements(),source.workerRequirements(),500,"Explicit warehouse opportunity");
        return new BusinessStartupGrants.WarehouseRequest(founding,100,Map.of(Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),1000,founder);
    }
    @Test void warehouseCapitalIsRealEmptyStockAndReplayCannotPayOrHireTwice()throws Exception {
        var request=warehouse(person,20);var saved=grants().startWarehouse(request,StartupGrantPolicy.prototype());
        assertEquals(8500,cash());assertEquals(1000,saved.store().hundredths());assertTrue(saved.store().units().values().stream().allMatch(units->units==0));
        assertEquals(person,saved.worker().residentId());assertEquals(1,count("startup_grants"));assertEquals(0,count("mineral_deposits"));
        db.close();open();assertEquals(saved,grants().startWarehouse(request,StartupGrantPolicy.disabled()));
        assertEquals(8500,cash());assertEquals(1,count("business_employment"));assertEquals(3,count("treasury_entries"));
        var changed=new BusinessStartupGrants.WarehouseRequest(request.founding(),100,request.space(),2000,person);
        assertThrows(SQLException.class,()->grants().startWarehouse(changed,StartupGrantPolicy.prototype()));assertEquals(8500,cash());
    }
    @Test void warehouseAndMineUseOneSharedPeriodAndLifetimeBudget()throws Exception {
        var other=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(other,"Other",town,null,300,Resident.Status.ALIVE));economy.pulse(3000,false);
        var policy=new StartupGrantPolicy(true,1500,2000,2500,2500,2,200,Set.of("mining","warehouse"));
        grants().startMine(request,policy);var warehouse=warehouse(other,20);
        var period=assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWarehouse(warehouse,policy));assertTrue(period.getMessage().contains("period"));
        assertEquals(8500,cash());assertEquals(1,count("startup_grants"));
        economy.pulse(4000,false);
        var lifetime=assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWarehouse(warehouse,policy));assertTrue(lifetime.getMessage().contains("lifetime"));
        assertEquals(8500,cash());assertEquals(1,count("businesses"));
        var raised=new StartupGrantPolicy(true,1500,2000,3000,2500,2,200,policy.industries());
        grants().startWarehouse(warehouse,raised);assertEquals(7000,cash());assertEquals(2,count("startup_grants"));
    }
    @Test void warehouseCannotSpendFoodProtectionOrClaimItsOwnIndustryAllowance()throws Exception {
        var warehouse=warehouse(person,20);
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWarehouse(warehouse,policy(5000,5000,20000,2500)));unchanged();
        var guarded=new StartupGrantPolicy(true,5000,5000,20000,8501,2,200,Set.of("warehouse"));
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWarehouse(warehouse,guarded));unchanged();
        sql("UPDATE needs_periods SET fulfilled=0");assertThrows(BusinessStartupGrants.Declined.class,()->grants().startWarehouse(warehouse,StartupGrantPolicy.prototype()));unchanged();
    }
    @Test void warehouseGrantReceiptFailureRollsBackCashSiteStockAndFirstJob()throws Exception {
        var warehouse=warehouse(person,20);sql("CREATE TRIGGER reject_warehouse_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->grants().startWarehouse(warehouse,StartupGrantPolicy.prototype()));unchanged();
        sql("DROP TRIGGER reject_warehouse_grant");grants().startWarehouse(warehouse,StartupGrantPolicy.prototype());assertEquals(8500,cash());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void warehouseRequiresQualifiedLivingFounderAndDoesNotOverrideExistingJob()throws Exception {
        var warehouse=warehouse(person,20);db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,100,Resident.Status.ALIVE));
        assertThrows(SQLException.class,()->grants().startWarehouse(warehouse,StartupGrantPolicy.prototype()));unchanged();
        db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));grants().startMine(request,StartupGrantPolicy.prototype());
        assertThrows(SQLException.class,()->grants().startWarehouse(warehouse,StartupGrantPolicy.prototype()));assertEquals(8500,cash());assertEquals(1,count("startup_grants"));assertEquals(1,count("businesses"));
    }
    void sql(String sql)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long cash()throws SQLException{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    @AfterEach void close(){db.close();}
    void unchanged()throws SQLException {
        assertEquals(10000,cash());assertEquals(1,count("treasury_entries"));assertEquals(0,count("startup_grants"));
        for(var table:List.of("businesses","business_sites","industry_stores","mineral_deposits","business_employment"))assertEquals(0,count(table),table);
        assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void grantDebitsRealStartupAndCapitalOnceAndReplaysAfterMiningRestartAndDisable()throws Exception {
        var saved=grants().startMine(request,StartupGrantPolicy.prototype());var id=request.founding().business().id();
        assertEquals(8500,cash());assertEquals(1000,saved.startup().store().hundredths());assertEquals(500,saved.grant().startup());
        assertEquals(2500,saved.grant().protectedCash());assertEquals(2,saved.grant().foodDemand());assertEquals(1,saved.grant().period());
        db.getIndustryRepository().mine(UUID.randomUUID(),id,saved.startup().discovery().deposit().id(),2,0,8,8);
        db.close();open();var replay=grants().startMine(request,StartupGrantPolicy.disabled());
        assertEquals(saved.grant(),replay.grant());assertEquals(8,replay.startup().discovery().deposit().remainingBatches());
        assertEquals(2,replay.startup().store().units().get(Commodity.COAL));assertEquals(8500,cash());assertEquals(1,count("startup_grants"));assertEquals(3,count("treasury_entries"));
    }
    @Test void foodShortageUnknownCurrentAssessmentOrPopulationChangeDeclinesWithoutSpending()throws Exception {
        sql("UPDATE needs_periods SET fulfilled=0");assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));unchanged();
        sql("UPDATE needs_periods SET fulfilled=demand");
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New arrival",town,null,300,Resident.Status.ALIVE));
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));unchanged();
    }
    @Test void missingOrStaleFoodPeriodCannotBeSubstitutedWithEarlierBalancedHistory()throws Exception {
        sql("UPDATE economy_accounts SET completed_periods=2");assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));unchanged();
    }
    @Test void treasuryProtectionUsesLargerDemandBufferOrMinimumAndAllowsExactBoundary()throws Exception {
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,policy(5000,5000,20000,8501)));unchanged();
        var saved=grants().startMine(request,policy(5000,5000,20000,8500));assertEquals(8500,saved.grant().afterCash());assertEquals(8500,saved.grant().protectedCash());
    }
    @Test void grantReceiptFailureRollsBackAllStartupDebitsIdentitySourcesStoreAndWorker()throws Exception {
        sql("CREATE TRIGGER reject_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'fixture grant failure'); END");
        assertThrows(SQLException.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));unchanged();
        sql("DROP TRIGGER reject_grant");assertNotNull(grants().startMine(request,StartupGrantPolicy.prototype()));assertEquals(8500,cash());
    }
    @Test void qualificationEmploymentAndOreExclusionFailuresCannotConsumeGrantQuota()throws Exception {
        new OrePlacementRepository(db.getConnection()).record(world,request.evidence().ore(),10);
        assertThrows(SQLException.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));unchanged();
        db.getPopulationRepository().updateResident(new Resident(person,"Founder",town,null,100,Resident.Status.ALIVE));
        var young=mine(person,10);assertThrows(SQLException.class,()->grants().startMine(young,StartupGrantPolicy.prototype()));unchanged();
    }
    @Test void periodQuotaPersistsAndLaterPeriodResumesWhileLifetimeStillLimitsPublicSpending()throws Exception {
        var p=policy(1500,1500,2500,2500);grants().startMine(request,p);
        var other=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(other,"Other",town,null,300,Resident.Status.ALIVE));
        economy.pulse(3000,false);var next=mine(other,10);
        var refusal=assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(next,p));assertTrue(refusal.getMessage().contains("lifetime"));
        assertEquals(8500,cash());assertEquals(1,count("startup_grants"));
        var higher=policy(1500,1500,3000,2500);var accepted=grants().startMine(next,higher);assertEquals(2,accepted.grant().period());assertEquals(7000,cash());
    }
    @Test void secondGrantInSamePeriodCannotEvadeBudgetAndReplayDoesNotConsumeMoreQuota()throws Exception {
        var other=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(other,"Other",town,null,300,Resident.Status.ALIVE));economy.pulse(3000,false);
        var p=policy(1500,1500,20000,2500);grants().startMine(request,p);grants().startMine(request,p);
        var refusal=assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(mine(other,10),p));assertTrue(refusal.getMessage().contains("period"));assertEquals(1,count("startup_grants"));assertEquals(8500,cash());
    }
    @Test void existingBusinessCannotReceiveRetroactiveGrantAndChangedReplayTermsFail()throws Exception {
        new MiningStartupRepository(db.getConnection()).start(request);
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,StartupGrantPolicy.prototype()));assertEquals(0,count("startup_grants"));assertEquals(8500,cash());
    }
    @Test void workshopFundingAndFirstJobShareOneConservedGrantTransaction()throws Exception {
        var original=request.founding();var b=original.business();
        var business=new Business(b.id(),town,"Smith",person,person,"blacksmith",b.siteId(),2,0);
        var founding=new BusinessFounding(business,original.site(),original.founderRequirements(),original.workerRequirements(),500,"Workshop fixture");
        var workshop=new BusinessStartupGrants.WorkshopRequest(founding,100,Map.of(Commodity.IRON,1L),1000,person);
        var saved=grants().startWorkshop(workshop,StartupGrantPolicy.prototype());assertEquals(8500,cash());assertEquals(1000,saved.store().hundredths());assertEquals(person,saved.worker().residentId());assertEquals(0,count("mineral_deposits"));
        assertEquals(saved,grants().startWorkshop(workshop,StartupGrantPolicy.disabled()));
        var changed=new BusinessStartupGrants.WorkshopRequest(founding,100,workshop.space(),2000,person);
        assertThrows(SQLException.class,()->grants().startWorkshop(changed,StartupGrantPolicy.prototype()));assertEquals(8500,cash());assertEquals(1,count("startup_grants"));
    }
    @Test void freshUpgradeHasNoFabricatedGrantsAndMigrationFailureRollsBackThenRetries()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),48);sql("CREATE INDEX village_startup_grants ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(48,r.getInt(1));}
        sql("DROP INDEX village_startup_grants");new SchemaMigrator().migrate(db.getConnection());unchanged();
        grants().startMine(request,StartupGrantPolicy.prototype());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    Map<String,List<String>> snapshot()throws SQLException {
        var names=new ArrayList<String>();var result=new TreeMap<String,List<String>>();
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {
            var rows=new ArrayList<String>();
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name.replace("\"","\"\"")+"\"")) {
                while(r.next()){var row=new ArrayList<Object>();for(int c=1;c<=r.getMetaData().getColumnCount();c++)row.add(r.getObject(c));rows.add(row.toString());}
            }Collections.sort(rows);result.put(name,rows);
        }return result;
    }
    @Test void repeatedAffordabilityPreviewLeavesEveryTableUnchangedAcrossRestart()throws Exception {
        var before=snapshot();var p=StartupGrantPolicy.prototype();
        var review=grants().preview(town,"warehouse",500,1000,p);assertTrue(review.eligible());assertEquals(2500,review.protectedHundredths());
        for(int i=0;i<4;i++)assertEquals(review,grants().preview(town,"warehouse",500,1000,p));
        assertEquals(before,snapshot());db.close();open();assertEquals(review,grants().preview(town,"warehouse",500,1000,p));assertEquals(before,snapshot());
        assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void previewsExplainDisabledIndustryFoodProtectionAndGrantCapsWithoutWrites()throws Exception {
        var before=snapshot();var p=StartupGrantPolicy.prototype();
        assertTrue(grants().preview(town,"warehouse",500,1000,StartupGrantPolicy.disabled()).reason().contains("disabled"));
        assertFalse(grants().preview(town,"bakery",500,1000,p).eligible());
        assertTrue(grants().preview(town,"warehouse",500,5000,p).reason().contains("single-grant"));
        assertTrue(grants().preview(town,"mining",500,1000,policy(5000,5000,20000,8501)).reason().contains("protected food"));
        assertTrue(grants().preview(town,"mining",500,1000,policy(5000,1499,20000,2500)).reason().contains("period"));
        assertTrue(grants().preview(town,"mining",500,1000,policy(5000,5000,1499,2500)).reason().contains("lifetime"));
        assertFalse(grants().preview(town,"mining",0,0,p).eligible());assertEquals(before,snapshot());
    }
    @Test void missingStaleOrShortageFoodAssessmentDefersWithoutMutatingAnyTables()throws Exception {
        var p=StartupGrantPolicy.prototype();
        sql("UPDATE needs_periods SET fulfilled=0");var before=snapshot();assertTrue(grants().preview(town,"mining",500,1000,p).reason().contains("shortage"));assertEquals(before,snapshot());
        sql("UPDATE needs_periods SET fulfilled=demand");
        db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"New arrival",town,null,300,Resident.Status.ALIVE));before=snapshot();
        assertTrue(grants().preview(town,"mining",500,1000,p).reason().contains("Population changed"));assertEquals(before,snapshot());
        sql("UPDATE economy_accounts SET completed_periods=2");before=snapshot();assertTrue(grants().preview(town,"mining",500,1000,p).reason().contains("assessment missing"));assertEquals(before,snapshot());
        assertFalse(grants().preview(UUID.randomUUID(),"mining",500,1000,p).eligible());assertEquals(before,snapshot());
    }
    @Test void previewReservesNothingAndActualAcceptanceRechecksCashAndSharedGrantQuota()throws Exception {
        var p=policy(5000,1500,20000,2500);assertTrue(grants().preview(town,"mining",500,1000,p).eligible());
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,-7500,"Explicit fixture spending after preview");var before=snapshot();
        assertThrows(BusinessStartupGrants.Declined.class,()->grants().startMine(request,p));assertEquals(before,snapshot());
        new TreasuryRepository(db.getConnection()).post(UUID.randomUUID(),town,7500,"Explicit fixture refund");grants().startMine(request,p);
        before=snapshot();assertTrue(grants().preview(town,"blacksmith",500,1000,p).reason().contains("period"));assertEquals(before,snapshot());
        assertEquals(8500,cash());assertEquals(1,count("startup_grants"));
    }
    @Test void invalidPreviewTermsAndRealSqlFaultsPropagateAndRestoreOwnerTransactionState()throws Exception {
        var before=snapshot();var p=StartupGrantPolicy.prototype();
        assertThrows(IllegalArgumentException.class,()->grants().preview(town,"mining",-1,1000,p));
        assertThrows(ArithmeticException.class,()->grants().preview(town,"mining",Long.MAX_VALUE,1,p));
        assertThrows(IllegalArgumentException.class,()->grants().preview(town,"Not an industry",500,1000,p));
        assertEquals(before,snapshot());assertTrue(db.getConnection().getAutoCommit());
        sql("ALTER TABLE startup_grants RENAME TO unavailable_grants");
        assertThrows(SQLException.class,()->grants().preview(town,"mining",500,1000,p));assertTrue(db.getConnection().getAutoCommit());
        sql("ALTER TABLE unavailable_grants RENAME TO startup_grants");assertEquals(before,snapshot());
    }

}
