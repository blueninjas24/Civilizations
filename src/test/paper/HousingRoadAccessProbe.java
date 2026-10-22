import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only joint planning on an actual five-cell marked disposable fixture; restores every change. */
public final class HousingRoadAccessProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();private final Map<Position,String> originals=new TreeMap<>();
    private World world;private Position need;private PaperHomeSiteSurvey survey;private PaperConstructionLibrary library;
    private CompletableFuture<String> checking;private boolean authorized,restored;private int ticks;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();
        for(var chunk:world.getLoadedChunks())world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        library=PaperConstructionLibrary.load((JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations"));
        new BukkitRunnable(){@Override public void run(){try{
            require(++ticks<1200,"Joint planning timeout");
            if(survey==null&&!startSurvey())return;
            if(checking==null){var frame=survey.advance(128,System.currentTimeMillis());if(frame.isEmpty())return;require(frame.get().survey().complete(),"Actual two-pass loaded scene required");var saved=frame.get();var worldId=world.getUID();checking=CompletableFuture.supplyAsync(()->{try{return inspect(saved,worldId);}catch(Exception failure){throw new CompletionException(failure);}},owner);return;}
            if(!checking.isDone())return;getLogger().info(checking.join());cancel();restore();getServer().shutdown();
        }catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_JOINT_ROAD_HOME_FAIL",failure);restore();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_JOINT_ROAD_HOME_SETUP_FAIL",failure);restore();if(authorized)getServer().shutdown();}}
    private boolean startSurvey(){
        Chunk chosen=null;
        outer:for(var chunk:world.getLoadedChunks()){
            // The17x17 frame centered at local8 reaches exactly this loaded2x2 neighborhood.
            for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++)if(!world.isChunkLoaded(chunk.getX()+dx,chunk.getZ()+dz)||!world.getChunkAt(chunk.getX()+dx,chunk.getZ()+dz).isEntitiesLoaded())continue outer;
            chosen=chunk;break;
        }
        if(chosen==null)return false; // Startup entity activation may finish on a later tick; never load it ourselves.
        int x=(chosen.getX()<<4)+8,z=(chosen.getZ()<<4)+8,y=world.getHighestBlockYAt(x,z);need=new Position(x,y+1,z);
        var road=new Position(x,y,z);remember(road);require(world.getBlockAt(x,y,z).getType()==Material.GRASS_BLOCK,"Natural fixture ground required");world.getBlockAt(x,y,z).setType(Material.DIRT_PATH,false);
        for(var at:List.of(new Position(x+2,y+1,z),new Position(x-2,y+1,z),new Position(x,y+1,z+2),new Position(x,y+1,z-2))){remember(at);require(world.getBlockAt(at.x(),at.y(),at.z()).getType()==Material.AIR,"Empty fixture headroom required");world.getBlockAt(at.x(),at.y(),at.z()).setType(Material.OAK_FENCE,false);}
        survey=new PaperHomeSiteSurvey(world.getUID(),need,8,4,System.currentTimeMillis());return true;
    }
    private String inspect(PaperHomeSiteSurvey.Frame frame,UUID worldId)throws Exception {
        var folder=getDataFolder().toPath().resolve("private-joint");require(!Files.exists(folder),"Fresh private fixture save required");
        var db=new DatabaseManager(folder.toFile(),getLogger());try{db.initialize();db.getVillageIdentityRepository().observeWorld(worldId,"Explicit joint-planning fixture");var c=db.getConnection();
            var clear=new ConstructionLandRepository(c).protectedColumns(worldId,frame.search());require(clear.isEmpty(),"Initially empty saved protection fixture");
            var result=review(frame,clear,worldId);require(result.status()==HousingRoadAccessPlanner.Status.READY&&result.expansion().isPresent(),"Actual world joint candidate: "+result.status());
            require(result.infill().selected().isEmpty()&&!result.rejected().isEmpty(),"Existing obstructed infill refused and alternatives explained");var plan=result.expansion().orElseThrow();
            var footprint=ConstructionPreview.footprint(library.templates().get("starter_home"),plan.homeTransform(),plan.homeOrigin());
            for(var stage:plan.road().template().stages())for(var local:stage.blocks().keySet()){var at=plan.road().origin().plus(local);require(!(at.x()>=footprint.minimum().x()&&at.x()<=footprint.maximum().x()&&at.z()>=footprint.minimum().z()&&at.z()<=footprint.maximum().z()),"Proposed paving never crosses its future home");}
            UUID actor=UUID.randomUUID();var excluded=new BuildingRegistry.Exclusion(UUID.randomUUID(),worldId,actor,new BuildingLayout.Bounds(frame.search().minimum(),frame.search().maximum()),System.currentTimeMillis());new BuildingRegistry(c).exclude(excluded,BuildingRegistry.Authorization.playerDeclaration(actor,true));
            var protectedResult=review(frame,new ConstructionLandRepository(c).protectedColumns(worldId,frame.search()),worldId);require(protectedResult.expansion().isEmpty(),"Current SQL protection overrides earlier clear native evidence");
            for(String table:List.of("construction_drafts","construction_site_reservations","construction_supply_orders","construction_block_journal","residents"))try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){require(r.getInt(1)==0,"Read-only planning created no "+table);}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Healthy joint save");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Valid joint foreign keys");}
            return "P4_JOINT_ROAD_HOME_PASS: real two-pass Paper scene and SQL protection; obstructed infill rejected, safe conditional home/road found with "+plan.road().template().blockCount()+" paving cells, "+plan.road().template().materials()+" and "+plan.road().template().work()+" earned work; road avoids future house; new protection refuses; no project/order/intent/resident created.";
        }finally{db.close();}
    }
    private HousingRoadAccessPlanner.Result review(PaperHomeSiteSurvey.Frame frame,Set<ConstructionLandRepository.Column> protectedColumns,UUID worldId){
        var cells=new HashMap<Position,Cell>();frame.survey().cells().forEach((at,cell)->cells.put(at,new Cell(cell.air(),cell.stableSupport(),cell.road(),cell.protectedWork()||protectedColumns.contains(new ConstructionLandRepository.Column(at.x(),at.z())))));
        var saved=new ConstructionSitePlanner.Survey(worldId,frame.survey().capturedMillis(),true,true,cells,frame.survey().protections(),frame.survey().entities());
        return HousingRoadAccessPlanner.select(library.templates().get("starter_home"),library.templates().get("starter_path"),worldId,need,saved,new ApprovedArea(UUID.randomUUID(),worldId,frame.search()),new ConstructionLandPolicy(0),frame.terrain(),LocalRoadAccessPlanner.Policy.prototype(),System.currentTimeMillis(),5000,7);
    }
    private void remember(Position at){require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Fixture cannot load chunks");originals.put(at,world.getBlockAt(at.x(),at.y(),at.z()).getBlockData().getAsString());}
    private void restore(){if(!authorized||restored||world==null)return;for(var e:originals.entrySet()){var at=e.getKey();require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(at.x(),at.y(),at.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();owner.shutdown();}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
