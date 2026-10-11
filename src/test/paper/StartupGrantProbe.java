import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.economy.FarmObservation;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.EconomyConfiguration;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Packaged configuration/SQLite smoke test in a separate marked disposable save; never ship. */
public final class StartupGrantProbe extends JavaPlugin {
    private ExecutorService owner;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25578 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            var world=getServer().getWorlds().getFirst().getUID();var folder=new File(getDataFolder(),"run-"+UUID.randomUUID());
            var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");
            final YamlConfiguration config;
            try(var input=new InputStreamReader(Objects.requireNonNull(plugin.getResource("economy.yml")),StandardCharsets.UTF_8)) {
                config=YamlConfiguration.loadConfiguration(input);
            }
            var settings=EconomyConfiguration.read(config);var policy=EconomyConfiguration.readStartupGrants(config);
            if(!policy.equals(StartupGrantPolicy.prototype()))throw new IllegalStateException("Packaged defaults differ from grant policy");
            owner=Executors.newSingleThreadExecutor();
            var pending=CompletableFuture.runAsync(()-> {
                var db=new DatabaseManager(folder,getLogger());
                try {
                    db.initialize();var town=UUID.randomUUID();var person=UUID.randomUUID();var merchant=UUID.randomUUID();
                    db.saveSettlement(new Settlement(town,"Grant probe","Fixture",0,64,0,1,0,0));
                    db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
                    db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
                    db.getPopulationRepository().insertResident(new Resident(merchant,"Warehouse founder",town,null,300,Resident.Status.ALIVE));
                    new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit disposable probe funds");
                    var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
                    economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));
                    long periodMillis=settings.secondsPerPeriod()*1000;economy.pulse(1000+periodMillis,false);
                    var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,10,"Synthetic explicit fixture evidence, not a live ore survey");
                    var b=new Business(UUID.randomUUID(),town,"Mine",person,person,"mining",site.id(),2,0);var req=new JobRequirements(216,Map.of());
                    var request=new MiningStartupRepository.Request(new BusinessFounding(b,site,req,req,500,"Probe startup grant"),
                            new MineralObservation(world,10,true,Commodity.COAL,List.of(new VillageObservation.Position(0,40,0))),
                            10,Map.of(Commodity.COAL,1L,Commodity.STONE,2L),100,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),1000,person);
                    var grants=new BusinessStartupGrants(db.getConnection());
                    var strict=new StartupGrantPolicy(true,5000,5000,20000,9000,2,200,policy.industries());
                    try {grants.startMine(request,strict);throw new IllegalStateException("Protected food money was spent");}
                    catch(BusinessStartupGrants.Declined expected){if(!expected.getMessage().contains("protected"))throw expected;}
                    var saved=grants.startMine(request,policy);
                    if(saved.grant().beforeCash()!=10000 || saved.grant().afterCash()!=8500 || saved.startup().store().hundredths()!=1000)
                        throw new IllegalStateException("Grant did not conserve startup/capital");
                    db.getIndustryRepository().mine(UUID.randomUUID(),b.id(),saved.startup().discovery().deposit().id(),2,0,8,8);
                    db.close();db=new DatabaseManager(folder,getLogger());db.initialize();grants=new BusinessStartupGrants(db.getConnection());
                    var replay=grants.startMine(request,StartupGrantPolicy.disabled());
                    if(!saved.grant().equals(replay.grant()) || replay.startup().discovery().deposit().remainingBatches()!=8
                            || replay.startup().store().units().get(Commodity.COAL)!=2 || new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()!=8500)
                        throw new IllegalStateException("Restart/disable repeated funding or extraction");
                    var warehouseSite=new BusinessSite(UUID.randomUUID(),town,world,20,64,0,10,"Explicit synthetic warehouse site, no building edits");
                    var warehouseBusiness=new Business(UUID.randomUUID(),town,"Warehouse",merchant,merchant,"warehouse",warehouseSite.id(),2,0);
                    var warehouse=new BusinessStartupGrants.WarehouseRequest(new BusinessFounding(warehouseBusiness,warehouseSite,req,req,500,"Probe warehouse grant"),
                            100,Map.of(Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),1000,merchant);
                    var sharedLimit=new StartupGrantPolicy(true,5000,2000,20000,2500,2,200,policy.industries());
                    try {grants.startWarehouse(warehouse,sharedLimit);throw new IllegalStateException("Warehouse bypassed shared period budget");}
                    catch(BusinessStartupGrants.Declined expected){if(!expected.getMessage().contains("period"))throw expected;}
                    var warehouseSaved=grants.startWarehouse(warehouse,policy);
                    if(warehouseSaved.store().hundredths()!=1000 || warehouseSaved.store().units().values().stream().anyMatch(units->units!=0)
                            || new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()!=7000)
                        throw new IllegalStateException("Warehouse capital or stock invented");
                    db.close();db=new DatabaseManager(folder,getLogger());db.initialize();
                    if(!warehouseSaved.equals(new BusinessStartupGrants(db.getConnection()).startWarehouse(warehouse,StartupGrantPolicy.disabled())))
                        throw new IllegalStateException("Warehouse replay changed accepted terms");
                    try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)+(SELECT SUM(startup_hundredths) FROM startup_grants)")) {
                        if(r.getLong(1)!=10000)throw new IllegalStateException("Warehouse/mine money conservation failed");
                    }
                    try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){if(!r.getString(1).equals("ok"))throw new IllegalStateException("Integrity failure");}
                    try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new IllegalStateException("FK failure");}
                } catch(Exception failure){throw new CompletionException(failure);}
                finally {db.close();}
            },owner);
            new BukkitRunnable(){int ticks;public void run(){
                if(!pending.isDone() && ++ticks<600)return;
                cancel();try {if(!pending.isDone())throw new IllegalStateException("Grant probe timeout");pending.join();
                    getLogger().info("NATIVE STARTUP GRANT PASS: packaged defaults, protected-food refusal, mine and warehouse shared budgets, real capital/expenses, empty warehouse and qualified worker, restart/disabled replay and healthy SQLite; separate synthetic save, no automatic founding");
                } catch(RuntimeException failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE STARTUP GRANT FAIL",failure);}
                getServer().getPluginManager().disablePlugin(StartupGrantProbe.this);
            }}.runTaskTimer(this,1,1);
        } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Grant fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(owner!=null)owner.shutdown();}
}
