import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.LivestockAdoption;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Marked disposable native egg laying, actual PDC and synthetic bucket/breeding dispatch. No client claim. */
public final class LivestockProductsProbe extends JavaPlugin implements Listener {
    private record Drop(UUID source,Material material,boolean cancelled,Item item) {}
    private final List<Entity> created=new ArrayList<>();private final List<Drop> drops=new ArrayList<>();
    private PopulationSession owner;private Chunk chunk;private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private <T extends Animals>T animal(World world,Location point,Class<T> type) {
        var animal=world.spawn(point,type);created.add(animal);animal.setAI(false);animal.setGravity(false);animal.setInvulnerable(true);animal.setCollidable(false);animal.setAdult();animal.setAgeLock(true);return animal;
    }
    private Player player(ItemStack stack,EquipmentSlot hand) {
        var pdc=(PersistentDataContainer)Proxy.newProxyInstance(PersistentDataContainer.class.getClassLoader(),new Class<?>[]{PersistentDataContainer.class},(p,m,a)->{if(m.getName().equals("has"))return false;throw new IllegalStateException("Unexpected fake player PDC "+m.getName());});
        var inventory=(PlayerInventory)Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->{check(m.getName().equals("getItem")&&a[0]==hand,"No inventory mutation/other hand read");return stack;});
        return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
            case "getInventory"->inventory;case "getPersistentDataContainer"->pdc;case "isSneaking"->false;case "sendMessage"->null;default->throw new IllegalStateException("Unexpected player identity/write "+m.getName());});
    }
    private PlayerInteractEntityEvent interact(Entity animal,Material material,EquipmentSlot hand) {
        var stack=new ItemStack(material);var event=new PlayerInteractEntityEvent(player(stack,hand),animal,hand);getServer().getPluginManager().callEvent(event);
        check(stack.getType()==material&&stack.getAmount()==1,"Bucket/item unchanged; no milk granted");return event;
    }
    private EntityBreedEvent breed(LivingEntity child,LivingEntity a,LivingEntity b,LivingEntity breeder,boolean cancelled) {
        var event=new EntityBreedEvent(child,a,b,breeder,null,3);event.setCancelled(cancelled);getServer().getPluginManager().callEvent(event);return event;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void drop(EntityDropItemEvent event) {
        if(created.stream().noneMatch(e->e.getUniqueId().equals(event.getEntity().getUniqueId())))return;
        var item=event.getItemDrop();drops.add(new Drop(event.getEntity().getUniqueId(),item.getItemStack().getType(),event.isCancelled(),item));created.add(item);
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        var plugin=getServer().getPluginManager().getPlugin("Civilizations");var registrations=HandlerList.getRegisteredListeners(plugin);
        check(registrations.stream().anyMatch(r->r.getListener() instanceof ManagedLivestockProducts)&&registrations.stream().anyMatch(r->r.getListener() instanceof LivestockBreedingProvenance),"Production listeners registered");
        new BukkitRunnable(){int stage,ticks;CompletableFuture<?> ready;Cow cow,playerCow,child,natural,grandchild,cancelled;List<Chicken> hens=new ArrayList<>();Map<UUID,Material> expected=new HashMap<>();Set<UUID> managed;AtomicBoolean enabled=new AtomicBoolean(true);
            @Override public void run(){try {
                check(++ticks<=600,"Livestock product fixture timeout");
                if(stage==0) {
                    var world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=Math.floorDiv(spawn.getBlockX(),16),z=Math.floorDiv(spawn.getBlockZ(),16);check(world.isChunkLoaded(x,z)||world.loadChunk(x,z,false),"Existing generated test chunk required");chunk=world.getChunkAt(x,z);chunk.addPluginChunkTicket(LivestockProductsProbe.this);
                    var point=new Location(world,x*16+8.5,Math.min(world.getMaxHeight()-2,spawn.getBlockY()+32),z*16+8.5);
                    cow=animal(world,point,Cow.class);playerCow=animal(world,point,Cow.class);child=animal(world,point,Cow.class);natural=animal(world,point,Cow.class);grandchild=animal(world,point,Cow.class);cancelled=animal(world,point,Cow.class);
                    var kinds=List.of(Chicken.Variant.TEMPERATE,Chicken.Variant.WARM,Chicken.Variant.COLD);var materials=List.of(Material.EGG,Material.BROWN_EGG,Material.BLUE_EGG);var managedIds=new HashSet<UUID>();managedIds.add(cow.getUniqueId());
                    for(int i=0;i<6;i++){var hen=animal(world,point,Chicken.class);hens.add(hen);hen.setVariant(kinds.get(i%3));hen.setEggLayTime(6000);expected.put(hen.getUniqueId(),materials.get(i%3));if(i<3)managedIds.add(hen.getUniqueId());}
                    managed=Set.copyOf(managedIds);var worldId=world.getUID();var playerId=playerCow.getUniqueId();var data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());var town=UUID.randomUUID();
                    ready=CompletableFuture.runAsync(()->{var db=new DatabaseManager(data.toFile(),getLogger());try {
                        db.initialize();db.saveSettlement(new Settlement(town,"Products fixture","Fixture",0,64,0,0,0,0));db.getVillageIdentityRepository().observeWorld(worldId,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,worldId);
                        var claims=new AnimalOwnershipRepository(db.getConnection());for(var id:managed)claims.record(new AnimalOwnershipRepository.Claim(id,worldId,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit disposable product fixture"));
                        claims.record(new AnimalOwnershipRepository.Claim(playerId,worldId,LivestockAdoption.Ownership.PLAYER,UUID.randomUUID(),null,1000,"Explicit disposable player fixture"));
                    }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}}).thenCompose(v->{owner=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),()->1000);return owner.start(false,Set.of(worldId));});stage=1;return;
                }
                if(stage==1) {
                    if(!ready.isDone())return;ready.join();check(owner.status().ready()&&owner.villageAnimals().equals(managed),"SQL-owner positive saved identities");
                    getServer().getPluginManager().registerEvents(new ManagedLivestockProducts(owner::villageAnimals,enabled::get),LivestockProductsProbe.this);getServer().getPluginManager().registerEvents(LivestockProductsProbe.this,LivestockProductsProbe.this);
                    for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND)){check(interact(cow,Material.BUCKET,hand).isCancelled(),"Village milking stopped");check(!interact(playerCow,Material.BUCKET,hand).isCancelled(),"Player cow normal");}
                    for(var material:List.of(Material.NAME_TAG,Material.LEAD,Material.MILK_BUCKET))check(!interact(cow,material,EquipmentSlot.HAND).isCancelled(),"Other village interactions normal");
                    enabled.set(false);check(!interact(cow,Material.BUCKET,EquipmentSlot.HAND).isCancelled(),"Disabled guard normal");enabled.set(true);
                    var breeder=player(new ItemStack(Material.WHEAT),EquipmentSlot.HAND);check(!breed(child,playerCow,natural,breeder,false).isCancelled(),"Player breeding normal");check(LivestockBreedingProvenance.recorded(child)&&!LivestockBreedingProvenance.recorded(playerCow),"Anonymous child evidence only");
                    check(!breed(grandchild,child,natural,null,false).isCancelled()&&LivestockBreedingProvenance.recorded(grandchild),"Protected descendant evidence");
                    breed(cancelled,playerCow,natural,breeder,true);check(!LivestockBreedingProvenance.recorded(cancelled),"Cancelled birth untagged");
                    byte[] saved=child.getPersistentDataContainer().serializeToBytes();child.getPersistentDataContainer().remove(new NamespacedKey("civilizations","livestock_player_bred"));check(!LivestockBreedingProvenance.recorded(child),"Fixture tag removed");child.getPersistentDataContainer().readFromBytes(saved,true);check(LivestockBreedingProvenance.recorded(child),"Real PDC serialization retains protection");
                    var frame=new PaperLivestockSource().capture(chunk,cow.getWorld().getUID(),System.currentTimeMillis(),256,32);check(frame.complete()&&frame.animals().stream().anyMatch(a->a.id().equals(child.getUniqueId())&&a.recordedPlayerBreeding()),"Immutable live protective evidence");
                    for(var hen:hens){hen.setAI(true);hen.setEggLayTime(1);}stage=2;return;
                }
                if(drops.size()<6)return;
                check(drops.size()==6,"One native egg per fixture hen");for(var d:drops){check(d.material()==expected.get(d.source()),"Actual native egg variant");check(d.cancelled()==managed.contains(d.source()),"Only village eggs cancelled");check(d.item().isValid()!=d.cancelled(),"Native cancellation prevents physical item; normal egg remains");}
                getLogger().info("LIVESTOCK_PRODUCTS_PROBE_PASSED saved SQL-owner identities, production listener registration, actual native three-colour egg laying/drop cancellation, synthetic both-hand bucket/breeding dispatch, normal player animals, real protective PDC serialization/capture; no human client or stock-trading claim");cancel();getServer().shutdown();
            }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_PRODUCTS_PROBE_FAILED",failure);cancel();getServer().shutdown();}}
        }.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_PRODUCTS_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    @Override public void onDisable(){try {
        HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);if(owner!=null)owner.close();for(var e:created)if(e.isValid())e.remove();check(created.stream().noneMatch(Entity::isValid),"All temporary actors/items removed");getLogger().info("LIVESTOCK_PRODUCTS_FIXTURE_RESTORED");
    }finally{if(chunk!=null)chunk.removePluginChunkTicket(this);}}
}
