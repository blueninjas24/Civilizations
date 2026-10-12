import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/** Disposable synthetic save, SQL owner thread; never accesses block/entity inventories. */
public final class WorkshopStarterProbe extends JavaPlugin {
    private ExecutorService executor;
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private void checks(UUID world)throws Exception {
        var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();
        var town=UUID.randomUUID();var founder=UUID.randomUUID();var business=UUID.randomUUID();var operation=UUID.randomUUID();
        var recipe=ProductionRecipe.smithTools();var supplyPolicy=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
        var request=new WorkshopStarterSupplies.Request(operation,business,recipe,1,0,1,128);
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Starter fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(founder,"Fixture founder",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit native fixture funds");
            var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
            var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L),
                Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.IRON,BigDecimal.TEN,Commodity.COAL,BigDecimal.TEN),bands);
            var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
            economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
            var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,1,"Synthetic workplace; no land rights");
            var requirements=new JobRequirements(216,Map.of());
            var workshop=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(new Business(business,town,"Smith",founder,founder,"blacksmith",site.id(),2,0),site,requirements,requirements,500,"Explicit fixture opportunity"),100,
                Map.of(Commodity.IRON,1L,Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),4000,founder);
            new BusinessStartupGrants(db.getConnection()).startWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),request,supplyPolicy,Set.of(world));
            require(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==8500,"Village paid starter sale missing");
            var store=db.getIndustryRepository().store(business).orElseThrow();require(store.hundredths()==1000&&store.reservedIncomingSpace()==3&&store.units().get(Commodity.IRON)==0,"Startup money or paid cargo mismatch");
        }finally{db.close();}
        db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();var supplies=new WorkshopStarterSupplies(db.getConnection());supplies.purchase(request,WorkshopStarterPolicy.disabled(),Set.of());
            var runtime=new IndustryRuntime(db.getConnection(),new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe)));
            runtime.start(100000);runtime.pulse(101000,Set.of());require(supplies.find(operation).orElseThrow().deliveredPeriod()==null,"Absent world delivered cargo");
            runtime.pulse(102000,Set.of(world));runtime.pulse(103000,Set.of(world));runtime.pulse(103000,Set.of(world));
            var store=db.getIndustryRepository().store(business).orElseThrow();require(store.units().get(ToolUpkeepPolicy.TOOLS)==1&&store.reservedIncomingSpace()==0,"Paid inputs did not produce exactly one toolkit");
            require(store.hundredths()==1000&&new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==8500,"Production minted or lost cash");
            try(var s=db.getConnection().createStatement()) {
                try(var r=s.executeQuery("SELECT units FROM economy_stocks WHERE commodity='iron'")){require(r.next()&&r.getLong(1)==8,"Public iron not debited exactly once");}
                try(var r=s.executeQuery("SELECT COUNT(*) FROM workshop_starter_orders")){require(r.next()&&r.getLong(1)==1,"Duplicate starter order");}
                try(var r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Integrity failure");}
                try(var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Foreign key failure");}
            }
        }finally{db.close();}
    }
    private void opportunityChecks(UUID world)throws Exception {
        var folder=getDataFolder().toPath().resolve("opportunity-run-"+UUID.randomUUID()).toFile();
        var town=UUID.randomUUID();var farmer=new UUID(0,1);var prospect=new UUID(0,2);
        var upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);var budget=FarmToolBudgetPolicy.prototype();
        var starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L,Commodity.IRON,10L,Commodity.COAL,10L),
            Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.IRON,BigDecimal.valueOf(12),Commodity.COAL,BigDecimal.valueOf(6)),bands);
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Workshop prospect fixture","Fixture",0,64,0,2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var resident:List.of(farmer,prospect))db.getPopulationRepository().insertResident(new Resident(resident,"Fixture resident",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funds");
            var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
            economy.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
            economy.configureFarmTools(upkeep,budget,IndustryPolicy.prototype());economy.configureWorkLearning(WorkLearningPolicy.prototype());
            economy.observe(new FarmObservation(town,world,1000,true,16,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,64,0)),1000);
            economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);economy.pulse(3000,false);
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,3000,true,List.of(
                new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(10,64,0),WorkplaceObservation.Kind.SMITHING_TABLE))));
        }finally{db.close();}
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),Map.of(Commodity.COAL,1L,Commodity.STONE,1L,Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L));
        var recipe=ProductionRecipe.smithTools();var industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
        var original=PopulationSession.Settings.prototype();var population=new PopulationSession.Settings(0,original.millisPerMonth(),original.maximumOfflineMonths(),original.monthsPerBatch(),original.population(),original.migration(),original.recognition(),original.enrollment());
        var clock=new java.util.concurrent.atomic.AtomicLong(3000);
        try(var session=new PopulationSession(folder,getLogger(),population,clock::get,settings,FarmObservation.Policy.prototype())) {
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(upkeep,budget);session.configureWorkLearning(WorkLearningPolicy.prototype());
            session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureIndustryRuntime(industry);
            session.configureWorkplaces(WorkplaceObserverPolicy.prototype());session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);
            session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
            session.start(false,Set.of()).get(10,TimeUnit.SECONDS);require(session.currentWorkshopOpportunities(town).get(10,TimeUnit.SECONDS).candidates().isEmpty(),"Absent world offered workshop");
            session.updateLoadedWorlds(Set.of(world)).get(10,TimeUnit.SECONDS);var found=session.currentWorkshopOpportunities(town).get(10,TimeUnit.SECONDS);
            require(found.candidates().size()==1&&found.candidates().getFirst().starter().price()==3000,"Actual funded first-workshop evidence missing");
            session.currentWorkshopOpportunities(town).get(10,TimeUnit.SECONDS);
            session.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);require(session.currentWorkshopOpportunities(town).get(10,TimeUnit.SECONDS).candidates().isEmpty(),"World pause ignored in investment review");
        }
        db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();require(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==30000,"Read-only review spent money");
            require(db.getPopulationRepository().findWorkProfile(prospect).orElseThrow().experience().getOrDefault("smithing",0L)==0,"Read-only review gave free skills");
            try(var s=db.getConnection().createStatement()) {
                try(var r=s.executeQuery("SELECT COUNT(*) FROM businesses")){require(r.next()&&r.getLong(1)==1,"Investment review founded a speculative firm");}
                try(var r=s.executeQuery("SELECT COUNT(*) FROM startup_grants")){require(r.next()&&r.getLong(1)==0,"Investment review reserved a grant");}
                try(var r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Opportunity integrity failure");}
                try(var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Opportunity FK failure");}
            }
        }finally{db.close();}
        getLogger().info("NATIVE WORKSHOP OPPORTUNITY PASS: actual working farm need, current synthetic marker, real fundable surplus, SQL-owner restart/world guards, no spending, founding or free skills");
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            var world=getServer().getWorlds().getFirst().getUID();executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(()->{try{checks(world);opportunityChecks(world);}catch(Exception failure){throw new CompletionException(failure);}},executor);
            new BukkitRunnable(){int ticks;public void run(){try {
                if(++ticks>600)throw new IllegalStateException("Starter probe timeout");if(!work.isDone())return;work.join();
                getLogger().info("NATIVE WORKSHOP STARTER PASS: real paid public surplus, bounded cargo, restart/absence, one toolkit, no duplicate or minted money; SQLite integrity verified");cancel();getServer().getPluginManager().disablePlugin(WorkshopStarterProbe.this);
            }catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKSHOP STARTER FAIL",failure);getServer().getPluginManager().disablePlugin(WorkshopStarterProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Starter fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
