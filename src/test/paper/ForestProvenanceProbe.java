import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.PaperForestObserver;
import com.blueninjas24.civilizations.paper.PaperForestSource;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

/** Marked disposable native test of read-only canopy evidence. Never ship. */
public final class ForestProvenanceProbe extends JavaPlugin {
    private final AtomicLong clock=new AtomicLong(1000);
    private final List<OreSurveyArea.Chunk> tickets=new ArrayList<>();
    private final Map<Location,BlockData> originals=new LinkedHashMap<>();
    private PopulationSession session;
    private PaperForestObserver observer;
    private World world;
    private UUID worldId;
    private boolean authorized;
    private int chunkX,chunkZ,rootY,ticks,stage;
    private CompletableFuture<Void> startup;

    private void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private boolean air(Material material){return material==Material.AIR||material==Material.CAVE_AIR||material==Material.VOID_AIR;}
    private VillageObservation.Position position(int x,int y,int z){return new VillageObservation.Position(x,y,z);}

    @Override public void onEnable(){
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim()
                    .equals("Disposable Civilizations Phase 1 test environment")
                    &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578
                    &&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture with zero players only");
            authorized=true;world=getServer().getWorlds().getFirst();worldId=world.getUID();
            var spawn=world.getSpawnLocation();chunkX=Math.floorDiv(spawn.getBlockX(),16);chunkZ=Math.floorDiv(spawn.getBlockZ(),16);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                var chunk=new OreSurveyArea.Chunk(chunkX+dx,chunkZ+dz);
                check(world.addPluginChunkTicket(chunk.x(),chunk.z(),this),"Fixture must own each explicit 3x3 chunk ticket");
                tickets.add(chunk);check(world.isChunkLoaded(chunk.x(),chunk.z()),"Ticketed fixture chunk must be loaded");
            }

            rootY=world.getMaxHeight()-12;
            int firstX=chunkX*16+3,secondX=chunkX*16+12,z=chunkZ*16+8;
            var modified=List.of(position(firstX,rootY,z),position(firstX,rootY+1,z),position(firstX,rootY+2,z),
                    position(firstX+1,rootY+2,z),position(secondX,rootY,z),position(secondX,rootY+1,z),
                    position(secondX,rootY+2,z),position(secondX+1,rootY+2,z),position(chunkX*16+7,rootY+4,z));
            for(var p:modified) {
                var block=world.getBlockAt(p.x(),p.y(),p.z());
                check(air(block.getType()),"Every forest probe edit must begin in verified AIR at high sky");
                originals.put(block.getLocation(),block.getBlockData().clone());
            }
            verticalLog(firstX,rootY,z);verticalLog(firstX,rootY+1,z);verticalLog(firstX,rootY+2,z);
            leaf(firstX+1,rootY+2,z,false);
            verticalLog(secondX,rootY,z);verticalLog(secondX,rootY+1,z);verticalLog(secondX,rootY+2,z);
            leaf(secondX+1,rootY+2,z,true);
            horizontalLog(chunkX*16+7,rootY+4,z);

            var homeA=position(firstX+2,rootY,z);var homeB=position(firstX+2,rootY+1,z+1);
            var personA=UUID.randomUUID();var personB=UUID.randomUUID();
            var observation=new VillageObservation(worldId,"Disposable forest observation fixture",1000,true,
                    List.of(homeA,homeB),List.of(new VillageObservation.Person(personA,homeA,true,"Fixture A"),
                    new VillageObservation.Person(personB,homeB,true,"Fixture B")));
            var runDirectory=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());
            session=new PopulationSession(runDirectory.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get);
            session.configureForestTracking();
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.ForestMutationTracker(session,getLogger()),this);
            startup=session.start(false,Set.of(worldId)).thenCompose(ignored->session.observe(observation)).thenApply(ignored->null);

            new BukkitRunnable(){
                @Override public void run(){try {
                    check(++ticks<=600,"Forest observation probe timed out");
                    if(!startup.isDone())return;
                    startup.join();
                    if(observer==null) {
                        var center=new OreSurveyArea.Chunk(chunkX,chunkZ);
                        var snapshot=world.getChunkAt(chunkX,chunkZ).getChunkSnapshot(false,false,false,false);
                        var source=new PaperForestSource(Map.of(center,snapshot),world.getMinHeight(),world.getMaxHeight());
                        check(source.kind(chunkX*16+4,rootY+2,chunkZ*16+8)==ForestScan.Kind.LEAF,
                                "Natural nonpersistent leaves classify as canopy evidence");
                        check(source.kind(chunkX*16+13,rootY+2,chunkZ*16+8)==ForestScan.Kind.OTHER,
                                "Persistent leaves cannot qualify as canopy evidence");
                        check(source.kind(chunkX*16+7,rootY+4,chunkZ*16+8)==ForestScan.Kind.OTHER,
                                "Horizontal logs cannot qualify as standing trunks");
                        int margin=ForestScan.Geometry.prototype().horizontalCanopyRadius();
                        var bounds=new ForestScan.Bounds(chunkX*16-margin,rootY-1,chunkZ*16-margin,
                                chunkX*16+16+margin,rootY+5,chunkZ*16+16+margin);
                        var centerOnly=new ForestScan(source,bounds,Set.of(),ForestScan.Geometry.prototype());
                        while(!centerOnly.complete())centerOnly.advance(4096);
                        check(!centerOnly.observation(worldId,1000).complete(),
                                "A center-only snapshot must remain incomplete when canopy margins are unknown");
                        observer=new PaperForestObserver(session,new ForestryObserverPolicy(true,0,8,512,20,
                                ForestScan.Geometry.prototype()),clock::get);
                        observer.runTaskTimer(ForestProvenanceProbe.this,1,1);
                        return;
                    }
                    var sample=observer.latestSample();if(sample.isEmpty())return;
                    observer.cancel();observer=null;
                    var saved=sample.orElseThrow();var town=session.settlementNames().keySet().stream().findFirst().orElseThrow();
                    var expected=List.of(position(chunkX*16+3,rootY,chunkZ*16+8),
                            position(chunkX*16+3,rootY+1,chunkZ*16+8),position(chunkX*16+3,rootY+2,chunkZ*16+8));
                    check(session.settlementNames().size()==1&&saved.town().equals(town),"Sample must identify the private saved town");
                    check(saved.chunkX()==chunkX&&saved.chunkZ()==chunkZ,"Sample must identify the saved home chunk");
                    check(saved.observation().observedMillis()==clock.get()&&saved.observation().complete(),
                            "Ordinary observer must preserve the fixed original capture time and complete evidence");
                    if(stage==0) {
                        check(saved.observation().trunks().equals(expected),"Initial natural canopy must remain recognizable");
                        var cancelled=new org.bukkit.event.world.StructureGrowEvent(world.getBlockAt(chunkX*16+12,rootY,chunkZ*16+8).getLocation(),TreeType.TREE,true,null,
                                List.of(world.getBlockAt(chunkX*16+12,rootY,chunkZ*16+8).getState()));
                        cancelled.setCancelled(true);getServer().getPluginManager().callEvent(cancelled);
                        var states=new ArrayList<org.bukkit.block.BlockState>();
                        for(var p:expected)states.add(world.getBlockAt(p.x(),p.y(),p.z()).getState());
                        getServer().getPluginManager().callEvent(new org.bukkit.event.world.StructureGrowEvent(states.getFirst().getLocation(),TreeType.TREE,true,null,states));
                        clock.set(1001);stage=1;return;
                    }
                    check(saved.observation().trunks().isEmpty(),"Saved native growth exclusions must prevent rediscovery of the original trunk");
                    check(session.forestDiscoveryReady(),"Accepted native captures must drain without losing tracking trust");
                    getLogger().info("NATIVE FOREST PROVENANCE PASS: native accepted growth saved exact exclusions; cancelled growth ignored; ordinary observer excluded protected trunks; clean drain trusted; no business founded");
                    cancel();getServer().getPluginManager().disablePlugin(ForestProvenanceProbe.this);getServer().shutdown();
                }catch(Exception failure){
                    cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FOREST PROVENANCE FAIL",failure);
                    getServer().getPluginManager().disablePlugin(ForestProvenanceProbe.this);if(authorized)getServer().shutdown();
                }}
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){
            getLogger().log(java.util.logging.Level.SEVERE,"Forest observation probe refused",failure);
            getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();
        }
    }

    private void verticalLog(int x,int y,int z) {
        var data=(Orientable)Material.OAK_LOG.createBlockData();data.setAxis(Axis.Y);
        world.getBlockAt(x,y,z).setBlockData(data,false);
    }
    private void horizontalLog(int x,int y,int z) {
        var data=(Orientable)Material.OAK_LOG.createBlockData();data.setAxis(Axis.X);
        world.getBlockAt(x,y,z).setBlockData(data,false);
    }
    private void leaf(int x,int y,int z,boolean persistent) {
        var data=(Leaves)Material.OAK_LEAVES.createBlockData();data.setPersistent(persistent);data.setDistance(1);
        world.getBlockAt(x,y,z).setBlockData(data,false);
    }
    private void restoreBlocks(){
        if(world==null)return;
        for(var entry:originals.entrySet()) {
            var location=entry.getKey();
            check(world.isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4),"Held fixture chunk must stay loaded for restoration");
            world.getBlockAt(location).setBlockData(entry.getValue(),false);
            check(world.getBlockAt(location).getBlockData().getAsString().equals(entry.getValue().getAsString()),"Fixture original block data must be restored exactly");
        }
        getLogger().info("FOREST FIXTURE BLOCKS RESTORED: "+originals.size());
        originals.clear();
    }
    private void releaseTickets(){
        if(world==null)return;
        for(var chunk:tickets)world.removePluginChunkTicket(chunk.x(),chunk.z(),this);
        tickets.clear();
    }
    @Override public void onDisable(){
        if(observer!=null)observer.cancel();
        try {restoreBlocks();}
        catch(RuntimeException failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FOREST PROVENANCE FAIL: restoration",failure);}
        finally {if(session!=null)session.close();releaseTickets();}
    }
}
