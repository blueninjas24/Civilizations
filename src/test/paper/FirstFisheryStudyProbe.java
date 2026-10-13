import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Marked native ordinary startup/runtime proof. Fixture actors/cash only; all initial water/banks are actual. */
public final class FirstFisheryStudyProbe extends JavaPlugin {
    private PopulationSession session;private PaperWaterObserver observer;private PaperFisheryStartup startup;private PaperFisheryWorkplaces banks;
    private World world;private Path folder;private UUID town;private boolean ticket,authorized;
    private final AtomicLong clock=new AtomicLong(2000);
    private final WaterObserverPolicy water=new WaterObserverPolicy(true,2,4096,200,0);
    private final FisheryShorelinePolicy shore=new FisheryShorelinePolicy(true,2,16,128,20);
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private PopulationSession owner(boolean live) {
        var base=PopulationSession.Settings.prototype();
        var settings=new PopulationSession.Settings(0,base.millisPerMonth(),base.maximumOfflineMonths(),base.monthsPerBatch(),base.population(),base.migration(),base.recognition(),base.enrollment());
        var s=new PopulationSession(folder.toFile(),getLogger(),settings,clock::get,foodSettings(),FarmObservation.Policy.prototype());
        s.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));s.configureStudy(StudyPolicy.prototype());s.configureCareerStudy(CareerStudyPolicy.prototype());
        s.configureWater(water);s.configureFishing(FishingPolicy.prototype());s.configureFisheryOpportunities(FisheryOpportunityPolicy.prototype());s.configureFisheryFounding(FisheryFoundingPolicy.prototype());s.configureFisheryShoreline(shore);
        s.configureWorkLearning(WorkLearningPolicy.prototype());s.configureIndustryRuntime(new IndustryRuntimePolicy(live,1000,8,IndustryPolicy.prototype(),Map.of()));s.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());s.configureVillageSupplies(VillageSupplyPolicy.prototype());return s;
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace((char)92,'/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){check(r.next(),"Missing SQL result");return r.getLong(1);}
    }
    private EconomySettings foodSettings() {
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        return new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,7L,Commodity.TIMBER,10L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE,Commodity.TIMBER,java.math.BigDecimal.ONE),bands);
    }
    private void seed()throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            try(var q=db.getConnection().createStatement()){q.execute("UPDATE residents SET age_steps=300");}
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit isolated fixture funds");
            var food=new EconomyRuntimeRepository(db.getConnection(),foodSettings(),FarmObservation.Policy.prototype(),216);
            food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
            food.observe(new FarmObservation(town,world.getUID(),1000,true,16,new VillageObservation.Position(10,64,10)),1000);food.startOnline(1000,Set.of(world.getUID()));food.pulse(2000,false);
            try(var q=db.getConnection().createStatement()){q.execute("UPDATE economy_stocks SET units=7 WHERE commodity='food'");}
        }finally{db.close();}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;
        world=getServer().getWorlds().getFirst();ticket=world.addPluginChunkTicket(0,0,this);folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner(false);
        var home=new VillageObservation.Position(0,64,0);var observation=new VillageObservation(world.getUID(),"Isolated fishing fixture",1000,true,List.of(home,new VillageObservation.Position(2,64,0)),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture adult"),new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture second adult")));
        var init=session.start(false,Set.of(world.getUID())).thenCompose(v->session.observe(observation));
        new BukkitRunnable(){CompletableFuture<?> pending=init;CompletableFuture<List<PopulationSession.FisheryWorksite>> sites;PopulationSession.FisheryWorksite site;WaterSupportScan support;ShorelineScan recovery;int stage,ticks,pause;long units,cash,work,skills,recoveryTime;
            @Override public void run(){try {
                check(++ticks<=3600,"Automatic fishery timeout stage "+stage+": "+(startup==null?"not started":startup.diagnostic()));
                if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(stage==0){town=session.settlementNames().keySet().iterator().next();session.close();seed();check(scalar("SELECT COALESCE(SUM(experience),0) FROM resident_skills WHERE skill='fishing'")==0,"No initial fishing experience permitted");session=owner(true);pending=session.start(true,Set.of(world.getUID()));stage=1;return;}
                if(stage==1){observer=new PaperWaterObserver(session,water,getLogger(),clock::get);observer.runTaskTimer(FirstFisheryStudyProbe.this,1,1);startup=new PaperFisheryStartup(session,shore,water,getLogger(),clock::get);startup.runTaskTimer(FirstFisheryStudyProbe.this,1,1);stage=2;return;}
                if(stage==2){if(scalar("SELECT COUNT(*) FROM fishery_workplaces")!=1||scalar("SELECT COUNT(*) FROM industry_runtime")!=1){if(ticks%20==0&&scalar("SELECT COUNT(*) FROM resident_courses WHERE skill='fishing' AND status='ACTIVE'")==1&&scalar("SELECT target_periods FROM economy_runtime")==scalar("SELECT completed_periods FROM economy_accounts"))clock.addAndGet(1000);return;}startup.cancel();startup=null;observer.cancel();observer=null;
                    check(scalar("SELECT COUNT(*) FROM startup_grants")==1,"One protected startup debit required");
                    check(scalar("SELECT COUNT(*) FROM study_spending WHERE course_id IN(SELECT course_id FROM fishery_founder_courses)")==10&&scalar("SELECT units FROM economy_stocks WHERE commodity='timber'")==10,"Ten real timber practice steps required before founding");
                    check(scalar("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")==9500,"Actual startup capital/money conservation required");
                    check(scalar("SELECT COUNT(*) FROM business_employment WHERE ended_step IS NULL")==2,"Real farm worker and new trained fishing worker required");
                    banks=new PaperFisheryWorkplaces(session,shore,getLogger(),clock::get);banks.runTaskTimer(FirstFisheryStudyProbe.this,1,1);clock.addAndGet(1000);stage=3;return;}
                if(stage==3){if(scalar("SELECT COALESCE(SUM(caught),0) FROM fishery_work")<4)return;
                    check(scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='fishing'")>=11,"Actual catch must earn fishing experience");
                    check(scalar("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")==9500,"Real money conserved after startup fee");banks.cancel();banks=null;
                    sites=session.fisheryWorksites(null,1);pending=sites;stage=20;return;}
                if(stage==20){site=sites.join().getFirst();recoveryTime=clock.addAndGet(1000);
                    support=new WaterSupportScan(new PaperWaterSupportSource(world),site.original(),world.getMinHeight(),world.getMaxHeight(),site.binding().minimumDepth());
                    var source=new PaperShorelineSource(world);var point=site.binding().point();
                    recovery=new ShorelineScan(p->p.equals(new ShorelineScan.Position(point.x(),point.y()-1,point.z()))?ShorelineScan.Cell.OTHER:source.cell(p),site.original(),site.binding().maximumRise());stage=21;return;}
                if(stage==21){if(!support.complete()){support.advance(128);return;}if(!recovery.complete()){recovery.advance(128);return;}
                    var decision=ShorelineRecovery.decide(site.binding().point(),site.binding().maximumDistance(),recovery.assessments());check(decision.action()==ShorelineRecovery.Action.RELOCATE,"Read-only damage fixture needs actual safe replacement");
                    pending=session.reconcileFisheryWorksite(site.binding().site(),new ShorelineEvidence(world.getUID(),site.binding().point(),recoveryTime,recovery.assessments()),support.observation(recoveryTime));stage=22;return;}
                if(stage==22){check(scalar("SELECT COUNT(*) FROM fishery_workplace_history WHERE action='RELOCATE'")==1&&scalar("SELECT SUM(unsafe) FROM fishery_workplaces")==0,"Persisted same-habitat recovery required");
                    check(scalar("SELECT COUNT(*) FROM businesses WHERE industry='fishing'")==1&&scalar("SELECT COUNT(*) FROM startup_grants")==1&&scalar("SELECT COUNT(*) FROM business_employment WHERE ended_step IS NULL")==2,"Recovery must retain firm/funding/jobs");
                    clock.addAndGet(1000);stage=23;return;}
                if(stage==23){if(scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period IS NOT NULL")<1||scalar("SELECT COUNT(*) FROM industry_runtime WHERE target<>completed OR last_ms<>"+clock.get())!=0)return;
                    check(scalar("SELECT units FROM economy_stocks WHERE commodity='food'")==8,"Paid real fish must enter public food reserve once");
                    check(scalar("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)")==9500,"Owned delivery must conserve real money");
                    pending=session.updateLoadedWorlds(Set.of());stage=4;return;}
                if(stage==4){units=scalar("SELECT SUM(available) FROM fishery_resources");cash=scalar("SELECT SUM(hundredths) FROM treasuries");work=scalar("SELECT COUNT(*) FROM fishery_work");skills=scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='fishing'");clock.addAndGet(10000);pause=ticks;stage=5;return;}
                if(stage==5){if(ticks-pause<45)return;check(units==scalar("SELECT SUM(available) FROM fishery_resources")&&cash==scalar("SELECT SUM(hundredths) FROM treasuries")&&work==scalar("SELECT COUNT(*) FROM fishery_work")&&skills==scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='fishing'"),"Absent-world catch/recovery/payment pause required");session.close();session=owner(true);pending=session.start(true,Set.of(world.getUID()));pause=ticks;stage=6;return;}
                if(stage==6){if(ticks-pause<40)return;check(units==scalar("SELECT SUM(available) FROM fishery_resources")&&cash==scalar("SELECT SUM(hundredths) FROM treasuries")&&work==scalar("SELECT COUNT(*) FROM fishery_work")&&skills==scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='fishing'"),"Restart must preserve settled baseline: fish "+units+"/"+scalar("SELECT SUM(available) FROM fishery_resources")+", cash "+cash+"/"+scalar("SELECT SUM(hundredths) FROM treasuries")+", work "+work+"/"+scalar("SELECT COUNT(*) FROM fishery_work")+", skills "+skills+"/"+scalar("SELECT SUM(experience) FROM resident_skills WHERE skill='fishing'"));
                    check(scalar("PRAGMA user_version")==70&&scalar("SELECT COUNT(*) FROM pragma_foreign_key_check")==0,"Healthy schema70 required");getLogger().info("NATIVE FIRST FISHERY STUDY PASS: zero initial fishing skill, actual first-owner timber practice and qualification; ordinary water observer, qualified startup, live exact water/bank proof, actual protected grant/trained worker/habitat, timed catch with earned experience, paid public food order/delivery/reserve, money conservation, persisted bank recovery using read-only damage mask and absence/restart; no block/entity changes");cancel();getServer().getPluginManager().disablePlugin(FirstFisheryStudyProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE AUTOMATIC FISHERY FAIL",failure);getServer().getPluginManager().disablePlugin(FirstFisheryStudyProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Automatic fishing fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(startup!=null)startup.cancel();if(banks!=null)banks.cancel();if(session!=null)session.close();if(ticket)world.removePluginChunkTicket(0,0,this);}
}
