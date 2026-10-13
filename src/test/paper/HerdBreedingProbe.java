import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.LivestockAdoption;
import com.blueninjas24.civilizations.paper.ManagedLivestockBreedingGuard;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;

/** Actual Paper event dispatch and saved identity publication. Synthetic events, no human players. */
public final class HerdBreedingProbe extends JavaPlugin {
    private final List<Entity> actors=new ArrayList<>();private PopulationSession owner;private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private LivingEntity actor(World world,EntityType type){var e=(LivingEntity)world.spawnEntity(new Location(world,8,world.getMaxHeight()-16,8),type);e.setGravity(false);e.setInvulnerable(true);((Mob)e).setAI(false);actors.add(e);return e;}
    private void cleanup(){org.bukkit.event.HandlerList.unregisterAll(this);if(owner!=null){owner.close();owner=null;}for(var e:actors)e.remove();actors.clear();}
    private EntityBreedEvent birth(LivingEntity child,LivingEntity a,LivingEntity b){var e=new EntityBreedEvent(child,a,b,null,null,0);getServer().getPluginManager().callEvent(e);return e;}
    private PlayerInteractEntityEvent interact(Entity animal,Material material,EquipmentSlot hand){
        var stack=new ItemStack(material,1);
        var pdc=(org.bukkit.persistence.PersistentDataContainer)Proxy.newProxyInstance(org.bukkit.persistence.PersistentDataContainer.class.getClassLoader(),new Class<?>[]{org.bukkit.persistence.PersistentDataContainer.class},(p,m,a)->{if(m.getName().equals("has"))return false;throw new IllegalStateException("Unexpected fixture PDC access "+m.getName());});
        var inventory=(PlayerInventory)Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->{if(m.getName().equals("getItem"))return stack;throw new IllegalStateException("Unexpected inventory access "+m.getName());});
        var player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){case "getInventory"->inventory;case "getPersistentDataContainer"->pdc;case "isSneaking"->false;case "sendMessage"->null;default->throw new IllegalStateException("Unexpected player access "+m.getName());});
        var e=new PlayerInteractEntityEvent(player,animal,hand);getServer().getPluginManager().callEvent(e);check(stack.getAmount()==1,"Guard must not mutate feeding stack");return e;
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        new BukkitRunnable(){int stage,ticks;CompletableFuture<?> ready;LivingEntity cow,playerCow,freeCow,babyCow,chicken,freeChicken,babyChicken;
            @Override public void run(){try {
                check(++ticks<=300,"Herd guard fixture timeout");
                if(stage==0){var world=getServer().getWorlds().getFirst();world.getChunkAt(0,0); // Explicit fixture setup only.
                    cow=actor(world,EntityType.COW);playerCow=actor(world,EntityType.COW);freeCow=actor(world,EntityType.COW);babyCow=actor(world,EntityType.COW);
                    chicken=actor(world,EntityType.CHICKEN);freeChicken=actor(world,EntityType.CHICKEN);babyChicken=actor(world,EntityType.CHICKEN);
                    var data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());var town=UUID.randomUUID();var worldId=world.getUID();var cowId=cow.getUniqueId();var chickenId=chicken.getUniqueId();var playerCowId=playerCow.getUniqueId();var logger=getLogger();
                    ready=CompletableFuture.runAsync(()->{var db=new DatabaseManager(data.toFile(),logger);try {
                        db.initialize();db.saveSettlement(new Settlement(town,"Guard fixture","Guard fixture",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(worldId,"Guard fixture");db.getVillageIdentityRepository().bindSettlement(town,worldId);
                        var registry=new AnimalOwnershipRepository(db.getConnection());for(var id:List.of(cowId,chickenId))registry.record(new AnimalOwnershipRepository.Claim(id,worldId,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit native fixture animals"));
                        registry.record(new AnimalOwnershipRepository.Claim(playerCowId,worldId,LivestockAdoption.Ownership.PLAYER,UUID.randomUUID(),null,1000,"Player fixture animal"));
                    }catch(Exception error){throw new CompletionException(error);}finally{db.close();}}).thenCompose(v->{owner=new PopulationSession(data.toFile(),logger,PopulationSession.Settings.prototype(),()->1000);return owner.start(true,Set.of(worldId));});stage=1;return;
                }
                if(!ready.isDone())return;ready.join();check(owner.status().ready(),"Owner readiness required");check(owner.villageAnimals().equals(Set.of(cow.getUniqueId(),chicken.getUniqueId())),"Saved village-only identity publication required");
                getServer().getPluginManager().registerEvents(new ManagedLivestockBreedingGuard(owner::villageAnimals,()->owner.status().ready()),HerdBreedingProbe.this);
                check(birth(babyCow,cow,playerCow).isCancelled(),"Mixed village parent cow birth must stop");check(!birth(babyCow,playerCow,freeCow).isCancelled(),"Player/unclaimed cow birth must remain available");
                check(birth(babyChicken,chicken,freeChicken).isCancelled(),"Village chicken birth must stop");
                for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND))check(interact(cow,Material.WHEAT,hand).isCancelled(),"Cow food must stop in either hand");
                check(interact(chicken,Material.WHEAT_SEEDS,EquipmentSlot.HAND).isCancelled(),"Actual chicken food tag required");
                check(!interact(playerCow,Material.WHEAT,EquipmentSlot.HAND).isCancelled(),"Player feeding must stay normal");
                for(var material:List.of(Material.BUCKET,Material.NAME_TAG,Material.LEAD))check(!interact(cow,material,EquipmentSlot.HAND).isCancelled(),"Non-breeding village interaction must stay available");
                cleanup();getLogger().info("HERD_BREEDING_PROBE_PASSED SQL-owner identity publication, actual cow/chicken food tags, synthetic birth/interaction dispatch, player/other-item preservation; temporary entities removed");cancel();getServer().shutdown();
            }catch(Throwable error){getLogger().log(java.util.logging.Level.SEVERE,"HERD_BREEDING_PROBE_FAILED",error);cleanup();cancel();getServer().shutdown();}}
        }.runTaskTimer(this,100L,1L);
    }catch(Throwable error){getLogger().log(java.util.logging.Level.SEVERE,"HERD_BREEDING_PROBE_FAILED",error);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){cleanup();}
}
