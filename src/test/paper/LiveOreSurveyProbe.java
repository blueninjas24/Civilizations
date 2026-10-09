import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.OreObserverPolicy;
import com.blueninjas24.civilizations.paper.PaperOreObserver;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Marked disposable native pipeline fixture, synthetic saved homes/people in its own DB; never ship. */
public final class LiveOreSurveyProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperOreObserver observer;
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();
    @Override public void onEnable() {
        new BukkitRunnable() {
            int attempts;
            public void run() {
                if(getServer().getWorlds().isEmpty() || getServer().getWorlds().getFirst().getLoadedChunks().length==0) {
                    if(++attempts<60)return;
                    cancel();getLogger().severe("Live survey fixture refused: no loaded chunk");getServer().getPluginManager().disablePlugin(LiveOreSurveyProbe.this);return;
                }
                cancel();begin();
            }
        }.runTaskTimer(this,20,20);
    }
    private void begin() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25577 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            var world=getServer().getWorlds().getFirst();var chunk=world.getLoadedChunks()[0];int y=world.getMaxHeight()-8;
            for(int x=5;x<=8;x++) {
                var block=chunk.getBlock(x,y,6);originals.put(block,block.getBlockData());
                block.setType(x==6 || x==7?Material.COAL_ORE:Material.AIR,false);
            }
            var home=new VillageObservation.Position(chunk.getX()*16+6,y,chunk.getZ()*16+6);
            var excluded=new VillageObservation.Position(home.x()+1,y,home.z());
            var home2=new VillageObservation.Position(home.x()+2,y,home.z());
            session=new PopulationSession(new java.io.File(getDataFolder(),"run-"+UUID.randomUUID()),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);
            session.configureOreTracking();
            CompletableFuture<?> ready=session.start(false,Set.of(world.getUID())).thenCompose(s->session.observe(new VillageObservation(world.getUID(),world.getName(),System.currentTimeMillis(),true,
                    List.of(home,home2),List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture A"),new VillageObservation.Person(UUID.randomUUID(),home2,true,"Fixture B")))))
                    .thenCompose(r->session.recordOrePlacements(world.getUID(),world.getName(),List.of(excluded),System.currentTimeMillis()));
            new BukkitRunnable() {
                int attempts;
                public void run() {
                    try {
                        if(++attempts>200)throw new IllegalStateException("Live observer fixture timed out");
                        if(!ready.isDone())return;ready.join();
                        if(observer==null){observer=new PaperOreObserver(session,new OreObserverPolicy(true,1,64,20));observer.runTaskTimer(LiveOreSurveyProbe.this,1,1);return;}
                        if(!observer.diagnostic().startsWith("Ore survey: "))return;
                        if(!observer.diagnostic().contains("1 observed patches; 0 linked"))throw new IllegalStateException("Unexpected live evidence: "+observer.diagnostic());
                        getLogger().info("NATIVE LIVE ORE SURVEY PASS: main-thread snapshot/tick budgets, owner validation, placement exclusion and positive patch; synthetic homes, no automatic founding");
                        cancel();getServer().getPluginManager().disablePlugin(LiveOreSurveyProbe.this);
                    } catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE LIVE ORE SURVEY FAIL",failure);getServer().getPluginManager().disablePlugin(LiveOreSurveyProbe.this);}
                }
            }.runTaskTimer(this,1,1);
        } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Live survey fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable() {
        if(observer!=null)observer.cancel();if(session!=null)session.close();
        originals.forEach((block,data)->block.setBlockData(data,false));
        originals.forEach((block,data)-> {if(!block.getBlockData().equals(data))throw new IllegalStateException("Fixture restoration failed");});
        originals.clear();
    }
}
