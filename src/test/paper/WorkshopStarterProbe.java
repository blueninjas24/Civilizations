import com.blueninjas24.civilizations.database.*;
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
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            var world=getServer().getWorlds().getFirst().getUID();executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(()->{try{checks(world);}catch(Exception failure){throw new CompletionException(failure);}},executor);
            new BukkitRunnable(){int ticks;public void run(){try {
                if(++ticks>600)throw new IllegalStateException("Starter probe timeout");if(!work.isDone())return;work.join();
                getLogger().info("NATIVE WORKSHOP STARTER PASS: real paid public surplus, bounded cargo, restart/absence, one toolkit, no duplicate or minted money; SQLite integrity verified");cancel();getServer().getPluginManager().disablePlugin(WorkshopStarterProbe.this);
            }catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKSHOP STARTER FAIL",failure);getServer().getPluginManager().disablePlugin(WorkshopStarterProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Starter fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
