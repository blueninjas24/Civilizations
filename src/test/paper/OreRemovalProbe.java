import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.OrePlacementTracker;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Test-only synthetic native events on restored real blocks, never a shipped plugin. */
public final class OreRemovalProbe extends JavaPlugin {
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();
    private final AtomicLong now=new AtomicLong(1000);
    private ExecutorService fixture;
    private PopulationSession session;
    private java.io.File folder;
    private World world;
    private List<Block> blocks;
    private List<VillageObservation.Position> positions;
    private UUID town,firm,deposit;
    private CompletableFuture<?> pending;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    ||!getServer().getIp().equals("127.0.0.1")||getServer().getPort()!=25578||!getServer().getOnlinePlayers().isEmpty())throw new IllegalStateException("Marked localhost disposable no-player server required");
            new BukkitRunnable() {
                int ticks;
                @Override public void run() {
                    if(++ticks>120){cancel();fail(new IllegalStateException("Console-loaded disposable chunk required; probe does not force chunks"));return;}
                    world=getServer().getWorlds().getFirst();
                    if(world.getLoadedChunks().length==0)return;
                    cancel();begin();
                }
            }.runTaskTimer(this,1,10);
        }catch(Exception failure){fail(failure);}
    }
    private void begin() {
        try {
            var chunk=world.getLoadedChunks()[0];int y=world.getMaxHeight()-8;
            blocks=java.util.stream.IntStream.range(3,7).mapToObj(x->chunk.getBlock(x,y,3)).toList();
            positions=blocks.stream().map(b->new VillageObservation.Position(b.getX(),b.getY(),b.getZ())).toList();
            for(var block:blocks){originals.put(block,block.getBlockData());block.setType(Material.COAL_ORE,false);}
            folder=new java.io.File(getDataFolder(),"run-"+UUID.randomUUID());
            var id=world.getUID();var name=world.getName();fixture=Executors.newSingleThreadExecutor();
            pending=CompletableFuture.runAsync(()->{try{seed(id,name);}catch(Exception failure){throw new CompletionException(failure);}},fixture);
            new BukkitRunnable() {
                int stage,ticks;
                @Override public void run() {
                    try {
                        if(++ticks>600)throw new IllegalStateException("Native removal probe timed out");
                        if(!pending.isDone())return;pending.join();
                        switch(stage++) {
                            case 0 -> {session=create();pending=session.start(false,Set.of(id));}
                            case 1 -> {
                                check(93);getServer().getPluginManager().registerEvents(new OrePlacementTracker(session,getLogger()),OreRemovalProbe.this);
                                var cancelled=new BlockBreakEvent(blocks.getFirst(),null);cancelled.setCancelled(true);getServer().getPluginManager().callEvent(cancelled);pending=barrier();
                            }
                            case 2 -> {check(93);getServer().getPluginManager().callEvent(new BlockBreakEvent(blocks.getFirst(),null));blocks.getFirst().setType(Material.AIR,false);pending=barrier();}
                            case 3 -> {check(70);getServer().getPluginManager().callEvent(new BlockPistonExtendEvent(blocks.getFirst(),List.of(blocks.get(1)),BlockFace.EAST));blocks.get(1).setType(Material.AIR,false);pending=barrier();}
                            case 4 -> {check(47);getServer().getPluginManager().callEvent(new BlockExplodeEvent(blocks.getFirst(),blocks.getFirst().getState(),List.of(blocks.get(2)),1,ExplosionResult.DESTROY));blocks.get(2).setType(Material.AIR,false);pending=barrier();}
                            case 5 -> {check(24);session.invalidateOreTracking("Injected native capture gap");now.set(2000);pending=session.pulse();}
                            case 6 -> {check(24);HandlerList.unregisterAll(OreRemovalProbe.this);session.close();now.set(1_000_000);session=create();pending=session.start(false,Set.of(id));}
                            case 7 -> {if(session.oreDiscoveryReady())throw new IllegalStateException("Restart repaired capture gap");now.set(1_001_000);pending=session.pulse();}
                            case 8 -> {check(24);getServer().getPluginManager().registerEvents(new OrePlacementTracker(session,getLogger()),OreRemovalProbe.this);getServer().getPluginManager().callEvent(new EntityExplodeEvent(null,blocks.get(3).getLocation(),List.of(blocks.get(3)),1,ExplosionResult.DESTROY_WITH_DECAY));blocks.get(3).setType(Material.AIR,false);pending=barrier();}
                            case 9 -> {check(0);session.close();session=null;restore();getLogger().info("NATIVE ORE REMOVAL PASS: cancelled break retained93, actual break70, piston origin47, block explosion24, immediate/restart gap paused production, entity explosion0; goods7/14 retained, restored blocks, no probe chunk tickets");cancel();getServer().getPluginManager().disablePlugin(OreRemovalProbe.this);}
                            default -> throw new IllegalStateException("Unexpected stage");
                        }
                    }catch(Exception failure){cancel();fail(failure);}
                }
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){fail(failure);}
    }
    private PopulationSession create() {
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var owner=new PopulationSession(folder,getLogger(),settings,now::get);owner.configureOreTracking();
        owner.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,2,100,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,2L),Map.of(Commodity.COAL,1L,Commodity.STONE,1L)),Map.of()));return owner;
    }
    private CompletableFuture<?> barrier(){return session.orePlacementsInChunk(world.getUID(),blocks.getFirst().getChunk().getX(),blocks.getFirst().getChunk().getZ(),512);}
    private void seed(UUID world,String name)throws Exception {
        town=UUID.randomUUID();firm=UUID.randomUUID();var person=UUID.randomUUID();var site=UUID.randomUUID();var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Removal fixture",name,positions.getFirst().x(),positions.getFirst().y(),positions.getFirst().z(),1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,name);db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Actual fixture capital");
            var p=positions.getFirst();var location=new BusinessSite(site,town,world,p.x(),p.y(),p.z(),1,"Explicit synthetic test evidence");var req=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(firm,town,"Mine",person,person,"mining",site,1,0),location,req,req,0,"Fixture opportunity"));
            db.getBusinessRepository().hire(UUID.randomUUID(),firm,person,0);db.getIndustryRepository().openStore(firm,10000,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),1000);
            deposit=new MineralDiscoveryRepository(db.getConnection()).discover(site,new MineralObservation(world,1,true,Commodity.COAL,positions),25,Map.of(Commodity.COAL,1L,Commodity.STONE,2L)).deposit().id();
            db.getIndustryRepository().mine(UUID.randomUUID(),firm,deposit,7,1,100,100);
        }finally{db.close();}
    }
    private void check(long remaining)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var q=c.createStatement()){q.execute("PRAGMA foreign_keys=ON");}
            var industry=new IndustryRepository(c);var stock=industry.store(firm).orElseThrow();
            if(industry.deposit(deposit).orElseThrow().remainingBatches()!=remaining||stock.units().get(Commodity.COAL)!=7||stock.units().get(Commodity.STONE)!=14)throw new IllegalStateException("Native source loss or output mismatch: expected "+remaining);
            if(new MineralSources(c).shares(deposit).stream().mapToLong(MineralSources.Share::remaining).sum()!=remaining)throw new IllegalStateException("Source sum mismatch");
            try(var q=c.createStatement();var r=q.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new IllegalStateException("Foreign keys failed");}
        }
    }
    private void restore(){originals.forEach((block,data)->block.setBlockData(data,false));originals.forEach((block,data)->{if(!block.getBlockData().equals(data))throw new IllegalStateException("Fixture block restoration failed");});originals.clear();}
    private void fail(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE ORE REMOVAL FAIL",failure);getServer().getPluginManager().disablePlugin(this);}
    @Override public void onDisable(){HandlerList.unregisterAll(this);if(session!=null){session.close();session=null;}if(!originals.isEmpty())restore();if(fixture!=null)fixture.shutdown();}
}
