import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Guarded synthetic fixture, actual application owner; never the player's database or world blocks. */
public final class WorkshopSupplyProbe extends JavaPlugin {
    private PopulationSession session;
    private ExecutorService fixture;
    private CompletableFuture<?> pending;
    private final AtomicLong now=new AtomicLong(1000);
    private java.io.File folder;
    private UUID world,smith,warehouse,town;
    private IndustryRuntimePolicy policy;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25577 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            world=getServer().getWorlds().getFirst().getUID();folder=new java.io.File(getDataFolder(),"run-"+UUID.randomUUID());
            if(getServer().getWorlds().getFirst().isChunkLoaded(128,128))throw new IllegalStateException("Fixture site chunk must be unloaded");
            var tools=new Commodity("tools");var sizes=Map.of(Commodity.COAL,1L,Commodity.IRON,1L,tools,1L);
            var recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
            policy=new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,128,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),
                    new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)));
            fixture=Executors.newSingleThreadExecutor();
            pending=CompletableFuture.runAsync(()-> {try{seed();}catch(Exception failure){throw new CompletionException(failure);}},fixture);
            new BukkitRunnable() {
                int ticks,stage;
                @Override public void run() {
                    try {
                        if(++ticks>400)throw new IllegalStateException("Workshop supply probe timed out");
                        if(!pending.isDone())return;pending.join();
                        switch(stage++) {
                            case 0 -> {session=create();pending=session.start(false,Set.of(world));}
                            case 1 -> {now.set(2000);pending=session.pulse();}
                            case 2 -> {check(0,500,500,2,2);pending=session.updateLoadedWorlds(Set.of());}
                            case 3 -> {now.set(100000);pending=session.pulse();}
                            case 4 -> {check(0,500,500,2,2);pending=session.updateLoadedWorlds(Set.of(world));}
                            case 5 -> {now.set(101000);pending=session.pulse();}
                            case 6 -> {check(1,500,500,2,0);session.close();now.set(1000000);session=create();pending=session.start(false,Set.of(world));}
                            case 7 -> {check(1,500,500,2,0);now.set(1001000);pending=session.pulse();}
                            case 8 -> {check(1,0,1000,4,2);now.set(1002000);pending=session.pulse();}
                            case 9 -> {
                                check(2,0,1000,4,0);getLogger().info("NATIVE WORKSHOP SUPPLY PASS: actual owner pays real warehouse, unavailable cargo, delayed input transformation, conserved money, absent-world pause and restart; unloaded fixture, separate database, no world writes");
                                cancel();getServer().getPluginManager().disablePlugin(WorkshopSupplyProbe.this);
                            }
                            default -> throw new IllegalStateException("Unexpected probe stage");
                        }
                    }catch(Exception failure){cancel();fail(failure);}
                }
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){fail(failure);}
    }
    private PopulationSession create() {
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var result=new PopulationSession(folder,getLogger(),settings,now::get);result.configureIndustryRuntime(policy);return result;
    }
    private void seed()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();town=UUID.randomUUID();var owner=UUID.randomUUID();var worker=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture opening capital");
            var req=new JobRequirements(216,Map.of());var ids=new ArrayList<UUID>();int offset=0;
            for(var kind:List.of("mining","warehouse","blacksmith")) {
                var site=new BusinessSite(UUID.randomUUID(),town,world,2048+offset++,40,2048,1,"Synthetic fixture site; no block ownership");
                var b=new Business(UUID.randomUUID(),town,kind,owner,owner,kind,site.id(),2,0);
                db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Explicit fixture startup"));
                db.getIndustryRepository().openStore(b.id(),100,policy.mining().storageSpacePerUnit(),kind.equals("blacksmith")?1000:0);ids.add(b.id());
            }
            var mine=ids.get(0);warehouse=ids.get(1);smith=ids.get(2);
            db.getBusinessRepository().hire(UUID.randomUUID(),mine,worker,0);
            var site=db.getBusinessRepository().findFounding(mine).orElseThrow().site();var deposit=UUID.randomUUID();
            db.getIndustryRepository().discover(deposit,site.id(),1,2,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
            db.getIndustryRepository().mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
            for(var e:Map.of(Commodity.IRON,4L,Commodity.COAL,2L).entrySet()) {
                var cargo=db.getIndustryRepository().purchase(UUID.randomUUID(),mine,warehouse,e.getKey(),e.getValue(),0,0,1,128);
                db.getIndustryRepository().deliver(cargo.id(),1);
            }
            db.getBusinessRepository().endEmployment(db.getBusinessRepository().currentWorkers(mine).getFirst().id(),0,"Fixture reserve exhausted");
            db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
        }finally{db.close();}
    }
    private void check(long tools,long buyerFunds,long sellerFunds,long orders,long pendingCount)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            if(scalar(c,"SELECT units FROM industry_stock WHERE business_id='"+smith+"' AND commodity='tools'")!=tools
                    || scalar(c,"SELECT hundredths FROM industry_stores WHERE business_id='"+smith+"'")!=buyerFunds
                    || scalar(c,"SELECT hundredths FROM industry_stores WHERE business_id='"+warehouse+"'")!=sellerFunds
                    || scalar(c,"SELECT COUNT(*) FROM industry_auto_shipments")!=orders
                    || scalar(c,"SELECT COUNT(*) FROM industry_auto_shipments a JOIN local_shipments t ON t.id=a.shipment_id WHERE delivered_period IS NULL")!=pendingCount
                    || scalar(c,"SELECT hundredths FROM treasuries WHERE settlement_id='"+town+"'")!=9000)
                throw new IllegalStateException("Supply, delivery or money conservation failed");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){if(!r.getString(1).equals("ok"))throw new IllegalStateException("Integrity failed");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new IllegalStateException("Foreign key failed");}
        }
    }
    private long scalar(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){if(!r.next())throw new IllegalStateException("Missing fixture row");return r.getLong(1);}}
    private void fail(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKSHOP SUPPLY FAIL",failure);getServer().getPluginManager().disablePlugin(this);}
    @Override public void onDisable(){if(session!=null){session.close();session=null;}if(fixture!=null)fixture.shutdown();}
}
