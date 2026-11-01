import com.blueninjas24.civilizations.application.PopulationSession.HousingPage;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Disposable native read-only scheduling proof; no registered houses, people, capacity or blocks created. */
public final class HousingQueueIsolationProbe extends JavaPlugin {
    private PaperHousingRuntime runtime;private boolean authorized;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        var world=getServer().getWorlds().getFirst();var chunk=world.getLoadedChunks()[0];
        var at=new Position((chunk.getX()<<4)+4,world.getMinHeight()+8,(chunk.getZ()<<4)+4);
        var first=layout(world.getUID(),at);var second=layout(world.getUID(),at);var third=layout(world.getUID(),at);
        var cursor=UUID.randomUUID();var pages=new ArrayList<UUID>();var reviews=new ArrayList<UUID>();
        var owner=new PaperHousingRuntime.Owner(){
            public CompletableFuture<HousingPage> page(UUID after){pages.add(after);return CompletableFuture.completedFuture(after==null?new HousingPage(List.of(first,second),cursor):new HousingPage(List.of(third),null));}
            public CompletableFuture<DwellingHousing.Report> review(BuildingLayout home,DwellingInspection.Observation observation){
                require(getServer().isPrimaryThread()&&observation.world().equals(world.getUID()),"Native observation on server thread required");reviews.add(home.id());
                if(home.equals(first))return CompletableFuture.failedFuture(new BuildingRegistry.Declined("Synthetic late permission refusal"));
                return CompletableFuture.completedFuture(new DwellingHousing.Report(DwellingOccupancy.Outcome.UNVERIFIED,0,0,"Read-only fixture; no housing authority"));
            }
        };
        runtime=new PaperHousingRuntime(owner,new PaperHousingRuntime.Settings(true,32,15),(home,now)->new PaperDwellingSurvey(home,now,true)::advance,System::currentTimeMillis,getLogger());
        new BukkitRunnable(){int ticks;public void run(){try{
            require(++ticks<200,"Housing queue timeout");runtime.run();
            if(reviews.size()<3)return;
            require(reviews.equals(List.of(first.id(),second.id(),third.id())),"Later homes must proceed exactly once");
            require(pages.size()==2&&pages.getFirst()==null&&cursor.equals(pages.getLast()),"Original pagination must continue");
            cancel();runtime.close();getLogger().info("P4_HOUSING_QUEUE_PASS: native two-pass observations continue after local owner refusal; queued homes and next-page cursor preserved; no blocks, housing, funds or SQL records changed");getServer().shutdown();
        }catch(Throwable failure){cancel();runtime.close();getLogger().log(java.util.logging.Level.SEVERE,"P4_HOUSING_QUEUE_FAIL",failure);getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_HOUSING_QUEUE_SETUP_FAIL",failure);if(authorized)getServer().shutdown();}}
    private BuildingLayout layout(UUID world,Position at){
        var bounds=new BuildingLayout.Bounds(at,at.plus(new Position(3,2,3)));var door=at.plus(new Position(1,1,0));
        return new BuildingLayout(UUID.randomUUID(),UUID.randomUUID(),world,BuildingLayout.Kind.HOUSING,bounds,bounds,door,List.of(),List.of(new BuildingLayout.Unit(UUID.randomUUID(),door,List.of(bounds),List.of(new BuildingLayout.Bed(at.plus(new Position(1,1,1)),Direction.EAST)))));
    }
    @Override public void onDisable(){if(runtime!=null)runtime.close();}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
