import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Private synthetic SQL + actual packaged owner clock; no physical animals, blocks or adoption. */
public final class LivestockLearningProbe extends JavaPlugin {
    private CompletableFuture<Void> work;private boolean authorized;private int ticks;
    private final AtomicLong now=new AtomicLong(1000);private UUID world,town=UUID.randomUUID(),herd=UUID.randomUUID(),seller=UUID.randomUUID();
    private UUID farmer;private Path data;private final Commodity grain=new Commodity("grain"),milk=new Commodity("milk");
    private void check(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst().getUID();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh private SQL required");
        work=CompletableFuture.runAsync(()->verify());
        getServer().getScheduler().runTaskTimer(this,()->{try{
            check(++ticks<600&&getServer().getOnlinePlayers().isEmpty(),"Bounded no-player fixture");
            if(work.isDone()){work.join();getLogger().info("LIVESTOCK_LEARNING_PROBE_PASSED actual packaged PopulationSession clock, feed-backed animal care teaches the real worker once, exact saved learning audit, restart/no downtime/world-absence/capture-gap replay, cash/goods conservation; private synthetic herd; no animal/world changes");getServer().shutdown();}
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_LEARNING_PROBE_FAILED",failure);getServer().shutdown();}},1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_LEARNING_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private PopulationSession open()throws Exception{
        var session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),now::get);
        var policy=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(),new WorkshopSupplyPolicy(true,2,8,Map.of(grain,25L)));
        session.configureIndustryRuntime(policy);session.configureWorkLearning(WorkLearningPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        session.configureLivestockWork(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype());session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
        check(session.livestockCaptureReady(),"Seeded clean capture must be trusted");return session;
    }
    private void verify(){try{
        seed();
        try(var session=open()){
            for(int n=2;n<=8;n++){now.set(n*1000L);session.pulse().get(5,TimeUnit.SECONDS);}
            check(!session.industryRuntimeDiagnostic().contains("failed"),"Live industry diagnostic");
        }
        var before=snapshot();check(before[0]==7&&before[1]>0&&before[2]>0,"Seven real feed-backed periods with milk and births");
        now.set(100_000);try(var session=open()){
            session.pulse().get(5,TimeUnit.SECONDS); // no elapsed time after startup
        }check(Arrays.equals(before,snapshot()),"Restart may not earn downtime or duplicate goods");
        try(var session=open()){
            session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);now.set(200_000);session.pulse().get(5,TimeUnit.SECONDS);
        }check(Arrays.equals(before,snapshot()),"Absent world cannot age herd or produce");
        try(var session=open()){
            session.markLivestockCaptureGap();now.set(201_000);session.pulse().get(5,TimeUnit.SECONDS);check(!session.livestockCaptureReady(),"Capture gap stays fenced");
        }check(Arrays.equals(before,snapshot()),"Untrusted capture cannot produce or buy feed");
    }catch(Exception failure){throw new CompletionException(failure);}}
    private void seed()throws Exception{
        var db=new DatabaseManager(data.toFile(),getLogger());try{
            db.initialize();new LivestockCapture(db.getConnection()).begin(1000);
            db.saveSettlement(new Settlement(town,"Private live herd fixture","Private herd world",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Private herd world");db.getVillageIdentityRepository().bindSettlement(town,world);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,1000,"Explicit disposable test funds");
            farmer=UUID.randomUUID();UUID merchant=UUID.randomUUID();var rules=new JobRequirements(216,Map.of());
            for(var id:List.of(farmer,merchant))db.getPopulationRepository().insertResident(new Resident(id,"Explicit fixture adult",town,null,300,Resident.Status.ALIVE));
            for(var id:List.of(herd,seller)){
                UUID person=id.equals(herd)?farmer:merchant;var site=new BusinessSite(UUID.randomUUID(),town,world,id.equals(herd)?0:8,64,0,1,"Explicit fixture workplace");
                db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Private "+(id.equals(herd)?"herd":"warehouse"),person,person,id.equals(herd)?"livestock":"warehouse",site.id(),1,0),site,rules,rules,0,"Explicit fixture opportunity"));
                db.getBusinessRepository().hire(UUID.randomUUID(),id,person,0);db.getIndustryRepository().openStore(id,256,id.equals(herd)?Map.of(grain,1L,milk,1L):Map.of(grain,1L),id.equals(herd)?1000:0);
            }
            try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=40 WHERE business_id='"+seller+"' AND commodity='grain'");}
            var roster=new LinkedHashMap<UUID,Integer>();roster.put(UUID.randomUUID(),0);roster.put(UUID.randomUUID(),0);roster.put(UUID.randomUUID(),2);
            for(var id:roster.keySet())new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit private fixture adoption, not automatic recognition"));
            new LivestockRepository(db.getConnection()).open(herd,LivestockHerd.Species.COW,grain,8,roster);
            new LivestockCapture(db.getConnection()).finish(1000,true);
            // Reconstruct a populated private predecessor for the real packaged migration.
            try(var s=db.getConnection().createStatement()){
                s.execute("DROP TABLE livestock_learning_entries");s.execute("DROP TABLE livestock_learning_reviews");s.execute("PRAGMA user_version=88");
            }
        }finally{db.close();}
    }
    private long scalar(DatabaseManager db,String sql)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(sql)){check(r.next(),"Missing scalar");return r.getLong(1);}}
    private long[] snapshot()throws Exception{
        var db=new DatabaseManager(data.toFile(),getLogger());try{
            db.initialize();var state=new LivestockRepository(db.getConnection()).load(herd).orElseThrow();var store=db.getIndustryRepository().store(herd).orElseThrow();var supply=db.getIndustryRepository().store(seller).orElseThrow();
            long experience=db.getPopulationRepository().findWorkProfile(farmer).orElseThrow().experience().getOrDefault("farming",0L);
            check(experience==scalar(db,"SELECT COUNT(*) FROM livestock_learning_entries"),"Exact real-care skill awards");
            check(experience>0,"Productive native owner work must teach its actual caretaker");
            long pending=scalar(db,"SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE delivered_period IS NULL");
            check(store.units().get(grain)+supply.units().get(grain)+pending+scalar(db,"SELECT COALESCE(SUM(feed_used),0) FROM livestock_periods")==40,"Feed conserved including cargo and actual use");
            check(store.hundredths()+supply.hundredths()+new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==1000,"Cash conserved");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys clean");}
            return new long[]{state.state().period(),store.units().get(milk),scalar(db,"SELECT COUNT(*) FROM livestock_births"),store.units().get(grain),store.hundredths(),supply.units().get(grain),pending,experience};
        }finally{db.close();}
    }
}
