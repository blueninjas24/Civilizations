import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.JobRequirements;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit isolated funded firms; ordinary SQL-owner timers. Not automatic discovery or physical digging. */
public final class QuarryRuntimeProbe extends JavaPlugin {
    private PopulationSession session;private World world;private Path folder;private UUID town;private boolean authorized;
    private final AtomicLong clock=new AtomicLong(2000);
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private PopulationSession owner(boolean live) {
        var s=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get);
        s.configureQuarries(QuarryPolicy.prototype());s.configureWorkLearning(WorkLearningPolicy.prototype());
        s.configureIndustryRuntime(new IndustryRuntimePolicy(live,1000,8,IndustryPolicy.prototype(),Map.of()));return s;
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace((char)92,'/')+"?mode=ro");var q=c.createStatement();var r=q.executeQuery(sql)){check(r.next(),"Missing scalar");return r.getLong(1);}
    }
    private void seed()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            var people=db.getPopulationRepository().findResidents(town);try(var q=db.getConnection().createStatement()){q.execute("UPDATE residents SET age_steps=300");}
            for(var person:people)db.getPopulationRepository().recordSkillExperience(person.id(),"mining",10);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit isolated fixture cash");
            var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
            var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,4L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE),bands);
            var food=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);food.observe(new FarmObservation(town,world.getUID(),1000,true,0),1000);food.startOnline(1000,Set.of(world.getUID()));food.pulse(2000,false);
            for(int index=0;index<2;index++) {
                String kind=index==0?"quarry":"sandpit";var good=index==0?Commodity.STONE:Commodity.SAND;var terms=QuarryPolicy.prototype().terms(good);var person=people.get(index).id();int x=20+index*20;
                var site=new BusinessSite(UUID.randomUUID(),town,world.getUID(),x,40,0,10,"Explicit trusted fixture coordinates; no live survey claim");
                var business=new Business(UUID.randomUUID(),town,"Fixture "+kind,person,person,kind,site.id(),1,0);
                var founding=new BusinessFounding(business,site,new JobRequirements(216,Map.of("mining",10L)),new JobRequirements(216,Map.of("mining",1L)),500,"Explicit real funded fixture");
                var observation=new MineralObservation(world.getUID(),10,true,good,List.of(new VillageObservation.Position(x,40,0),new VillageObservation.Position(x+1,40,0)));
                new BusinessStartupGrants(db.getConnection()).startMine(new MiningStartupRepository.Request(founding,observation,terms.reserveBatchesPerBlock(),QuarryPolicy.prototype().bundle(good),1024,Map.of(good,1L),1000,person),StartupGrantPolicy.prototype());
            }
        }finally{db.close();}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;
        world=getServer().getWorlds().getFirst();folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner(false);var home=new VillageObservation.Position(0,64,0);
        var observation=new VillageObservation(world.getUID(),"Extraction fixture",1000,true,List.of(home,new VillageObservation.Position(2,64,0)),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture operator A"),new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture operator B")));
        var init=session.start(false,Set.of(world.getUID())).thenCompose(v->session.observe(observation));
        new BukkitRunnable(){CompletableFuture<?> pending=init;int stage,ticks,pause;long remaining,work,skills;
            @Override public void run(){try {
                check(++ticks<=1200,"Quarry timer fixture timeout stage "+stage);if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(stage==0){town=session.settlementNames().keySet().iterator().next();session.close();seed();session=owner(true);pending=session.start(true,Set.of(world.getUID()));stage=1;return;}
                if(stage==1){check(scalar("SELECT COUNT(*) FROM industry_runtime")==2,"Both existing extraction clocks must enroll");clock.set(3000);stage=2;return;}
                if(stage==2){if(scalar("SELECT COUNT(*) FROM industry_production")<2||scalar("SELECT COUNT(*) FROM industry_runtime WHERE target<>completed OR last_ms<>3000")!=0)return;
                    check(scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='stone'")==18&&scalar("SELECT SUM(units) FROM industry_stock WHERE commodity='sand'")==48,"Distinct real stone/sand work rates required");
                    check(scalar("SELECT SUM(remaining_batches) FROM mineral_deposits")==238&&scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='mining'")==22,"Finite depletion and actual productive learning required");
                    check(scalar("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")==9000,"Actual startup fees/cash conservation required");pending=session.updateLoadedWorlds(Set.of());stage=3;return;}
                if(stage==3){remaining=scalar("SELECT SUM(remaining_batches) FROM mineral_deposits");work=scalar("SELECT COUNT(*) FROM industry_production");skills=scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='mining'");clock.set(13000);pause=ticks;stage=4;return;}
                if(stage==4){if(ticks-pause<40)return;check(remaining==scalar("SELECT SUM(remaining_batches) FROM mineral_deposits")&&work==scalar("SELECT COUNT(*) FROM industry_production")&&skills==scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='mining'"),"Absent worlds must pause extraction/learning");session.close();session=owner(true);pending=session.start(true,Set.of(world.getUID()));pause=ticks;stage=5;return;}
                if(stage==5){if(ticks-pause<40)return;check(remaining==scalar("SELECT SUM(remaining_batches) FROM mineral_deposits")&&work==scalar("SELECT COUNT(*) FROM industry_production")&&skills==scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='mining'"),"Restart cannot invent offline extraction");check(scalar("SELECT COUNT(*) FROM pragma_foreign_key_check")==0,"Healthy foreign keys required");getLogger().info("NATIVE QUARRY TIMERS PASS: ordinary SQL owner, actual finite stone/sand rates, real startup/owned stock, productive skills, absence/restart and no refill; explicit trusted fixture sites, no automatic survey or block/entity changes");cancel();getServer().getPluginManager().disablePlugin(QuarryRuntimeProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE QUARRY TIMERS FAIL",failure);getServer().getPluginManager().disablePlugin(QuarryRuntimeProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Quarry fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(session!=null)session.close();}
}
