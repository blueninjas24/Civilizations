import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import java.math.BigDecimal;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Independent synthetic save; actual SQL owners, no world/entity/chunk mutation. Never ship. */
public final class FarmWarehouseProbe extends JavaPlugin {
    private ExecutorService executor;
    private final UUID town=UUID.randomUUID(),farmer=new UUID(0,1),smith=new UUID(0,2),firm=UUID.randomUUID(),buyer=UUID.randomUUID(),mine=UUID.randomUUID();
    private UUID world;private File folder;
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private void seed()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Farm tool fixture","Fixture",0,64,0,2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(farmer,smith))db.getPopulationRepository().insertResident(new Resident(person,"Fixture resident",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funds");
            var site=new BusinessSite(UUID.randomUUID(),town,world,10,64,0,1,"Synthetic existing workshop");var requirements=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(firm,town,"Smith",smith,smith,"blacksmith",site.id(),1,0),site,requirements,requirements,0,"Fixture founding"));
            db.getBusinessRepository().hire(UUID.randomUUID(),firm,smith,0);db.getIndustryRepository().openStore(firm,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);
            try(var statement=db.getConnection().createStatement()){statement.execute("UPDATE industry_stock SET units=4 WHERE commodity='iron'");}
            db.getIndustryRepository().process(UUID.randomUUID(),firm,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),4,4),4,0);
            var warehouseSite=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,1,"Synthetic existing warehouse");
            db.getBusinessRepository().found(new BusinessFounding(new Business(buyer,town,"Buyer",smith,smith,"warehouse",warehouseSite.id(),1,0),warehouseSite,requirements,requirements,0,"Fixture founding"));
            db.getIndustryRepository().openStore(buyer,10,Map.of(ToolUpkeepPolicy.TOOLS,1L,Commodity.IRON,1L),27000);
            db.getIndustryRepository().purchase(UUID.randomUUID(),firm,buyer,ToolUpkeepPolicy.TOOLS,1,26000,0,1,10);
        }finally{db.close();}
    }
    private PopulationSession session(AtomicLong clock) {
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var economy=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        var session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get,economy,FarmObservation.Policy.prototype());
        session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));
        session.configureFarmTools(new FarmToolUpkeepPolicy(true,1,2,50,8),FarmToolBudgetPolicy.prototype());
        session.configureUpkeepContributions(UpkeepContributionPolicy.prototype());
        session.configureBusinessOperations(BusinessOperationPolicy.prototype());return session;
    }
    private void check(long periods,long invoices,long uses)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var statement=c.createStatement()){statement.execute("PRAGMA foreign_keys=ON");}
            var industry=new IndustryRepository(c);var farm=FarmEnterprises.businessId(town);
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*),COALESCE(SUM(tools_used),0),COALESCE(SUM(output),0) FROM farm_tool_work")) {
                require(r.next()&&r.getLong(1)==periods&&r.getLong(2)==uses&&r.getLong(3)==4*periods,"Food/wear/audit mismatch");
            }
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM farm_tool_purchases")){require(r.next()&&r.getLong(1)==invoices,"Invoice mismatch");}
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM upkeep_contributions")){require(r.next()&&r.getLong(1)==invoices,"Contribution mismatch");}
            require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()==2500,"Protected village reserve changed");
            var store=industry.store(farm).orElseThrow();require(store.hundredths()==0,"Public payment left private farm cash");
            require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()+industry.store(firm).orElseThrow().hundredths()+industry.store(buyer).orElseThrow().hundredths()==30000,"Farm upkeep created money");
            require(uses+store.units().get(ToolUpkeepPolicy.TOOLS)+store.reservedIncomingSpace()+industry.store(firm).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS)+industry.store(buyer).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS)+industry.store(buyer).orElseThrow().reservedIncomingSpace()==4,"Manufactured tool conservation failure");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Integrity failure");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"FK failure");}
        }
    }
    private void restock()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();var businesses=db.getBusinessRepository();var requirements=new JobRequirements(216,Map.of());
            UUID warehouseWorker=new UUID(0,3),miner=new UUID(0,4);
            for(var id:List.of(warehouseWorker,miner))db.getPopulationRepository().insertResident(new Resident(id,"Fixture worker",town,null,300,Resident.Status.ALIVE));
            businesses.hire(UUID.randomUUID(),buyer,warehouseWorker,0);
            var site=new BusinessSite(UUID.randomUUID(),town,world,30,64,0,1,"Synthetic source site");
            businesses.found(new BusinessFounding(new Business(mine,town,"Mine",smith,smith,"mining",site.id(),1,0),site,requirements,requirements,0,"Explicit fixture"));
            businesses.hire(UUID.randomUUID(),mine,miner,0);db.getIndustryRepository().openStore(mine,10,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),0);
            UUID deposit=UUID.randomUUID();db.getIndustryRepository().discover(deposit,site.id(),1,1,Map.of(Commodity.IRON,2L));
            db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,1,0,1,1);
            for(var job:businesses.currentWorkers(mine))businesses.endEmployment(job.id(),0,"Explicit fixture extraction complete");
            var recipe=new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1);
            var policy=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,100L)),new WarehouseSupplyPolicy(true,16,16));
            var runtime=new IndustryRuntime(db.getConnection(),policy);runtime.start(1000);runtime.pulse(2000,Set.of(world));runtime.pulse(2000,Set.of(world));
            require(db.getIndustryRepository().store(buyer).orElseThrow().hundredths()==900,"Warehouse did not pay for real upstream goods");
            require(db.getIndustryRepository().store(mine).orElseThrow().hundredths()==100,"Actual producer not paid");
            require(db.getIndustryRepository().store(buyer).orElseThrow().reservedIncomingSpace()==2,"One original toolkit plus actual upstream iron cargo required");
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM industry_auto_shipments")){require(r.next()&&r.getLong(1)==1,"Duplicate upstream purchase");}
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")){require(r.next()&&r.getLong(1)==30000,"Upstream transfer created cash");}
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Restock integrity failure");}
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Restock FK failure");}
        }finally{db.close();}
    }
    private void runChecks() {
        try {
            seed();var clock=new AtomicLong(1000);
            try(var session=session(clock)) {
                session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
                session.observeFarms(new FarmObservation(town,world,1000,true,16,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,64,0))).get(10,TimeUnit.SECONDS);
                clock.set(2000);session.pulse().get(10,TimeUnit.SECONDS);check(1,1,0);
                clock.set(3000);session.pulse().get(10,TimeUnit.SECONDS);check(2,2,1);
                session.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);clock.set(100000);session.pulse().get(10,TimeUnit.SECONDS);check(2,2,1);
            }
            clock.set(1000000);
            try(var session=session(clock)) {
                session.start(false,Set.of()).get(10,TimeUnit.SECONDS);check(2,2,1);
                session.updateLoadedWorlds(Set.of(world)).get(10,TimeUnit.SECONDS);
                clock.set(1001000);session.pulse().get(10,TimeUnit.SECONDS);check(3,2,1);
                clock.set(1002000);session.pulse().get(10,TimeUnit.SECONDS);check(4,3,2);
                session.pulse().get(10,TimeUnit.SECONDS);check(4,3,2);
            }
            restock();
            publicFarmDemand();
        }catch(Exception failure){throw new CompletionException(failure);}
    }
    private void publicFarmDemand()throws Exception {
        folder=new File(getDataFolder(),"public-demand-"+UUID.randomUUID());
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Public demand fixture","Fixture",0,64,0,3,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            UUID clerk=new UUID(0,3);for(var id:List.of(farmer,smith,clerk))db.getPopulationRepository().insertResident(new Resident(id,"Fixture worker",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funds");
            var requirements=new JobRequirements(216,Map.of());
            for(var id:List.of(firm,buyer)) {
                var owner=id.equals(firm)?smith:clerk;var site=new BusinessSite(UUID.randomUUID(),town,world,id.equals(firm)?10:20,64,0,1,"Explicit fixture site");
                db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Fixture business",owner,owner,id.equals(firm)?"blacksmith":"warehouse",site.id(),1,0),site,requirements,requirements,0,"Explicit fixture"));
                db.getBusinessRepository().hire(UUID.randomUUID(),id,owner,0);db.getIndustryRepository().openStore(id,100,Map.of(Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L),id.equals(firm)?0:4000);
            }
            try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=1 WHERE commodity='iron' AND business_id='"+firm+"'");}
            db.getIndustryRepository().process(UUID.randomUUID(),firm,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1),1,0);
        }finally{db.close();}
        var clock=new AtomicLong(1000);
        try(var session=session(clock)) {
            session.configureFarmTools(new FarmToolUpkeepPolicy(true,1,2,50,8),FarmToolBudgetPolicy.disabled());session.configureWorkLearning(WorkLearningPolicy.prototype());
            session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
            session.observeFarms(new FarmObservation(town,world,1000,true,16,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,64,0))).get(10,TimeUnit.SECONDS);
            clock.set(2000);session.pulse().get(10,TimeUnit.SECONDS);
        }
        try(var session=demandSession(clock)) {
            session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);clock.set(3000);session.pulse().get(10,TimeUnit.SECONDS);
            session.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);clock.set(100000);session.pulse().get(10,TimeUnit.SECONDS);
        }
        clock.set(1000000);
        try(var session=demandSession(clock)) {
            session.start(false,Set.of()).get(10,TimeUnit.SECONDS);session.updateLoadedWorlds(Set.of(world)).get(10,TimeUnit.SECONDS);
            session.observeFarms(new FarmObservation(town,world,clock.get(),true,16,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,64,0))).get(10,TimeUnit.SECONDS);
            clock.set(1001000);session.pulse().get(10,TimeUnit.SECONDS);clock.set(1002000);session.pulse().get(10,TimeUnit.SECONDS);session.pulse().get(10,TimeUnit.SECONDS);
        }
        db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();var industry=db.getIndustryRepository();var farm=FarmEnterprises.businessId(town);
            require(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==22000,"Public farm budget mismatch");
            require(industry.store(buyer).orElseThrow().hundredths()==4000&&industry.store(firm).orElseThrow().hundredths()==4000,"Producer/warehouse payment mismatch");
            try(var s=db.getConnection().createStatement()) {
                for(var table:List.of("industry_auto_shipments","farm_tool_purchases"))try(var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){require(r.next()&&r.getLong(1)==1,"Duplicate or missing "+table);}
                try(var r=s.executeQuery("SELECT service_periods FROM business_tools WHERE business_id='"+farm+"'")){require(r.next()&&r.getLong(1)>0,"Delivered tool not used");}
                try(var r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Public demand integrity failure");}
                try(var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Public demand FK failure");}
            }
        }finally{db.close();}
        getLogger().info("Public farm demand PASS: actual toolkit, paid producer/warehouse/farm chain, restart/absence, no duplicate or minted money");
    }
    private PopulationSession demandSession(AtomicLong clock) {
        var session=session(clock);session.configureUpkeepContributions(UpkeepContributionPolicy.disabled());session.configureWorkLearning(WorkLearningPolicy.prototype());
        var recipe=new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1);
        session.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,100L)),new WarehouseSupplyPolicy(true,16,16),new ToolUpkeepPolicy(true,3,8,50,4000,Set.of())));return session;
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>600)throw new IllegalStateException("Farm tool probe timeout");if(!work.isDone())return;work.join();getLogger().info("NATIVE FARM WAREHOUSE PASS: actual PopulationSession food work, real purchases/deliveries, once-only service/use, world pause and restart; earned supplier surplus funds protected invoices; audited farm store permits actual upstream warehouse purchase; cash and goods conserved");cancel();getServer().getPluginManager().disablePlugin(FarmWarehouseProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FARM WAREHOUSE FAIL",failure);getServer().getPluginManager().disablePlugin(FarmWarehouseProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Farm tool fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
