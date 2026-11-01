import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;

/** Read-only production survey/planner on explicitly prepared and restored disposable terrain. */
public final class TerraceHousingSurveyProbe extends JavaPlugin {
    private final Map<Position,String> originals=new TreeMap<>();private World world;private boolean authorized,restored;
    private PaperHomeSiteSurvey survey;private PaperConstructionLibrary library;private Position need;private int phase,ticks;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();var chunk=world.getLoadedChunks()[0];
        // Test bootstrap only. Production survey/planner never loads chunks.
        for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++){var prepared=world.getChunkAt(chunk.getX()+dx,chunk.getZ()+dz);world.addPluginChunkTicket(prepared.getX(),prepared.getZ(),this);}
        int x=(chunk.getX()<<4)+8,z=(chunk.getZ()<<4)+8,y=world.getHighestBlockYAt(x,z);need=new Position(x,y+1,z);
        for(int xx=x-6;xx<=x+6;xx++)for(int zz=z-6;zz<=z+6;zz++)for(int yy=y;yy<=y+2;yy++){
            var at=new Position(xx,yy,zz);var block=world.getBlockAt(xx,yy,zz);require(yy==y?block.getType()==Material.GRASS_BLOCK:block.getType()==Material.AIR,"Natural empty terrace fixture required");originals.put(at,block.getBlockData().getAsString());block.setType(yy==y+2?Material.GRASS_BLOCK:Material.DIRT,false);
        }
        world.getBlockAt(x,y+2,z).setType(Material.DIRT_PATH,false);
        library=PaperConstructionLibrary.load((JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations"));survey=new PaperHomeSiteSurvey(world.getUID(),need,8,4,0,System.currentTimeMillis());
        new BukkitRunnable(){@Override public void run(){try{
            require(++ticks<600,"Terrace survey timeout");long now=System.currentTimeMillis();var frame=survey.advance(128,now);if(frame.isEmpty())return;
            var seen=frame.orElseThrow();require(seen.survey().complete(),"Complete two-pass terrace scene required");var result=review(seen,now,seen.survey());
            if(phase==0){require(result.selected().isEmpty(),"Legacy vertical coverage must refuse the missing raised roof");phase=1;survey=new PaperHomeSiteSurvey(world.getUID(),need,8,4,2,now);return;}
            require(result.selected().isPresent(),"Full raised terrace/roof must become a safe proposal: "+result.status());require(result.selected().orElseThrow().origin().y()==need.y()+2,"Actual higher terrace chosen");
            var protectedScene=new ConstructionSitePlanner.Survey(world.getUID(),seen.survey().capturedMillis(),true,true,seen.survey().cells(),List.of(seen.search()),seen.survey().entities());require(review(seen,now,protectedScene).selected().isEmpty(),"Protection still excludes the higher terrace");
            cancel();restore();getLogger().info("P4_TERRACE_SURVEY_PASS: legacy incomplete roof refuses; configured extra2 layers selects a complete home two blocks higher; protection refuses; no project/payment/build rights; all fixture terrain restored");getServer().shutdown();
        }catch(Throwable e){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_TERRACE_SURVEY_FAIL",e);restore();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"P4_TERRACE_SETUP_FAIL",e);restore();if(authorized)getServer().shutdown();}}
    private ConstructionSitePlanner.Result review(PaperHomeSiteSurvey.Frame frame,long now,ConstructionSitePlanner.Survey evidence){
        return ConstructionSitePlanner.select(library.templates().get("starter_home"),world.getUID(),need,evidence,new ApprovedArea(UUID.randomUUID(),world.getUID(),frame.search()),now,5000,42,new ConstructionLandPolicy(0),frame.terrain());
    }
    private void restore(){if(!authorized||restored||world==null)return;for(var e:originals.entrySet()){var p=e.getKey();require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
