import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import com.blueninjas24.civilizations.paper.PaperWaterObserver;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Guarded native scheduler/SQL-owner proof; test DB and ticket only, no block/entity changes. */
public final class WaterObserverProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperWaterObserver observer;
    private World world;
    private Path folder;
    private UUID town;
    private boolean authorized,ticket;
    private final AtomicLong clock=new AtomicLong(1000);
    private final WaterObserverPolicy policy=new WaterObserverPolicy(true,2,4096,20,0);
    private final Set<FisheryObservation.Column> expected=new HashSet<>();
    private void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    private PopulationSession owner(){var s=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get);s.configureWater(policy);s.configureFishing(FishingPolicy.prototype());return s;}
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){check(r.next(),"Missing SQL row");return r.getLong(1);}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture only");authorized=true;
        getServer().getScheduler().runTask(this,()->{try {
            world=getServer().getWorlds().getFirst();ticket=world.addPluginChunkTicket(0,0,this);var snapshot=world.getChunkAt(0,0).getChunkSnapshot(false,false,false,false);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                int y=world.getMaxHeight()-1;while(y>=world.getMinHeight()&&snapshot.getBlockType(x,y,z).isAir())y--;
                int depth=0;while(y>=world.getMinHeight()&&snapshot.getBlockType(x,y,z)==Material.WATER){depth++;y--;}
                if(depth>=2)expected.add(new FisheryObservation.Column(x,z));
            }
            folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner();var home=new VillageObservation.Position(0,64,0);
            var village=new VillageObservation(world.getUID(),"Disposable water fixture",1000,true,List.of(home,new VillageObservation.Position(2,64,0)),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture adult"),new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture second adult")));
            var startup=session.start(false,Set.of(world.getUID())).thenCompose(s->session.observe(village));
            new BukkitRunnable(){CompletableFuture<?> pending=startup;int ticks,stage;
                public void run(){try {
                    check(++ticks<=600,"Native water observer timeout");if(pending!=null&&!pending.isDone())return;if(pending!=null){pending.join();pending=null;}
                    if(stage==0){town=session.settlementNames().keySet().iterator().next();observer=new PaperWaterObserver(session,policy,getLogger(),clock::get);observer.runTaskTimer(WaterObserverProbe.this,1,1);stage=1;return;}
                    if(stage==1){if(scalar("SELECT COUNT(*) FROM water_surveys WHERE complete=1 AND captured_millis=1000")!=1)return;
                        check(scalar("SELECT COUNT(*) FROM water_columns")==expected.size(),"Native column count differs from independent reference");
                        pending=session.currentWaterOpportunities(town,100,512).thenAccept(r->check(new HashSet<>(r.columns().stream().map(com.blueninjas24.civilizations.database.WaterOpportunities.Candidate::position).toList()).equals(expected),"Native saved coordinates differ"));stage=2;return;}
                    if(stage==2){pending=session.currentFishingPatches(town,100,32,1).thenAccept(patches->{check(patches.size()==1,"Native connected fishery patch missing");var patch=patches.getFirst();check(patch.evidence().columns().size()==32&&expected.containsAll(patch.evidence().columns()),"Native patch not bounded by actual water evidence");});stage=6;return;}
                    if(stage==6){clock.set(2000);stage=3;return;}
                    if(stage==3){if(scalar("SELECT MAX(captured_millis) FROM water_surveys")!=2000)return;observer.cancel();observer=null;
                        check(scalar("PRAGMA user_version")==68,"Schema68 expected");check(scalar("SELECT COUNT(*) FROM businesses")==0&&scalar("SELECT COUNT(*) FROM fishery_resources")==0,"Water evidence invented assets");
                        pending=session.updateLoadedWorlds(Set.of()).thenCompose(v->session.currentWaterOpportunities(town,100,512)).thenAccept(r->check(r.columns().isEmpty(),"Absent world exposes candidates"));stage=4;return;}
                    if(stage==4){session.close();session=owner();pending=session.start(false,Set.of(world.getUID())).thenCompose(v->session.currentWaterOpportunities(town,100,512)).thenAccept(r->check(r.columns().size()==expected.size(),"Restart lost current water evidence"));stage=5;return;}
                    getLogger().info("NATIVE WATER OBSERVER PASS: ordinary scheduled full-height capture/SQL owner matched "+expected.size()+" independent columns; bounded 32-column connected patch selected; newer capture, absent-world pause and restart; schema68; no invented resources/businesses or block/entity changes");cancel();getServer().getPluginManager().disablePlugin(WaterObserverProbe.this);getServer().shutdown();
                }catch(Exception error){cancel();fail(error);}}
            }.runTaskTimer(this,1,1);
        }catch(Exception error){fail(error);}});
    }catch(Exception error){fail(error);}}
    private void fail(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WATER OBSERVER FAIL",error);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(session!=null)session.close();if(ticket){world.removePluginChunkTicket(0,0,this);ticket=false;}}
}
