import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;

/** Guarded disposable actual natural animals + production observer + private paid startup. Not a player client. */
public final class LivestockAdoptionProbe extends JavaPlugin {
    private boolean authorized;private World world;private Chunk chunk;private Path data;
    private UUID worldId;private final UUID town=UUID.randomUUID(),person=UUID.randomUUID();private int x,y,z,ticks,stage;
    private final Map<Block,BlockData> originals=new LinkedHashMap<>();private final List<Cow> cows=new ArrayList<>();
    private PopulationSession session;private PaperLivestockAdoption adoption;private CompletableFuture<?> work;private Block fence;
    private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
            &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try{prepare();getServer().getScheduler().runTaskTimer(this,()->{
            try{check(++ticks<600&&getServer().getOnlinePlayers().isEmpty(),"Bounded no-player check stage="+stage);step();}
            catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_ADOPTION_PROBE_FAILED stage="+stage,failure);restore();getServer().shutdown();}
        },1,1);}catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_ADOPTION_PROBE_FAILED",failure);restore();getServer().shutdown();}},60);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_ADOPTION_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void prepare()throws Exception{
        world=getServer().getWorlds().getFirst();worldId=world.getUID();var chunks=world.getLoadedChunks();
        if(chunks.length>0)chunk=chunks[0];else{var at=world.getSpawnLocation();int cx=Math.floorDiv(at.getBlockX(),16),cz=Math.floorDiv(at.getBlockZ(),16);check(world.isChunkGenerated(cx,cz)&&world.loadChunk(cx,cz,false),"Existing generated test chunk required");chunk=world.getChunkAt(cx,cz);}
        chunk.addPluginChunkTicket(this);x=chunk.getX()*16+8;z=chunk.getZ()*16+8;y=world.getMaxHeight()-16;
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-1;dy<=2;dy++){
            var b=world.getBlockAt(x+dx,y+dy,z+dz);check(b.getType()==Material.AIR,"Empty sky fixture required");originals.put(b,b.getBlockData().clone());
        }
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)world.getBlockAt(x+dx,y-1,z+dz).setType(Material.GRASS_BLOCK,false);
        fence=world.getBlockAt(x-1,y,z+1);fence.setType(Material.OAK_FENCE,false);
        for(int n=0;n<3;n++){final int index=n;
            cows.add(world.spawn(new Location(world,x+0.5+(n==1?1:0),y,z+0.5+(n==2?1:0)),Cow.class,SpawnReason.NATURAL,c->{c.setAdult();c.setAgeLock(true);c.setAI(false);c.setGravity(false);c.setPersistent(true);if(index==2)c.setCustomName("Protected named test animal");}));
        }
        check(new PaperFreeRangeSource(world).cell(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(x-1,y,z+1))==FreeRangeScan.Cell.BARRIER,"Actual fence must block clearance");
        data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh private database required");
        work=CompletableFuture.runAsync(()->{try{seed();session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis,economy(),FarmObservation.Policy.prototype());
            if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))session.configureBusinessAllocation(BusinessAllocationPolicy.prototype());
            var policy=new LivestockAdoptionPolicy(true,8,2,2,8,1000,20,4,4096,500,2000,16,1,0,1);
            session.configureLivestockAdoption(policy);session.configureLivestockWork(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype());
            session.configureIndustryRuntime(new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(),new WorkshopSupplyPolicy(true,1,8,Map.of(new Commodity("grain"),25L))));
            session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());session.start(false,Set.of(worldId)).get(5,TimeUnit.SECONDS);
        }catch(Exception failure){throw new CompletionException(failure);}});
    }
    private EconomySettings economy(){var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var c:CommodityAccounting.Condition.values())bands.put(c,100);
        return new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,BigDecimal.ONE),bands);}
    private void seed()throws Exception{var db=new DatabaseManager(data.toFile(),getLogger());try{
        db.initialize();db.saveSettlement(new Settlement(town,"Private natural dairy","Fixture",x,y,z,1,0,0));db.getVillageIdentityRepository().observeWorld(worldId,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,worldId);
        db.getPopulationRepository().insertResident(new Resident(person,"Private farmer",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(person,"farming",1);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit disposable test budget");
        long now=System.currentTimeMillis();var economy=new EconomyRuntimeRepository(db.getConnection(),economy(),FarmObservation.Policy.prototype(),216);economy.observe(new FarmObservation(town,worldId,now-1000,true,0),now-1000);economy.startOnline(now-1000,Set.of(worldId));economy.pulse(now,false);
    }finally{db.close();}}
    private void step()throws Exception{
        if(stage==0){if(!work.isDone())return;work.join();check(session.livestockCaptureReady(),"Clean capture required");
            adoption=new PaperLivestockAdoption(session,new LivestockAdoptionPolicy(true,8,2,2,8,1000,20,4,4096,500,2000,16,1,0,1),getLogger());stage=1;ticks=0;}
        adoption.run();
        if(stage==1&&ticks>=80){check(session.villageAnimals().isEmpty(),"Fence must prevent actual adoption");fence.setType(Material.AIR,false);stage=2;}
        if(stage==2&&adoption.diagnostic().endsWith("opened 1")){
            check(session.villageAnimals().size()==2&&!session.villageAnimals().contains(cows.get(2).getUniqueId()),"Only two natural unnamed animals adopted");
            adoption.close();session.close();session=null;var db=new DatabaseManager(data.toFile(),getLogger());try{
                db.initialize();check(new AnimalOwnershipRepository(db.getConnection()).villageAnimals().equals(Set.of(cows.get(0).getUniqueId(),cows.get(1).getUniqueId())),"Permanent exact animal ownership");
                check(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==7500,"One real protected startup grant");
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT business_id FROM livestock_herds")){check(r.next(),"One saved herd");var herd=UUID.fromString(r.getString(1));check(!r.next(),"No duplicate herd");check(db.getIndustryRepository().store(herd).orElseThrow().hundredths()==2000,"Real starting cash");check(new LivestockRepository(db.getConnection()).load(herd).orElseThrow().state().adults()==2,"Only real initial adults");}
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Clean FKs");}
                if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*),SUM(demand_value) FROM resource_investment_reviews WHERE industry='livestock'")) {
                    check(r.next()&&r.getLong(1)==1&&r.getLong(2)==0,"One shared fallback herd startup audit without invented demand");
                }
            }finally{db.close();}
            restore();getLogger().info("LIVESTOCK_ADOPTION_PROBE_PASSED actual natural cows, fence exclusion, repeated bounded local clearance, named-animal protection, production PaperLivestockAdoption and PopulationSession, paid atomic herd startup, exact durable ownership/cash; private database; test blocks restored and exact test bodies removed");
            if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))getLogger().info("NATIVE SHARED LIVESTOCK SELECTION PASS: real repeated free-range proof and protected paid herd startup share one atomic selection audit; no fabricated demand, test bodies removed and blocks/ticket restored");
            getServer().shutdown();stage=3;
        }
    }
    private void restore(){
        if(adoption!=null){adoption.close();adoption=null;}if(session!=null){session.close();session=null;}
        int bodies=cows.size(),blocks=originals.size();
        for(var cow:cows)cow.remove();
        for(var entry:originals.entrySet())entry.getKey().setBlockData(entry.getValue(),false);
        boolean removed=cows.stream().noneMatch(Cow::isValid);
        boolean restored=originals.entrySet().stream().allMatch(e->e.getKey().getBlockData().getAsString().equals(e.getValue().getAsString()));
        cows.clear();originals.clear();if(chunk!=null){chunk.removePluginChunkTicket(this);chunk=null;}
        check(removed&&restored,"Exact fixture bodies/blocks must be restored");
        if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))getLogger().info("SHARED LIVESTOCK FIXTURE RESTORED: "+bodies+" exact test bodies removed; "+blocks+" original block states verified; fixture ticket released");
    }
}
