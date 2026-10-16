import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.ManagedLivestockDeaths;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual native cow deaths and owner handoff in a marked disposable fixture. No human server. */
public final class LivestockLossProbe extends JavaPlugin implements Listener {
    private final List<Entity> created=new ArrayList<>();private final Set<UUID> cowIds=new HashSet<>();private final Set<UUID> nativeDeaths=new HashSet<>();
    private static final NamespacedKey FIXTURE_DROP=new NamespacedKey("civilizationslossprobe","fixture_drop");
    private PopulationSession owner;private ManagedLivestockDeaths listener;private Chunk chunk;private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void died(EntityDeathEvent event){if(cowIds.contains(event.getEntity().getUniqueId())){
        check(!event.isCancelled(),"Native death remains uncancelled");nativeDeaths.add(event.getEntity().getUniqueId());
        // Mark only this helper's fixture loot so cleanup never removes unrelated items.
        for(var stack:event.getDrops()){var meta=stack.getItemMeta();meta.getPersistentDataContainer().set(FIXTURE_DROP,org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);stack.setItemMeta(meta);}
    }}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void item(ItemSpawnEvent event){if(event.getEntity().getItemStack().getItemMeta().getPersistentDataContainer().has(FIXTURE_DROP))created.add(event.getEntity());}
    private void kill(Cow cow){cow.setInvulnerable(false);cow.setHealth(0);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        var plugin=getServer().getPluginManager().getPlugin("Civilizations");check(HandlerList.getRegisteredListeners(plugin).stream().anyMatch(r->r.getListener() instanceof ManagedLivestockDeaths),"Production death listener registered");
        getServer().getPluginManager().registerEvents(this,this);
        new BukkitRunnable(){int stage,ticks;List<Cow> cows=new ArrayList<>();CompletableFuture<?> ready,proof;Path data;UUID business,worldId;
            @Override public void run(){try {
                check(++ticks<=600,"Native herd loss timeout");
                if(stage==0) {
                    var world=getServer().getWorlds().getFirst();worldId=world.getUID();var spawn=world.getSpawnLocation();int x=Math.floorDiv(spawn.getBlockX(),16),z=Math.floorDiv(spawn.getBlockZ(),16);check(world.isChunkLoaded(x,z)||world.loadChunk(x,z,false),"Existing generated test chunk required");chunk=world.getChunkAt(x,z);chunk.addPluginChunkTicket(LivestockLossProbe.this);
                    var point=new Location(world,x*16+8.5,Math.min(world.getMaxHeight()-2,spawn.getBlockY()+32),z*16+8.5);
                    for(int i=0;i<4;i++){var cow=world.spawn(point,Cow.class);cow.setGravity(false);cow.setAI(false);cow.setCollidable(false);cow.setInvulnerable(true);if(i==2)cow.setBaby();else cow.setAdult();cow.setAgeLock(true);cows.add(cow);created.add(cow);cowIds.add(cow.getUniqueId());}
                    var ids=cows.stream().map(Entity::getUniqueId).toList();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());business=UUID.randomUUID();
                    ready=CompletableFuture.runAsync(()->prepare(data,business,worldId,ids)).thenCompose(v->{owner=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->1000);return owner.start(false,Set.of(worldId));});stage=1;return;
                }
                if(stage==1) {
                    if(!ready.isDone())return;ready.join();check(owner.status().ready()&&owner.villageAnimals().size()==3,"Saved herd-only identities published");
                    listener=new ManagedLivestockDeaths(owner,getLogger());getServer().getPluginManager().registerEvents(listener,LivestockLossProbe.this);listener.runTaskTimer(LivestockLossProbe.this,1,1);
                    kill(cows.get(0));kill(cows.get(2));kill(cows.get(3));check(nativeDeaths.containsAll(List.of(cows.get(0).getUniqueId(),cows.get(2).getUniqueId(),cows.get(3).getUniqueId())),"Actual native death events fired for adult, young and player animal");stage=2;return;
                }
                if(stage==2) {
                    if(!listener.diagnostic().contains("pending: 0"))return;
                    listener.cancel();listener.flush();owner.close();owner=null;var expected=cows.get(1).getUniqueId();proof=CompletableFuture.runAsync(()->verify(data,business,expected));stage=3;return;
                }
                if(!proof.isDone())return;proof.join();getLogger().info("LIVESTOCK_LOSS_PROBE_PASSED actual native adult/young/player cow deaths, SQL-owner handoff, no player herd loss, reduced future feed/output, immutable old receipt, restart/retry/absence and schema82; no automatic adopter, physical offspring or crash-gap certification");cancel();getServer().shutdown();
            }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_LOSS_PROBE_FAILED",failure);cancel();getServer().shutdown();}}
        }.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_LOSS_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void prepare(Path data,UUID business,UUID world,List<UUID> ids) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var town=UUID.randomUUID();var worker=UUID.randomUUID();db.saveSettlement(new Settlement(town,"Loss fixture","Fixture",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(worker,"Fixture herd worker",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,1000,"Actual disposable animals");db.getBusinessRepository().found(new BusinessFounding(new Business(business,town,"Loss herd",worker,worker,"livestock",site.id(),1,0),site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Explicit fixture herd"));db.getBusinessRepository().hire(UUID.randomUUID(),business,worker,0);
            var grain=new Commodity("grain");db.getIndustryRepository().openStore(business,256,Map.of(grain,1L,new Commodity("milk"),1L),0);try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=100 WHERE business_id='"+business+"' AND commodity='grain'");}
            var registry=new AnimalOwnershipRepository(db.getConnection());for(var id:ids.subList(0,3))registry.record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit fixture animals"));registry.record(new AnimalOwnershipRepository.Claim(ids.get(3),world,LivestockAdoption.Ownership.PLAYER,UUID.randomUUID(),null,1000,"Explicit player fixture"));
            new LivestockRepository(db.getConnection()).open(business,LivestockHerd.Species.COW,grain,8,Map.of(ids.get(0),0,ids.get(1),0,ids.get(2),2));
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    private void verify(Path data,UUID business,UUID survivor) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var losses=new LivestockLosses(db.getConnection());var repository=new LivestockRepository(db.getConnection());var herd=repository.load(business).orElseThrow();check(herd.state().adults()==1&&herd.state().young().isEmpty()&&herd.state().period()==0,"Exactly adult and young losses; no invented time");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM livestock_animal_losses")){check(r.getInt(1)==2,"Only saved original herd animals lost");}
            var before=db.getIndustryRepository().store(business).orElseThrow();check(before.units().get(new Commodity("grain"))==100&&before.units().get(new Commodity("milk"))==0,"Loss creates/consumes no goods");
            var operation=UUID.randomUUID();repository.advance(operation,business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);check(before.equals(db.getIndustryRepository().store(business).orElseThrow()),"Absent world exact pause");
            var plan=repository.advance(operation,business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);check(plan.renewableUnits()==1&&plan.feedUsed()==1&&plan.births()==0,"Only surviving adult consumes feed and produces");
            var after=db.getIndustryRepository().store(business).orElseThrow();check(after.hundredths()==0&&after.units().get(new Commodity("grain"))==99&&after.units().get(new Commodity("milk"))==1,"Actual current stock");
            db.close();db=new DatabaseManager(data.toFile(),getLogger());db.initialize();repository=new LivestockRepository(db.getConnection());check(plan.equals(repository.advance(operation,business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true)),"Restart old production receipt replay");check(repository.load(business).orElseThrow().state().adults()==1,"Applied losses not subtracted twice");check(after.equals(db.getIndustryRepository().store(business).orElseThrow()),"Replay no duplicate goods");
            var observed=new ArrayList<LivestockLosses.Observation>();try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM livestock_animal_losses")){while(r.next())observed.add(new LivestockLosses.Observation(UUID.fromString(r.getString("operation_id")),UUID.fromString(r.getString("animal_id")),UUID.fromString(r.getString("observed_world")),r.getLong("observed_millis"),r.getString("reason")));}
            losses=new LivestockLosses(db.getConnection());for(var o:observed)losses.record(o);check(repository.load(business).orElseThrow().state().heads()==1,"Exact death replay retained");
            check(new AnimalOwnershipRepository(db.getConnection()).load(survivor).orElseThrow().kind()==LivestockAdoption.Ownership.VILLAGE,"Survivor identity unchanged");try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys clean");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onDisable(){try {
        HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);if(listener!=null){try{listener.cancel();}catch(IllegalStateException ignored){/* Already cancelled after proof. */}if(owner!=null)listener.flush();}if(owner!=null)owner.close();for(var e:created)if(e.isValid())e.remove();check(created.stream().noneMatch(Entity::isValid),"All fixture actors/items removed");getLogger().info("LIVESTOCK_LOSS_FIXTURE_RESTORED");
    }finally{if(chunk!=null)chunk.removePluginChunkTicket(this);}}
}
