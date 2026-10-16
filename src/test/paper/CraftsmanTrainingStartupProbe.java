import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Guarded disposable training and qualified opening and real Paper point-confirmation proof. Never included in the plugin JAR. */
public final class CraftsmanTrainingStartupProbe extends JavaPlugin {
    private boolean authorized;private Block marker;private BlockData original;private Chunk fixtureChunk;
    private volatile PopulationSession session;private PaperCraftsmanStartup startup;
    private record Ready(File folder,UUID town,UUID owner,UUID buyer,UUID world,CraftsmanInvestment.Draft draft,IndustryRuntimePolicy industry,long timber,long stone) {}
    private void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;var world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();
        int x=spawn.getBlockX()+1,y=Math.min(world.getMaxHeight()-2,spawn.getBlockY()+64),z=spawn.getBlockZ();
        int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);
        // Explicit setup in the guarded disposable fixture only; never generate new terrain.
        check(world.isChunkLoaded(cx,cz)||world.loadChunk(cx,cz,false),"Existing generated fixture chunk required");
        fixtureChunk=world.getChunkAt(cx,cz);fixtureChunk.addPluginChunkTicket(this);
        marker=world.getBlockAt(x,y,z);check(marker.getType()==Material.AIR,"Disposable marker must replace only air");
        original=marker.getBlockData().clone();marker.setType(Material.CRAFTING_TABLE,false);
        var worldId=world.getUID();var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();
        var prepared=CompletableFuture.supplyAsync(()->{DatabaseManager db=new DatabaseManager(folder,getLogger());try {
            var c=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(ProductionRecipe.class.getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8));
            // Helper-only acceleration and explicit starting test capital/materials.
            c.set("pace.seconds-per-period",1);c.set("industry-runtime.real-millis-per-period",1000);
            c.set("starting-units-per-resident.food",100);c.set("starting-units-per-resident.timber",40);c.set("starting-units-per-resident.stone",20);
            var economySettings=EconomyConfiguration.read(c);var industry=EconomyConfiguration.readIndustryRuntime(c);
            long base=System.currentTimeMillis()-20000;db.initialize();UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),merchant=UUID.randomUUID(),buyer=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Automatic craftsman fixture","Fixture",x,y,z,2,0,0));
            db.getVillageIdentityRepository().observeWorld(worldId,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,worldId);
            db.getPopulationRepository().insertResident(new Resident(owner,"Explicit qualified craft founder",town,null,300,Resident.Status.ALIVE));
            // No initial crafting experience: the real course must earn it.
            db.getPopulationRepository().insertResident(new Resident(merchant,"Explicit buyer",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(merchant,"commerce",10);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit native fixture capital");
            var site=new BusinessSite(UUID.randomUUID(),town,worldId,x+2,y,z,base,"Explicit existing stocked customer");var requirements=new JobRequirements(216,Map.of("commerce",1L));
            db.getBusinessRepository().found(new BusinessFounding(new Business(buyer,town,"Fixture warehouse",merchant,merchant,"warehouse",site.id(),2,0),site,requirements,requirements,0,"Explicit native customer"));
            db.getBusinessRepository().hire(UUID.randomUUID(),buyer,merchant,0);db.getIndustryRepository().openStore(buyer,100,Map.of(Commodity.COAL,1L),6000);
            try(var st=db.getConnection().createStatement()){st.execute("UPDATE industry_stock SET units=80 WHERE business_id='"+buyer+"' AND commodity='coal'");st.execute("INSERT INTO industry_runtime VALUES('"+buyer+"',1000,"+(base+1000)+",0,0,0)");}
            var economy=new EconomyRuntimeRepository(db.getConnection(),economySettings,FarmObservation.Policy.prototype(),216);
            economy.observe(new FarmObservation(town,worldId,base,true,0),base);economy.startOnline(base,Set.of(worldId));economy.pulse(base+1000,false);
            long timber=stock(db,town,"timber"),stone=stock(db,town,"stone");
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(worldId,Math.floorDiv(x,16),Math.floorDiv(z,16),base+1000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(x,y,z),WorkplaceObservation.Kind.CRAFTING_TABLE))));
            db.close();var defaults=PopulationSession.Settings.prototype();
            var settings=new PopulationSession.Settings(0,defaults.millisPerMonth(),defaults.maximumOfflineMonths(),defaults.monthsPerBatch(),defaults.population(),defaults.migration(),defaults.recognition(),defaults.enrollment());
            var time=new AtomicLong(base+1000);
            session=new PopulationSession(folder,getLogger(),settings,time::get,economySettings,FarmObservation.Policy.prototype());
            configure(session,c,industry);session.start(false,Set.of(worldId)).get(10,TimeUnit.SECONDS);
            var initial=session.reviewAutomaticCraftsmanInvestment(town).get(5,TimeUnit.SECONDS);check(initial.draft().isEmpty()&&initial.explanation().contains("prospective craftsman owner"),initial.explanation());
            for(int n=1;n<=10;n++){time.set(base+1000+n*1000);session.pulse().get(5,TimeUnit.SECONDS);}
            var draft=session.reviewAutomaticCraftsmanInvestment(town).get(5,TimeUnit.SECONDS).draft().orElseThrow();session.close();session=null;
            db=new DatabaseManager(folder,getLogger());db.initialize();
            check(db.getPopulationRepository().findWorkProfile(owner).orElseThrow().experience().getOrDefault("crafting",0L)==10,"Actual saved course experience");
            try(var st=db.getConnection().createStatement();var r=st.executeQuery("SELECT COUNT(*) FROM study_spending")){check(r.next()&&r.getInt(1)==10,"Ten actual practice expenses");}
            check(stock(db,town,"timber")==timber-10,"Practice consumed actual public timber");db.close();
            session=new PopulationSession(folder,getLogger(),settings,System::currentTimeMillis,economySettings,FarmObservation.Policy.prototype());
            configure(session,c,industry);session.start(false,Set.of(worldId)).get(10,TimeUnit.SECONDS);
            return new Ready(folder,town,owner,buyer,worldId,draft,industry,timber,stone);
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        new BukkitRunnable(){int ticks;CompletableFuture<Void> finished;@Override public void run(){try {
            check(++ticks<=600,"Automatic craftsman timeout: "+(startup==null?"preparing":startup.diagnostic()));if(!prepared.isDone())return;var ready=prepared.join();
            if(finished!=null){if(!finished.isDone())return;finished.join();getLogger().info("CRAFTSMAN_TRAINING_STARTUP_PROBE_PASSED actual SQL-owner training, ten real practice expenses, restart, main-thread crafting-table confirmation, automatic paid opening, produced fitting customer, work learning, replay and absence");cancel();getServer().shutdown();return;}
            if(startup==null)startup=new PaperCraftsmanStartup(session,getLogger());startup.run();
            if(!startup.diagnostic().contains("Automatic craftsman started"))return;
            finished=CompletableFuture.runAsync(()->{session.close();session=null;var db=new DatabaseManager(ready.folder(),getLogger());try {
                db.initialize();var id=ready.draft().anchor().business();check(db.getBusinessRepository().findBusiness(id).isPresent(),"Paper cycle founded qualified business");
                check(new TreasuryRepository(db.getConnection()).load(ready.town()).orElseThrow().hundredths()==21000,"One real grant, actual starter payment returned to treasury");
                var store=db.getIndustryRepository().store(id).orElseThrow();check(store.hundredths()==2500,"Actual paid starter ingredients");
                check(stock(db,ready.town(),"timber")==ready.timber()-13&&stock(db,ready.town(),"stone")==ready.stone()-1,"Actual public surplus spent once");
                var oldReview=new CraftsmanOpportunities(db.getConnection(),WorkshopOpportunityPolicy.disabled(),ready.industry(),StartupGrantPolicy.disabled(),WorkshopStarterPolicy.disabled(),BusinessOperationPolicy.prototype());
                new CraftsmanInvestment(db.getConnection(),oldReview,WorkshopFoundingPolicy.disabled(),WorkshopOpportunityPolicy.disabled(),ready.industry(),StartupGrantPolicy.disabled(),WorkshopStarterPolicy.disabled(),216,0)
                        .accept(ready.draft(),new WorkplaceOperations.Confirmation(ready.draft().anchor(),true,System.currentTimeMillis()),System.currentTimeMillis(),Set.of(ready.world()));
                check(store.equals(db.getIndustryRepository().store(id).orElseThrow()),"Restart-style historical replay cannot pay or deliver again");
                var runtime=new IndustryRuntime(db.getConnection(),ready.industry(),BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());long now=System.currentTimeMillis();runtime.start(now);
                for(int n=1;n<=3;n++)runtime.pulse(now+n*ready.industry().millisPerPeriod(),Set.of(ready.world()));
                store=db.getIndustryRepository().store(id).orElseThrow();var customer=db.getIndustryRepository().store(ready.buyer()).orElseThrow();
                check(db.getPopulationRepository().findWorkProfile(ready.owner()).orElseThrow().experience().get("crafting")==11,"Actual productive work earns once-only additional experience");
                check(customer.capacity()==200&&customer.units().get(Commodity.COAL)==80,"Actual customer's paid fitting installed");
                check(store.hundredths()==4500&&customer.hundredths()==4000&&store.units().get(StorageUpgrades.FITTINGS)==0,"Actual manufactured fitting sold exactly once");
                check(store.hundredths()+customer.hundredths()+new TreasuryRepository(db.getConnection()).load(ready.town()).orElseThrow().hundredths()==29500,"Cash conserved except recorded500 startup expense");
                runtime.pulse(now+1000000,Set.of());check(store.equals(db.getIndustryRepository().store(id).orElseThrow()),"Absent-world exact pause");
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
            }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"CRAFTSMAN_TRAINING_STARTUP_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"CRAFTSMAN_TRAINING_STARTUP_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void configure(PopulationSession s,YamlConfiguration c,IndustryRuntimePolicy industry) {
        s.configureIndustryRuntime(industry);s.configureBusinessOperations(BusinessOperationPolicy.prototype());s.configureWorkplaces(EconomyConfiguration.readWorkplaceObserver(c));
        s.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),EconomyConfiguration.readWorkshopStarterSupplies(c));
        s.configureCraftsman(EconomyConfiguration.readCraftsmanFounding(c),EconomyConfiguration.readCraftsmanOpportunities(c));
        s.configureMineFounding(MineFoundingPolicy.disabled(),EconomyConfiguration.readStartupGrants(c));
        s.configureStudy(EconomyConfiguration.readStudy(c));s.configureCareerStudy(EconomyConfiguration.readCareerStudy(c));s.configureWorkLearning(EconomyConfiguration.readWorkLearning(c));
    }
    private long stock(DatabaseManager db,UUID town,String good)throws java.sql.SQLException {
        try(var s=db.getConnection().prepareStatement("SELECT units FROM economy_stocks WHERE settlement_id=? AND commodity=?")) {
            s.setString(1,town.toString());s.setString(2,good);try(var r=s.executeQuery()){check(r.next(),"Saved public stock");return r.getLong(1);}
        }
    }
    @Override public void onDisable(){if(session!=null)session.close();try{if(original!=null&&marker.getType()==Material.CRAFTING_TABLE){marker.setBlockData(original,false);check(marker.getBlockData().equals(original),"Original disposable marker block restored");getLogger().info("CRAFTSMAN_TRAINING_MARKER_RESTORED");}}finally{if(fixtureChunk!=null)fixtureChunk.removePluginChunkTicket(this);}}
}
