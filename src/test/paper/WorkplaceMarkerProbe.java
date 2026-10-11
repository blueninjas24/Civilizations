import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import com.blueninjas24.civilizations.paper.PaperWorkplaceObserver;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Temporary two-block fixture under marker/localhost/no-player guards; never ship. */
public final class WorkplaceMarkerProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperWorkplaceObserver observer;
    private World world;
    private Chunk chunk;
    private Path folder;
    private UUID town;
    private int y;
    private BlockData first,second;
    private boolean placed,ticket;
    private final AtomicLong clock=new AtomicLong(1000);
    private final WorkplaceObserverPolicy policy=new WorkplaceObserverPolicy(true,1,512,20,0,Set.of(WorkplaceObservation.Kind.BARREL,WorkplaceObservation.Kind.SMITHING_TABLE));
    private void check(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    private PopulationSession owner(){var s=new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get);s.configureWorkplaces(policy);return s;}
    private void restore() {
        if(placed){world.getBlockAt(4,y,4).setBlockData(first,false);world.getBlockAt(5,y,4).setBlockData(second,false);placed=false;}
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){check(r.next(),"Missing SQL row");return r.getLong(1);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture required");
            world=getServer().getWorlds().getFirst();y=world.getMaxHeight()-4;
            // Explicit helper setup only: keep one disposable chunk loaded, then release it.
            chunk=world.getChunkAt(0,0);ticket=chunk.addPluginChunkTicket(this);
            first=world.getBlockAt(4,y,4).getBlockData().clone();second=world.getBlockAt(5,y,4).getBlockData().clone();check(first.getMaterial().isAir()&&second.getMaterial().isAir(),"Refuse to replace existing terrain/containers");
            placed=true;world.getBlockAt(4,y,4).setType(Material.BARREL,false);world.getBlockAt(5,y,4).setType(Material.SMITHING_TABLE,false);
            // Own freshly created test barrel; production observer never uses inventory APIs.
            ((org.bukkit.block.Barrel)world.getBlockAt(4,y,4).getState()).getInventory().setItem(0,new org.bukkit.inventory.ItemStack(Material.WHEAT,3));
            folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=owner();
            var home=new VillageObservation.Position(0,y,0);var village=new VillageObservation(world.getUID(),"Disposable fixture",1000,true,List.of(home,new VillageObservation.Position(2,y,0)),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture adult"),new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture second adult")));
            var startup=session.start(false,Set.of(world.getUID())).thenCompose(s->session.observe(village));
            new BukkitRunnable() {
                CompletableFuture<?> pending=startup;int ticks,stage;
                public void run(){try {
                    if(++ticks>600)throw new IllegalStateException("Workplace probe timeout");if(pending!=null&&!pending.isDone())return;
                    if(pending!=null){pending.join();pending=null;}
                    if(stage==0){town=session.settlementNames().keySet().iterator().next();observer=new PaperWorkplaceObserver(session,policy,clock::get);observer.runTaskTimer(WorkplaceMarkerProbe.this,1,1);stage=1;return;}
                    if(stage==1) {
                        if(scalar("SELECT COUNT(*) FROM workplace_markers")!=2)return;
                        check(world.getBlockAt(4,y,4).getType()==Material.BARREL&&world.getBlockAt(5,y,4).getType()==Material.SMITHING_TABLE,"Observer altered markers");
                        check(((org.bukkit.block.Barrel)world.getBlockAt(4,y,4).getState()).getInventory().getItem(0).getAmount()==3,"Observer consumed contents");
                        pending=session.currentWorkplaceOpportunities(town,100,512).thenAccept(r->check(r.candidates().size()==2,"Scheduled owner candidates missing"));stage=2;return;
                    }
                    if(stage==2){restore();clock.set(2000);stage=3;return;}
                    if(stage==3) {
                        if(scalar("SELECT MAX(captured_millis) FROM workplace_surveys")!=2000||scalar("SELECT COUNT(*) FROM workplace_markers")!=0)return;
                        observer.cancel();observer=null;check(scalar("SELECT COUNT(*) FROM workplace_sightings")==2,"Positive history lost");check(scalar("SELECT COUNT(*) FROM businesses")==0,"Marker invented business");
                        pending=session.updateLoadedWorlds(Set.of()).thenCompose(v->session.currentWorkplaceOpportunities(town,100,512)).thenAccept(r->check(r.candidates().isEmpty(),"Absent world exposed markers"));stage=4;return;
                    }
                    if(stage==4){session.close();session=owner();pending=session.start(false,Set.of(world.getUID())).thenCompose(v->session.currentWorkplaceOpportunities(town,100,512)).thenAccept(r->check(r.candidates().isEmpty(),"Restart restored old markers"));stage=5;return;}
                    check(scalar("SELECT COUNT(*) FROM workplace_sightings")==2,"Restart lost history");check(world.getBlockAt(4,y,4).getBlockData().getAsString().equals(first.getAsString())&&world.getBlockAt(5,y,4).getBlockData().getAsString().equals(second.getAsString()),"Blocks not restored exactly");
                    getLogger().info("NATIVE WORKPLACE MARKER PASS: scheduled main-thread snapshot/SQL owner; two existing marker types and untouched test barrel contents; newer empty frame, retained history, absent world and restart; zero invented businesses; two temporary blocks restored");cancel();getServer().getPluginManager().disablePlugin(WorkplaceMarkerProbe.this);
                }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKPLACE MARKER FAIL",failure);getServer().getPluginManager().disablePlugin(WorkplaceMarkerProbe.this);}}
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Workplace fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(session!=null)session.close();restore();if(ticket){chunk.removePluginChunkTicket(this);ticket=false;}}
}
