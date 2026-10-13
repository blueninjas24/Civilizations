import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Guarded native SQL-owner scheduling test. No world blocks/entities/chunks are changed. */
public final class FisheryOpportunityProbe extends JavaPlugin {
    private Path folder;private UUID world,town,learner;private EconomySettings settings;
    private PopulationSession session;private ExecutorService executor;private boolean authorized;
    private void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private void setup()throws Exception {
        town=UUID.randomUUID();learner=UUID.randomUUID();var seller=UUID.randomUUID();
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L,Commodity.STONE,0L,Commodity.TIMBER,0L),
            Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            db.saveSettlement(new Settlement(town,"Native supply fixture","Fixture",0,64,0,2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(seller,learner))db.getPopulationRepository().insertResident(new Resident(person,"Explicit fixture adult",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture money");
            var food=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);food.configureStudy(StudyPolicy.prototype());
            food.observe(new FarmObservation(town,world,1000,true,0),1000);food.startOnline(1000,Set.of(world));food.pulse(2000,false);
            var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,10,"Explicit isolated supplier fixture");
            var b=new Business(UUID.randomUUID(),town,"Fixture supplier",seller,seller,"fishing",site.id(),2,0);var req=new JobRequirements(216,Map.of());
            new BusinessStartupGrants(db.getConnection()).startWorkshop(new BusinessStartupGrants.WorkshopRequest(
                new BusinessFounding(b,site,req,req,500,"Explicit supplier fixture startup"),100,Map.of(Commodity.FOOD,1L,Commodity.STONE,1L,Commodity.TIMBER,1L),1000,seller),StartupGrantPolicy.prototype());
            // Explicit test source only; application production never receives free goods.
            try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=10");s.execute("UPDATE economy_stocks SET units=12 WHERE commodity='food'");}
            food.enrollStudy(UUID.randomUUID(),learner,town,"mining",2000);
        }finally{db.close();}
    }
    private PopulationSession owner(boolean purchases) {
        var d=PopulationSession.Settings.prototype();var p=new PopulationSession.Settings(0,d.millisPerMonth(),d.maximumOfflineMonths(),d.monthsPerBatch(),d.population(),d.migration(),d.recognition(),d.enrollment());
        var owner=new PopulationSession(folder.toFile(),getLogger(),p,System::currentTimeMillis,settings,FarmObservation.Policy.prototype());
        owner.configureWater(WaterObserverPolicy.prototype());owner.configureFishing(FishingPolicy.prototype());owner.configureFisheryOpportunities(FisheryOpportunityPolicy.prototype());
        owner.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of()));owner.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
        owner.configureStudy(StudyPolicy.prototype());owner.configureVillageSupplies(purchases?VillageSupplyPolicy.prototype():VillageSupplyPolicy.disabled());return owner;
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){require(r.next(),"Missing result");return r.getLong(1);}
    }
    private long xp()throws Exception{return scalar("SELECT COALESCE((SELECT experience FROM resident_skills WHERE skill='mining'),0)");}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost server only");
        authorized=true;world=getServer().getWorlds().getFirst().getUID();folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();
        var init=CompletableFuture.runAsync(()->{try{setup();}catch(Exception failure){throw new CompletionException(failure);}},executor);
        new BukkitRunnable(){CompletableFuture<?> pending=init;int phase,ticks,pauseTick;long cash,orders,skill;
            @Override public void run(){try {
                if(++ticks>600)throw new IllegalStateException("Native customer timeout");
                if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(phase==0){session=owner(true);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==1){
                    if(xp()<1)return;
                    require(scalar("SELECT COUNT(*) FROM village_supply_orders WHERE applied_period IS NOT NULL")>=2,"Actual cargo arrival required");
                    require(scalar("SELECT COUNT(*) FROM village_supply_orders WHERE commodity='food'")>=1,"Healthy village must buy a real food reserve");
                    require(scalar("SELECT demand-fulfilled FROM needs_periods ORDER BY period DESC LIMIT 1")==0,"Reserve customer must precede a shortage");
                    require(scalar("SELECT SUM(hundredths) FROM treasuries")+scalar("SELECT SUM(hundredths) FROM industry_stores")==9500,"Money conservation after real fee");
                    require(scalar("SELECT SUM(units) FROM study_spending")>=2,"Actual practice materials consumed");
                    pending=session.currentFisheryOpportunities(town).thenAccept(r->require(r.candidates().isEmpty()&&r.explanation().contains("identity"),"Read-only first-firm gate must retain saved fishery identity"))
                        .thenCompose(v->session.updateLoadedWorlds(Set.of()));phase++;return;
                }
                if(phase==2){cash=scalar("SELECT SUM(hundredths) FROM treasuries");orders=scalar("SELECT COUNT(*) FROM village_supply_orders");skill=xp();pauseTick=ticks;phase++;return;}
                if(phase==3){if(ticks-pauseTick<45)return;require(cash==scalar("SELECT SUM(hundredths) FROM treasuries")&&orders==scalar("SELECT COUNT(*) FROM village_supply_orders")&&skill==xp(),"Absent world must pause purchases and study");
                    session.close();session=owner(false);pending=session.start(true,Set.of(world));phase++;return;}
                if(phase==4){if(xp()<=skill)return;require(orders==scalar("SELECT COUNT(*) FROM village_supply_orders"),"Restart with new orders disabled must preserve and deliver old cargo only");
                    require(scalar("PRAGMA user_version")==68,"Current schema required");require(scalar("SELECT COUNT(*) FROM pragma_foreign_key_check")==0,"Healthy foreign keys required");
                    getLogger().info("NATIVE FISHERY OPPORTUNITY PASS: read-only SQL-owner first-firm identity safeguard and ordinary timers bought real emergency food before shortage and practice goods, conserved cash, delivered/consumed cargo, paused absent world and resumed after restart with new orders disabled; no world changes");
                    cancel();getServer().getPluginManager().disablePlugin(FisheryOpportunityProbe.this);getServer().shutdown();
                }
            }catch(Exception error){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FISHERY OPPORTUNITY FAIL",error);getServer().getPluginManager().disablePlugin(FisheryOpportunityProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"Village fixture refused",error);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(session!=null)session.close();if(executor!=null)executor.shutdown();}
}
