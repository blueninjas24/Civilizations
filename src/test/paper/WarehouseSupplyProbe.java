import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit synthetic firms in an independent marked disposable database. Never ship. */
public final class WarehouseSupplyProbe extends JavaPlugin {
    private ExecutorService executor;
    private final UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),warehouse=new UUID(0,1),smith=new UUID(0,2),mine=new UUID(0,3),deposit=UUID.randomUUID();
    private UUID world;
    private File folder;
    private final Commodity tools=new Commodity("tools");
    private final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,tools,1L);
    private IndustryRuntimePolicy policy(boolean enabled) {
        var recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
        return new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,3,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)),new WarehouseSupplyPolicy(enabled,16,16));
    }
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private void fixture()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,4,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(owner,"Owner",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Actual disposable capital");
            for(var id:List.of(warehouse,smith,mine)) {
                String kind=id.equals(warehouse)?"warehouse":id.equals(smith)?"blacksmith":"mining";
                var site=new BusinessSite(UUID.randomUUID(),town,world,(int)id.getLeastSignificantBits(),40,0,1,"Explicit disposable site");
                var business=new Business(id,town,kind,owner,owner,kind,site.id(),2,0);var req=new JobRequirements(216,Map.of());
                db.getBusinessRepository().found(new BusinessFounding(business,site,req,req,0,"Explicit disposable opportunity"));
                db.getIndustryRepository().openStore(id,100,sizes,id.equals(mine)?0:1000);
                UUID worker=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(worker,"Worker",town,null,300,Resident.Status.ALIVE));
                db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);
            }
            var industry=db.getIndustryRepository();industry.discover(deposit,db.getBusinessRepository().findBusiness(mine).orElseThrow().siteId(),1,10,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
            industry.mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
            for(var job:db.getBusinessRepository().currentWorkers(mine))db.getBusinessRepository().endEmployment(job.id(),0,"Fixture extraction complete");
        }finally{db.close();}
    }
    private void check(int shipments,int output,long mineCash,long warehouseCash,long smithCash,int warehousePending)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            var industry=new IndustryRepository(c);var a=industry.store(mine).orElseThrow();var b=industry.store(warehouse).orElseThrow();var d=industry.store(smith).orElseThrow();
            require(a.hundredths()==mineCash&&b.hundredths()==warehouseCash&&d.hundredths()==smithCash,"Payment mismatch");
            require(d.units().get(tools)==output&&b.reservedIncomingSpace()==warehousePending,"Cargo/production mismatch");
            require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()+a.hundredths()+b.hundredths()+d.hundredths()==10000,"Money not conserved");
            require(industry.deposit(deposit).orElseThrow().remainingBatches()==8,"Reserve refilled or drained without workers");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM industry_auto_shipments")){require(r.getLong(1)==shipments,"Duplicate/missing orders");}
            try(var s=c.createStatement();var r=s.executeQuery("SELECT SUM(t.units) FROM industry_auto_shipments a JOIN local_shipments t ON t.id=a.shipment_id GROUP BY haul_window,haul_cadence")){while(r.next())require(r.getLong(1)<=3,"Shared hauling exceeded");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Integrity failure");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Foreign key failure");}
        }
    }
    private void runChecks() {
        try {
            fixture();var clock=new AtomicLong(1000);
            try(var session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get)) {
                session.configureIndustryRuntime(policy(true));session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
                clock.set(2000);session.pulse().get(10,TimeUnit.SECONDS);check(2,0,500,500,1000,3);
                session.pulse().get(10,TimeUnit.SECONDS);check(2,0,500,500,1000,3);
                session.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);clock.set(100000);session.pulse().get(10,TimeUnit.SECONDS);check(2,0,500,500,1000,3);
            }
            try(var session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get)) {
                session.configureIndustryRuntime(policy(false));session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);session.pulse().get(10,TimeUnit.SECONDS);check(2,0,500,500,1000,3);
                clock.set(101000);session.pulse().get(10,TimeUnit.SECONDS);check(4,0,500,1000,500,0);
                clock.set(102000);session.pulse().get(10,TimeUnit.SECONDS);check(4,1,500,1000,500,0);
            }
        }catch(Exception failure){throw new CompletionException(failure);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player disposable fixture required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>1200)throw new IllegalStateException("Warehouse probe timeout");if(!work.isDone())return;work.join();getLogger().info("NATIVE WAREHOUSE SUPPLY PASS: paid upstream goods, delayed two-leg chain, real tools, conservation, shared hauling, owner restart, disable and world pause");cancel();getServer().getPluginManager().disablePlugin(WarehouseSupplyProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WAREHOUSE SUPPLY FAIL",failure);getServer().getPluginManager().disablePlugin(WarehouseSupplyProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Warehouse probe refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
