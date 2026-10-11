import com.blueninjas24.civilizations.database.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;




public final class AutomaticMineFoundingProbe extends JavaPlugin {
    Path directory;
    DatabaseManager db;
    UUID town=new UUID(1,1),world=new UUID(1,2),owner=new UUID(0,1),smithWorker=new UUID(0,2),warehouseWorker=new UUID(0,3),founder=new UUID(0,4),other=new UUID(0,5);
    UUID smith=new UUID(2,1),warehouse=new UUID(2,2);
    MineFoundingPolicy founding=MineFoundingPolicy.prototype();
    boolean healthy=true;
    int batches=1;
    long capture=1999;
    EconomySettings economySettings;
    final Map<Commodity,Long> sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,Commodity.STONE,1L,ToolUpkeepPolicy.TOOLS,1L);
    IndustryRuntimePolicy industry() {
        var recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),batches,batches);
        return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of("tools",recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)),new WarehouseSupplyPolicy(true,16,16));
    }
    AutomaticMineFounding automation(){return new AutomaticMineFounding(db.getConnection(),founding,industry(),BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),216,42,()->healthy);}
    String review()throws Exception{return automation().review(town,1,Math.max(2000,capture),Set.of(world));}
    void setup()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutomaticMineTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,40,0,5,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(var id:List.of(owner,smithWorker,warehouseWorker,founder,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture resident",town,null,300,Resident.Status.ALIVE));
        for(var id:List.of(founder,other))db.getPopulationRepository().recordSkillExperience(id,"mining",10);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100000,"Explicit fixture funds");
        business(smith,"blacksmith",0,20000,smithWorker);business(warehouse,"warehouse",1,10000,warehouseWorker);
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var condition:Condition.values())bands.put(condition,100);
        var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,BigDecimal.ONE),bands);
        economySettings=settings;
        var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,VillagePriorities.Policy.prototype(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype().seed());
        economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
        new OreTrackingRepository(db.getConnection()).begin(1000);survey(List.of(new VillageObservation.Position(12,40,0),new VillageObservation.Position(13,40,0),new VillageObservation.Position(30,40,0)));
    }
    void business(UUID id,String kind,int x,long cash,UUID worker)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,40,0,1,"Explicit fixture site");var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,kind,owner,owner,kind,site.id(),1,0),site,requirements,requirements,0,"Fixture"));
        db.getIndustryRepository().openStore(id,1000,sizes,cash);db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);
    }
    void survey(List<VillageObservation.Position> coal)throws Exception {
        capture++;
        new MineralOpportunities(db.getConnection()).observe(town,List.of(new MineralObservation(world,capture,true,Commodity.COAL,coal),new MineralObservation(world,capture,true,Commodity.IRON,List.of())));
    }
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long scalar(String query)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    long cash()throws Exception{return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();}
    long mines()throws Exception{return scalar("SELECT COUNT(*) FROM businesses WHERE industry='mining'");}
    UUID mine()throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT id FROM businesses WHERE industry='mining' ORDER BY id")){assertTrue(r.next());return UUID.fromString(r.getString(1));}}
    void unchanged()throws Exception{assertEquals(0,mines());assertEquals(70000,cash());assertEquals(0,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(0,scalar("SELECT COUNT(*) FROM mineral_deposits"));assertTrue(db.getConnection().getAutoCommit());}
    void close(){if(db!=null)db.close();}


    void assertTrue(boolean value){if(!value)throw new IllegalStateException("Probe assertion failed");}
    void assertEquals(long expected,long actual){if(expected!=actual)throw new IllegalStateException("Expected "+expected+" but got "+actual);}
    private ExecutorService executor;
    com.blueninjas24.civilizations.application.PopulationSession session(AtomicLong clock) {
        var s=new com.blueninjas24.civilizations.application.PopulationSession(directory.toFile(),getLogger(),com.blueninjas24.civilizations.application.PopulationSession.Settings.prototype(),clock::get,economySettings,FarmObservation.Policy.prototype());
        s.configureOreTracking();s.configureIndustryRuntime(industry());s.configureMineFounding(founding,StartupGrantPolicy.prototype());return s;
    }
    void runChecks() {
        try {
            setup();new OreTrackingRepository(db.getConnection()).finish(2000,true);db.close();db=null;
            var clock=new AtomicLong(2000);
            try(var s=session(clock)) {
                s.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);
                assertTrue(s.industryRuntimeDiagnostic().contains("mine")||s.industryRuntimeDiagnostic().contains("ore"));
                clock.set(3000);s.pulse().get(10,TimeUnit.SECONDS);
                s.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);clock.set(100000);s.pulse().get(10,TimeUnit.SECONDS);
            }
            db=new DatabaseManager(directory.toFile(),getLogger());db.initialize();assertEquals(1,mines());assertEquals(67500,cash());
            long remaining=scalar("SELECT SUM(remaining_batches) FROM mineral_deposits"),initial=scalar("SELECT SUM(initial_batches) FROM mineral_deposits");assertEquals(8,initial-remaining);
            long stock=scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='coal'");assertEquals(16,stock);
            assertEquals(1,scalar("SELECT COUNT(*) FROM startup_grants"));assertEquals(500,scalar("SELECT startup_hundredths FROM startup_grants"));
            db.close();db=null;
            try(var s=session(clock)){s.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);}
            db=new DatabaseManager(directory.toFile(),getLogger());db.initialize();assertEquals(1,mines());assertEquals(remaining,scalar("SELECT SUM(remaining_batches) FROM mineral_deposits"));assertEquals(stock,scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='coal'"));
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("PRAGMA integrity_check")){assertTrue(r.getString(1).equals("ok"));}
            try(var q=db.getConnection().createStatement();var r=q.executeQuery("PRAGMA foreign_key_check")){assertTrue(!r.next());}
            getLogger().info("NATIVE AUTOMATIC MINE PASS: actual SQL owner founds one funded qualified mine; 2500 real grant, 500 expense, 2000 capital; eight finite batches produce sixteen coal; absent-world pause and restart conserve assets; no physical world changes");
        }catch(Exception failure){throw new CompletionException(failure);}finally{if(db!=null){db.close();db=null;}}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            assertTrue(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty());
            world=getServer().getWorlds().getFirst().getUID();directory=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>1200)throw new IllegalStateException("Mine probe timeout");if(!work.isDone())return;work.join();cancel();getServer().getPluginManager().disablePlugin(AutomaticMineFoundingProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE AUTOMATIC MINE FAIL",failure);getServer().getPluginManager().disablePlugin(AutomaticMineFoundingProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Automatic mine probe refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
