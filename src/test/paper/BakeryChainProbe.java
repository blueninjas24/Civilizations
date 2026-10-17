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

/** Private synthetic SQL + actual packaged owner clock; no physical animals or blocks. */
public final class BakeryChainProbe extends JavaPlugin {
    private CompletableFuture<Void> work;private boolean authorized;private int ticks;
    private final AtomicLong now=new AtomicLong(1000);private UUID world,town=UUID.randomUUID();
    private UUID cows=UUID.randomUUID(),chickens=UUID.randomUUID(),warehouse=UUID.randomUUID(),bakery=UUID.randomUUID();
    private UUID cowWorker,chickenWorker,warehouseWorker,baker;private Path data;
    private final Commodity grain=new Commodity("grain"),milk=new Commodity("milk"),eggs=new Commodity("eggs");
    private final Map<String,ProductionRecipe> recipes=Map.of("bakery_bread",ProductionRecipe.bakeryBread(),"bakery_cake",ProductionRecipe.bakeryCake());
    private final IndustryRuntimePolicy industry=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),recipes,
            new WorkshopSupplyPolicy(true,2,16,Map.of(grain,25L,milk,150L,eggs,75L)),new WarehouseSupplyPolicy(true,16,16));
    private void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst().getUID();data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh private SQL required");
        work=CompletableFuture.runAsync(this::verify);
        getServer().getScheduler().runTaskTimer(this,()->{try{
            check(++ticks<900&&getServer().getOnlinePlayers().isEmpty(),"Bounded no-player fixture");
            if(work.isDone()){work.join();getLogger().info("BAKERY_CHAIN_PROBE_PASSED actual packaged owner clock, saved cow milk/chicken eggs, paid warehouse restocking and bakery ingredient purchases, cake recipe flows, cash/goods conservation, replay/restart/world absence; private synthetic fixture; no block/entity changes");getServer().shutdown();}
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_CHAIN_PROBE_FAILED",failure);getServer().shutdown();}},1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_CHAIN_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private PopulationSession open()throws Exception{
        var session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),now::get);
        session.configureIndustryRuntime(industry);session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        session.configureLivestockWork(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype());
        session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);check(session.livestockCaptureReady(),"Trusted saved herd capture required");return session;
    }
    private void verify(){try{
        seed();long[] start=snapshot();check(start[0]==0&&start[1]==0&&start[2]==0,"Clean empty bakery-chain baseline");
        try(var session=open()){
            for(int period=2;period<=40;period++){now.set(period*1000L);session.pulse().get(5,TimeUnit.SECONDS);}
        }
        var after=snapshot();check(after[0]>0&&after[1]>0&&after[2]>0,"Both real saved herds must produce milk and eggs");
        check(after[3]>0,"Bakery must produce cake meals");check(after[4]==20_000,"All real treasury/store cash remains conserved");
        check(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+warehouse+"' AND buyer_id='"+bakery+"' AND commodity='milk'")>0,"Bakery must pay its warehouse for milk");
        check(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+warehouse+"' AND buyer_id='"+bakery+"' AND commodity='eggs'")>0,"Bakery must pay its warehouse for eggs");
        check(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+cows+"' AND buyer_id='"+warehouse+"' AND commodity='milk'")>0,"Warehouse must pay the cow herd for milk");
        check(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+chickens+"' AND buyer_id='"+warehouse+"' AND commodity='eggs'")>0,"Warehouse must pay the chicken herd for eggs");
        check(scalar("SELECT SUM(units) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='"+bakery+"' AND r.recipe_id='bakery_cake' AND f.direction='INPUT' AND f.commodity='milk'")==after[3],"Every cake must consume one actual milk");
        check(scalar("SELECT SUM(units) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='"+bakery+"' AND r.recipe_id='bakery_cake' AND f.direction='INPUT' AND f.commodity='eggs'")==after[3],"Every cake must consume one actual egg");
        check(scalar("SELECT SUM(units) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE r.business_id='"+bakery+"' AND r.recipe_id='bakery_cake' AND f.direction='OUTPUT' AND f.commodity='food'")==after[3]*8,"Cake output is eight abstract meals per batch");
        conserve();
        var before=after.clone();now.set(500_000);try(var session=open()){session.pulse().get(5,TimeUnit.SECONDS);}check(Arrays.equals(before,snapshot()),"Restart cannot earn downtime or duplicate production");
        var runPeriod=scalar("SELECT MAX(period) FROM recipe_runs WHERE business_id='"+bakery+"' AND recipe_id='bakery_cake'");
        var operation=UUID.nameUUIDFromBytes(("civilizations:timed-industry-v1:"+bakery+":"+runPeriod).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();db.getIndustryRepository().process(operation,bakery,ProductionRecipe.bakeryCake(),4,runPeriod);}finally{db.close();}
        check(Arrays.equals(before,snapshot()),"Exact production receipt replay cannot consume or produce twice");
        try(var session=open()){session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);now.set(600_000);session.pulse().get(5,TimeUnit.SECONDS);}check(Arrays.equals(before,snapshot()),"Absent world pauses all chain work and payments");
    }catch(Exception failure){throw new CompletionException(failure);}}
    private void seed()throws Exception{
        var db=new DatabaseManager(data.toFile(),getLogger());try{
            db.initialize();new LivestockCapture(db.getConnection()).begin(1000);
            db.saveSettlement(new Settlement(town,"Private bakery chain fixture","Private chain world",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Private chain world");db.getVillageIdentityRepository().bindSettlement(town,world);
            var residents=new ArrayList<UUID>();for(int i=0;i<5;i++){var id=UUID.randomUUID();residents.add(id);db.getPopulationRepository().insertResident(new Resident(id,"Explicit fixture adult",town,null,300,Resident.Status.ALIVE));}
            cowWorker=residents.get(0);chickenWorker=residents.get(1);warehouseWorker=residents.get(2);baker=residents.get(3);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,20_000,"Explicit private fixture treasury");
            var ids=List.of(cows,chickens,warehouse,bakery);var kinds=List.of("livestock","livestock","warehouse","bakery");var owners=List.of(cowWorker,chickenWorker,warehouseWorker,baker);
            var requirements=new JobRequirements(216,Map.of());for(int i=0;i<ids.size();i++){
                var site=new BusinessSite(UUID.randomUUID(),town,world,i*8,64,0,1000,"Explicit private chain workplace");
                db.getBusinessRepository().found(new BusinessFounding(new Business(ids.get(i),town,"Private "+kinds.get(i),owners.get(i),owners.get(i),kinds.get(i),site.id(),1,0),site,requirements,requirements,0,"Explicit fixture business"));
                db.getBusinessRepository().hire(UUID.randomUUID(),ids.get(i),owners.get(i),0);
            }
            db.getIndustryRepository().openStore(cows,256,Map.of(grain,1L,milk,1L),2500);
            db.getIndustryRepository().openStore(chickens,256,Map.of(grain,1L,eggs,1L),2500);
            db.getIndustryRepository().openStore(warehouse,512,Map.of(grain,1L,milk,1L,eggs,1L,Commodity.FOOD,1L),5000);
            db.getIndustryRepository().openStore(bakery,512,Map.of(grain,1L,milk,1L,eggs,1L,Commodity.FOOD,1L),5000);
            try(var s=db.getConnection().createStatement()){
                s.execute("UPDATE industry_stock SET units=20 WHERE business_id='"+cows+"' AND commodity='grain'");
                s.execute("UPDATE industry_stock SET units=20 WHERE business_id='"+chickens+"' AND commodity='grain'");
                s.execute("UPDATE industry_stock SET units=100 WHERE business_id='"+warehouse+"' AND commodity='grain'");
            }
            var cowIds=List.of(UUID.randomUUID(),UUID.randomUUID());var chickenIds=List.of(UUID.randomUUID(),UUID.randomUUID());
            for(var id:cowIds)new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit private fixture herd"));
            for(var id:chickenIds)new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(id,world,LivestockAdoption.Ownership.VILLAGE,null,town,1000,"Explicit private fixture herd"));
            new LivestockRepository(db.getConnection()).open(cows,LivestockHerd.Species.COW,grain,8,Map.of(cowIds.get(0),0,cowIds.get(1),0));
            new LivestockRepository(db.getConnection()).open(chickens,LivestockHerd.Species.CHICKEN,grain,8,Map.of(chickenIds.get(0),0,chickenIds.get(1),0));
            new LivestockCapture(db.getConnection()).finish(1000,true);
        }finally{db.close();}
    }
    private long scalar(String sql)throws Exception{var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();try(var s=db.getConnection().createStatement();var r=s.executeQuery(sql)){check(r.next(),"Missing SQL result");return r.getLong(1);}}finally{db.close();}}
    private void conserve()throws Exception{
        long grainStock=scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='grain'");
        long grainPending=scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE commodity='grain' AND delivered_period IS NULL");
        long grainUsed=scalar("SELECT COALESCE(SUM(feed_used),0) FROM livestock_periods")+scalar("SELECT COALESCE(SUM(f.units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE f.commodity='grain' AND f.direction='INPUT'");
        check(grainStock+grainPending+grainUsed==140,"Seeded warehouse/herd grain conserved through paid deliveries and use");
        for(var product:List.of(milk,eggs)){
            long produced=scalar("SELECT COALESCE(SUM(renewable_units),0) FROM livestock_periods WHERE business_id='"+(product.equals(milk)?cows:chickens)+"'");
            long held=scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='"+product.id()+"'");
            long pending=scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE commodity='"+product.id()+"' AND delivered_period IS NULL");
            long used=scalar("SELECT COALESCE(SUM(f.units),0) FROM recipe_flows f JOIN recipe_runs r ON r.operation_id=f.operation_id WHERE f.commodity='"+product.id()+"' AND f.direction='INPUT'");
            check(produced==held+pending+used,"Saved "+product.id()+" conserved through warehouse, bakery and recipe use");
        }
    }
    private long[] snapshot()throws Exception{
        var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();
            long periods=scalar("SELECT COALESCE(SUM(period),0) FROM livestock_periods");
            long cow=scalar("SELECT COALESCE(SUM(renewable_units),0) FROM livestock_periods WHERE business_id='"+cows+"'");
            long hen=scalar("SELECT COALESCE(SUM(renewable_units),0) FROM livestock_periods WHERE business_id='"+chickens+"'");
            long cakes=scalar("SELECT COALESCE(SUM(o.quantity),0) FROM recipe_runs r JOIN industry_operations o ON o.id=r.operation_id WHERE r.business_id='"+bakery+"' AND r.recipe_id='bakery_cake'");
            long cash=scalar("SELECT COALESCE(SUM(hundredths),0) FROM industry_stores")+new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
            long pending=scalar("SELECT COUNT(*) FROM local_shipments WHERE delivered_period IS NULL");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys clean");}
            return new long[]{periods,cow,hen,cakes,cash,pending};
        }finally{db.close();}
    }
}
