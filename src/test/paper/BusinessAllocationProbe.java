import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Real PopulationSession prioritization with a private fixture. No world edits, entities or chunk tickets. Never ship. */
public final class BusinessAllocationProbe extends JavaPlugin {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),smithWorker=UUID.randomUUID();
    private final UUID smith=new UUID(1,1),coal=new UUID(1,2),iron=new UUID(1,3);
    private UUID world;private String worldName;private Path folder;private PopulationSession session;
    private CompletableFuture<?> pending;private int stage,ticks;private boolean authorized;
    private final Commodity tools=new Commodity("tools");
    private IndustryRuntimePolicy policy(){
        var recipe=new ProductionRecipe("smith_tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
        return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)));
    }
    private static void check(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked disposable localhost fixture required");
        authorized=true;world=getServer().getWorlds().getFirst().getUID();worldName=getServer().getWorlds().getFirst().getName();
        folder=getDataFolder().toPath().resolve("private-"+UUID.randomUUID());
        pending=CompletableFuture.runAsync(()->{try{seed();}catch(Exception failure){throw new CompletionException(failure);}},worker);
        new BukkitRunnable(){@Override public void run(){try{advance();}catch(Exception|AssertionError failure){fail(failure);}}}.runTaskTimer(this,1,1);
    }catch(Exception|AssertionError failure){fail(failure);}}
    private void advance()throws Exception {
        check(++ticks<400,"Business allocation probe timeout at stage "+stage);
        if(!pending.isDone())return;pending.join();
        switch(stage){
            case 0->{session=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);
                session.configureIndustryRuntime(policy());session.configureBusinessStaffing(new BusinessStaffingPolicy(true,64,1,Set.of("mining")));
                session.configureBusinessAllocation(BusinessAllocationPolicy.prototype());pending=session.start(false,Set.of(world));stage=1;}
            case 1->{pending=session.pulse();stage=2;}
            case 2->{verify();getLogger().info("NATIVE BUSINESS ALLOCATION PASS: real owner pulse chose funded iron demand400 over earlier coal demand100; one new qualified job; existing smith retained; funds unchanged; no world edits");
                getServer().getPluginManager().disablePlugin(this);getServer().shutdown();stage=3;}
        }
    }
    private void seed()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());try{
            db.initialize();db.saveSettlement(new Settlement(town,"Allocation fixture",worldName,0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,worldName);db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var id:List.of(owner,smithWorker))db.getPopulationRepository().insertResident(new Resident(id,"Fixture adult",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Private fixture money");
            int x=0;for(var id:List.of(smith,coal,iron)){
                var site=new BusinessSite(UUID.randomUUID(),town,world,x++,40,0,1,"Private explicit site");var requirements=new JobRequirements(216,Map.of());
                var firm=new Business(id,town,"Fixture firm",owner,owner,id.equals(smith)?"blacksmith":"mining",site.id(),2,0);
                db.getBusinessRepository().found(new BusinessFounding(firm,site,requirements,requirements,0,"Private opportunity"));
                db.getIndustryRepository().openStore(id,100,Map.of(Commodity.COAL,1L,Commodity.IRON,1L,tools,1L),id.equals(smith)?1000:0);
                if(!id.equals(smith))db.getIndustryRepository().discover(UUID.randomUUID(),site.id(),1,100,Map.of(id.equals(coal)?Commodity.COAL:Commodity.IRON,1L));
            }
            db.getBusinessRepository().hire(UUID.randomUUID(),smith,smithWorker,0);
        }finally{db.close();}
    }
    private void verify()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("civilizations.db").toUri()+"?mode=ro")){
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");}
            var businesses=new BusinessRepository(c);
            check(businesses.currentWorkers(iron).size()==1&&businesses.currentWorkers(coal).isEmpty(),"Stronger funded need wins across firm IDs");
            check(businesses.currentWorkers(smith).getFirst().residentId().equals(smithWorker),"Existing worker cannot be poached");
            check(new TreasuryRepository(c).load(town).orElseThrow().hundredths()==9000,"No allocation spending or new funds");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT business_id,demand_value FROM business_allocation_reviews")){
                check(r.next()&&r.getString(1).equals(iron.toString())&&r.getLong(2)==400&&!r.next(),"Exactly one audited selection");
            }
        }
    }
    private void fail(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE BUSINESS ALLOCATION FAIL",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}
    @Override public void onDisable(){getServer().getScheduler().cancelTasks(this);if(session!=null)session.close();worker.shutdown();}
}
