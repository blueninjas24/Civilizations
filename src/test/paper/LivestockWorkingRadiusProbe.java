import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.database.LivestockBirthProjections.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.ManagedLivestockDeaths;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Trusted fixture handoff with actual prepared/added cows and a real native death event. No automatic
 * adoption, site policy, production scheduler, general projection reconciliation or real JVM crash claim. */
public final class LivestockWorkingRadiusProbe extends JavaPlugin {
    private com.blueninjas24.civilizations.paper.PaperLivestockPresence presence;private boolean authorized,recorderScheduled;private PopulationSession session;private ManagedLivestockDeaths deaths;private Chunk chunk;
    private final List<Cow> created=new ArrayList<>();private Cow first,unspawned,second;private Location location;
    private Path data;private UUID world,business;private LivestockBirth a,b;private Claim firstClaim,failedClaim,secondClaim;
    private IndustryRepository.Store openingStore;private long openingHeads;private CompletableFuture<?> work;private int stage,ticks;
    private void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        var level=getServer().getWorlds().getFirst();world=level.getUID();var spawn=level.getSpawnLocation();int cx=Math.floorDiv(spawn.getBlockX(),16),cz=Math.floorDiv(spawn.getBlockZ(),16);
        check(level.isChunkLoaded(cx,cz)||level.loadChunk(cx,cz,false),"Existing generated test chunk required");chunk=level.getChunkAt(cx,cz);chunk.addPluginChunkTicket(this);
        location=new Location(level,cx*16+8.5,Math.min(level.getMaxHeight()-3,spawn.getBlockY()+32),cz*16+8.5);
        data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Unique fresh fixture directory required");int x=location.getBlockX(),y=location.getBlockY(),z=location.getBlockZ();work=CompletableFuture.runAsync(()->seed(x,y,z));
        new BukkitRunnable(){public void run(){try {
            check(Bukkit.isPrimaryThread()&&getServer().getOnlinePlayers().isEmpty(),"No-player main-thread native fixture required");check(++ticks<=1000,"Birth claim native timeout stage="+stage);step();
            if(stage==99){getLogger().info("LIVESTOCK_WORKING_RADIUS_PROBE_PASSED real prepared births and individual native death regression; bounded positive native sightings, actual cow moved away and returned inside loaded test chunk, durable away evidence before restart observation, retained ownership/heads/goods; not automatic adoption, scheduled production, chunk-unload or JVM crash proof");cancel();getServer().shutdown();}
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_WORKING_RADIUS_PROBE_FAILED stage="+stage,failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,10,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_WORKING_RADIUS_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private boolean ready(){if(!work.isDone())return false;work.join();return true;}
    private Cow prepare(LivestockBirth birth) {
        var cow=location.getWorld().createEntity(location,Cow.class);created.add(cow);cow.setBaby();cow.setAgeLock(true);cow.setAI(false);cow.setGravity(false);cow.setInvulnerable(true);cow.setPersistent(true);
        cow.setCustomName("Disposable newborn handoff fixture");cow.getPersistentDataContainer().set(new NamespacedKey("civilizations_fixture","birth"),PersistentDataType.STRING,birth.id().toString());
        check(!cow.isValid(),"Prepared cow has not entered the world");return cow;
    }
    private Claim request(LivestockBirth birth,Cow cow){return new Claim(birth.id(),cow.getUniqueId(),world,LivestockHerd.Species.COW,location.getBlockX(),location.getBlockY(),location.getBlockZ(),System.currentTimeMillis());}
    private void step()throws Exception {
        switch(stage) {
            case 0 -> {if(!ready())return;session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);work=session.start(false,Set.of(world));stage=1;}
            case 1 -> {if(!ready())return;check(session.livestockCaptureReady(),"Explicit fresh fixture capture trusted");
                deaths=new ManagedLivestockDeaths(session,getLogger());getServer().getPluginManager().registerEvents(deaths,this);deaths.runTaskTimer(this,1,1);recorderScheduled=true;
                first=prepare(a);firstClaim=request(a,first);work=session.claimLivestockBirth(firstClaim);stage=2;}
            case 2 -> {if(!ready())return;check(((Projection)work.join()).state()==State.CLAIMED&&session.villageAnimals().contains(first.getUniqueId()),"Committed village identity published before native add");
                check(!first.isValid(),"SQL cannot spawn animals");location.getWorld().addEntity(first);check(first.isValid(),"Native cow added once");work=session.activateLivestockBirth(a.id(),first.getUniqueId());stage=3;}
            case 3 -> {if(!ready())return;check(Boolean.TRUE.equals(work.join()),"First native cow activated");unspawned=prepare(b);failedClaim=request(b,unspawned);work=session.claimLivestockBirth(failedClaim);stage=4;}
            case 4 -> {if(!ready())return;check(!unspawned.isValid(),"Positive local evidence: failed candidate never added");work=session.failUnspawnedLivestockBirth(b.id(),unspawned.getUniqueId());stage=5;}
            case 5 -> {if(!ready())return;check(Boolean.TRUE.equals(work.join()),"Proven unspawned claim can retry without a new birth");second=prepare(b);secondClaim=request(b,second);work=session.claimLivestockBirth(secondClaim);stage=6;}
            case 6 -> {if(!ready())return;check(session.villageAnimals().contains(second.getUniqueId()),"Second claim publication precedes spawn");location.getWorld().addEntity(second);check(second.isValid(),"Second saved child has one body");work=session.activateLivestockBirth(b.id(),second.getUniqueId());stage=7;}
            case 7 -> {if(!ready())return;check(Boolean.TRUE.equals(work.join()),"Retry body activated");check(location.getWorld().getEntity(unspawned.getUniqueId())==null,"Failed candidate never materialized");
                first.setInvulnerable(false);first.setHealth(0);work=session.livestockBirthProjection(a.id());stage=8;}
            case 8 -> {if(!ready())return;@SuppressWarnings("unchecked")var result=(Optional<Projection>)work.join();check(result.orElseThrow().state()==State.DECEASED,"Actual native death reached the offspring ledger");
                deaths.run();check(session.livestockCaptureReady()&&second.isValid(),"Saved death drained capture fence and other child remains");
                work=session.activateLivestockBirth(a.id(),firstClaim.entity()).thenCompose(accepted->{check(!accepted,"No resurrection from stale activation");return session.failUnspawnedLivestockBirth(a.id(),firstClaim.entity());});stage=9;}
            case 9 -> {if(!ready())return;check(Boolean.FALSE.equals(work.join()),"Confirmed death cannot become unspawned failure");work=session.activateLivestockBirth(b.id(),failedClaim.entity());stage=10;}
            case 10 -> {if(!ready())return;check(Boolean.FALSE.equals(work.join()),"Stale failed candidate cannot activate the current child");
                deaths.cancel();recorderScheduled=false;org.bukkit.event.HandlerList.unregisterAll(deaths);deaths.flush();session.close();session=null;deaths=null;work=CompletableFuture.runAsync(this::audit);stage=11;}
            case 11 -> {if(!ready())return;session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);work=session.start(false,Set.of(world));stage=12;}
            case 12 -> {if(!ready())return;check(session.livestockCaptureReady()&&session.villageAnimals().contains(secondClaim.entity()),"Owner restart restores known body identities without repair");work=session.livestockBirthProjection(a.id());stage=13;}
            case 13 -> {if(!ready())return;@SuppressWarnings("unchecked")var saved=(Optional<Projection>)work.join();check(saved.orElseThrow().state()==State.DECEASED,"Restart cannot resurrect confirmed death");work=session.livestockBirthProjection(b.id());stage=14;}
            case 14 -> {if(!ready())return;@SuppressWarnings("unchecked")var saved=(Optional<Projection>)work.join();check(saved.orElseThrow().claim().equals(secondClaim)&&saved.orElseThrow().state()==State.ACTIVE,"Restart retains same surviving physical UUID");session.close();session=null;work=CompletableFuture.runAsync(this::audit);stage=15;}
            case 15 -> {if(!ready())return;session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);session.configureLivestockWorkingArea(new LivestockWorkingArea(2));work=session.start(false,Set.of(world));stage=16;}
            case 16 -> {if(!ready())return;presence=new com.blueninjas24.civilizations.paper.PaperLivestockPresence(session,getLogger());presence.runTaskTimer(this,1,1);work=session.livestockSight(secondClaim.entity());stage=17;}
            case 17 -> {if(!ready())return;@SuppressWarnings("unchecked")var seen=(Optional<LivestockPresence.Sight>)work.join();if(seen.isEmpty()){work=session.livestockSight(secondClaim.entity());return;}
                check(seen.get().position().x()==location.getBlockX(),"Actual live saved body positively seen at farm");check(second.teleport(location.clone().add(4,0,0)),"Actual cow moved within already loaded test chunk");work=session.livestockSight(secondClaim.entity());stage=18;}
            case 18 -> {if(!ready())return;@SuppressWarnings("unchecked")var seen=(Optional<LivestockPresence.Sight>)work.join();if(seen.isEmpty()||seen.get().position().x()!=location.getBlockX()+4){work=session.livestockSight(secondClaim.entity());return;}
                check(!new LivestockWorkingArea(2).contains(world,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(location.getBlockX(),location.getBlockY(),location.getBlockZ()),seen.get().world(),seen.get().position()),"Positive actual away sight, not a missing animal");
                presence.cancel();presence.flush();presence=null;session.close();session=null;work=CompletableFuture.runAsync(()->auditPosition(location.getBlockX()+4));stage=19;}
            case 19 -> {if(!ready())return;session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);session.configureLivestockWorkingArea(new LivestockWorkingArea(2));work=session.start(false,Set.of(world));stage=20;}
            case 20 -> {if(!ready())return;work=session.livestockSight(secondClaim.entity());stage=21;}
            case 21 -> {if(!ready())return;@SuppressWarnings("unchecked")var seen=(Optional<LivestockPresence.Sight>)work.join();check(seen.orElseThrow().position().x()==location.getBlockX()+4,"Away evidence survives owner restart before any new observation");
                check(second.teleport(location),"Actual saved body returned to original farm");presence=new com.blueninjas24.civilizations.paper.PaperLivestockPresence(session,getLogger());presence.runTaskTimer(this,1,1);work=session.livestockSight(secondClaim.entity());stage=22;}
            case 22 -> {if(!ready())return;@SuppressWarnings("unchecked")var seen=(Optional<LivestockPresence.Sight>)work.join();if(seen.isEmpty()||seen.get().position().x()!=location.getBlockX()){work=session.livestockSight(secondClaim.entity());return;}
                presence.cancel();presence.flush();presence=null;session.close();session=null;work=CompletableFuture.runAsync(()->auditPosition(location.getBlockX()));stage=23;}
            case 23 -> {if(!ready())return;stage=99;}
            default -> throw new IllegalStateException("Unknown native birth stage");
        }
    }
    private void seed(int x,int y,int z) {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var capture=new LivestockCapture(db.getConnection());check(capture.begin(1000).trusted(),"Explicit empty fixture begins trusted");UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID();business=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Explicit birth handoff fixture","Fixture",x,y,z,2,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Explicit fixture adult",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,world,x,y,z,1,"Trusted test site, not automatic adoption");var rules=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(business,town,"Fixture dairy",owner,owner,"livestock",site.id(),1,0),site,rules,rules,0,"Explicit fixture founding"));db.getBusinessRepository().hire(UUID.randomUUID(),business,worker,0);
            var grain=new Commodity("grain");db.getIndustryRepository().openStore(business,256,Map.of(grain,1L,new Commodity("milk"),1L),0);var roster=new HashMap<UUID,Integer>();
            for(int n=0;n<3;n++){var id=UUID.randomUUID();roster.put(id,n==2?2:0);new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1,"Explicit unmaterialized initial test identity"));}
            var herd=new LivestockRepository(db.getConnection());herd.open(business,LivestockHerd.Species.COW,grain,8,roster);
            try(var s=db.getConnection().prepareStatement("UPDATE industry_stock SET units=100 WHERE business_id=? AND commodity='grain'")){s.setString(1,business.toString());s.executeUpdate();}
            for(int period=1;period<=7;period++)herd.advance(UUID.randomUUID(),business,period,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
            var children=new LivestockBirths(db.getConnection()).page(business,0,-1,256);check(children.size()==2,"Two births from consumed fixture feed/work");a=children.getFirst();b=children.getLast();openingStore=db.getIndustryRepository().store(business).orElseThrow();openingHeads=herd.load(business).orElseThrow().state().heads();capture.finish(2000,true);
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    private void audit() {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var projections=new LivestockBirthProjections(db.getConnection());check(projections.find(a.id()).orElseThrow().state()==State.DECEASED,"Saved child death survives owner close/reopen");
            check(projections.find(b.id()).orElseThrow().claim().equals(secondClaim)&&projections.find(b.id()).orElseThrow().state()==State.ACTIVE,"Other child current body retained");
            check(projections.forEntity(failedClaim.entity()).orElseThrow().state()==State.FAILED,"Old failed alias preserved");
            check(new LivestockRepository(db.getConnection()).load(business).orElseThrow().state().heads()==openingHeads-1,"Only actual dead child removed");
            check(openingStore.equals(db.getIndustryRepository().store(business).orElseThrow()),"Native death minted no goods or cash");
            for(var expected:Map.of("livestock_births",2,"livestock_birth_entities",3,"livestock_birth_projections",2,"livestock_birth_losses",1).entrySet())
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+expected.getKey())){check(r.next()&&r.getInt(1)==expected.getValue(),"Exact retained "+expected.getKey());}
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){check(r.next()&&r.getString(1).equals("ok"),"Fixture integrity");}
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    private void auditPosition(int expectedX) {
        audit();var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();var sightings=new LivestockPresence(db.getConnection());var saved=sightings.find(secondClaim.entity()).orElseThrow();check(saved.position().x()==expectedX,"Latest actual positive position saved");
            check(sightings.record(List.of())==0&&sightings.find(secondClaim.entity()).orElseThrow().equals(saved),"Empty sightings do not turn absence into departure or return");
            check(sightings.find(firstClaim.entity()).isEmpty(),"Actual dead child not inferred as present");check(sightings.page(null,16).size()==4,"Only three retained original identities plus living child, no failed/dead aliases");
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onDisable(){try {
        if(presence!=null){presence.cancel();presence.flush();}
        if(deaths!=null){if(recorderScheduled)deaths.cancel();org.bukkit.event.HandlerList.unregisterAll(deaths);deaths.flush();}
        if(session!=null)session.close();for(var cow:created)if(cow.isValid())cow.remove();check(created.stream().noneMatch(Entity::isValid),"Only created test bodies removed");
        getLogger().info("LIVESTOCK_WORKING_RADIUS_FIXTURE_RESTORED test bodies removed; SQL fixture/backups retained; no blocks changed");
    }finally{if(chunk!=null)chunk.removePluginChunkTicket(this);}}
}
