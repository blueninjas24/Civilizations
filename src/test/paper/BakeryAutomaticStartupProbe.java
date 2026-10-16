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

/** Guarded disposable training and real Paper point-confirmation proof. Never included in the plugin JAR. */
public final class BakeryAutomaticStartupProbe extends JavaPlugin {
    private boolean authorized;private Block marker;private BlockData original;private Chunk fixtureChunk;
    private volatile PopulationSession session;private PaperBakeryStartup startup;
    private record Ready(File folder,UUID town,UUID baker,UUID supplier,UUID world,BakeryInvestment.Draft draft,IndustryRuntimePolicy industry) {}
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
        original=marker.getBlockData().clone();marker.setType(Material.SMOKER,false);
        var worldId=world.getUID();var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();
        var prepared=CompletableFuture.supplyAsync(()->{DatabaseManager db=new DatabaseManager(folder,getLogger());try {
            var c=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(ProductionRecipe.class.getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8));
            // Explicit accelerated fixture knobs, never installed on the main plugin or human server.
            c.set("pace.seconds-per-period",1);c.set("starting-units-per-resident.food",100);c.set("starting-units-per-resident.timber",40);
            c.set("village-supply-purchases.food-stock-reserve-periods",100);
            var economySettings=EconomyConfiguration.read(c);var customers=EconomyConfiguration.readVillageSupplies(c);var industry=EconomyConfiguration.readIndustryRuntime(c);
            long base=System.currentTimeMillis()-20000;var time=new AtomicLong(base+1000);
            db.initialize();UUID town=UUID.randomUUID(),baker=UUID.randomUUID(),merchant=UUID.randomUUID(),supplier=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Automatic bakery fixture","Fixture",x,y,z,2,0,0));
            db.getVillageIdentityRepository().observeWorld(worldId,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,worldId);
            db.getPopulationRepository().insertResident(new Resident(baker,"Fixture learner",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().insertResident(new Resident(merchant,"Fixture supplier",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,worldId,x+2,y,z,base,"Explicit stocked source fixture");var requirements=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(supplier,town,"Fixture grain",merchant,merchant,"warehouse",site.id(),1,0),site,requirements,requirements,0,"Explicit native source"));
            db.getBusinessRepository().hire(UUID.randomUUID(),supplier,merchant,0);db.getIndustryRepository().openStore(supplier,100,Map.of(new Commodity("grain"),1L),0);
            try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=10 WHERE business_id='"+supplier+"' AND commodity='grain'");}
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture treasury");
            var economy=new EconomyRuntimeRepository(db.getConnection(),economySettings,FarmObservation.Policy.prototype(),216);
            economy.observe(new FarmObservation(town,worldId,base,true,0),base);economy.startOnline(base,Set.of(worldId));economy.pulse(base+1000,false);
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(worldId,Math.floorDiv(x,16),Math.floorDiv(z,16),base+1000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(x,y,z),WorkplaceObservation.Kind.SMOKER))));
            db.close();var defaults=PopulationSession.Settings.prototype();
            var settings=new PopulationSession.Settings(0,defaults.millisPerMonth(),defaults.maximumOfflineMonths(),defaults.monthsPerBatch(),defaults.population(),defaults.migration(),defaults.recognition(),defaults.enrollment());
            session=new PopulationSession(folder,getLogger(),settings,time::get,economySettings,FarmObservation.Policy.prototype());
            configure(session,c,industry,customers);session.start(false,Set.of(worldId)).get(10,TimeUnit.SECONDS);
            var initial=session.reviewAutomaticBakeryInvestment(town).get(5,TimeUnit.SECONDS);check(initial.draft().isEmpty()&&initial.explanation().contains("prospective bakery owner"),initial.explanation());
            for(int n=1;n<=10;n++){time.set(base+1000+n*1000);session.pulse().get(5,TimeUnit.SECONDS);}
            var draft=session.reviewAutomaticBakeryInvestment(town).get(5,TimeUnit.SECONDS).draft().orElseThrow();session.close();session=null;
            db=new DatabaseManager(folder,getLogger());db.initialize();
            check(db.getPopulationRepository().findWorkProfile(baker).orElseThrow().experience().getOrDefault("baking",0L)==10,"Real saved baking skill earned");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM study_spending")){check(r.next()&&r.getInt(1)==10,"Ten real practice expenses");}
            db.close();session=new PopulationSession(folder,getLogger(),settings,System::currentTimeMillis,economySettings,FarmObservation.Policy.prototype());
            configure(session,c,industry,customers);session.start(false,Set.of(worldId)).get(10,TimeUnit.SECONDS);
            return new Ready(folder,town,baker,supplier,worldId,draft,industry);
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        new BukkitRunnable(){int ticks;CompletableFuture<Void> finished;@Override public void run(){try {
            check(++ticks<=600,"Automatic bakery timeout: "+(startup==null?"preparing":startup.diagnostic()));if(!prepared.isDone())return;var ready=prepared.join();
            if(finished!=null){if(!finished.isDone())return;finished.join();getLogger().info("BAKERY_AUTOMATIC_STARTUP_PROBE_PASSED real SQL-owner training, ten paid practice periods, restart, main-thread smoker confirmation, automatic paid opening, timed bread, replay and absent-world pause");cancel();getServer().shutdown();return;}
            if(startup==null)startup=new PaperBakeryStartup(session,getLogger());startup.run();
            if(!startup.diagnostic().contains("Automatic bakery started"))return;
            finished=CompletableFuture.runAsync(()->{session.close();session=null;var db=new DatabaseManager(ready.folder(),getLogger());try {
                db.initialize();var id=ready.draft().anchor().business();check(db.getBusinessRepository().findBusiness(id).isPresent(),"Paper cycle founded trained business");
                check(new TreasuryRepository(db.getConnection()).load(ready.town()).orElseThrow().hundredths()==5500,"One real protected grant");
                var store=db.getIndustryRepository().store(id).orElseThrow();check(store.hundredths()==3700,"First private ingredients paid");
                check(db.getIndustryRepository().store(ready.supplier()).orElseThrow().hundredths()==300,"Supplier received cash");
                var oldReview=new BakeryOpportunities(db.getConnection(),WorkshopOpportunityPolicy.disabled(),ready.industry(),StartupGrantPolicy.disabled(),WorkshopStarterPolicy.disabled(),VillageSupplyPolicy.disabled(),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
                new BakeryInvestment(db.getConnection(),oldReview,BakeryFoundingPolicy.disabled(),WorkshopOpportunityPolicy.disabled(),ready.industry(),StartupGrantPolicy.disabled(),WorkshopStarterPolicy.disabled(),216,0)
                        .accept(ready.draft(),new WorkplaceOperations.Confirmation(ready.draft().anchor(),true,System.currentTimeMillis()),System.currentTimeMillis(),Set.of(ready.world()));
                check(store.equals(db.getIndustryRepository().store(id).orElseThrow()),"Accepted historical replay cannot pay or deliver again");
                var runtime=new IndustryRuntime(db.getConnection(),ready.industry(),BusinessOperationPolicy.prototype());long now=System.currentTimeMillis();runtime.start(now);runtime.pulse(now+ready.industry().millisPerPeriod(),Set.of(ready.world()));
                store=db.getIndustryRepository().store(id).orElseThrow();check(store.units().get(Commodity.FOOD)==3,"Actual delivered grain produced bread");
                runtime.pulse(now+1000000,Set.of());check(store.equals(db.getIndustryRepository().store(id).orElseThrow()),"Absent-world exact pause");
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
            }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_AUTOMATIC_STARTUP_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_AUTOMATIC_STARTUP_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void configure(PopulationSession s,YamlConfiguration c,IndustryRuntimePolicy industry,VillageSupplyPolicy customers) {
        s.configureIndustryRuntime(industry);s.configureBusinessOperations(BusinessOperationPolicy.prototype());s.configureWorkplaces(EconomyConfiguration.readWorkplaceObserver(c));
        s.configureVillageSupplies(customers);s.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),EconomyConfiguration.readWorkshopStarterSupplies(c));
        s.configureBakery(EconomyConfiguration.readBakeryFounding(c),EconomyConfiguration.readBakeryOpportunities(c),EconomyConfiguration.readBakeryMarket(c));
        s.configureMineFounding(MineFoundingPolicy.disabled(),EconomyConfiguration.readStartupGrants(c));s.configureStudy(EconomyConfiguration.readStudy(c));s.configureCareerStudy(EconomyConfiguration.readCareerStudy(c));
    }
    @Override public void onDisable(){if(session!=null)session.close();try{if(original!=null&&marker.getType()==Material.SMOKER){marker.setBlockData(original,false);check(marker.getBlockData().equals(original),"Original disposable marker block restored");getLogger().info("BAKERY_AUTOMATIC_MARKER_RESTORED");}}finally{if(fixtureChunk!=null)fixtureChunk.removePluginChunkTicket(this);}}
}
