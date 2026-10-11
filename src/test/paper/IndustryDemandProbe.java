import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Marked disposable funded-demand fixture. Explicit synthetic industry in its own DB; never ship. */
public final class IndustryDemandProbe extends JavaPlugin {
    private ExecutorService executor;
    private PopulationSession session;
    private UUID world,town=UUID.randomUUID(),owner=UUID.randomUUID(),smith,warehouse,mine;
    private int offset;
    private File folder;
    private final AtomicLong clock=new AtomicLong(1000);
    private final Commodity tools=new Commodity("tools");
    private final Map<Commodity,Long> sizes=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,tools,1L);
    private IndustryRuntimePolicy policy() {
        var recipe=new ProductionRecipe("tools","blacksmith",Map.of(Commodity.IRON,2L,Commodity.COAL,1L),Map.of(tools,1L),1,1);
        return new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(recipe.id(),recipe),
            new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.IRON,200L,Commodity.COAL,100L)));
    }
    private void check(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private UUID business(DatabaseManager db,String kind,long cash)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,offset++,40,0,1,"Explicit disposable fixture site");
        var id=UUID.randomUUID();var business=new Business(id,town,kind,owner,owner,kind,site.id(),2,0);var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(business,site,req,req,0,"Explicit disposable fixture opportunity"));
        db.getIndustryRepository().openStore(id,100,sizes,cash);return id;
    }
    private void fixture() {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(owner,"Owner",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Actual disposable fixture capital");
            smith=business(db,"blacksmith",1000);warehouse=business(db,"warehouse",1000);mine=business(db,"mining",0);
            UUID worker=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(worker,"Smith",town,null,300,Resident.Status.ALIVE));
            db.getBusinessRepository().hire(UUID.randomUUID(),smith,worker,0);
            new OreTrackingRepository(db.getConnection()).begin(1000);
            new MineralOpportunities(db.getConnection()).observe(town,List.of(
                new MineralObservation(world,1000,true,Commodity.COAL,List.of(new VillageObservation.Position(10,40,0))),
                new MineralObservation(world,1000,true,Commodity.IRON,List.of(new VillageObservation.Position(12,40,0)))));
            var demand=new LocalIndustryDemand(db.getConnection(),policy(),BusinessOperationPolicy.prototype());
            var initial=demand.reviewMining(town,1000,100,Set.of(world),8);
            check(initial.candidates().size()==2&&initial.demand().uncovered().equals(Map.of(Commodity.IRON,2L,Commodity.COAL,1L)),"Funded input pressure missing");
            var miner=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(miner,"Miner",town,null,300,Resident.Status.ALIVE));
            db.getBusinessRepository().hire(UUID.randomUUID(),mine,miner,0);var deposit=UUID.randomUUID();var industry=db.getIndustryRepository();
            industry.discover(deposit,db.getBusinessRepository().findBusiness(mine).orElseThrow().siteId(),1,10,Map.of(Commodity.IRON,2L,Commodity.COAL,1L));
            industry.mine(UUID.randomUUID(),mine,deposit,2,0,2,2);
            for(var e:Map.of(Commodity.IRON,2L,Commodity.COAL,1L).entrySet()) {
                long price=e.getKey().equals(Commodity.IRON)?400:100;
                var shipment=industry.purchase(UUID.randomUUID(),mine,warehouse,e.getKey(),e.getValue(),price,0,1,128);industry.deliver(shipment.id(),1);
            }
            check(demand.review(town,Set.of(world),8).uncovered().isEmpty(),"Owned warehouse goods ignored");
            for(var e:Map.of(Commodity.IRON,2L,Commodity.COAL,1L).entrySet()) {
                long price=e.getKey().equals(Commodity.IRON)?400:100;
                industry.purchase(UUID.randomUUID(),warehouse,smith,e.getKey(),e.getValue(),price,1,1,128);
            }
            check(demand.reviewMining(town,1000,100,Set.of(world),8).candidates().isEmpty(),"Paid pending cargo duplicated demand");
            check(industry.store(smith).orElseThrow().units().get(Commodity.IRON)==0&&industry.store(smith).orElseThrow().reservedIncomingSpace()==3,"Pending cargo became usable goods");
            check(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()+
                industry.store(smith).orElseThrow().hundredths()+industry.store(warehouse).orElseThrow().hundredths()+industry.store(mine).orElseThrow().hundredths()==10000,"Funds not conserved");
            new OreTrackingRepository(db.getConnection()).finish(1000,true);
        } catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                ||!getServer().getIp().equals("127.0.0.1")||getServer().getPort()!=25578||!getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable fixture without players required");
            world=getServer().getWorlds().getFirst().getUID();folder=new File(getDataFolder(),"run-"+UUID.randomUUID());
            executor=Executors.newSingleThreadExecutor();var ready=CompletableFuture.runAsync(this::fixture,executor);
            new BukkitRunnable() {
                CompletableFuture<?> pending=ready;int ticks;boolean reopened;
                public void run() {
                    try {
                        if(++ticks>600)throw new IllegalStateException("Industry demand fixture timed out");if(!pending.isDone())return;pending.join();
                        if(!reopened) {
                            reopened=true;session=new PopulationSession(folder,getLogger(),PopulationSession.Settings.prototype(),clock::get);
                            session.configureOreTracking();session.configureIndustryRuntime(policy());
                            pending=session.start(false,Set.of(world)).thenCompose(s->session.miningInputDemand(town,100,8)).thenCompose(review->{
                                check(review.demand().complete()&&review.candidates().isEmpty(),"Owner restart lost paid cargo coverage");return session.updateLoadedWorlds(Set.of());
                            }).thenCompose(s->session.miningInputDemand(town,100,8)).thenAccept(review->check(!review.demand().complete()&&review.candidates().isEmpty(),"Absent world created pressure"));
                            return;
                        }
                        getLogger().info("NATIVE FUNDED INDUSTRY DEMAND PASS: real workshop input gap, paid warehouse stock, pending cargo, conserved money, SQL-owner restart and world pause");
                        cancel();getServer().getPluginManager().disablePlugin(IndustryDemandProbe.this);
                    }catch(RuntimeException failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE FUNDED INDUSTRY DEMAND FAIL",failure);getServer().getPluginManager().disablePlugin(IndustryDemandProbe.this);}
                }
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Industry demand fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(session!=null)session.close();if(executor!=null)executor.shutdown();}
}
