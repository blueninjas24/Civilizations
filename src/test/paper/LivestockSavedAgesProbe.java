import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Excluded trusted fixture: real production births projected on existing safe natural ground.
 * No automatic adoption, human client, forced chunks, block changes or real JVM crash claim. */
public final class LivestockSavedAgesProbe extends JavaPlugin implements Listener {
    private boolean authorized,visible,projectorScheduled,deathsScheduled;private Chunk chunk;private Location home;
    private final List<Cow> initial=new ArrayList<>();private final Map<UUID,Integer> realRoster=new HashMap<>();private Cow initialYoung;private PaperLivestockPresence presence;private boolean presenceScheduled;private int accountedPeriod;
    private UUID world,business;private Path data;private PopulationSession session;private PaperLivestockNewborns projector;private ManagedLivestockDeaths deaths;
    private final Set<UUID> expectedBirths=new HashSet<>();private final List<Animals> created=new ArrayList<>();private Animals first,second;
    private LivestockBirth a,b;private Projection pa,pb;private IndustryRepository.Store opening;private long openingHeads,feedUsed,products;
    private CompletableFuture<?> work;private int stage,ticks,nextPeriod=8,waitUntil;
    private static final NamespacedKey BIRTH=new NamespacedKey("civilizations","livestock_birth"),PENDING=new NamespacedKey("civilizations","livestock_birth_pending");
    private void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
            &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked no-player localhost fixture required");authorized=true;
        var level=getServer().getWorlds().getFirst();world=level.getUID();var spawn=level.getSpawnLocation();int cx=Math.floorDiv(spawn.getBlockX(),16),cz=Math.floorDiv(spawn.getBlockZ(),16);
        check(level.isChunkLoaded(cx,cz)||level.loadChunk(cx,cz,false),"Existing generated fixture chunk required");chunk=level.getChunkAt(cx,cz);chunk.addPluginChunkTicket(this);
        for(int x=cx*16+2;x<cx*16+14&&home==null;x++)for(int z=cz*16+2;z<cz*16+14&&home==null;z++) {
            int y=level.getHighestBlockYAt(x,z)+1;var floor=level.getBlockAt(x,y-1,z).getType();
            if(y+1<level.getMaxHeight()&&floor.isOccluding()&&floor!=Material.MAGMA_BLOCK&&floor!=Material.CACTUS
                &&level.getBlockAt(x,y,z).getType().isAir()&&level.getBlockAt(x,y+1,z).getType().isAir())home=new Location(level,x+0.5,y,z+0.5);
        }
        check(home!=null,"Existing safe natural fixture floor required; no block changes permitted");
        check(home.getBlockY()+14<home.getWorld().getMaxHeight(),"Initial fixture sky positions fit existing world");
        for(int n=0;n<3;n++) {
            var cow=home.getWorld().createEntity(home.clone().add(0,8+n*2,0),Cow.class);cow.setAI(false);cow.setGravity(false);cow.setPersistent(true);cow.setAgeLock(false);
            if(n==0)cow.setBaby();else cow.setAdult();cow.setCustomName("Explicit initial-age fixture");
            initial.add(cow);realRoster.put(cow.getUniqueId(),n==2?9:0);if(n==2)initialYoung=cow;
        }
        data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh fixture SQL required");int x=home.getBlockX(),y=home.getBlockY(),z=home.getBlockZ();work=CompletableFuture.runAsync(()->seed(x,y,z));
        getServer().getPluginManager().registerEvents(this,this);
        new BukkitRunnable(){public void run(){try {
            check(Bukkit.isPrimaryThread()&&getServer().getOnlinePlayers().isEmpty(),"Main-thread empty fixture required");check(++ticks<=2000,"Projection timeout stage="+stage);step();
            if(stage==99){getLogger().info("LIVESTOCK_SAVED_AGES_PROBE_PASSED three real initial cows corrected from saved age/maturity without changing names or movement flags; exact period9 maturation, native stale-view rejection and native unspawned snapshot round-trip of age/lock/PDC; two automatic newborns, controller restart, paid feed/output, missing body and confirmed death regression; not automatic adoption, actual chunk unload or JVM crash proof");cancel();getServer().shutdown();}
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_SAVED_AGES_PROBE_FAILED stage="+stage,failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,10,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_SAVED_AGES_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void spawned(CreatureSpawnEvent event) {
        if(event.isCancelled()||!(event.getEntity() instanceof Animals animal)||!animal.getWorld().getUID().equals(world))return;
        var tag=animal.getPersistentDataContainer().get(BIRTH,PersistentDataType.STRING);if(tag==null)return;
        if(expectedBirths.stream().anyMatch(id->id.toString().equals(tag))&&!created.contains(animal))created.add(animal);
    }
    private boolean ready(){if(!work.isDone())return false;work.join();return true;}
    private void start(){session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);session.configureBusinessOperations(BusinessOperationPolicy.prototype());work=session.start(false,Set.of(world));}
    private void adapters(){presence=new PaperLivestockPresence(session,getLogger());presence.runTaskTimer(this,1,1);presenceScheduled=true;deaths=new ManagedLivestockDeaths(session,getLogger());getServer().getPluginManager().registerEvents(deaths,this);deaths.runTaskTimer(this,1,1);deathsScheduled=true;projector=new PaperLivestockNewborns(session,()->true,p->visible,getLogger());projector.runTaskTimer(this,1,1);projectorScheduled=true;}
    private void stop(){if(presence!=null){if(presenceScheduled)presence.cancel();presenceScheduled=false;presence.flush();presence=null;}if(projector!=null){if(projectorScheduled)projector.cancel();projectorScheduled=false;projector.close();projector=null;}if(deaths!=null){if(deathsScheduled)deaths.cancel();deathsScheduled=false;HandlerList.unregisterAll(deaths);deaths.flush();deaths=null;}if(session!=null){session.close();session=null;}}
    @SuppressWarnings("unchecked") private Optional<Projection> saved(){return (Optional<Projection>)work.join();}
    private boolean released(Projection p) { var entity=Bukkit.getEntity(p.claim().entity());return entity!=null&&entity.isValid()&&!entity.getPersistentDataContainer().has(PENDING); }
    private Animals body(Projection p) {
        var entity=Bukkit.getEntity(p.claim().entity());check(entity instanceof Cow&&entity.isValid()&&!entity.isDead(),"Actual same saved live cow UUID required");var animal=(Animals)entity;
        check(animal.hasAI()&&animal.hasGravity()&&!animal.isInvulnerable(),"Activation releases movement without invulnerability");
        check(animal.getAgeLock()&&!animal.isAdult()&&!animal.getPersistentDataContainer().has(PENDING),"Saved young age and no pending marker after activation");if(!created.contains(animal))created.add(animal);return animal;
    }
    private void step()throws Exception {
        switch(stage) {
            case 0->{if(!ready())return;expectedBirths.add(a.id());expectedBirths.add(b.id());start();stage=1;}
            case 1->{if(!ready())return;check(session.livestockCaptureReady(),"Fresh fixture history trusted");adapters();check(session.villageAnimals().containsAll(realRoster.keySet()),"Initial village identities published before actual add");for(var cow:initial){check(!cow.isValid(),"Initial fixture body unadded before owner publication");home.getWorld().addEntity(cow);check(cow.isValid(),"Actual initial fixture cow added after saved ownership");}waitUntil=ticks+12;work=session.livestockBirthProjection(a.id());stage=2;}
            case 2->{if(!ready())return;check(saved().isEmpty(),"No nearby observer means no new claim");if(ticks<waitUntil||initial.stream().anyMatch(c->!c.getAgeLock())||!initial.getFirst().isAdult()||initialYoung.isAdult()){work=session.livestockBirthProjection(a.id());return;}check(initial.stream().allMatch(c->!c.hasAI()&&!c.hasGravity()&&!c.isInvulnerable()&&"Explicit initial-age fixture".equals(c.getCustomName())),"Initial saved age preserves names and movement/protection flags");check(created.isEmpty(),"Invisible fixture creates no bodies");visible=true;work=session.livestockBirthProjection(a.id());stage=3;}
            case 3->{if(!ready())return;var p=saved();if(p.isEmpty()||p.get().state()!=State.ACTIVE||!released(p.get())){work=session.livestockBirthProjection(a.id());return;}pa=p.get();first=body(pa);first.setAI(false);first.setGravity(false);work=session.livestockBirthProjection(b.id());stage=4;}
            case 4->{if(!ready())return;var p=saved();if(p.isEmpty()||p.get().state()!=State.ACTIVE||!released(p.get())){work=session.livestockBirthProjection(b.id());return;}pb=p.get();second=body(pb);second.setAI(false);second.setGravity(false);check(created.size()==2,"Exactly two actual native births");stop();work=CompletableFuture.runAsync(()->audit(false));stage=5;}
            case 5->{if(!ready())return;start();stage=6;}
            case 6->{if(!ready())return;adapters();work=session.livestockBirthProjection(a.id());stage=7;}
            case 7->{if(!ready())return;check(saved().orElseThrow().equals(pa),"Restart retains exact first physical identity");work=session.livestockBirthProjection(b.id());stage=8;}
            case 8->{if(!ready())return;check(saved().orElseThrow().equals(pb),"Restart retains exact second physical identity");work=advance(nextPeriod);stage=9;}
            case 9->{if(!ready())return;var p=(LivestockHerd.Plan)work.join();if(accountedPeriod!=nextPeriod){feedUsed+=p.feedUsed();products+=p.renewableUnits();accountedPeriod=nextPeriod;}check(p.births()==0,"Retuned zero-birth verification avoids unrelated new children");var clock=initialYoung.getPersistentDataContainer().get(new NamespacedKey("civilizations","livestock_age_clock"),PersistentDataType.STRING);if(!("v1|"+business+"|9|"+nextPeriod).equals(clock))return;check(initialYoung.isAdult()==(nextPeriod>=9),"Initial youngster matures at saved period9, not native time");
                if(++nextPeriod<=13){work=advance(nextPeriod);return;}stage=10;}
            case 10->{if(!first.isAdult())return;
                check(initialYoung.isAdult()&&initialYoung.getAgeLock(),"Original young animal reached saved maturity");
                var ageKey=new NamespacedKey("civilizations","livestock_age_clock");var ageClock=initialYoung.getPersistentDataContainer().get(ageKey,PersistentDataType.STRING);
                var ageType=getServer().getPluginManager().getPlugin("Civilizations").getClass().getClassLoader().loadClass("com.blueninjas24.civilizations.paper.LivestockAgeReconciler");
                var apply=ageType.getDeclaredMethod("apply",Animals.class,UUID.class,long.class,long.class);apply.setAccessible(true);check(Boolean.FALSE.equals(apply.invoke(null,initialYoung,business,8L,9L)),"Actual native body rejects stale saved age view");
                check(initialYoung.isAdult()&&ageClock.equals(initialYoung.getPersistentDataContainer().get(ageKey,PersistentDataType.STRING)),"Stale view cannot undo physical maturity or native clock");
                var snapshot=initialYoung.createSnapshot();check(snapshot!=null,"Native animal snapshot available");var copy=snapshot.createEntity(home.getWorld());check(copy instanceof Cow&&!copy.isValid(),"Native serialization copy remains unspawned");
                var copied=(Cow)copy;check(copied.isAdult()&&copied.getAgeLock()&&ageClock.equals(copied.getPersistentDataContainer().get(ageKey,PersistentDataType.STRING)),"Saved age/lock/PDC round-trip through actual native serialization");check(!second.isAdult()&&first.getAgeLock()&&second.getAgeLock(),"Only saved first maturity reached at period13");check(!first.hasAI()&&!first.hasGravity(),"Ordinary active reconciliation preserves external movement flags");first.remove();waitUntil=ticks+24;work=session.livestockBirthProjection(a.id());stage=11;}
            case 11->{if(!ready())return;check(saved().orElseThrow().equals(pa),"Missing active body remains same claim");check(Bukkit.getEntity(pa.claim().entity())==null&&created.size()==2,"Removed body never replaced or inferred dead");if(ticks<waitUntil){work=session.livestockBirthProjection(a.id());return;}second.setHealth(0);work=session.livestockBirthProjection(b.id());stage=12;}
            case 12->{if(!ready())return;if(saved().orElseThrow().state()!=State.DECEASED){work=session.livestockBirthProjection(b.id());return;}waitUntil=ticks+12;work=session.livestockBirthProjection(a.id());stage=13;}
            case 13->{if(!ready())return;check(saved().orElseThrow().equals(pa)&&created.size()==2,"Other child death does not replace unknown body");if(ticks<waitUntil){work=session.livestockBirthProjection(a.id());return;}stop();work=CompletableFuture.runAsync(()->audit(true));stage=14;}
            case 14->{if(!ready())return;stage=99;}
            default->throw new IllegalStateException("Unknown fixture stage");
        }
    }
    private CompletableFuture<LivestockHerd.Plan> advance(int period) {
        var p=LivestockPolicy.prototype();var noBirth=new LivestockPolicy(true,p.maturityPeriods(),p.breedingDelayPeriods(),p.feedPerBirth(),p.workPerBirth(),0,p.cow(),p.chicken());
        return session.advanceLivestock(UUID.nameUUIDFromBytes(("fixture-period-"+period).getBytes(java.nio.charset.StandardCharsets.UTF_8)),business,period,10,10,noBirth);
    }
    private void seed(int x,int y,int z) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var capture=new LivestockCapture(db.getConnection());check(capture.begin(1000).trusted(),"Explicit empty history trusted");UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID();business=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Explicit newborn projector fixture","Fixture",x,y,z,2,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Explicit fixture worker",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,world,x,y,z,1,"Existing natural floor for trusted fixture, not live adoption");var rules=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(business,town,"Fixture dairy",owner,owner,"livestock",site.id(),1,0),site,rules,rules,0,"Explicit fixture founding"));db.getBusinessRepository().hire(UUID.randomUUID(),business,worker,0);
            var grain=new Commodity("grain");db.getIndustryRepository().openStore(business,256,Map.of(grain,1L,new Commodity("milk"),1L),0);var roster=new HashMap<UUID,Integer>();
            for(var entry:realRoster.entrySet()){var id=entry.getKey();roster.put(id,entry.getValue());new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1,"Explicit real initial fixture identity; not automatic adoption"));}
            var herd=new LivestockRepository(db.getConnection());herd.open(business,LivestockHerd.Species.COW,grain,8,roster);
            try(var s=db.getConnection().prepareStatement("UPDATE industry_stock SET units=100 WHERE business_id=? AND commodity='grain'")){s.setString(1,business.toString());s.executeUpdate();}
            for(int period=1;period<=7;period++)herd.advance(UUID.randomUUID(),business,period,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
            var births=new LivestockBirths(db.getConnection()).page(business,0,-1,8);check(births.size()==2,"Two real paid fixture births");a=births.getFirst();b=births.getLast();opening=db.getIndustryRepository().store(business).orElseThrow();openingHeads=herd.load(business).orElseThrow().state().heads();capture.finish(2000,true);
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    private void audit(boolean completed) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var projections=new LivestockBirthProjections(db.getConnection());check(projections.find(a.id()).orElseThrow().equals(pa),"Original unknown body remains active and never replaced");
            var other=projections.find(b.id()).orElseThrow();check(other.claim().equals(pb.claim())&&other.state()==(completed?State.DECEASED:State.ACTIVE),"Exact second identity/current outcome");
            var herd=new LivestockRepository(db.getConnection()).load(business).orElseThrow();check(herd.state().heads()==openingHeads-(completed?1:0),"Only actual confirmed death reduces herd");
            var store=db.getIndustryRepository().store(business).orElseThrow();check(store.hundredths()==opening.hundredths()&&store.capacity()==opening.capacity(),"Projection never changes money/capacity");
            check(store.units().get(new Commodity("grain"))==opening.units().get(new Commodity("grain"))-feedUsed&&store.units().get(new Commodity("milk"))==opening.units().get(new Commodity("milk"))+products,"Exact real feed and product balances");
            for(var count:Map.of("livestock_births",2,"livestock_birth_entities",2,"livestock_birth_projections",2,"livestock_birth_losses",completed?1:0).entrySet())try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+count.getKey())){check(r.next()&&r.getInt(1)==count.getValue(),"Exact "+count.getKey());}
            check(new LivestockBirths(db.getConnection()).find(a.id()).orElseThrow().equals(a)&&new LivestockBirths(db.getConnection()).find(b.id()).orElseThrow().equals(b),"Maturity/identity fixed through retuning");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){check(r.next()&&r.getString(1).equals("ok"),"Fixture integrity");}try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onDisable(){try {
        stop();HandlerList.unregisterAll((Listener)this);for(var cow:initial)if(cow.isValid())cow.remove();check(initial.stream().noneMatch(Entity::isValid),"Exact initial fixture bodies removed");
        // An early failure may precede the probe's ACTIVE acknowledgement. Read only this fresh
        // fixture's aliases, require its expected birth tag, and clean those exact native UUIDs.
        if(data!=null&&Files.isRegularFile(data.resolve("civilizations.db")))try {
            var readOnly=new org.sqlite.SQLiteConfig();readOnly.setReadOnly(true);
            try(var c=java.sql.DriverManager.getConnection("jdbc:sqlite:"+data.resolve("civilizations.db"),readOnly.toProperties());var s=c.createStatement();var r=s.executeQuery("SELECT entity_id,birth_id FROM livestock_birth_entities")) {
                while(r.next()) {
                    var birth=UUID.fromString(r.getString(2));if(!expectedBirths.contains(birth))continue;
                    var entity=Bukkit.getEntity(UUID.fromString(r.getString(1)));
                    if(entity instanceof Animals animal&&birth.toString().equals(animal.getPersistentDataContainer().get(BIRTH,PersistentDataType.STRING))&&!created.contains(animal))created.add(animal);
                }
            }
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Fixture alias cleanup read failed",failure);}
        for(var animal:created)if(animal.isValid())animal.remove();check(created.stream().noneMatch(Entity::isValid),"Only explicitly tagged created fixture bodies removed");
        getLogger().info("LIVESTOCK_SAVED_AGES_FIXTURE_RESTORED exact fixture bodies removed; SQL/backups retained; no blocks changed");
    }finally{if(chunk!=null)chunk.removePluginChunkTicket(this);}}
}
