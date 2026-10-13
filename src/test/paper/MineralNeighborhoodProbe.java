import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.MineralOpportunities;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Marked read-only terrain/ordinary observer proof. Helper-only tickets are released; never ship. */
public final class MineralNeighborhoodProbe extends JavaPlugin {
    private PopulationSession session;private PaperOreObserver observer;private World world;private boolean authorized;
    private final List<OreSurveyArea.Chunk> tickets=new ArrayList<>();
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private boolean air(Material m){return m==Material.AIR||m==Material.CAVE_AIR||m==Material.VOID_AIR;}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;world=getServer().getWorlds().getFirst();
        var snapshots=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();
        for(var c:List.of(new OreSurveyArea.Chunk(0,0),new OreSurveyArea.Chunk(-1,0),new OreSurveyArea.Chunk(1,0),new OreSurveyArea.Chunk(0,-1),new OreSurveyArea.Chunk(0,1))) {
            if(world.addPluginChunkTicket(c.x(),c.z(),this))tickets.add(c);check(world.isChunkLoaded(c.x(),c.z()),"Fixture snapshot chunk required");snapshots.put(c,world.getChunkAt(c.x(),c.z()).getChunkSnapshot(false,false,false,false));
        }
        var scan=OreScan.quarries(PaperOreNeighborhood.quarries(snapshots,0,0,world.getMinHeight(),world.getMaxHeight()),0,0,62,66,Set.of());
        session=new PopulationSession(getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile(),getLogger(),PopulationSession.Settings.prototype(),()->1000L);session.configureOreTracking();
        var home=new VillageObservation.Position(0,64,0);var observation=new VillageObservation(world.getUID(),"Neighborhood fixture",1000,true,List.of(home,new VillageObservation.Position(2,64,0)),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture A"),new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture B")));
        var ready=session.start(false,Set.of(world.getUID())).thenCompose(v->session.observe(observation));
        new BukkitRunnable(){int stage,ticks,cursor,found;final List<VillageObservation.Position> stone=new ArrayList<>(),sand=new ArrayList<>();CompletableFuture<MineralOpportunities.Review> review;
            @Override public void run(){try {
                check(++ticks<=1200,"Neighborhood fixture timeout stage "+stage);if(!ready.isDone())return;ready.join();
                if(stage==0){check(scan.advance(128)<=128,"Strict snapshot cell budget required");if(!scan.complete())return;stage=1;return;}
                if(stage==1){int used=0;while(cursor<1024&&used++<128){int y=62+cursor/256,x=cursor%16,z=cursor%256/16;cursor++;var type=world.getBlockAt(x,y,z).getType();boolean isStone=type==Material.STONE||type==Material.DEEPSLATE,isSand=type==Material.SAND||type==Material.RED_SAND;if(!isStone&&!isSand)continue;boolean visible=false;
                    for(var d:new int[][]{{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}})visible|=air(world.getBlockAt(x+d[0],y+d[1],z+d[2]).getType());
                    if(visible){if(found++<512)(isStone?stone:sand).add(new VillageObservation.Position(x,y,z));}}
                    if(cursor<1024)return;var actual=scan.observations(world.getUID(),1000);check(actual.getFirst().complete()==(found<=512)&&actual.getLast().complete()==(found<=512),"Real terrain completeness differs");check(actual.getFirst().ore().equals(stone.stream().sorted(java.util.Comparator.comparingInt(VillageObservation.Position::x).thenComparingInt(VillageObservation.Position::y).thenComparingInt(VillageObservation.Position::z)).toList())&&actual.getLast().ore().equals(sand.stream().sorted(java.util.Comparator.comparingInt(VillageObservation.Position::x).thenComparingInt(VillageObservation.Position::y).thenComparingInt(VillageObservation.Position::z)).toList()),"Snapshot quarry evidence differs from independent actual blocks");
                    observer=new PaperOreObserver(session,new OreObserverPolicy(true,4,256,20,0),()->1000L);observer.runTaskTimer(MineralNeighborhoodProbe.this,1,1);stage=2;return;}
                if(stage==2){if(!observer.diagnostic().startsWith("Ore survey: "))return;observer.cancel();observer=null;var town=session.settlementNames().keySet().iterator().next();review=session.currentMineralOpportunities(town,10000);stage=3;return;}
                if(stage==3){if(!review.isDone())return;check(review.join().status()==MineralOpportunities.Status.CURRENT,"Ordinary known-neighbor survey must remain current on SQL owner");getLogger().info("NATIVE MINERAL NEIGHBORHOOD PASS: real five-chunk snapshots, independently checked stone/sand positions "+found+", bounded reads, centre-only coordinates and ordinary ore observer/SQL-owner current evidence; missing neighbors stay unknown; no block/entity edits; helper-only tickets released");cancel();getServer().getPluginManager().disablePlugin(MineralNeighborhoodProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE MINERAL NEIGHBORHOOD FAIL",failure);getServer().getPluginManager().disablePlugin(MineralNeighborhoodProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Neighborhood fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(session!=null)session.close();for(var c:tickets)world.removePluginChunkTicket(c.x(),c.z(),this);}
}
