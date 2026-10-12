import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Guarded disposable first-merchant study/startup/marker test, never an installable gameplay plugin. */
public final class WarehouseStartupProbe extends JavaPlugin {
    private PopulationSession session;private PaperWarehouseStartup startup;private PaperWorkplaceOperations observer;
    private ExecutorService executor;private World world;private Chunk chunk;private Path folder;
    private final Map<Position,BlockData> originals=new LinkedHashMap<>();private boolean ticket,authorized;
    private UUID town,merchant,business;private int y;private EconomySettings settings;private IndustryRuntimePolicy industry;
    private final FarmToolUpkeepPolicy upkeep=new FarmToolUpkeepPolicy(true,1,2,50,8);
    private final WorkshopStarterPolicy starter=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
    private void require(boolean okay,String why){if(!okay)throw new IllegalStateException(why);}
    private void setup(UUID id)throws Exception {
        town=UUID.randomUUID();merchant=new UUID(0,3);var farmer=new UUID(0,1);var smith=new UUID(0,2);
        var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L,Commodity.STONE,10L,Commodity.TIMBER,20L),
            Map.of(Commodity.FOOD,BigDecimal.valueOf(2),Commodity.IRON,BigDecimal.valueOf(12),Commodity.COAL,BigDecimal.valueOf(6),Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
        var mining=new IndustryPolicy(150,8,32,1024,1,128,Map.of(Commodity.COAL,1L,Commodity.STONE,3L),Map.of(Commodity.COAL,1L,Commodity.STONE,1L,Commodity.IRON,1L,Commodity.TIMBER,1L,ToolUpkeepPolicy.TOOLS,1L));
        industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of("smith_tools",ProductionRecipe.smithTools()),new WorkshopSupplyPolicy(true,1,16,starter.prices()),new WarehouseSupplyPolicy(true,16,16));
        var db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();try {
            db.saveSettlement(new Settlement(town,"Native merchant fixture","Fixture",0,y,0,3,0,0));db.getVillageIdentityRepository().observeWorld(id,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,id);
            for(var person:List.of(farmer,smith,merchant))db.getPopulationRepository().insertResident(new Resident(person,"Explicit fixture adult",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(smith,"smithing",10); // Existing qualified customer; merchant starts with zero commerce.
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit fixture funds");
            var food=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
            food.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0),BusinessOperationPolicy.prototype());food.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype(),mining);food.configureWorkLearning(WorkLearningPolicy.prototype());
            food.observe(new FarmObservation(town,id,1000,true,16,new Position(0,y,0)),1000);food.startOnline(1000,Set.of(id));food.pulse(2000,false);food.pulse(3000,false);
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(id,0,0,3000,true,List.of(new WorkplaceObservation.Marker(new Position(4,y,4),WorkplaceObservation.Kind.SMITHING_TABLE))));
            var opportunities=new WorkshopOpportunities(db.getConnection(),WorkshopOpportunityPolicy.prototype(),industry,BusinessOperationPolicy.prototype(),StartupGrantPolicy.prototype(),starter,upkeep,FarmToolBudgetPolicy.prototype());
            var investment=new WorkshopInvestment(db.getConnection(),opportunities,WorkshopFoundingPolicy.prototype(),WorkshopOpportunityPolicy.prototype(),industry,StartupGrantPolicy.prototype(),starter,216,0);
            var draft=investment.prepare(town,0,3000,Set.of(id)).draft().orElseThrow();investment.accept(draft,new WorkplaceOperations.Confirmation(draft.anchor(),true,3000),3000,Set.of(id));
            var production=new IndustryRuntime(db.getConnection(),industry,BusinessOperationPolicy.prototype(),WorkLearningPolicy.prototype());production.start(3000);production.pulse(4000,Set.of(id));
            new FarmToolPurchases(db.getConnection(),BusinessOperationPolicy.prototype()).purchase(new FarmToolPurchases.Request(UUID.randomUUID(),FarmEnterprises.businessId(town),draft.anchor().business(),2,1,128),FarmToolBudgetPolicy.prototype(),Set.of(id));food.pulse(4000,false);
            long capture=System.currentTimeMillis();new OreTrackingRepository(db.getConnection()).begin(1000);
            new MineralOpportunities(db.getConnection()).observe(town,List.of(new MineralObservation(id,capture,true,Commodity.COAL,List.of(new Position(12,y,4))),new MineralObservation(id,capture,true,Commodity.IRON,List.of(new Position(14,y,4)))));
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(id,0,0,capture,true,List.of(new WorkplaceObservation.Marker(new Position(4,y,4),WorkplaceObservation.Kind.SMITHING_TABLE),new WorkplaceObservation.Marker(new Position(8,y,4),WorkplaceObservation.Kind.BARREL))));
        }finally{db.close();}
    }
    private PopulationSession owner(){
        var d=PopulationSession.Settings.prototype();var population=new PopulationSession.Settings(0,d.millisPerMonth(),d.maximumOfflineMonths(),d.monthsPerBatch(),d.population(),d.migration(),d.recognition(),d.enrollment());
        var owner=new PopulationSession(folder.toFile(),getLogger(),population,System::currentTimeMillis,settings,FarmObservation.Policy.prototype());
        owner.configureFarmBusinesses(new FarmBusinessPolicy(true,1,0,216,0));owner.configureFarmTools(upkeep,FarmToolBudgetPolicy.prototype());owner.configureWorkLearning(WorkLearningPolicy.prototype());owner.configureBusinessOperations(BusinessOperationPolicy.prototype());
        owner.configureIndustryRuntime(industry);owner.configureStudy(StudyPolicy.prototype());owner.configureCareerStudy(CareerStudyPolicy.prototype());owner.configureWorkplaces(WorkplaceObserverPolicy.prototype());
        owner.configureMineFounding(MineFoundingPolicy.prototype(),StartupGrantPolicy.prototype());owner.configureWarehouseOpportunities(WarehouseOpportunityPolicy.prototype());owner.configureWarehouseFounding(WarehouseFoundingPolicy.prototype());return owner;
    }
    private long scalar(String sql)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:file:"+folder.resolve("civilizations.db").toAbsolutePath().toString().replace('\\','/')+"?mode=ro");var s=c.createStatement();var r=s.executeQuery(sql)){require(r.next(),"Missing fixture result");return r.getLong(1);}
    }
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked isolated localhost fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();y=world.getMaxHeight()-4;chunk=world.getChunkAt(0,0);ticket=chunk.addPluginChunkTicket(this);
        // Explicit fixture setup only. Production adapters never load/ticket chunks or change blocks/inventories.
        var blocks=new LinkedHashMap<Position,Material>();blocks.put(new Position(4,y,4),Material.SMITHING_TABLE);blocks.put(new Position(8,y,4),Material.BARREL);blocks.put(new Position(12,y,4),Material.COAL_ORE);blocks.put(new Position(14,y,4),Material.IRON_ORE);
        for(var p:blocks.keySet()){var old=world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().clone();require(old.getMaterial().isAir(),"Refuse to replace terrain or containers");originals.put(p,old);}
        blocks.forEach((p,type)->world.getBlockAt(p.x(),p.y(),p.z()).setType(type,false));folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());executor=Executors.newSingleThreadExecutor();var id=world.getUID();
        CompletableFuture<Void> init=CompletableFuture.runAsync(()->{try{setup(id);}catch(Exception error){throw new CompletionException(error);}},executor);
        new BukkitRunnable(){CompletableFuture<?> pending=init;CompletableFuture<BusinessOperations.Availability> availability;int stage,ticks,waitUntil;
            public void run(){try{
                if(++ticks>800)throw new IllegalStateException("Merchant startup fixture timeout");if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                if(stage==0){session=owner();pending=session.start(true,Set.of(id));stage=1;return;}
                if(stage==1){business=UUID.nameUUIDFromBytes(("civilizations:first-warehouse-business-v1:"+town+":"+id+":8:"+y+":4").getBytes(java.nio.charset.StandardCharsets.UTF_8));startup=new PaperWarehouseStartup(session,getLogger());startup.runTaskTimer(WarehouseStartupProbe.this,1,1);stage=2;return;}
                if(stage==2){if(scalar("SELECT COUNT(*) FROM businesses WHERE industry='warehouse'")==0)return;startup.cancel();startup=null;
                    require(scalar("SELECT experience FROM resident_skills WHERE resident_id='"+merchant+"' AND skill='commerce'")==10&&scalar("SELECT COUNT(*) FROM resident_courses WHERE status='COMPLETED'")==1,"Merchant did not earn real commerce course");
                    require(scalar("SELECT COUNT(*) FROM study_spending")==10&&scalar("SELECT COUNT(*) FROM warehouse_founder_courses")==1,"Missing saved practice/link receipts");
                    require(scalar("SELECT hundredths FROM treasuries")+scalar("SELECT SUM(hundredths) FROM industry_stores")==29000,"Warehouse startup did not conserve funds/accounted fees");
                    require(scalar("SELECT SUM(units) FROM industry_stock WHERE business_id='"+business+"'")==0&&scalar("SELECT COUNT(*) FROM startup_grants")==2,"Warehouse invented stock or duplicate grant");
                    observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WarehouseStartupProbe.this,1,1);world.getBlockAt(8,y,4).setType(Material.AIR,false);stage=3;return;}
                if(availability==null){availability=session.currentBusinessAvailability(business);return;}if(!availability.isDone())return;boolean permitted=availability.join().permitted();availability=null;
                if(stage==3){if(permitted)return;world.getBlockAt(8,y,4).setType(Material.SMITHING_TABLE,false);waitUntil=ticks+10;stage=4;return;}
                if(stage==4){require(!permitted,"Wrong block resumed warehouse");if(ticks<waitUntil)return;world.getBlockAt(8,y,4).setType(Material.BARREL,false);stage=5;return;}
                if(stage==5){if(!permitted)return;world.getBlockAt(8,y,4).setType(Material.AIR,false);stage=6;return;}
                if(stage==6){if(permitted)return;observer.cancel();observer=null;session.close();session=owner();pending=session.start(false,Set.of());stage=7;return;}
                if(stage==7){require(!permitted,"Restart cleared marker pause");world.getBlockAt(8,y,4).setType(Material.BARREL,false);observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WarehouseStartupProbe.this,1,1);pending=session.updateLoadedWorlds(Set.of(id));stage=8;return;}
                if(!permitted)return;require(scalar("SELECT COUNT(*) FROM business_workplace_anchors")==2&&scalar("SELECT COUNT(*) FROM workplace_operation_history WHERE business_id='"+business+"'")==5,"Fixed markers/history lost");
                require(scalar("SELECT COUNT(*) FROM business_employment WHERE ended_step IS NULL")==3&&scalar("SELECT COUNT(*) FROM startup_grants")==2,"Jobs or grants changed on marker/restart");
                observer.cancel();observer=null;restore();getLogger().info("NATIVE WAREHOUSE STARTUP PASS: unskilled merchant earned ten real supply-backed commerce periods, ordinary SQL timer and scheduled loaded-barrel confirmation founded funded empty warehouse for paid workshop/farm chain; missing/wrong/restored marker and restart retained cash/jobs/history; four temporary blocks restored");
                cancel();getServer().getPluginManager().disablePlugin(WarehouseStartupProbe.this);getServer().shutdown();
            }catch(Exception error){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WAREHOUSE STARTUP FAIL",error);getServer().getPluginManager().disablePlugin(WarehouseStartupProbe.this);if(authorized)getServer().shutdown();}}
        }.runTaskTimer(this,1,1);
    }catch(Exception error){getLogger().log(java.util.logging.Level.SEVERE,"Warehouse fixture refused",error);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}}
    private void restore(){for(var e:originals.entrySet()){var p=e.getKey();world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(e.getValue(),false);require(world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString().equals(e.getValue().getAsString()),"Temporary block not exactly restored");}originals.clear();}
    @Override public void onDisable(){if(startup!=null)startup.cancel();if(observer!=null)observer.cancel();if(session!=null)session.close();if(world!=null)restore();if(ticket){chunk.removePluginChunkTicket(this);ticket=false;}if(executor!=null)executor.shutdown();}
}
