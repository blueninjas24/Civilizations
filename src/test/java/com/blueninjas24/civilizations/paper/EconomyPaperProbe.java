package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only real Paper probe; refuses unmarked worlds and online players. Never ships in plugin JAR. */
public final class EconomyPaperProbe extends JavaPlugin {
    private final AtomicLong now = new AtomicLong(1000);
    private PopulationSession session;
    private PaperFarmObserver observer;
    private EconomySettings economy;
    private PopulationSession.Settings population;
    private File scenario;
    private CompletableFuture<?> future;
    private World world;
    private UUID town;
    private Set<UUID> residents;
    private Result famine;
    private final Set<Long> tickets = new HashSet<>();
    private int stage, ticks;
    private long deadline;
    private static final int X=1280,Z=512;
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    @Override public void onEnable() {
        try {
            var root=getDataFolder().toPath().toAbsolutePath().normalize().getParent().getParent();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment"),"Unmarked server");
            require(Bukkit.getOnlinePlayers().isEmpty(),"Players present"); world=Bukkit.getWorld("civilizations-phase1-test"); require(world!=null,"Wrong world");
            var plugin=Bukkit.getPluginManager().getPlugin("Civilizations");
            for(var name:List.of("observer","farms","projector")) {
                var field=plugin.getClass().getDeclaredField(name);field.setAccessible(true);
                Bukkit.getScheduler().runTaskLater(this,()->{try{if(field.get(plugin) instanceof BukkitRunnable task)task.cancel();}catch(Exception e){throw new IllegalStateException(e);}},30);
            }
            var config=new YamlConfiguration(); try(var reader=new InputStreamReader(Objects.requireNonNull(plugin.getResource("economy.yml")),java.nio.charset.StandardCharsets.UTF_8)){config.load(reader);}
            economy=EconomyConfiguration.read(config); economy=tune(economy,100);
            var base=PopulationSession.Settings.prototype();
            population=new PopulationSession.Settings(base.seed(),base.millisPerMonth(),base.maximumOfflineMonths(),base.monthsPerBatch(),
                    new PopulationPolicy(12,216,240,480,720,0,0,0),new MigrationPolicy(0,0),base.recognition(),base.enrollment());
            scenario=new File(getDataFolder(),"food-"+UUID.randomUUID());open(); deadline=System.currentTimeMillis()+120000;
            new BukkitRunnable(){public void run(){
                if(++ticks<40)return;
                try{require(System.currentTimeMillis()<deadline,"Timeout stage "+stage);step();}
                catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"ECONOMY_PROBE_FAILED stage "+stage,failure);cleanup();cancel();}
                if(stage==99){cleanup();cancel();getLogger().info("ECONOMY_PROBE_PASSED: actual planted-field scanning, saved food production/consumption, famine, forward feast retuning, restart identity/history and unloaded operation");}
            }}.runTaskTimer(this,1,1);
        }catch(Exception failure){throw new IllegalStateException("Economy probe refused",failure);}
    }
    private EconomySettings tune(EconomySettings base,int output){return new EconomySettings(1,0,2,base.adultFoodUnits(),base.childFoodUnits(),base.foodUnitsPerWorker(),
            output,base.shortages(),base.startingPerResident(),base.baseTradeValues(),base.pricePercent());}
    private void open(){session=new PopulationSession(scenario,getLogger(),population,now::get,economy,FarmObservation.Policy.prototype());future=session.start(false);}
    private boolean ready(){if(!future.isDone())return false;future.join();return true;}
    private void scan(){observer=new PaperFarmObserver(session,FarmObservation.Policy.prototype());observer.runTaskTimer(this,1,1);}
    private void crops(boolean planted){for(int x=X+4;x<X+12;x++)for(int z=Z+4;z<Z+12;z++){
        world.getBlockAt(x,63,z).setType(Material.FARMLAND,false);world.getBlockAt(x,64,z).setType(planted?Material.WHEAT:Material.AIR,false);
    }}
    private void fixture(){
        for(int x=Math.floorDiv(X,16)-2;x<=Math.floorDiv(X,16)+2;x++)for(int z=Math.floorDiv(Z,16)-2;z<=Math.floorDiv(Z,16)+2;z++){
            world.addPluginChunkTicket(x,z,this);tickets.add(((long)x<<32)|(z&0xffffffffL));
        }
        for(int x=X-2;x<X+14;x++)for(int z=Z-2;z<Z+14;z++){world.getBlockAt(x,63,z).setType(Material.STONE,false);world.getBlockAt(x,64,z).setType(Material.AIR,false);world.getBlockAt(x,65,z).setType(Material.AIR,false);}
        var beds=new ArrayList<VillageObservation.Position>();
        for(int x=X;x<X+2;x++){
            var data=(org.bukkit.block.data.type.Bed)Bukkit.createBlockData(Material.WHITE_BED);data.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            world.getBlockAt(x,64,Z).setBlockData(data,false);beds.add(new VillageObservation.Position(x,64,Z));
        }
        var people=new ArrayList<VillageObservation.Person>();
        for(int i=0;i<2;i++){var entity=world.spawn(new Location(world,X+i+0.5,64,Z+2.5),Villager.class);entity.setAdult();entity.setAI(false);entity.setInvulnerable(true);
            people.add(new VillageObservation.Person(entity.getUniqueId(),new VillageObservation.Position(X+i,64,Z+2),true,"Food test "+i));}
        crops(true);future=session.observe(new VillageObservation(world.getUID(),world.getName(),System.currentTimeMillis(),true,beds,people));
    }
    private void step(){switch(stage){
        case 0->{if(!ready())return;fixture();stage++;}
        case 1->{if(!ready())return;town=session.snapshot().orElseThrow().households().getFirst().settlementId();residents=new HashSet<>();session.snapshot().orElseThrow().residents().forEach(p->residents.add(p.id()));scan();stage++;}
        case 2->{var view=session.economyView().get(town);if(view==null)return;require(view.farm().complete()&&view.farm().plantedCrops()==64,"Field count wrong");now.addAndGet(1000);future=session.pulse();stage++;}
        case 3->{if(!ready())return;var report=session.economyView().get(town).latest().reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
            require(report.flow().production()==8&&report.demand()==4&&report.after().units()==28,"Production/demand wrong");crops(false);observer.cancel();scan();stage++;}
        case 4->{if(session.economyView().get(town).farm().plantedCrops()!=0)return;require(session.economyView().get(town).farm().complete(),"Empty field was unknown");now.addAndGet(1000);future=session.pulse();stage++;}
        case 5->{if(!ready())return;var view=session.economyView().get(town);if(view.account().state().completedPeriods()<11){now.addAndGet(1000);future=session.pulse();return;}
            famine=view.latest();require(famine.reports().stream().anyMatch(r->r.commodity().equals(Commodity.FOOD)&&r.condition()==Condition.SEVERE_SHORTAGE),"Famine did not escalate");
            observer.cancel();session.close();economy=tune(economy,200);now.addAndGet(10000);open();stage++;}
        case 6->{if(!ready())return;require(famine.equals(session.economyView().get(town).latest()),"Past famine report changed on retuning");
            var ids=new HashSet<UUID>();session.snapshot().orElseThrow().residents().forEach(p->ids.add(p.id()));require(ids.equals(residents),"Residents changed on restart");crops(true);scan();stage++;}
        case 7->{if(session.economyView().get(town).farm().plantedCrops()!=64)return;now.addAndGet(1000);future=session.pulse();stage++;}
        case 8->{if(!ready())return;require(session.economyView().get(town).account().state().stocks().get(Commodity.FOOD).units()==12,"Feast recovery wrong");
            for(long key:tickets)world.removePluginChunkTicket((int)(key>>32),(int)key,this);tickets.clear();stage++;}
        case 9->{if(world.isChunkLoaded(Math.floorDiv(X,16),Math.floorDiv(Z,16)))return;now.addAndGet(1000);future=session.pulse();stage++;}
        case 10->{if(!ready())return;require(!world.isChunkLoaded(Math.floorDiv(X,16),Math.floorDiv(Z,16)),"Economy force loaded chunk");
            require(session.economyView().get(town).account().state().stocks().get(Commodity.FOOD).units()==24,"Unloaded food accounting failed");stage=99;}
        default->throw new AssertionError("Unknown stage");
    }}
    private void cleanup(){if(observer!=null)observer.cancel();if(session!=null)session.close();if(world!=null)for(long key:tickets)world.removePluginChunkTicket((int)(key>>32),(int)key,this);tickets.clear();}
    @Override public void onDisable(){cleanup();}
}
