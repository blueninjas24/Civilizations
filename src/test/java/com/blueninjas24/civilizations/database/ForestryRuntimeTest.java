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
import static org.junit.jupiter.api.Assertions.*;

class ForestryRuntimeTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    final UUID loggerId=new UUID(0,10),warehouseId=new UUID(0,20),craftsmanId=new UUID(0,30);
    final UUID loggerOwner=UUID.randomUUID(),warehouseOwner=UUID.randomUUID(),craftsmanOwner=UUID.randomUUID();
    final Map<Commodity,Long> sizes=Map.of(Commodity.TIMBER,1L,Commodity.COAL,1L,
            Commodity.FOOD,1L,new Commodity("storage_fittings"),1L);

    @BeforeEach void setup()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("ForestryRuntimeTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(loggerOwner,warehouseOwner,craftsmanOwner))resident(id);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Fixture funds");
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var condition:Condition.values())bands.put(condition,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,BigDecimal.ONE),bands);
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
    }

    @AfterEach void close(){if(db!=null)db.close();}

    private void resident(UUID id)throws Exception {
        db.getPopulationRepository().insertResident(new Resident(id,"Fixture resident",town,null,300,Resident.Status.ALIVE));
    }

    private BusinessStartupGrants.WorkshopRequest loggerRequest() {
        var source=producer("logging",loggerOwner,20,1000);var original=source.founding();var old=original.business();
        var business=new Business(loggerId,town,"Fixture forestry",loggerOwner,loggerOwner,"logging",old.siteId(),2,0);
        var founding=new BusinessFounding(business,original.site(),original.founderRequirements(),original.workerRequirements(),
                original.startupHundredths(),"Funded canopy-backed forestry fixture");
        return new BusinessStartupGrants.WorkshopRequest(founding,source.capacity(),source.space(),source.capital(),source.worker());
    }

    private BusinessStartupGrants.WorkshopRequest producer(String kind,UUID founder,int x,long capital) {
        var source=mine(founder,x).founding();var b=source.business();
        var business=new Business(UUID.randomUUID(),town,"Fixture producer",founder,founder,kind,b.siteId(),2,0);
        var founding=new BusinessFounding(business,source.site(),source.founderRequirements(),source.workerRequirements(),500,"Fixture producer");
        return new BusinessStartupGrants.WorkshopRequest(founding,100,Map.of(Commodity.TIMBER,1L),capital,founder);
    }

    private MiningStartupRepository.Request mine(UUID owner,int x) {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,10,"Fixture workplace");
        var business=new Business(UUID.randomUUID(),town,"Fixture mine",owner,owner,"mining",site.id(),2,0);
        var requirements=new JobRequirements(216,Map.of());
        return new MiningStartupRepository.Request(new BusinessFounding(business,site,requirements,requirements,500,"Fixture"),
                new MineralObservation(world,10,true,Commodity.COAL,List.of(new VillageObservation.Position(x,40,0))),
                10,Map.of(Commodity.COAL,1L),100,Map.of(Commodity.COAL,1L,Commodity.TIMBER,1L),1000,owner);
    }

    private ForestObservation forest(long captured,int trunks) {
        var positions=new ArrayList<VillageObservation.Position>();
        for(int i=0;i<trunks;i++)positions.add(new VillageObservation.Position(20+i,40,0));
        return new ForestObservation(world,captured,true,positions);
    }

    private IndustryRuntimePolicy runtimePolicy() {
        var recipe=new ProductionRecipe("crafted_wood","craftsman",Map.of(Commodity.TIMBER,3L),
                Map.of(new Commodity("storage_fittings"),1L),1,4);
        return new IndustryRuntimePolicy(true,1000,8,
                new IndustryPolicy(10,1,8,256,1,128,Map.of(Commodity.COAL,1L),sizes),
                Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.TIMBER,100L)),
                new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());
    }

    private void business(UUID id,UUID owner,String kind,long cash)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,100+(int)Math.floorMod(id.getLeastSignificantBits(),1_000_000L),40,0,1,"Fixture business");
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Fixture "+kind,owner,owner,kind,site.id(),2,0),
                site,requirements,requirements,0,"Actual configured production fixture"));
        db.getIndustryRepository().openStore(id,100,sizes,cash);
        db.getBusinessRepository().hire(UUID.randomUUID(),id,owner,0);
    }

    private long count(String table)throws Exception {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}
    }
    private long totalFunds()throws Exception {
        long total=new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
        for(var id:List.of(loggerId,warehouseId,craftsmanId)) {
            var store=db.getIndustryRepository().store(id);if(store.isPresent())total=Math.addExact(total,store.orElseThrow().hundredths());
        }
        return total;
    }
    private long countDelivered()throws Exception {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM local_shipments WHERE delivered_period IS NOT NULL")){return r.getLong(1);}
    }
    private void sql(String statement)throws Exception {try(var s=db.getConnection().createStatement()){s.execute(statement);}}

    @Test void paidTimberFlowsThroughWarehouseToRealCraftsmanAndProducesOnce()throws Exception {
        var request=loggerRequest();var startup=new BusinessStartupGrants(db.getConnection()).startForestry(
                request,forest(2000,6),ForestryPolicy.prototype(),StartupGrantPolicy.prototype());
        business(warehouseId,warehouseOwner,"warehouse",1000);business(craftsmanId,craftsmanOwner,"craftsman",1000);
        long funds=totalFunds();var runtime=new IndustryRuntime(db.getConnection(),runtimePolicy(),BusinessOperationPolicy.disabled(),WorkLearningPolicy.prototype());
        runtime.configureForestry(ForestryPolicy.prototype());runtime.start(1000);
        assertEquals(3,runtime.pulse(2000,Set.of(world)));
        assertEquals(92,new ForestryRepository(db.getConnection()).find(startup.startup().founding().site().id()).orElseThrow().stock().available());
        assertEquals(1,count("industry_learning_entries"));
        assertEquals(1,count("local_shipments"));
        assertTrue(db.getIndustryRepository().store(loggerId).orElseThrow().hundredths()>1000);
        long loggerTimber=db.getIndustryRepository().store(loggerId).orElseThrow().units().get(Commodity.TIMBER);
        try(var s=db.getConnection().prepareStatement("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE seller_id=?")) {
            s.setString(1,loggerId.toString());try(var r=s.executeQuery()){assertEquals(4,loggerTimber+r.getLong(1));}
        }
        assertEquals(funds,totalFunds());
        runtime.pulse(3000,Set.of(world));runtime.pulse(4000,Set.of(world));
        assertEquals(1,db.getIndustryRepository().store(craftsmanId).orElseThrow().units().get(new Commodity("storage_fittings")));
        assertTrue(countDelivered()>=2);
        assertEquals(funds,totalFunds());
        assertEquals(0,runtime.pulse(4000,Set.of(world)));
    }

    @Test void replayRestartAbsentWorldAndUnregisteredLoggingCannotInventTimber()throws Exception {
        var startup=new BusinessStartupGrants(db.getConnection()).startForestry(loggerRequest(),forest(2000,6),ForestryPolicy.prototype(),StartupGrantPolicy.prototype());
        business(UUID.randomUUID(),warehouseOwner,"logging",0); // No saved forestry resource: never enroll this logging firm.
        var runtime=new IndustryRuntime(db.getConnection(),runtimePolicy(),BusinessOperationPolicy.disabled(),WorkLearningPolicy.prototype());
        runtime.configureForestry(ForestryPolicy.prototype());runtime.start(1000);
        assertEquals(1,runtime.pulse(2000,Set.of(world)));
        assertEquals(0,runtime.pulse(2000,Set.of(world)));
        assertEquals(0,runtime.pulse(3000,Set.of()));
        assertEquals(1,runtime.pulse(4000,Set.of(world)));
        runtime.start(100_000);assertEquals(0,runtime.pulse(100_999,Set.of(world)));
        assertEquals(1,runtime.pulse(101_000,Set.of(world)));
        assertEquals(12,db.getIndustryRepository().store(loggerId).orElseThrow().units().get(Commodity.TIMBER));
        try(var s=db.getConnection().prepareStatement("SELECT COUNT(*) FROM industry_runtime WHERE business_id<>?")) {
            s.setString(1,loggerId.toString());try(var r=s.executeQuery()){assertEquals(0,r.getLong(1));}
        }
        assertTrue(new ForestryRepository(db.getConnection()).find(startup.startup().founding().site().id()).orElseThrow().stock().available()<96);
    }

    @Test void lateCheckpointFailureRollsBackTimberHabitatSkillAndReceiptThenRetries()throws Exception {
        var startup=new BusinessStartupGrants(db.getConnection()).startForestry(loggerRequest(),forest(2000,6),ForestryPolicy.prototype(),StartupGrantPolicy.prototype());
        var runtime=new IndustryRuntime(db.getConnection(),runtimePolicy(),BusinessOperationPolicy.disabled(),WorkLearningPolicy.prototype());
        runtime.configureForestry(ForestryPolicy.prototype());runtime.start(1000);
        sql("CREATE TRIGGER fail_forestry_checkpoint BEFORE UPDATE OF completed ON industry_runtime BEGIN SELECT RAISE(ABORT,'injected'); END");
        assertThrows(SQLException.class,()->runtime.pulse(2000,Set.of(world)));
        assertEquals(0,db.getIndustryRepository().store(loggerId).orElseThrow().units().get(Commodity.TIMBER));
        assertEquals(96,new ForestryRepository(db.getConnection()).find(startup.startup().founding().site().id()).orElseThrow().stock().available());
        assertEquals(0,count("forestry_work"));assertEquals(0,count("industry_production"));assertEquals(0,count("industry_learning_entries"));
        sql("DROP TRIGGER fail_forestry_checkpoint");assertEquals(1,runtime.pulse(2000,Set.of(world)));
        assertEquals(4,db.getIndustryRepository().store(loggerId).orElseThrow().units().get(Commodity.TIMBER));
        assertEquals(92,new ForestryRepository(db.getConnection()).find(startup.startup().founding().site().id()).orElseThrow().stock().available());
    }

    @Test void invalidHabitatAndGrantFailureLeaveAllStartupAssetsAndCashUntouched()throws Exception {
        var request=loggerRequest();
        assertThrows(IllegalArgumentException.class,()->new BusinessStartupGrants(db.getConnection()).startForestry(
                request,forest(2000,5),ForestryPolicy.prototype(),StartupGrantPolicy.prototype()));
        assertEquals(10000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        sql("CREATE TRIGGER reject_forestry_grant BEFORE INSERT ON startup_grants BEGIN SELECT RAISE(ABORT,'fixture rejection'); END");
        assertThrows(SQLException.class,()->new BusinessStartupGrants(db.getConnection()).startForestry(
                request,forest(2000,6),ForestryPolicy.prototype(),StartupGrantPolicy.prototype()));
        assertEquals(10000,new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths());
        for(var table:List.of("businesses","business_sites","industry_stores","business_employment","forestry_resources","forestry_supports","startup_grants"))assertEquals(0,count(table),table);
        assertEquals(1,count("treasury_entries"));assertTrue(db.getConnection().getAutoCommit());
    }
}
