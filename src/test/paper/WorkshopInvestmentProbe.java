import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.paper.PaperWorkplaceOperations;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** One air-block fixture under strict disposable guards. Production never loads chunks or touches inventory. */
public final class WorkshopInvestmentProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperWorkplaceOperations observer;
    private com.blueninjas24.civilizations.paper.PaperWorkshopStartup startupObserver;
    private ExecutorService executor;
    private World world;
    private Chunk chunk;
    private Path folder;
    private UUID business,town;
    private EconomySettings settings;
    private IndustryRuntimePolicy industry;
    private FarmToolUpkeepPolicy upkeep;
    private WorkshopStarterPolicy starter;
    private int y;
    private BlockData original;
    private boolean placed,ticket;
    private void require(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    private void setup(UUID world)throws Exception {
        var folder=this.folder.toFile();
        town=UUID.randomUUID();var farmer=new UUID(0,1);var prospect=new UUID(0,2);
        upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);var budget=FarmToolBudgetPolicy.prototype();
        starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L,Commodity.STONE,10L,Commodity.TIMBER,10L),
            Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.IRON,BigDecimal.valueOf(12),Commodity.COAL,BigDecimal.valueOf(6),Commodity.STONE,BigDecimal.valueOf(2),Commodity.TIMBER,BigDecimal.valueOf(4)),bands);
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Workshop prospect fixture","Fixture",0,y,0,2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var resident:List.of(farmer,prospect))db.getPopulationRepository().insertResident(new Resident(resident,"Fixture resident",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funds");
            var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
            economy.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());
            economy.configureFarmTools(upkeep,budget,IndustryPolicy.prototype());economy.configureWorkLearning(WorkLearningPolicy.prototype());
            economy.observe(new FarmObservation(town,world,1000,true,16,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(0,y,0)),1000);
            economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);economy.pulse(3000,false);
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,System.currentTimeMillis(),true,List.of(
                new WorkplaceObservation.Marker(new com.blueninjas24.civilizations.settlement.VillageObservation.Position(4,y,4),WorkplaceObservation.Kind.SMITHING_TABLE))));
        }finally{db.close();}
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),Map.of(Commodity.COAL,1L,Commodity.STONE,1L,Commodity.IRON,1L,ToolUpkeepPolicy.TOOLS,1L));
        var recipe=ProductionRecipe.smithTools();industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of(recipe.id(),recipe),new WorkshopSupplyPolicy(true,1,16,starter.prices()));
    }
    private PopulationSession owner() {
        var d=PopulationSession.Settings.prototype();var population=new PopulationSession.Settings(0,d.millisPerMonth(),d.maximumOfflineMonths(),d.monthsPerBatch(),d.population(),d.migration(),d.recognition(),d.enrollment());
        var session=new PopulationSession(folder.toFile(),getLogger(),population,System::currentTimeMillis,settings,FarmObservation.Policy.prototype());
        if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))session.configureBusinessAllocation(BusinessAllocationPolicy.prototype());
        session.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));session.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype());session.configureWorkLearning(WorkLearningPolicy.prototype());
        session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureIndustryRuntime(industry);session.configureWorkplaces(WorkplaceObserverPolicy.prototype());
        session.configureStudy(StudyPolicy.prototype());session.configureCareerStudy(CareerStudyPolicy.prototype());
        session.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());
        session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),starter);session.configureWorkshopFounding(WorkshopFoundingPolicy.prototype());session.configureMineFounding(MineFoundingPolicy.disabled(),StartupGrantPolicy.prototype());
        return session;
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){require(r.next(),"Missing fixture row");return r.getLong(1);}
    }
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture required");
            world=getServer().getWorlds().getFirst();y=world.getMaxHeight()-4;
            // Explicit fixture setup only; production adapter never loads or tickets chunks.
            chunk=world.getChunkAt(0,0);ticket=chunk.addPluginChunkTicket(this);
            original=world.getBlockAt(4,y,4).getBlockData().clone();require(original.getMaterial().isAir(),"Refuse to replace terrain or containers");
            placed=true;world.getBlockAt(4,y,4).setType(Material.SMITHING_TABLE,false);
            folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();var id=world.getUID();
            CompletableFuture<Void> startup=CompletableFuture.runAsync(()->{try{setup(id);}catch(Exception failure){throw new CompletionException(failure);}},executor);
            new BukkitRunnable() {
                CompletableFuture<?> pending=startup;CompletableFuture<BusinessOperations.Availability> availability;
                int ticks,stage,waitUntil;
                CompletableFuture<WarehouseOpportunities.Review> warehouseReview;
                public void run(){try {
                    if(++ticks>800)throw new IllegalStateException("Workplace operation probe timeout");
                    if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                    if(stage==0){session=owner();pending=session.start(true,Set.of(id));stage=1;return;}
                    if(stage==1) {
                        business=UUID.nameUUIDFromBytes(("civilizations:first-workshop-business-v1:"+town+":"+id+":4:"+y+":4").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        startupObserver=new com.blueninjas24.civilizations.paper.PaperWorkshopStartup(session,getLogger());startupObserver.runTaskTimer(WorkshopInvestmentProbe.this,1,1);stage=11;return;
                    }
                    if(stage==11){if(scalar("SELECT COUNT(*) FROM business_workplace_anchors")==0)return;startupObserver.cancel();startupObserver=null;observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WorkshopInvestmentProbe.this,1,1);world.getBlockAt(4,y,4).setType(Material.AIR,false);stage=2;return;}
                    if(availability==null){availability=session.currentBusinessAvailability(business);return;}
                    if(!availability.isDone())return;boolean permitted=availability.join().permitted();availability=null;
                    if(stage==2){if(permitted)return;world.getBlockAt(4,y,4).setType(Material.BARREL,false);waitUntil=ticks+10;stage=3;return;}
                    if(stage==3){require(!permitted,"Wrong kind restored the workplace");if(ticks<waitUntil)return;world.getBlockAt(4,y,4).setType(Material.SMITHING_TABLE,false);stage=4;return;}
                    if(stage==4){if(!permitted)return;pending=session.updateLoadedWorlds(Set.of());stage=5;return;}
                    if(stage==5){world.getBlockAt(4,y,4).setType(Material.AIR,false);waitUntil=ticks+10;stage=6;return;}
                    if(stage==6){require(permitted,"Absent-world marker read changed state");if(ticks<waitUntil)return;pending=session.updateLoadedWorlds(Set.of(id));stage=7;return;}
                    if(stage==7){if(permitted)return;observer.cancel();observer=null;session.close();session=owner();pending=session.start(false,Set.of());stage=8;return;}
                    if(stage==8){require(!permitted,"Restart erased marker pause");world.getBlockAt(4,y,4).setType(Material.SMITHING_TABLE,false);observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WorkshopInvestmentProbe.this,1,1);pending=session.updateLoadedWorlds(Set.of(id));stage=9;return;}
                    if(!permitted)return;
                    if(warehouseReview==null){warehouseReview=session.currentWarehouseOpportunities(town);return;}
                    if(!warehouseReview.isDone())return;require(warehouseReview.join().candidates().isEmpty(),"Disabled upstream chain invented a warehouse prospect");
                    require(scalar("SELECT COUNT(*) FROM business_workplace_anchors")==1&&scalar("SELECT COUNT(*) FROM workplace_operation_history")==5,"Anchor/history not retained");
                    require(scalar("SELECT hundredths FROM treasuries")+scalar("SELECT hundredths FROM industry_stores WHERE business_id='"+business+"'")==29500,"Training/startup/marker changes lost real money");
                    require(scalar("SELECT COUNT(*) FROM business_employment WHERE ended_step IS NULL")==2&&scalar("SELECT COUNT(*) FROM workshop_starter_orders")==1,"Marker changes lost workers or cargo");
                    require(scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'")==1&&scalar("SELECT COUNT(*) FROM study_spending")==20,"First owner did not earn ten actual two-material practice periods");
                    if(Boolean.getBoolean("civilizations.probe.sharedAllocation"))require(scalar("SELECT COUNT(*) FROM resource_investment_reviews WHERE industry='blacksmith'")==1
                            &&scalar("SELECT sequence FROM resource_investment_state")==1,"Shared marker selection must survive restart with exactly one paid startup audit");
                    observer.cancel();observer=null;world.getBlockAt(4,y,4).setBlockData(original,false);placed=false;
                    require(world.getBlockAt(4,y,4).getBlockData().getAsString().equals(original.getAsString()),"Temporary block not exactly restored");
                    getLogger().info("NATIVE WORKSHOP INVESTMENT PASS: unskilled fixture adult earned ten supply-backed practice periods through normal SQL timer, automatic Paper review/table proof/paid startup for real farm customer; scheduled marker lifecycle, absence/restart, conserved money/jobs/cargo/history; temporary block restored");
                    cancel();getServer().getPluginManager().disablePlugin(WorkshopInvestmentProbe.this);
                    if(Boolean.getBoolean("civilizations.probe.sharedAllocation")){getLogger().info("NATIVE SHARED MARKER INVESTMENT PASS: ordinary Paper review/point proof and SQL-owner paid grant share one selection audit across absence/restart; original block restored and fixture ticket released");getServer().shutdown();}
                }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKSHOP INVESTMENT FAIL",failure);getServer().getPluginManager().disablePlugin(WorkshopInvestmentProbe.this);}}
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Workplace operation fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(startupObserver!=null)startupObserver.cancel();if(observer!=null)observer.cancel();if(session!=null)session.close();if(placed){world.getBlockAt(4,y,4).setBlockData(original,false);placed=false;}if(ticket){chunk.removePluginChunkTicket(this);ticket=false;}if(executor!=null)executor.shutdown();}
}
