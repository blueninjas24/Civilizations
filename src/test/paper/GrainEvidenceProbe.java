import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.paper.PaperFarmObserver;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/** Real read-only farm observer and restart evidence in a marked disposable fixture. Never ship. */
public final class GrainEvidenceProbe extends JavaPlugin {
    private PopulationSession session;private PaperFarmObserver observer;private World world;private boolean authorized;
    private Path data;private final List<int[]> tickets=new ArrayList<>();
    private final FarmObservation.Policy policy=new FarmObservation.Policy(8,2,16);
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private PopulationSession create() {
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
        for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        var economy=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
        return new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->1000L,economy,policy);
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture only");authorized=true;world=getServer().getWorlds().getFirst();
        for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++){if(world.addPluginChunkTicket(x,z,this))tickets.add(new int[]{x,z});check(world.isChunkLoaded(x,z),"Explicit fixture chunks required");}
        data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());session=create();
        var a=new VillageObservation.Position(0,64,0);var b=new VillageObservation.Position(2,64,0);
        var observation=new VillageObservation(world.getUID(),"Grain fixture",1000,true,List.of(a,b),List.of(new VillageObservation.Person(UUID.randomUUID(),a,true,"Fixture A"),new VillageObservation.Person(UUID.randomUUID(),b,true,"Fixture B")));
        var ready=session.start(false,Set.of(world.getUID())).thenCompose(v->session.observe(observation));
        new BukkitRunnable(){int ticks,stage,cursor;long crops,grain;FarmObservation saved;List<VillageObservation.Position> anchors;CompletableFuture<?> restarted;
            @Override public void run(){try {
                check(++ticks<=1200,"Grain fixture timeout stage "+stage);if(!ready.isDone())return;ready.join();
                if(stage==0){anchors=session.projectionView().orElseThrow().identities().homes().values().stream().map(h->h.position()).toList();check(!anchors.isEmpty(),"Actual saved farm homes required");observer=new PaperFarmObserver(session,policy);observer.runTaskTimer(GrainEvidenceProbe.this,1,1);stage=1;return;}
                if(stage==1){int used=0;while(cursor<5*32*32&&used++<128){int y=62+cursor/1024,x=-16+cursor%32,z=-16+cursor%1024/32;cursor++;var p=new VillageObservation.Position(x,y,z);if(anchors.stream().noneMatch(h->CropScan.near(p,h,policy)))continue;var type=world.getBlockAt(x,y,z).getType();if((type==Material.WHEAT||type==Material.CARROTS||type==Material.POTATOES||type==Material.BEETROOTS)&&world.getBlockAt(x,y-1,z).getType()==Material.FARMLAND){crops++;if(type==Material.WHEAT)grain++;}}
                    if(cursor<5*32*32)return;stage=3;return;}
                if(stage==3){if(session.economyView().isEmpty())return;var view=session.economyView().values().iterator().next();saved=view.farm();check(saved.complete()&&saved.grainCrops()!=null&&saved.plantedCrops()==crops&&saved.grainCrops()==grain,"Ordinary saved grain evidence differs from independent actual terrain");check(view.account().state().stocks().equals(Map.of(Commodity.FOOD,new CommodityAccounting.Stock(24,0))),"Observation must not convert existing food or mint grain ingredients");observer.cancel();observer=null;session.close();session=create();restarted=session.start(false,Set.of(world.getUID()));stage=4;return;}
                if(stage==4){if(!restarted.isDone())return;restarted.join();var view=session.economyView().values().iterator().next();check(view.farm().equals(saved)&&view.account().state().stocks().equals(Map.of(Commodity.FOOD,new CommodityAccounting.Stock(24,0))),"Grain evidence/food changed across restart");getLogger().info("NATIVE GRAIN EVIDENCE PASS: ordinary read-only farm observer matched "+crops+" crops / "+grain+" wheat against actual loaded terrain; known zero distinct from unknown; restart retained evidence and all24 food units; no ingredient minting, block/entity edits or simulation chunk forcing; helper tickets released");cancel();getServer().getPluginManager().disablePlugin(GrainEvidenceProbe.this);getServer().shutdown();}
            }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE GRAIN EVIDENCE FAIL",failure);getServer().getPluginManager().disablePlugin(GrainEvidenceProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Grain fixture refused",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(session!=null)session.close();for(var c:tickets)world.removePluginChunkTicket(c[0],c[1],this);}
}
