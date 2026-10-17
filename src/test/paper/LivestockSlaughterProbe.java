import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Guarded actual native death/drop proof against an isolated private livestock database. */
public final class LivestockSlaughterProbe extends JavaPlugin {
    private boolean authorized; private World world; private UUID worldId; private Path data; private int sx,sy,sz;
    private final UUID town=UUID.randomUUID(),herd=UUID.randomUUID(),worker=UUID.randomUUID();
    private final AtomicLong now=new AtomicLong(1000); private final List<UUID> bodies=new ArrayList<>();
    private final Set<UUID> initialLooseEntities=new HashSet<>();
    private final Map<UUID,Integer> drops=new HashMap<>(); private final Set<UUID> controlled=new HashSet<>();
    private PopulationSession session; private PaperLivestockSlaughter slaughter; private ManagedLivestockDeaths deaths;
    private CompletableFuture<?> work; private int stage,ticks; private boolean observedControlledDrops,vanillaLootEnabled,cancelOnce=true,cancelled;
    private UUID ordinaryBody;private final Set<UUID> fixtureLoose=new HashSet<>();
    private Chunk fixtureChunk;private boolean ticketAdded;
    private final Listener events=new Listener() {
        @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
        public void guard(EntityDeathEvent event) {if(cancelOnce&&stage==1&&bodies.contains(event.getEntity().getUniqueId())){event.setReviveHealth(1);event.setCancelled(true);cancelOnce=false;cancelled=true;}}
        @EventHandler(priority=EventPriority.MONITOR)
        public void record(EntityDeathEvent event) {
            if(bodies.contains(event.getEntity().getUniqueId())&&!event.isCancelled()) {
                drops.put(event.getEntity().getUniqueId(),event.getDrops().size());
                if(stage==4){controlled.add(event.getEntity().getUniqueId());if(!event.getDrops().isEmpty()||event.getDroppedExp()!=0)observedControlledDrops=true;}
            }
        }
    };
    private void check(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");
        authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try{prepare();}catch(Throwable failure){fail(failure);}},60);
    } catch(Throwable failure){fail(failure);}}
    private void prepare()throws Exception {
        world=getServer().getWorlds().getFirst();worldId=world.getUID();
        var spawn=world.getSpawnLocation();sx=spawn.getBlockX();sy=spawn.getBlockY();sz=spawn.getBlockZ();int cx=Math.floorDiv(sx,16),cz=Math.floorDiv(sz,16);
        // Test setup only: an empty server need not keep spawn loaded. Production adapter never loads chunks.
        fixtureChunk=world.getChunkAt(cx,cz);ticketAdded=fixtureChunk.addPluginChunkTicket(this);
        check(fixtureChunk.isLoaded(),"One explicit disposable fixture chunk required");
        vanillaLootEnabled=Boolean.TRUE.equals(world.getGameRuleValue(GameRule.DO_MOB_LOOT));
        var centerX=cx*16+8; var centerZ=cz*16+8; var y=world.getHighestBlockYAt(centerX,centerZ)+4;
        sx=centerX;sy=y;sz=centerZ;var center=new Location(world,sx,sy,sz);
        world.getEntitiesByClass(Item.class).stream().filter(e->e.getLocation().distanceSquared(center)<256).forEach(e->initialLooseEntities.add(e.getUniqueId()));
        world.getEntitiesByClass(ExperienceOrb.class).stream().filter(e->e.getLocation().distanceSquared(center)<256).forEach(e->initialLooseEntities.add(e.getUniqueId()));
        for(int i=0;i<3;i++) {
            final int n=i; var cow=world.spawn(new Location(world,centerX+.5+n, y,centerZ+.5),Cow.class,c->{c.setAdult();c.setAgeLock(true);c.setAI(false);c.setGravity(false);c.setPersistent(true);});
            bodies.add(cow.getUniqueId());
        }
        data=getDataFolder().toPath().resolve("private-"+UUID.randomUUID()); check(!Files.exists(data),"Fresh private database required");
        work=CompletableFuture.runAsync(()->{try {seed(); session=open();now.set(2000);session.pulse().get(10,TimeUnit.SECONDS);} catch(Exception e){throw new CompletionException(e);}});
        getServer().getScheduler().runTaskTimer(this,()->{try {
            check(++ticks<900&&getServer().getOnlinePlayers().isEmpty(),"Bounded no-player fixture; stage="+stage);
            step();
        } catch(Throwable failure){fail(failure);}},1,1);
    }
    private void step() throws Exception {
        if(stage==0) {
            if(!work.isDone())return; work.join(); check(session.livestockCaptureReady(),"Clean private capture required");
            deaths=new ManagedLivestockDeaths(session,getLogger(),event->slaughter!=null&&slaughter.observeDeath(event));
            getServer().getPluginManager().registerEvents(deaths,this);
            getServer().getPluginManager().registerEvents(events,this);startController();stage=1;ticks=0;
        }
        if(stage==1) {
            slaughter.run();deaths.run();
            if(cancelled){slaughter.close();slaughter=null;work=CompletableFuture.supplyAsync(()->{try{session.pulse().get(10,TimeUnit.SECONDS);return state();}catch(Exception e){throw new CompletionException(e);}});stage=2;ticks=0;}
        } else if(stage==2) {
            if(!work.isDone())return;var v=(long[])work.join();check(v[0]==0&&v[1]==0&&v[2]==0&&v[3]==3&&v[4]==0&&v[5]==1&&aliveCount()==3,"Cancelled native death retains every head and no food; exact cancellation releases the permit");
            work=CompletableFuture.runAsync(()->{try{now.set(3000);session.pulse().get(10,TimeUnit.SECONDS);}catch(Exception e){throw new CompletionException(e);}});stage=3;
        } else if(stage==3) {
            if(!work.isDone())return;work.join();startController();stage=4;
        } else if(stage==4) {
            slaughter.run();deaths.run();
            if(slaughter.diagnostic().contains("confirmed bodies=1")&&deaths.diagnostic().contains("pending: 0")) {
                check(controlled.size()==1&&aliveCount()==2,"One confirmed exact body dies; the other two remain alive");
                check(drops.getOrDefault(controlled.iterator().next(),-1)==0&&!observedControlledDrops,"Registered controller/death listener suppresses native drops and XP");
                check(loose().isEmpty(),"No new native item or XP entities around fixture");
                slaughter.close(); slaughter=null;
                work=CompletableFuture.supplyAsync(()->{try{now.set(4000);session.pulse().get(10,TimeUnit.SECONDS);return adultCount();}catch(Exception e){throw new CompletionException(e);}});stage=5;ticks=0;
            }
        } else if(stage==5) {
            if(!work.isDone())return;check((Long)work.join()==2,"Saved herd applies the confirmed loss and retains two adults");
            var cow=bodies.stream().map(Bukkit::getEntity).filter(Cow.class::isInstance).map(Cow.class::cast).filter(e->e.isValid()&&!e.isDead()).findFirst().orElseThrow();ordinaryBody=cow.getUniqueId();cow.setHealth(0);stage=6;ticks=0;
        } else if(stage==6) {
            deaths.run();
            if(drops.containsKey(ordinaryBody)) {
                if(deaths.diagnostic().contains("pending: 0")) {
                    fixtureLoose.addAll(loose());work=CompletableFuture.supplyAsync(()->{try{now.set(5000);session.pulse().get(10,TimeUnit.SECONDS);return adultCount();}catch(Exception e){throw new CompletionException(e);}});stage=7;
                }
            }
        } else if(stage==7) {
            if(!work.isDone())return;check((Long)work.join()==1,"Ordinary saved loss reduces herd to one adult");
            cleanupSession();work=CompletableFuture.supplyAsync(()->{try{return state();}catch(Exception e){throw new CompletionException(e);}});stage=8;
        } else if(stage==8) {
            if(!work.isDone())return; var v=(long[])work.join();
            check(v[0]==1&&v[1]==2&&v[2]==4&&v[3]==1&&v[4]==1&&v[5]==1,"Ordinary death recorded once without business meat");
            check((drops.getOrDefault(ordinaryBody,0)>0)==vanillaLootEnabled,"Ordinary death follows the unchanged mob-loot gamerule");
            cleanup();getLogger().info("LIVESTOCK_SLAUGHTER_PROBE_PASSED cancelled death retains body/zero goods, exact native controlled death suppresses drops/XP, one confirmed permit and four food units, ordinary death retains native drops without extra food; private SQL and exact fixture cleanup");getServer().shutdown();stage=9;
        }
    }
    private void startController(){slaughter=new PaperLivestockSlaughter(session,new LivestockSlaughterPolicy(true,20,10,5000),LivestockWorkingArea.prototype(),getLogger(),now::get);slaughter.setDeathSink(deaths::captureControlled);}
    private Set<UUID> loose(){var ids=new HashSet<UUID>();for(var e:world.getNearbyEntities(new Location(world,sx,sy,sz),16,16,16))if((e instanceof Item||e instanceof ExperienceOrb)&&!initialLooseEntities.contains(e.getUniqueId()))ids.add(e.getUniqueId());return ids;}
    private PopulationSession open() throws Exception {
        var s=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),now::get);
        s.configureLivestockWork(LivestockPolicy.prototype(),new LivestockRuntimePolicy(true,10,10));
        s.configureLivestockSlaughter(new LivestockSlaughterPolicy(true,20,10,5000));
        s.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(),
                new WorkshopSupplyPolicy(true,1,8,Map.of(new Commodity("grain"),25L))));
        s.configureBusinessOperations(BusinessOperationPolicy.prototype());
        s.start(false,Set.of(worldId)).get(10,TimeUnit.SECONDS); return s;
    }
    private void seed() throws Exception {
        var db=new DatabaseManager(data.toFile(),getLogger()); try {
            db.initialize(); new LivestockCapture(db.getConnection()).begin(1000);
            db.saveSettlement(new Settlement(town,"Private slaughter fixture","Fixture",sx,sy,sz,1,0,0));
            db.getVillageIdentityRepository().observeWorld(worldId,"Fixture"); db.getVillageIdentityRepository().bindSettlement(town,worldId);
            db.getPopulationRepository().insertResident(new Resident(worker,"Private qualified farmer",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(worker,"farming",1);
            var site=new BusinessSite(UUID.randomUUID(),town,worldId,sx,sy,sz,1000,"Private fixture workplace");
            var req=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(herd,town,"Private cow herd",worker,worker,"livestock",site.id(),1,0),site,req,req,0,"Private fixture"));
            db.getBusinessRepository().hire(UUID.randomUUID(),herd,worker,0);
            db.getIndustryRepository().openStore(herd,256,Map.of(new Commodity("grain"),1L,new Commodity("milk"),1L,Commodity.FOOD,1L),0);
            try(var q=db.getConnection().createStatement()){q.executeUpdate("UPDATE industry_stock SET units=100 WHERE business_id='"+herd+"' AND commodity='grain'");}
            for(var id:bodies)new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,worldId,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit private slaughter fixture"));
            new LivestockRepository(db.getConnection()).open(herd,LivestockHerd.Species.COW,new Commodity("grain"),8,Map.of(bodies.get(0),0,bodies.get(1),0,bodies.get(2),0));
            new LivestockCapture(db.getConnection()).finish(1000,true);
        } finally {db.close();}
    }
    private long[] state() throws Exception {
        var db=new DatabaseManager(data.toFile(),getLogger()); try {db.initialize();
            long confirmed=scalar(db,"SELECT COUNT(*) FROM livestock_slaughter WHERE state='CONFIRMED'");
            long actual=scalar(db,"SELECT COUNT(*) FROM livestock_animal_losses WHERE animal_id IN ('"+bodies.get(0)+"','"+bodies.get(1)+"','"+bodies.get(2)+"')");
            long food=scalar(db,"SELECT units FROM industry_stock WHERE business_id='"+herd+"' AND commodity='food'");
            long adults=new LivestockRepository(db.getConnection()).load(herd).orElseThrow().state().adults();
            long intents=scalar(db,"SELECT COUNT(*) FROM livestock_slaughter WHERE state IN ('CONFIRMED','PENDING')");
            long cancelled=scalar(db,"SELECT COUNT(*) FROM livestock_slaughter WHERE state='CANCELLED'");
            return new long[]{confirmed,actual,food,adults,intents,cancelled};
        } finally {db.close();}
    }
    private long scalar(DatabaseManager db,String sql)throws Exception {try(var q=db.getConnection().createStatement();var r=q.executeQuery(sql)){check(r.next(),"SQL result missing");return r.getLong(1);}}
    private long adultCount() throws Exception {var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();return new LivestockRepository(db.getConnection()).load(herd).orElseThrow().state().adults();}finally{db.close();}}
    private int aliveCount(){return (int)bodies.stream().map(Bukkit::getEntity).filter(Cow.class::isInstance).map(Cow.class::cast).filter(e->e.isValid()&&!e.isDead()).count();}
    private void cleanupSession(){if(slaughter!=null){slaughter.close();slaughter=null;}if(deaths!=null){deaths.flush();}if(session!=null){session.close();session=null;}}
    private void cleanup(){cleanupSession();
        for(var id:bodies){var entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}
        for(var id:fixtureLoose){var entity=Bukkit.getEntity(id);if(entity!=null&&entity.isValid())entity.remove();}
        if(fixtureChunk!=null&&ticketAdded){fixtureChunk.removePluginChunkTicket(this);ticketAdded=false;}}
    private void fail(Throwable t){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_SLAUGHTER_PROBE_FAILED stage="+stage,t);cleanup();if(authorized)getServer().shutdown();}
    @Override public void onDisable(){cleanup();}
}
