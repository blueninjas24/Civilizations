import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Guarded separate owner/database. No world edits, chunk tickets or production save mutations. */
public final class IndustryRuntimeProbe extends JavaPlugin {
    private PopulationSession session;
    private ExecutorService fixture;
    private CompletableFuture<?> pending;
    private final AtomicLong now=new AtomicLong(1000);
    private java.io.File folder;
    private UUID world;
    private UUID founder, heir, mine;
    private UUID saleOwner,saleFirm;
    private BusinessSales.Request paidSale;
    private UUID pendingMine,pendingOwner;
    private IndustryRuntimePolicy policy;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25578 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Marked localhost disposable server without players required");
            world=getServer().getWorlds().getFirst().getUID();folder=new java.io.File(getDataFolder(),"run-"+UUID.randomUUID());
            if(getServer().getWorlds().getFirst().isChunkLoaded(128,128))throw new IllegalStateException("Fixture requires its abstract site chunk to be unloaded");
            policy=new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,2,100,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,1L),Map.of(Commodity.COAL,1L,Commodity.STONE,1L)),Map.of());
            fixture=Executors.newSingleThreadExecutor();
            pending=CompletableFuture.runAsync(()-> {try{seed();}catch(Exception failure){throw new CompletionException(failure);}},fixture);
            new BukkitRunnable() {
                int ticks,stage;
                @Override public void run() {
                    try {
                        if(++ticks>400)throw new IllegalStateException("Timed industry probe timed out");
                        if(!pending.isDone())return;pending.join();
                        switch(stage++) {
                            case 0 -> {session=create();pending=session.start(false,Set.of(world));}
                            case 1 -> {now.set(2000);pending=session.pulse();}
                            case 2 -> {check(1,9);paidSale=new BusinessSales.Request(UUID.randomUUID(),saleFirm,saleOwner,heir,1,mine,250,null,0);pending=session.openBusinessSale(saleFirm,250,5000);}
                            case 3 -> {pending=session.sellBusiness(paidSale);}
                            case 4 -> {checkSale();pending=session.updateLoadedWorlds(Set.of());}
                            case 5 -> {now.set(100_000);pending=session.pulse();}
                            case 6 -> {check(1,9);checkSale();pending=session.updateLoadedWorlds(Set.of(world));}
                            case 7 -> {now.set(101_000);pending=session.pulse();}
                            case 8 -> {check(2,8);session.close();now.set(1_000_000);session=create();pending=session.start(false,Set.of(world));}
                            case 9 -> {check(2,8);pending=session.sellBusiness(paidSale);}
                            case 10 -> {checkSale();now.set(1_001_000);pending=session.pulse();}
                            case 11 -> {check(3,7);checkSale();pending=session.openBusinessSale(pendingMine,100,1000);}
                            case 12 -> {now.set(1_002_000);pending=session.pulse();}
                            case 13 -> {check(4,6);checkClosure();session.close();now.set(10_000_000);session=create();pending=session.start(false,Set.of(world));}
                            case 14 -> {check(4,6);checkClosure();now.set(10_001_000);pending=session.pulse();}
                            case 15 -> {
                                check(5,5);checkSale();checkClosure();getLogger().info("NATIVE TIMED INDUSTRY PASS: qualified inheritance, exact saved offer and conserved paid resale/replay, qualified staff continue until unsold expiry, retained closure releases jobs and stops output across restart, finite output and absent-world pause; separate database, no world writes");
                                cancel();getServer().getPluginManager().disablePlugin(IndustryRuntimeProbe.this);
                            }
                            default -> throw new IllegalStateException("Unexpected probe stage");
                        }
                    } catch(Exception failure){cancel();fail(failure);}
                }
            }.runTaskTimer(this,1,1);
        } catch(Exception failure){fail(failure);}
    }
    private PopulationSession create() {
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        var result=new PopulationSession(folder,getLogger(),settings,now::get);result.configureIndustryRuntime(policy);
        result.configureBusinessPurchases(new BusinessPurchasePolicy(500,50,100));result.configureBusinessOperations(BusinessOperationPolicy.prototype());return result;
    }
    private void seed()throws Exception {
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();var town=UUID.randomUUID();var worker=UUID.randomUUID();founder=worker;heir=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(worker,"Fixture",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().insertResident(new Resident(heir,"Qualified Heir",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(worker,"mining",10);
            db.getPopulationRepository().recordSkillExperience(heir,"mining",10);
            db.getPopulationRepository().insertFamilyTie(new FamilyTie(worker,heir,FamilyTie.Type.PARENT_CHILD));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit fixture opening capital");
            var site=new BusinessSite(UUID.randomUUID(),town,world,2048,40,2048,1,"Synthetic explicit test patch; no block ownership");
            var b=new Business(UUID.randomUUID(),town,"Fixture Mine",worker,worker,"mining",site.id(),1,0);mine=b.id();var req=new JobRequirements(216,Map.of("mining",10L));
            new MiningStartupRepository(db.getConnection()).start(new MiningStartupRepository.Request(new BusinessFounding(b,site,req,req,500,"Explicit fixture startup"),
                    new MineralObservation(world,1,true,Commodity.COAL,List.of(new VillageObservation.Position(2048,40,2048))),10,
                    policy.mining().miningBundle(),100,policy.mining().storageSpacePerUnit(),1000,worker));
            var job=db.getBusinessRepository().currentWorkers(mine).getFirst();db.getBusinessRepository().endEmployment(job.id(),0,"Fixture owner deceased");
            db.getPopulationRepository().updateResident(new Resident(worker,"Fixture",town,null,300,Resident.Status.DECEASED));
            db.getBusinessRepository().hire(UUID.randomUUID(),mine,heir,0);
            saleOwner=UUID.randomUUID();db.getPopulationRepository().insertResident(new Resident(saleOwner,"Fixture Seller",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(saleOwner,"mining",10);
            var spareSite=new BusinessSite(UUID.randomUUID(),town,world,2050,40,2048,1,"Synthetic resale site; no block ownership");
            var spare=new Business(UUID.randomUUID(),town,"Fixture Resale Firm",saleOwner,saleOwner,"mining",spareSite.id(),1,0);saleFirm=spare.id();
            db.getBusinessRepository().found(new BusinessFounding(spare,spareSite,req,req,0,"Explicit fixture resale"));
            db.getPopulationRepository().updateResident(new Resident(saleOwner,"Fixture Seller",town,null,300,Resident.Status.DECEASED));
            pendingOwner=UUID.randomUUID();UUID employee=UUID.randomUUID();
            db.getPopulationRepository().insertResident(new Resident(pendingOwner,"Fixture Pending Owner",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().insertResident(new Resident(employee,"Fixture Qualified Employee",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(pendingOwner,"mining",20);db.getPopulationRepository().recordSkillExperience(employee,"mining",5);
            var pendingSite=new BusinessSite(UUID.randomUUID(),town,world,2052,40,2048,1,"Synthetic separate pending-sale patch");
            var pendingBusiness=new Business(UUID.randomUUID(),town,"Fixture Staffed Mine",pendingOwner,pendingOwner,"mining",pendingSite.id(),1,0);pendingMine=pendingBusiness.id();
            new MiningStartupRepository(db.getConnection()).start(new MiningStartupRepository.Request(new BusinessFounding(pendingBusiness,pendingSite,
                    new JobRequirements(216,Map.of("mining",20L)),new JobRequirements(216,Map.of("mining",2L)),0,"Existing qualified staff without a qualified successor"),
                    new MineralObservation(world,1,true,Commodity.COAL,List.of(new VillageObservation.Position(2052,40,2048))),5,
                    policy.mining().miningBundle(),100,policy.mining().storageSpacePerUnit(),0,employee));
            db.getPopulationRepository().updateResident(new Resident(pendingOwner,"Fixture Pending Owner",town,null,300,Resident.Status.DECEASED));
        } finally{db.close();}
    }
    private void check(long stock,long remaining)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            var businesses=new BusinessRepository(c);
            if(!businesses.findBusiness(mine).orElseThrow().ownerId().equals(heir)
                    ||!businesses.findFounding(mine).orElseThrow().business().ownerId().equals(founder))throw new IllegalStateException("Owner/founding identity mismatch");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM business_ownership")){if(r.getLong(1)!=1)throw new IllegalStateException("Duplicate or missing succession");}
            try(var s=c.prepareStatement("SELECT units FROM industry_stock WHERE commodity='coal' AND business_id=?")) {
                boolean closed=new BusinessClosure(c).find(pendingMine).isPresent();
                for(var id:List.of(mine,pendingMine)){s.setString(1,id.toString());try(var r=s.executeQuery()){if(r.getLong(1)!=(id.equals(pendingMine)&&closed?3:stock))throw new IllegalStateException("Unexpected timed stock");}}
            }
            try(var s=c.prepareStatement("SELECT d.remaining_batches FROM mineral_deposits d JOIN businesses b ON b.site_id=d.site_id WHERE b.id=?")) {
                s.setString(1,mine.toString());try(var r=s.executeQuery()){if(r.getLong(1)!=remaining)throw new IllegalStateException("Unexpected finite reserve");}
                s.setString(1,pendingMine.toString());try(var r=s.executeQuery()){if(r.getLong(1)!=5-(new BusinessClosure(c).find(pendingMine).isPresent()?3:stock))throw new IllegalStateException("Unexpected pending-sale reserve");}
            }
            if(!businesses.findBusiness(pendingMine).orElseThrow().ownerId().equals(pendingOwner))throw new IllegalStateException("Employee was invented as qualified owner");
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){if(!r.getString(1).equals("ok"))throw new IllegalStateException("Integrity failed");}
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new IllegalStateException("Foreign key failed");}
        }
    }
    private void fail(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE TIMED INDUSTRY FAIL",failure);getServer().getPluginManager().disablePlugin(this);}
    private void checkClosure()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            var saved=new BusinessClosure(c).find(pendingMine).orElseThrow();
            if(!saved.seller().equals(pendingOwner)||!new BusinessRepository(c).currentWorkers(pendingMine).isEmpty())throw new IllegalStateException("Closure identity/staff mismatch");
            try(var s=c.prepareStatement("SELECT COUNT(*) FROM business_employment WHERE business_id=? AND ended_step IS NULL")){s.setString(1,pendingMine.toString());try(var r=s.executeQuery()){if(r.getLong(1)!=0)throw new IllegalStateException("Closed jobs still open");}}
            if(new BusinessOperations(c,BusinessOperationPolicy.prototype()).evaluate(pendingMine).permitted())throw new IllegalStateException("Closed mine permitted");
        }
    }
    private void checkSale()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.toPath().resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro")) {
            try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
            var sales=new BusinessSales(c,BusinessPurchasePolicy.disabled());
            var listing=new BusinessSaleListings(c).find(saleFirm,saleOwner).orElseThrow();
            if(listing.status()!=BusinessSaleListings.Status.SOLD||listing.price()!=250||!listing.saleOperation().equals(paidSale.operation()))throw new IllegalStateException("Listing completion mismatch");
            if(!sales.find(paidSale.operation()).orElseThrow().equals(paidSale)||sales.entries(paidSale.operation()).size()!=2
                    ||sales.estate(saleOwner).orElseThrow()!=250||new IndustryRepository(c).store(mine).orElseThrow().hundredths()!=750
                    ||!new BusinessRepository(c).findBusiness(saleFirm).orElseThrow().ownerId().equals(heir)
                    ||!new BusinessRepository(c).findFounding(saleFirm).orElseThrow().business().ownerId().equals(saleOwner))
                throw new IllegalStateException("Paid resale identity or conserved money mismatch");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT SUM(hundredths) FROM treasuries")){if(r.getLong(1)!=8500)throw new IllegalStateException("Resale changed village treasury");}
        }
    }
    @Override public void onDisable(){if(session!=null){session.close();session=null;}if(fixture!=null)fixture.shutdown();}
}
