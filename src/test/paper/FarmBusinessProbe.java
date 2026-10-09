import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.FarmBusinessPolicy;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.paper.PaperFarmObserver;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Manual native smoke fixture. Compile separately; NEVER distribute with the production plugin. */
public final class FarmBusinessProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperFarmObserver farms;
    private final List<Villager> entities=new ArrayList<>();
    private final List<Chunk> chunks=new ArrayList<>();
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25577 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Probe requires the marked localhost disposable server without players");
            var world=getServer().getWorlds().getFirst();
            // Test fixture tickets only. Production observation/simulation never forces chunks.
            for(int x=62;x<=66;x++)for(int z=62;z<=66;z++) {
                var chunk=world.getChunkAt(x,z);chunk.addPluginChunkTicket(this);chunks.add(chunk);
            }
            for(int x=0;x<8;x++)for(int z=0;z<4;z++) {
                for(int y=65;y<=81;y++)world.getBlockAt(1024+x,y,1024+z).setType(Material.AIR,false);
                world.getBlockAt(1024+x,63,1024+z).setType(Material.FARMLAND,false);
                world.getBlockAt(1024+x,64,1024+z).setType(Material.WHEAT,false);
            }
            world.getBlockAt(1023,63,1025).setType(Material.WATER,false);
            world.getBlockAt(1032,63,1025).setType(Material.WATER,false);
            for(int x:List.of(1024,1031))for(int z:List.of(1024,1027))
                world.getBlockAt(x,67,z).setType(Material.SEA_LANTERN,false);
            for(int n=0;n<2;n++) {
                var person=(Villager)world.spawnEntity(new Location(world,1024+n*2,65,1024),EntityType.VILLAGER);
                person.setAdult();person.setAI(false);person.setGravity(false);entities.add(person);
            }
            var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);
            for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
            var economy=new EconomySettings(1,0,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),
                    Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.valueOf(2)),bands);
            var settings=new PopulationSession.Settings(42,1_000_000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),
                    MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
            session=new PopulationSession(new java.io.File(getDataFolder(),"run-"+UUID.randomUUID()),getLogger(),settings,System::currentTimeMillis,
                    economy,FarmObservation.Policy.prototype(),VillagePriorities.Policy.prototype(),new OpeningFunds(true,5000));
            session.configureFarmBusinesses(new FarmBusinessPolicy(true,4,2500,216,0));
            var startup=session.start(false,Set.of(world.getUID()));
            var bed=new VillageObservation.Position(1024,65,1024);var other=new VillageObservation.Position(1026,65,1024);
            var observation=new VillageObservation(world.getUID(),world.getName(),System.currentTimeMillis(),true,List.of(bed,other),List.of(
                    new VillageObservation.Person(entities.get(0).getUniqueId(),bed,true,"Probe adult"),
                    new VillageObservation.Person(entities.get(1).getUniqueId(),other,true,"Probe adult")));
            new BukkitRunnable() {
                int stage,ticks;long period,pausedAt;Object stocks;CompletableFuture<?> pending=startup;
                @Override public void run() {
                    try {
                        if(++ticks>360)throw new IllegalStateException("Native farm timeout: "+(farms==null?"startup":farms.diagnostic()));
                        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                        if(stage==0){pending=session.observe(observation);stage=1;return;}
                        if(stage==1){farms=new PaperFarmObserver(session,FarmObservation.Policy.prototype());farms.runTaskTimer(FarmBusinessProbe.this,1,1);stage=2;}
                        if(stage==2) {
                            var view=session.economyView().values().stream().findFirst().orElse(null);
                            if(view!=null && view.account().state().completedPeriods()>=2) {
                                var food=view.latest().reports().getFirst();
                                if(view.farm().plantedCrops()!=32 || view.farm().representativeCrop()==null || food.flow().production()!=8
                                        || view.treasury().hundredths()!=7500 || session.businessView().total()!=1)
                                    throw new IllegalStateException("Native automatic farm accounting mismatch");
                                getLogger().info("NATIVE FARM PASS: real 32-crop snapshot; one farm, two workers, output 8, startup charged once");
                                period=view.account().state().completedPeriods();stocks=view.account().state().stocks();
                                farms.cancel();pending=session.updateLoadedWorlds(Set.of());pausedAt=System.currentTimeMillis();stage=3;return;
                            }
                            pending=session.pulse();return;
                        }
                        if(stage==3) {
                            if(System.currentTimeMillis()-pausedAt<3000){pending=session.pulse();return;}
                            var view=session.economyView().values().iterator().next();
                            if(view.account().state().completedPeriods()!=period || !view.account().state().stocks().equals(stocks))
                                throw new IllegalStateException("Absent world progressed in native probe");
                            getLogger().info("NATIVE PAUSE PASS: food periods and stocks frozen while world unavailable");
                            cancel();getServer().getPluginManager().disablePlugin(FarmBusinessProbe.this);
                        }
                    } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FARM FAIL",failure);cancel();getServer().getPluginManager().disablePlugin(FarmBusinessProbe.this);}
                }
            }.runTaskTimer(this,1,10);
        } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Native probe refused/startup failed",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable() {
        if(farms!=null)farms.cancel();
        if(session!=null)session.close();
        entities.forEach(Entity::remove);chunks.forEach(c->c.removePluginChunkTicket(this));
    }
}
