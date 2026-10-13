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

/** Normal owner timer against explicitly trusted test fields. No claim of natural field discovery. */
public final class GrainHarvestProbe extends JavaPlugin {
    private PopulationSession session;private World world;private Path data;private boolean authorized;
    private final AtomicLong clock=new AtomicLong(1000);
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private PopulationSession owner() {
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        var economy=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        var s=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get,economy,FarmObservation.Policy.prototype(),VillagePriorities.Policy.prototype(),OpeningFunds.prototype());
        s.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));s.configureGrainHarvest(GrainHarvestPolicy.prototype());s.configureWorkLearning(WorkLearningPolicy.prototype());return s;
    }
    private long[] snapshot()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+data.resolve("civilizations.db").toAbsolutePath().toString().replace((char)92,'/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery("""
                SELECT (SELECT completed_periods FROM economy_accounts),
                  COALESCE((SELECT units FROM grain_stocks),0),(SELECT units FROM economy_stocks WHERE commodity='food'),
                  (SELECT hundredths FROM treasuries),COALESCE((SELECT SUM(harvest) FROM grain_harvests),0),
                  COALESCE((SELECT SUM(food_output) FROM grain_harvests),0),COALESCE((SELECT SUM(grain_units) FROM grain_harvests),0),
                  COALESCE((SELECT SUM(experience) FROM resident_skills WHERE skill='farming'),0)
                """)){check(r.next(),"Consistent harvest checkpoint required");var values=new long[8];for(int i=0;i<8;i++)values[i]=r.getLong(i+1);return values;}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;world=getServer().getWorlds().getFirst();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner();
        var a=new VillageObservation.Position(0,64,0);var b=new VillageObservation.Position(2,64,0);
        var observation=new VillageObservation(world.getUID(),"Harvest fixture",1000,true,List.of(a,b),List.of(new VillageObservation.Person(UUID.randomUUID(),a,true,"Fixture A"),new VillageObservation.Person(UUID.randomUUID(),b,true,"Fixture B")));
        CompletableFuture<?> ready=session.start(true,Set.of(world.getUID())).thenCompose(v->session.observe(observation)).thenCompose(v->session.observeFarms(new FarmObservation(session.settlementNames().keySet().iterator().next(),world.getUID(),1000,true,32,new VillageObservation.Position(3,64,5),32L)));
        new BukkitRunnable(){int stage,ticks,pause;CompletableFuture<?> pending=ready;long[] held;
            @Override public void run(){try {
                check(++ticks<=1200,"Harvest fixture timeout stage "+stage);if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(stage==0){clock.set(2000);stage=1;return;}
                if(stage==1){var row=snapshot();if(row[0]<1)return;check(Arrays.equals(row,new long[]{1,2,26,2300,8,6,2,2}),"Actual future harvest split, paid startup and real skills required: "+Arrays.toString(row));held=row;pending=session.updateLoadedWorlds(Set.of());stage=2;return;}
                if(stage==2){clock.set(50000);pause=ticks;stage=3;return;}
                if(stage==3){if(ticks-pause<40)return;check(Arrays.equals(held,snapshot()),"Absent worlds cannot earn grain or skill");session.close();session=owner();pending=session.start(true,Set.of(world.getUID()));pause=ticks;stage=4;return;}
                if(stage==4){if(ticks-pause<40)return;check(Arrays.equals(held,snapshot()),"Restart cannot earn offline grain or repeat startup");clock.set(51000);stage=5;return;}
                if(stage==5){var row=snapshot();if(row[0]<2)return;check(Arrays.equals(row,new long[]{2,4,28,2300,16,12,4,4}),"Future resumed work must continue once: "+Arrays.toString(row));getLogger().info("NATIVE GRAIN HARVEST PASS: ordinary SQL-owner timer, two staffed workers, each8 future work split into6 food/2 grain, protected meals, actual2500 startup debit, earned farming skills, absent-world/restart pause and resumed work; explicit trusted crop fixture, no natural discovery claim, block/entity edits or chunk tickets");cancel();getServer().getPluginManager().disablePlugin(GrainHarvestProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE GRAIN HARVEST FAIL",failure);getServer().getPluginManager().disablePlugin(GrainHarvestProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Harvest fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(session!=null)session.close();}
}
