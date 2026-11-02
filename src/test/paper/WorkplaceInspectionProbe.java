import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.paper.PaperWorkplaceSurvey;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;

/** Explicit restored disposable shelter; production reader/inspection perform no world writes or chunk loading. */
public final class WorkplaceInspectionProbe extends JavaPlugin {
    private boolean authorized,restored;private World world;private final Map<Position,String> originals=new TreeMap<>();
    private BuildingLayout layout;private Position origin,marker;private PaperWorkplaceSurvey survey;private int phase,ticks;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();var chunk=world.getLoadedChunks()[0];
        // Fixture retention only: keep this already loaded disposable chunk present through the test.
        world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        int x=(chunk.getX()<<4)+4,z=(chunk.getZ()<<4)+4,y=world.getHighestBlockYAt(x,z);origin=new Position(x,y,z);marker=origin.plus(new Position(3,1,2));
        for(int dx=0;dx<5;dx++)for(int dz=0;dz<5;dz++)for(int dy=0;dy<4;dy++){
            var at=origin.plus(new Position(dx,dy,dz));var block=world.getBlockAt(at.x(),at.y(),at.z());require(dy==0?block.getType()==Material.GRASS_BLOCK:block.getType()==Material.AIR,"Empty natural fixture required");originals.put(at,block.getBlockData().getAsString());
        }
        for(var at:originals.keySet())set(at,at.y()==y||at.y()==y+3?Material.COBBLESTONE:Material.AIR);set(marker,Material.SMITHING_TABLE);
        var bounds=new BuildingLayout.Bounds(origin,origin.plus(new Position(4,3,4)));layout=new BuildingLayout(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),BuildingLayout.Kind.WORKPLACE,bounds,bounds,origin.plus(new Position(0,1,2)),List.of(),List.of());
        survey=new PaperWorkplaceSurvey(layout,marker,System.currentTimeMillis());
        new BukkitRunnable(){public void run(){try{
            require(++ticks<400,"Workplace inspection timeout");long now=System.currentTimeMillis();var frame=survey.advance(32,now);if(frame.isEmpty())return;
            var assessment=WorkplaceInspection.inspect(layout,frame.orElseThrow(),now,5000,WorkplaceInspection.Policy.prototype());
            if(phase==0){
                require(assessment.suitable(),"Native covered entrance-to-marker access must pass: "+assessment.issues());
                survey=new PaperWorkplaceSurvey(layout,marker,now);require(survey.advance(1,now).isEmpty(),"One-read budget must not complete structure");set(marker,Material.CRAFTING_TABLE);phase=1;return;
            }
            if(phase==1){
                require(assessment.issues().contains(WorkplaceInspection.Issue.MARKER_UNVERIFIED),"Mid-survey marker replacement must refuse");set(marker,Material.SMITHING_TABLE);
                for(int dz=0;dz<5;dz++)for(int dy=1;dy<3;dy++)set(origin.plus(new Position(2,dy,dz)),Material.COBBLESTONE);
                phase=2;survey=new PaperWorkplaceSurvey(layout,marker,now);return;
            }
            if(phase==2){
                require(assessment.issues().contains(WorkplaceInspection.Issue.WORK_UNREACHABLE),"Blocked work route must refuse");
                for(int dz=0;dz<5;dz++)for(int dy=1;dy<3;dy++)set(origin.plus(new Position(2,dy,dz)),Material.AIR);
                for(int dx=0;dx<5;dx++)for(int dz=0;dz<5;dz++)set(origin.plus(new Position(dx,3,dz)),Material.AIR);
                phase=3;survey=new PaperWorkplaceSurvey(layout,marker,now);return;
            }
            require(assessment.issues().contains(WorkplaceInspection.Issue.WORK_UNCOVERED),"Missing work shelter must refuse under covered policy");
            cancel();restore();getLogger().info("P4_WORKPLACE_INSPECTION_PASS: covered real access passes; one-read budget, changed marker, blocked route and missing shelter checks pass; all100 captured cells restored; no reservation, capacity or inventory rights created");getServer().shutdown();
        }catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_WORKPLACE_INSPECTION_FAIL",failure);try{restore();}finally{getServer().shutdown();}}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_WORKPLACE_INSPECTION_SETUP_FAIL",failure);try{restore();}finally{if(authorized)getServer().shutdown();}}}
    private void set(Position at,Material type){require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Fixture cannot load chunks");world.getBlockAt(at.x(),at.y(),at.z()).setType(type,false);}
    private void restore(){if(!authorized||restored||world==null)return;for(var e:originals.entrySet()){var p=e.getKey();require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
