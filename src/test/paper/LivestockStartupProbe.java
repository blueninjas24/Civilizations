import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Guarded disposable main-thread capture and trusted backend funding proof; no automatic adoption/live births. */
public final class LivestockStartupProbe extends JavaPlugin {
    private boolean authorized;private Chunk fixtureChunk;private final List<Animals> created=new ArrayList<>();
    private void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;var world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int cx=Math.floorDiv(spawn.getBlockX(),16),cz=Math.floorDiv(spawn.getBlockZ(),16);
        check(world.isChunkLoaded(cx,cz)||world.loadChunk(cx,cz,false),"Existing generated test chunk required");fixtureChunk=world.getChunkAt(cx,cz);fixtureChunk.addPluginChunkTicket(this);
        var location=new Location(world,cx*16+8.5,Math.min(world.getMaxHeight()-2,spawn.getBlockY()+32),cz*16+8.5);
        for(int kind=0;kind<2;kind++)for(int n=0;n<3;n++) {
            Animals animal=kind==0?world.spawn(location,Cow.class):world.spawn(location,Chicken.class);created.add(animal);
            animal.setAI(false);animal.setGravity(false);animal.setInvulnerable(true);animal.setCollidable(false);if(n==2)animal.setBaby();else animal.setAdult();animal.setAgeLock(true);
            if(kind==0&&n==0)animal.setCustomName("Disposable capture fixture");
        }
        new BukkitRunnable(){int ticks;CompletableFuture<Void> proof;@Override public void run(){try {
            check(++ticks<=600,"Livestock probe timeout");
            if(proof==null) {
                var frame=new PaperLivestockSource().capture(fixtureChunk,world.getUID(),System.currentTimeMillis(),256,32);if(!frame.complete())return;
                var visible=new HashMap<UUID,LivestockObservation.Animal>();for(var a:frame.animals())visible.put(a.id(),a);
                for(var animal:created)check(visible.containsKey(animal.getUniqueId()),"Actual animal identity captured");
                check(visible.get(created.getFirst().getUniqueId()).named(),"Native name protection evidence");
                check(!visible.get(created.get(2).getUniqueId()).adult()&&visible.get(created.get(2).getUniqueId()).ageTicks()<0,"Native young-age evidence");
                var selected=created.stream().map(a->visible.get(a.getUniqueId())).toList();
                proof=CompletableFuture.runAsync(()->verify(frame.world(),selected,location.getBlockX(),location.getBlockY(),location.getBlockZ(),frame.capturedMillis()));return;
            }
            if(!proof.isDone())return;proof.join();getLogger().info("LIVESTOCK_STARTUP_PROBE_PASSED actual main-thread cow/chicken capture, named/young evidence, protected real grants, atomic confirmed herds, real paid feed/delivery/products, restart replay and absence; trusted fixture ownership, no automatic adoption or physical births");cancel();getServer().shutdown();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_STARTUP_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_STARTUP_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void verify(UUID world,List<LivestockObservation.Animal> observed,int x,int y,int z,long now) {
        var config=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(LivestockPolicy.class.getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8));
        var settings=EconomyConfiguration.read(config);var grants=EconomyConfiguration.readStartupGrants(config);check(grants.industries().contains("livestock")&&grants.maximumGrant()==5000&&grants.perPeriodLimit()==5000,"Packaged unchanged shared grant limits");
        for(var species:LivestockHerd.Species.values()) {
            var folder=getDataFolder().toPath().resolve(species+"-"+UUID.randomUUID()).toFile();DatabaseManager db=new DatabaseManager(folder,getLogger());try {
                db.initialize();var town=UUID.randomUUID();var owner=UUID.randomUUID();var merchant=UUID.randomUUID();
                db.saveSettlement(new Settlement(town,"Livestock startup fixture","Fixture",x,y,z,2,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
                db.getPopulationRepository().insertResident(new Resident(owner,"Qualified fixture herd owner",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(owner,"farming",10);
                db.getPopulationRepository().insertResident(new Resident(merchant,"Fixture merchant",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(merchant,"commerce",10);
                new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit disposable fixture capital");
                var grain=new Commodity("grain");var milk=new Commodity("milk");var eggs=new Commodity("eggs");var supplier=UUID.randomUUID();var supplierSite=new BusinessSite(UUID.randomUUID(),town,world,x+2,y,z,now,"Explicit existing source");var merchantRules=new JobRequirements(216,Map.of("commerce",1L));
                db.getBusinessRepository().found(new BusinessFounding(new Business(supplier,town,"Fixture feed",merchant,merchant,"warehouse",supplierSite.id(),1,0),supplierSite,merchantRules,merchantRules,0,"Explicit source fixture"));db.getBusinessRepository().hire(UUID.randomUUID(),supplier,merchant,0);db.getIndustryRepository().openStore(supplier,100,Map.of(grain,1L),0);
                try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=24 WHERE business_id='"+supplier+"' AND commodity='grain'");}
                var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);long base=now-Math.multiplyExact(settings.secondsPerPeriod(),1000);
                economy.observe(new FarmObservation(town,world,base,true,0),base);economy.startOnline(base,Set.of(world));economy.pulse(now,false);
                var animals=new TreeMap<UUID,Integer>();for(var animal:observed)if(animal.species()==species) {
                    animals.put(animal.id(),animal.adult()?0:LivestockPolicy.prototype().maturityPeriods());
                    new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal.id(),world,LivestockAdoption.Ownership.VILLAGE,null,town,now,"Explicit disposable fixture ownership; not automatic adoption"));
                }
                var id=UUID.randomUUID();var site=new BusinessSite(UUID.randomUUID(),town,world,x,y,z,now,"Trusted actual animal fixture location");
                var founding=new BusinessFounding(new Business(id,town,"Fixture "+species,owner,owner,"livestock",site.id(),2,0),site,new JobRequirements(216,Map.of("farming",10L)),new JobRequirements(216,Map.of("farming",1L)),500,"Explicit qualified fixture opportunity");
                var request=new BusinessStartupGrants.WorkshopRequest(founding,128,Map.of(grain,1L,milk,1L,eggs,1L),4000,owner);
                var opened=new BusinessStartupGrants(db.getConnection()).startLivestock(request,species,grain,8,animals,grants);check(opened.herd().state().heads()==3&&opened.startup().store().hundredths()==4000,"Actual three-animal herd and paid capital");check(opened.startup().store().units().values().stream().allMatch(v->v==0),"No free feed/products");
                var ship=UUID.randomUUID();db.getIndustryRepository().purchase(ship,supplier,id,grain,10,1000,0,1,128);check(db.getIndustryRepository().store(id).orElseThrow().units().get(grain)==0,"Paid feed unavailable until arrival");db.getIndustryRepository().deliver(ship,1);
                var used=new LivestockRepository(db.getConnection()).advance(UUID.randomUUID(),id,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);check(used.feedUsed()==6&&used.renewableUnits()==2,"Actual paid feed consumed for abstract milk/eggs and logical cohort");
                var before=db.getIndustryRepository().store(id).orElseThrow();check(before.hundredths()==3000&&before.units().get(grain)==4&&before.units().get(species==LivestockHerd.Species.COW?milk:eggs)==2,"Real owned stores");
                check(before.hundredths()+db.getIndustryRepository().store(supplier).orElseThrow().hundredths()+new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==9500,"Cash conserved except500 recorded startup cost");
                new LivestockRepository(db.getConnection()).advance(UUID.randomUUID(),id,2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);check(before.equals(db.getIndustryRepository().store(id).orElseThrow()),"Exact absent-world pause");
                db.close();db=new DatabaseManager(folder,getLogger());db.initialize();var replay=new BusinessStartupGrants(db.getConnection()).startLivestock(request,species,grain,8,animals,StartupGrantPolicy.disabled());check(before.equals(replay.startup().store())&&replay.herd().state().heads()==4,"Restart disabled replay preserves current money/feed/cohorts");
                check(!new BusinessStartupGrants(db.getConnection()).preview(town,"craftsman",500,4000,grants).eligible(),"Shared grant budget used");
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
            }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
        }
    }
    @Override public void onDisable(){try{for(var animal:created)if(animal.isValid())animal.remove();check(created.stream().noneMatch(Entity::isValid),"Temporary test animals removed");getLogger().info("LIVESTOCK_STARTUP_FIXTURE_RESTORED");}finally{if(fixtureChunk!=null)fixtureChunk.removePluginChunkTicket(this);}}
}
