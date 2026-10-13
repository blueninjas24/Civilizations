import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Normal owner timer and paid public food customers. Explicit test fields/funds; no automatic terrain claim. */
public final class FarmMarketProbe extends JavaPlugin {
    private PopulationSession session;private World world;private Path data;private boolean authorized;
    private final AtomicLong clock=new AtomicLong(1000);
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private PopulationSession owner() {
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        var economy=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        var s=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get,economy,FarmObservation.Policy.prototype(),VillagePriorities.Policy.prototype(),new OpeningFunds(true,10000));
        s.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));s.configureGrainHarvest(GrainHarvestPolicy.prototype());s.configureFarmMarket(FarmMarketPolicy.prototype());
        s.configureVillageSupplies(VillageSupplyPolicy.prototype());s.configureWorkLearning(WorkLearningPolicy.prototype());return s;
    }
    private long[] snapshot()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+data.resolve("civilizations.db").toAbsolutePath().toString().replace((char)92,'/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery("""
                SELECT (SELECT completed_periods FROM economy_accounts),(SELECT target_periods FROM economy_runtime),
                  (SELECT last_observed_millis FROM economy_runtime),(SELECT units FROM economy_stocks WHERE commodity='food'),
                  COALESCE((SELECT units FROM industry_stock WHERE commodity='food'),0),COALESCE((SELECT units FROM industry_stock WHERE commodity='grain'),0),
                  (SELECT hundredths FROM treasuries),COALESCE((SELECT hundredths FROM industry_stores),0),
                  COALESCE((SELECT SUM(actual_work) FROM farm_market_harvests),0),COALESCE((SELECT SUM(public_food) FROM farm_market_harvests),0),
                  COALESCE((SELECT SUM(units) FROM village_supply_orders WHERE applied_period IS NULL),0),
                  COALESCE((SELECT SUM(experience) FROM resident_skills WHERE skill='farming'),0)
                """)){check(r.next(),"Consistent checkpoint required");var values=new long[12];for(int i=0;i<values.length;i++)values[i]=r.getLong(i+1);return values;}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;world=getServer().getWorlds().getFirst();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner();
        var a=new VillageObservation.Position(0,64,0);var b=new VillageObservation.Position(2,64,0);
        var observation=new VillageObservation(world.getUID(),"Farm market fixture",1000,true,List.of(a,b),List.of(new VillageObservation.Person(UUID.randomUUID(),a,true,"Fixture A"),new VillageObservation.Person(UUID.randomUUID(),b,true,"Fixture B")));
        CompletableFuture<?> ready=session.start(true,Set.of(world.getUID())).thenCompose(v->session.observe(observation)).thenCompose(v->session.observeFarms(new FarmObservation(session.settlementNames().keySet().iterator().next(),world.getUID(),1000,true,32,new VillageObservation.Position(3,64,5),32L)));
        new BukkitRunnable(){int stage,ticks,pause;CompletableFuture<?> pending=ready;long[] held;
            @Override public void run(){try {
                check(++ticks<=1200,"Farm market fixture timeout stage "+stage);if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(stage==0){clock.set(2000);stage=1;return;}
                if(stage==1){var row=snapshot();if(row[0]!=row[1]||row[2]!=clock.get())return;
                    check(row[6]+row[7]==17500,"Real startup debit and cash conservation required");check(24+row[8]-row[0]*4==row[3]+row[4]+row[5]+row[10],"Public/private/cargo goods conservation required");
                    if(row[0]<6){clock.addAndGet(1000);return;}
                    check(Arrays.equals(row,new long[]{6,6,7000,7,28,12,17100,400,48,6,1,12}),"Actual six-period paid food loop differs: "+Arrays.toString(row));held=row;pending=session.updateLoadedWorlds(Set.of());stage=2;return;}
                if(stage==2){clock.set(50000);pause=ticks;stage=3;return;}
                if(stage==3){if(ticks-pause<40)return;var row=snapshot();for(int i=3;i<row.length;i++)check(held[i]==row[i],"Absent world changed stock/cash/work/learning");session.close();session=owner();pending=session.start(true,Set.of(world.getUID()));pause=ticks;stage=4;return;}
                if(stage==4){if(ticks-pause<40)return;var row=snapshot();for(int i=3;i<row.length;i++)check(held[i]==row[i],"Restart repeated harvest, startup, food payment or skill");clock.set(51000);stage=5;return;}
                if(stage==5){var row=snapshot();if(row[0]<7||row[0]!=row[1]||row[2]!=clock.get())return;check(row[6]+row[7]==17500&&row[8]==56&&row[11]==14,"Resumed work must conserve money and learn once");check(row[3]+row[4]+row[5]+row[10]==24+56-28,"Resumed goods must have one owner");
                    getLogger().info("NATIVE FARM MARKET PASS: ordinary SQL-owner harvest/customer timer, capped contributions, owned food/grain, two real paid200 food orders and delayed cargo, conserved17500 cash after2500 startup, actual skills, absence/restart and resumed work; explicit trusted fields and20000 initial fixture funds; no block/entity/chest edits or chunk tickets");cancel();getServer().getPluginManager().disablePlugin(FarmMarketProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FARM MARKET FAIL",failure);getServer().getPluginManager().disablePlugin(FarmMarketProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Farm market fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(session!=null)session.close();}
}
