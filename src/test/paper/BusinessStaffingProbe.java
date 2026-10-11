import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
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
public final class BusinessStaffingProbe extends JavaPlugin {
    private ExecutorService executor;
    private final UUID town=UUID.randomUUID(),owner=new UUID(0,1),a=new UUID(0,2),b=new UUID(0,3),firm=UUID.randomUUID(),deposit=UUID.randomUUID();
    private UUID world;private File folder;
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private void seed()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Staffing fixture","Fixture",0,64,0,3,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(owner,a,b)){db.getPopulationRepository().insertResident(new Resident(person,"Fixture resident",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(person,"mining",person.equals(owner)?0:2);}
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture funds");
            var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,1,"Synthetic existing site");
            db.getBusinessRepository().found(new BusinessFounding(new Business(firm,town,"Existing mine",owner,owner,"mining",site.id(),1,0),site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of("mining",2L)),0,"Fixture founding"));
            db.getIndustryRepository().openStore(firm,100,Map.of(Commodity.COAL,1L),1000);
            db.getIndustryRepository().discover(deposit,site.id(),1,10,Map.of(Commodity.COAL,1L));
        }finally{db.close();}
    }
    private PopulationSession session(AtomicLong clock) {
        var session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get);
        session.configureBusinessStaffing(new BusinessStaffingPolicy(true,64,1,Set.of("mining")));
        session.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,10,Map.of(Commodity.COAL,1L),Map.of(Commodity.COAL,1L)),Map.of()));
        return session;
    }
    private void check(long jobs,long audits,long reserve)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            try(var s=c.createStatement();var r=s.executeQuery("SELECT (SELECT COUNT(*) FROM business_employment),(SELECT COUNT(*) FROM business_hiring_reviews),(SELECT SUM(remaining_batches) FROM mineral_deposits)")) {require(r.getLong(1)==jobs&&r.getLong(2)==audits&&r.getLong(3)==reserve,"Job/audit/reserve mismatch");}
            require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()+new IndustryRepository(c).store(firm).orElseThrow().hundredths()==10000,"Hiring created money");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Integrity failure");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"FK failure");}
        }
    }
    private void runChecks() {
        try {
            seed();var clock=new AtomicLong(1000);
            try(var session=session(clock)) {
                session.start(false,Set.of()).get(10,TimeUnit.SECONDS);check(0,0,10);
                session.updateLoadedWorlds(Set.of(world)).get(10,TimeUnit.SECONDS);check(1,1,10);
                clock.set(2000);session.pulse().get(10,TimeUnit.SECONDS);check(1,1,9);
            }
            var db=new DatabaseManager(folder,getLogger());
            try{db.initialize();require(db.getBusinessRepository().currentWorkers(firm).getFirst().residentId().equals(a),"Stable candidate order changed");db.getPopulationRepository().updateResident(new Resident(a,"Fixture resident",town,null,300,Resident.Status.DECEASED));}finally{db.close();}
            clock.set(100000);
            try(var session=session(clock)) {
                session.start(false,Set.of()).get(10,TimeUnit.SECONDS);check(1,1,9);
                session.updateLoadedWorlds(Set.of(world)).get(10,TimeUnit.SECONDS);check(2,2,9);
                clock.set(101000);session.pulse().get(10,TimeUnit.SECONDS);check(2,2,8);
                session.pulse().get(10,TimeUnit.SECONDS);check(2,2,8);
            }
        }catch(Exception failure){throw new CompletionException(failure);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
            var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>600)throw new IllegalStateException("Staffing probe timeout");if(!work.isDone())return;work.join();getLogger().info("NATIVE BUSINESS STAFFING PASS: actual PopulationSession hires qualified local workers, finite production, saved death separation/replacement, once-only audit, world pause and restart; cash conserved");cancel();getServer().getPluginManager().disablePlugin(BusinessStaffingProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE BUSINESS STAFFING FAIL",failure);getServer().getPluginManager().disablePlugin(BusinessStaffingProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Staffing fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
