import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.PaperForestRecovery;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Full production forest adapter with a private DB and exactly restored disposable fixture. Never ship. */
public final class ForestRecoveryProbe extends JavaPlugin {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<VillageObservation.Position,BlockData> originals=new LinkedHashMap<>();
    private final List<String> messages=new ArrayList<>();
    private final List<VillageObservation.Position> supports=new ArrayList<>();
    private World world;private int cx,cz,ticks,stage;private boolean authorized,ticket;
    private UUID site,town,person,business;private Path folder;private PopulationSession session;private PaperForestRecovery adapter;
    private CompletableFuture<?> pending;
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked disposable zero-player localhost fixture required");
        authorized=true;world=getServer().getWorlds().getFirst();cx=Math.floorDiv(world.getSpawnLocation().getBlockX(),16);cz=Math.floorDiv(world.getSpawnLocation().getBlockZ(),16);
        ticket=world.addPluginChunkTicket(cx,cz,this);check(ticket,"Own explicit fixture-only chunk ticket");
        // All canopy margins stay in this fixture chunk, so production scan never loads chunks.
        int y=world.getMaxHeight()-16,z=cz*16+8;
        for(int x:new int[]{cx*16+4,cx*16+11}){
            for(int dy=0;dy<3;dy++)supports.add(new VillageObservation.Position(x,y+dy,z));
            save(new VillageObservation.Position(x+1,y+3,z));
        }
        for(var p:supports)save(p);
        for(var p:supports)if(!p.equals(supports.getFirst()))block(p).setType(Material.OAK_LOG,false);
        for(var p:originals.keySet())if(!supports.contains(p)){
            var leaf=(Leaves)Material.OAK_LEAVES.createBlockData();leaf.setPersistent(false);leaf.setDistance(1);block(p).setBlockData(leaf,false);
        }
        town=UUID.randomUUID();person=UUID.randomUUID();site=UUID.randomUUID();business=UUID.randomUUID();folder=getDataFolder().toPath().resolve("private-"+UUID.randomUUID());
        var worldId=world.getUID();var worldName=world.getName();
        pending=CompletableFuture.runAsync(()->{try{seed(worldId,worldName);}catch(Exception failure){throw new CompletionException(failure);}},worker);
        new BukkitRunnable(){@Override public void run(){try{advance();}catch(Exception|AssertionError failure){fail(failure);}}}.runTaskTimer(this,1,1);
    }catch(Exception|AssertionError failure){fail(failure);}}
    private void save(VillageObservation.Position p){check(world.isChunkLoaded(cx,cz)&&block(p).getType()==Material.AIR,"All fixture cells must start in AIR");originals.put(p,block(p).getBlockData().clone());}
    private org.bukkit.block.Block block(VillageObservation.Position p){return world.getBlockAt(p.x(),p.y(),p.z());}
    private void advance()throws Exception {
        check(++ticks<600,"Forest recovery timed out at stage "+stage);
        switch(stage){
            case 0->{if(!pending.isDone())return;pending.join();session=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);
                session.configureForestTracking();pending=session.start(false,Set.of(world.getUID()));stage=1;}
            case 1->{if(!pending.isDone())return;pending.join();check(session.forestCaptureReady()&&!session.forestDiscoveryReady(),"Healthy current capture with fenced history required");
                adapter=new PaperForestRecovery(this,session,ForestScan.Geometry.prototype());invoke("review "+site);stage=2;}
            case 2->{var line=messages.stream().filter(s->s.startsWith("Approve within120 seconds:")).findFirst();if(line.isEmpty())return;
                check(messages.stream().anyMatch(s->s.contains("6 supports; capacity 96 -> 48; standing timber 96 -> 48")),"Natural canopy must preserve one intact tree and exclude broken column habitat");
                invoke("confirm "+line.orElseThrow().substring(line.orElseThrow().lastIndexOf(' ')+1));stage=3;}
            case 3->{if(messages.stream().noneMatch(s->s.contains("standing timber 48; lost 48")))return;
                check(!session.forestDiscoveryReady(),"Recovery must not unlock new discovery");verify();
                getLogger().info("NATIVE FOREST RECOVERY PASS: six original supports completely reviewed; one missing trunk retired; canopy-backed habitat 96 -> 48; no refill/global unlock; private DB only");
                getServer().getPluginManager().disablePlugin(this);getServer().shutdown();stage=4;}
        }
    }
    private void invoke(String operation){
        var sender=(CommandSender)Proxy.newProxyInstance(CommandSender.class.getClassLoader(),new Class<?>[]{CommandSender.class},(proxy,method,args)->switch(method.getName()){
            case "hasPermission"->true;case "sendMessage"->{check(Bukkit.isPrimaryThread(),"Messages must return on server thread");messages.add((String)args[0]);yield null;}
            default->throw new AssertionError("Unexpected sender method "+method.getName());});
        adapter.onCommand(sender,getServer().getPluginCommand("civ"),"civ",("debug recover-forest "+operation).split(" "));
    }
    private void seed(UUID worldId,String worldName)throws Exception {
        var db=new DatabaseManager(folder.toFile(),getLogger());try{
            db.initialize();db.saveSettlement(new Settlement(town,"Private recovery forest",worldName,0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(worldId,worldName);db.getVillageIdentityRepository().bindSettlement(town,worldId);
            db.getPopulationRepository().insertResident(new Resident(person,"Fixture logger",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Private fixture funds");
            var p=supports.getFirst();var location=new BusinessSite(site,town,worldId,p.x(),p.y(),p.z(),1,"Private existing forest");
            var requirements=new JobRequirements(216,Map.of());var firm=new Business(business,town,"Private logger",person,person,"logging",site,1,0);
            db.getBusinessRepository().found(new BusinessFounding(firm,location,requirements,requirements,0,"Private opportunity"));
            db.getBusinessRepository().hire(UUID.randomUUID(),business,person,0);db.getIndustryRepository().openStore(business,10000,Map.of(Commodity.TIMBER,1L),1000);
            new ForestryRepository(db.getConnection()).open(site,new ForestObservation(worldId,1,true,supports),ForestryPolicy.prototype());
            var tracking=new ForestryTrackingRepository(db.getConnection());tracking.begin(2);tracking.finish(3,true);
        }finally{db.close();}
    }
    private void verify()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("civilizations.db").toUri()+"?mode=ro")){
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");}
            check(!new ForestryTrackingRepository(c).load().orElseThrow().trusted()&&new ForestRecovery(c).permitted(site),"Only selected forest approved");
            var saved=new ForestryRepository(c).find(site).orElseThrow();check(saved.stock().capacity()==48&&saved.stock().available()==48,"Standing timber must not refill");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT (SELECT COUNT(*) FROM forest_recoveries),(SELECT COUNT(*) FROM forestry_supports),(SELECT COUNT(*) FROM forestry_source_losses)")){
                check(r.next()&&r.getLong(1)==1&&r.getLong(2)==6&&r.getLong(3)==1,"One audited review; six identities preserved; only one trunk retired");}
        }
    }
    private void fail(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FOREST RECOVERY FAIL",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}
    @Override public void onDisable(){try{
        Bukkit.getScheduler().cancelTasks(this);if(session!=null)session.close();
        for(var e:originals.entrySet()){check(world.isChunkLoaded(cx,cz),"Fixture chunk stays loaded for restoration");block(e.getKey()).setBlockData(e.getValue(),false);
            check(block(e.getKey()).getBlockData().getAsString().equals(e.getValue().getAsString()),"Exact original state required");}
        if(!originals.isEmpty())getLogger().info("FOREST RECOVERY BLOCK RESTORATION PASS: all eight original AIR states restored");
    }finally{if(ticket&&world!=null)world.removePluginChunkTicket(cx,cz,this);worker.shutdown();}}
}
