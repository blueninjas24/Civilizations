import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Guarded synthetic native handler test on restored blocks, not a gameplay plugin or automatic quarry. */
public final class QuarryMutationProbe extends JavaPlugin {
    private Path folder;private World world;private Chunk chunk;private boolean ticket,authorized;
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();
    private List<Block> blocks;private List<Position> positions;private UUID business,deposit;
    private ExecutorService executor;private PopulationSession session;private OrePlacementTracker tracker;
    private void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private void setup(UUID id)throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            var town=UUID.randomUUID();var person=UUID.randomUUID();var site=UUID.randomUUID();business=UUID.randomUUID();var p=positions.getFirst();
            db.saveSettlement(new Settlement(town,"Quarry mutation fixture","Fixture",p.x(),p.y(),p.z(),1,0,0));
            db.getVillageIdentityRepository().observeWorld(id,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,id);
            db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture funds");
            var location=new BusinessSite(site,town,id,p.x(),p.y(),p.z(),1,"Explicit isolated quarry evidence");var req=new JobRequirements(216,Map.of());
            var b=new Business(business,town,"Fixture quarry",person,person,"quarry",site,1,0);
            var started=new MiningStartupRepository(db.getConnection()).start(new MiningStartupRepository.Request(
                new BusinessFounding(b,location,req,req,0,"Explicit native test opportunity"),new MineralObservation(id,1,true,Commodity.STONE,positions),
                20,Map.of(Commodity.STONE,1L),100,Map.of(Commodity.STONE,1L),0,person));
            deposit=started.discovery().deposit().id();db.getIndustryRepository().mine(UUID.randomUUID(),business,deposit,6,1,6,32);
        }finally{db.close();}
    }
    private CompletableFuture<?> barrier(){return session.orePlacementsInChunk(world.getUID(),chunk.getX(),chunk.getZ(),512);}
    private void check(long remaining)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            var industry=new IndustryRepository(c);require(industry.deposit(deposit).orElseThrow().remainingBatches()==remaining,"Unexpected reserve loss");
            require(industry.store(business).orElseThrow().units().get(Commodity.STONE)==6,"Already extracted goods changed");
            require(new MineralSources(c).shares(deposit).stream().mapToLong(MineralSources.Share::remaining).sum()==remaining,"Coordinate shares do not conserve reserve");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Foreign keys failed");}
        }
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture only");
        authorized=true;world=getServer().getWorlds().getFirst();chunk=world.getChunkAt(0,0);ticket=chunk.addPluginChunkTicket(this);int y=world.getMaxHeight()-12;
        // Only explicit test setup loads/tickets a chunk. Application adapters never do.
        blocks=List.of(chunk.getBlock(4,y,12),chunk.getBlock(6,y,12),chunk.getBlock(8,y,12));
        for(var block:blocks){var old=block.getBlockData().clone();require(old.getMaterial().isAir(),"Refuse to replace terrain or player blocks");originals.put(block,old);}
        blocks.forEach(b->b.setType(Material.STONE,false));positions=blocks.stream().map(b->new Position(b.getX(),b.getY(),b.getZ())).toList();
        folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();var id=world.getUID();
        var init=CompletableFuture.runAsync(()->{try{setup(id);}catch(Exception failure){throw new CompletionException(failure);}},executor);
        new BukkitRunnable(){CompletableFuture<?> pending=init;int phase,ticks;
            @Override public void run(){try {
                if(++ticks>600)throw new IllegalStateException("Native quarry test timed out");if(!pending.isDone())return;pending.join();
                switch(phase++) {
                    case 0->{session=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);pending=session.start(false,Set.of(id));}
                    case 1->{check(54);tracker=new OrePlacementTracker(session,getLogger());var event=new BlockBreakEvent(blocks.getFirst(),null);event.setCancelled(true);tracker.onBreak(event);pending=barrier();}
                    case 2->{check(54);tracker.onBreak(new BlockBreakEvent(blocks.getFirst(),null));blocks.getFirst().setType(Material.AIR,false);pending=barrier();}
                    case 3->{check(36);tracker.onExtend(new BlockPistonExtendEvent(blocks.getFirst(),List.of(blocks.get(1)),BlockFace.EAST));blocks.get(1).setType(Material.AIR,false);pending=barrier();}
                    case 4->{check(18);tracker.onEntityChange(new EntityChangeBlockEvent(null,blocks.get(2),Material.AIR.createBlockData()));blocks.get(2).setType(Material.AIR,false);pending=barrier();}
                    case 5->{check(0);tracker.onEntityChange(new EntityChangeBlockEvent(null,blocks.getFirst(),Material.SAND.createBlockData()));blocks.getFirst().setType(Material.SAND,false);pending=barrier();}
                    case 6->{check(0);var state=blocks.get(2).getState();state.setType(Material.STONE);tracker.onForm(new BlockFormEvent(blocks.get(2),state));blocks.get(2).setType(Material.STONE,false);pending=barrier();}
                    case 7->{check(0);var excluded=(Collection<?>)pending.join();require(excluded.contains(positions.getFirst())&&excluded.contains(positions.get(2)),"Moved/generated destinations not excluded");
                        var snapshot=chunk.getChunkSnapshot(false,false,false,false);var source=PaperOreSource.quarries(snapshot,world.getMinHeight(),world.getMaxHeight());
                        require(source.kind(4,y,12)==OreScan.Kind.SAND&&source.kind(8,y,12)==OreScan.Kind.STONE,"Native quarry snapshot classification failed");
                        require(new PaperOreSource(snapshot,world.getMinHeight(),world.getMaxHeight()).kind(8,y,12)==OreScan.Kind.SOLID,"Default ore snapshot behavior changed");
                        session.close();session=null;restore();getLogger().info("NATIVE QUARRY MUTATION PASS: actual Paper blocks/snapshots and native handler entry points; cancelled54, break36, piston18, entity removal0; moved/generated destinations excluded; extracted6 retained; three fixture blocks restored");
                        cancel();getServer().getPluginManager().disablePlugin(QuarryMutationProbe.this);getServer().shutdown();}
                    default->throw new IllegalStateException("Unexpected test phase");
                }
            }catch(Exception error){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE QUARRY MUTATION FAIL",error);getServer().getPluginManager().disablePlugin(QuarryMutationProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"Quarry fixture refused",error);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    private void restore(){originals.forEach((b,d)->b.setBlockData(d,false));originals.forEach((b,d)->require(b.getBlockData().getAsString().equals(d.getAsString()),"Fixture block restoration failed"));originals.clear();}
    @Override public void onDisable(){if(session!=null)session.close();if(!originals.isEmpty())restore();if(ticket){chunk.removePluginChunkTicket(this);ticket=false;}if(executor!=null)executor.shutdown();}
}
