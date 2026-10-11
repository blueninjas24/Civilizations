import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit independent marked disposable database; never ship with the plugin. */
public final class CareerStudyProbe extends JavaPlugin {
    private ExecutorService executor;
    private Path folder;
    private UUID world;
    private final UUID town=UUID.randomUUID(),owner=new UUID(0,1),student=new UUID(0,2),other=new UUID(0,3),firm=new UUID(1,1);
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private StudyPolicy study(){return new StudyPolicy(true,1,100,2,8,1,2,Map.of("mining",new StudyPolicy.Course(2,Map.of(Commodity.TIMBER,1L,Commodity.STONE,1L))));}
    private EconomySettings settings() {
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        return new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,25L,Commodity.TIMBER,10L,Commodity.STONE,10L),Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE),bands);
    }
    private void fixture()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,3,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var id:List.of(owner,student,other))db.getPopulationRepository().insertResident(new Resident(id,"Fixture",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,0,"Explicit fixture mine");var b=new Business(firm,town,"Mine",owner,owner,"mining",site.id(),1,0);
            db.getBusinessRepository().found(new BusinessFounding(b,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of("mining",2L)),0,"Actual saved vacancy"));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Actual fixture treasury");
            var e=new EconomyRuntimeRepository(db.getConnection(),settings(),FarmObservation.Policy.prototype(),216);e.configureStudy(study());e.observe(new FarmObservation(town,world,1000,true,0),1000);
        }finally{db.close();}
    }
    private PopulationSession session(AtomicLong clock) {
        var s=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get,settings(),FarmObservation.Policy.prototype());
        s.configureStudy(study());s.configureCareerStudy(CareerStudyPolicy.prototype());s.configureBusinessStaffing(new BusinessStaffingPolicy(true,64,1,Set.of("mining")));return s;
    }
    private void check(long experience,int jobs,long timber,int courses)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            require(experience(c,student)==experience,"Wrong gradual experience");require(experience(c,owner)==0,"Idle ownership trained");require(experience(c,other)==0,"Unselected person trained");
            var firms=new BusinessRepository(c);require(firms.currentWorkers(firm).size()==jobs,"Incorrect job count");if(jobs>0)require(firms.currentWorkers(firm).getFirst().residentId().equals(student),"Wrong hire");
            try(var query=c.prepareStatement("SELECT units FROM economy_stocks WHERE settlement_id=? AND commodity='timber'")){query.setString(1,town.toString());try(var rows=query.executeQuery()){require(rows.next()&&rows.getLong(1)==timber,"Practice supplies not conserved");}}require(new TreasuryRepository(c).load(town).orElseThrow().hundredths()==10000,"Study created/spent money");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM resident_courses")){require(r.getLong(1)==courses,"Duplicate/phantom course");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Integrity failed");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Foreign keys failed");}
        }
    }
    private long experience(Connection connection,UUID person)throws SQLException {
        try(var s=connection.prepareStatement("SELECT experience FROM resident_skills WHERE resident_id=? AND skill='mining'")) {
            s.setString(1,person.toString());try(var r=s.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }
    private void runChecks() {
        try {
            fixture();var clock=new AtomicLong(1000);
            try(var s=session(clock)) {
                s.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);clock.set(2000);s.pulse().get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);check(0,0,30,1);
                clock.set(3000);s.pulse().get(10,TimeUnit.SECONDS);check(1,0,29,1);
                s.updateLoadedWorlds(Set.of()).get(10,TimeUnit.SECONDS);clock.set(100000);s.pulse().get(10,TimeUnit.SECONDS);check(1,0,29,1);
            }
            try(var s=session(clock)) {
                s.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);check(1,0,29,1);
                clock.set(101000);s.pulse().get(10,TimeUnit.SECONDS);s.pulse().get(10,TimeUnit.SECONDS);check(2,1,28,1);
                s.pulse().get(10,TimeUnit.SECONDS);check(2,1,28,1);
            }
            getLogger().info("NATIVE CAREER STUDY PASS: actual vacancy selects one adult; two real practice periods reach qualification; normal hiring; no free skills/cash, replay, restart and absent-world pause");
        }catch(Exception failure){throw new CompletionException(failure);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost no-player fixture required");
            world=getServer().getWorlds().getFirst().getUID();folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();var work=CompletableFuture.runAsync(this::runChecks,executor);
            new BukkitRunnable(){int ticks;public void run(){try{if(++ticks>1200)throw new IllegalStateException("Career probe timeout");if(!work.isDone())return;work.join();cancel();getServer().getPluginManager().disablePlugin(CareerStudyProbe.this);}catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE CAREER STUDY FAIL",failure);getServer().getPluginManager().disablePlugin(CareerStudyProbe.this);}}}.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Career study probe refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(executor!=null)executor.shutdown();}
}
