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
public final class FarmToolsProbe extends JavaPlugin {
    private ExecutorService executor;
    private final UUID town=UUID.randomUUID(),farmer=new UUID(0,1),smith=new UUID(0,2),firm=UUID.randomUUID();
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
            try(var statement=db.getConnection().createStatement()){statement.execute("UPDATE industry_stock SET units=3 WHERE commodity='iron'");}
            db.getIndustryRepository().process(UUID.randomUUID(),firm,new ProductionRecipe("fixture_tools","blacksmith",Map.of(Commodity.IRON,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),3,3),3,0);
        }finally{db.close();}
    }
    private PopulationSession session(AtomicLong clock) {
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        var economy=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        var session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get,economy,FarmObservation.Policy.prototype());
        session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));
        session.configureFarmTools(new FarmToolUpkeepPolicy(true,1,2,50,8),FarmToolBudgetPolicy.prototype());
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
            var store=industry.store(farm).orElseThrow();require(store.hundredths()==0,"Public payment left private farm cash");
            require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()+industry.store(firm).orElseThrow().hundredths()==30000,"Farm upkeep created money");
            require(uses+store.units().get(ToolUpkeepPolicy.TOOLS)+store.reservedIncomingSpace()+industry.store(firm).orElseThrow().units().get(ToolUpkeepPolicy.TOOLS)==3,"Manufactured tool conservation failure");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.next()&&r.getString(1).equals("ok"),"Integrity failure");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"FK failure");}
        }
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
        }catch(Exception failure){throw new CompletionException(failure);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>600)throw new IllegalStateException("Farm tool probe timeout");if(!work.isDone())return;work.join();getLogger().info("NATIVE FARM TOOLS PASS: actual PopulationSession food work, real purchases/deliveries, once-only service/use, world pause and restart; cash and manufactured tools conserved");cancel();getServer().getPluginManager().disablePlugin(FarmToolsProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FARM TOOLS FAIL",failure);getServer().getPluginManager().disablePlugin(FarmToolsProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Farm tool fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
