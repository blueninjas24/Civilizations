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
public final class WorkplaceOperationProbe extends JavaPlugin {
    private PopulationSession session;
    private PaperWorkplaceOperations observer;
    private ExecutorService executor;
    private World world;
    private Chunk chunk;
    private Path folder;
    private UUID business;
    private int y;
    private BlockData original;
    private boolean placed,ticket;
    private void require(boolean okay,String message){if(!okay)throw new IllegalStateException(message);}
    private void setup(UUID world)throws Exception {
        var folder=this.folder.toFile();
        var town=UUID.randomUUID();var founder=UUID.randomUUID();business=UUID.randomUUID();var operation=UUID.randomUUID();
        var recipe=ProductionRecipe.smithTools();var supplyPolicy=new WorkshopStarterPolicy(true,1,4000,2,Map.of(Commodity.IRON,1200L,Commodity.COAL,600L));
        var request=new WorkshopStarterSupplies.Request(operation,business,recipe,1,0,1,128);
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Starter fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(founder,"Fixture founder",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit native fixture funds");
            var bands=new EnumMap<Condition,Integer>(Condition.class);for(var c:Condition.values())bands.put(c,100);
            var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),
                Map.of(Commodity.FOOD,100L,Commodity.IRON,10L,Commodity.COAL,10L),
                Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.IRON,BigDecimal.TEN,Commodity.COAL,BigDecimal.TEN),bands);
            var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216);
            economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(2000,false);
            var site=new BusinessSite(UUID.randomUUID(),town,world,4,y,4,System.currentTimeMillis(),"Synthetic workplace; no land rights");
            var requirements=new JobRequirements(216,Map.of());
            var workshop=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(new Business(business,town,"Smith",founder,founder,"blacksmith",site.id(),2,0),site,requirements,requirements,500,"Explicit fixture opportunity"),100,
                Map.of(Commodity.IRON,1L,Commodity.COAL,1L,ToolUpkeepPolicy.TOOLS,1L),4000,founder);
            new BusinessStartupGrants(db.getConnection()).startMarkerWorkshopWithSupplies(workshop,StartupGrantPolicy.prototype(),request,supplyPolicy,Set.of(world),new WorkplaceOperations.Confirmation(new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(4,y,4),WorkplaceObservation.Kind.SMITHING_TABLE),true,System.currentTimeMillis()),System.currentTimeMillis());
            require(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==8500,"Village paid starter sale missing");
            var store=db.getIndustryRepository().store(business).orElseThrow();require(store.hundredths()==1000&&store.reservedIncomingSpace()==3&&store.units().get(Commodity.IRON)==0,"Startup money or paid cargo mismatch");
        }finally{db.close();}
    }
    private PopulationSession owner(){return new PopulationSession(folder.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);}
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
                public void run(){try {
                    if(++ticks>800)throw new IllegalStateException("Workplace operation probe timeout");
                    if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
                    if(stage==0){session=owner();pending=session.start(false,Set.of(id));stage=1;return;}
                    if(stage==1){observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WorkplaceOperationProbe.this,1,1);world.getBlockAt(4,y,4).setType(Material.AIR,false);stage=2;return;}
                    if(availability==null){availability=session.currentBusinessAvailability(business);return;}
                    if(!availability.isDone())return;boolean permitted=availability.join().permitted();availability=null;
                    if(stage==2){if(permitted)return;world.getBlockAt(4,y,4).setType(Material.BARREL,false);waitUntil=ticks+10;stage=3;return;}
                    if(stage==3){require(!permitted,"Wrong kind restored the workplace");if(ticks<waitUntil)return;world.getBlockAt(4,y,4).setType(Material.SMITHING_TABLE,false);stage=4;return;}
                    if(stage==4){if(!permitted)return;pending=session.updateLoadedWorlds(Set.of());stage=5;return;}
                    if(stage==5){world.getBlockAt(4,y,4).setType(Material.AIR,false);waitUntil=ticks+10;stage=6;return;}
                    if(stage==6){require(permitted,"Absent-world marker read changed state");if(ticks<waitUntil)return;pending=session.updateLoadedWorlds(Set.of(id));stage=7;return;}
                    if(stage==7){if(permitted)return;observer.cancel();observer=null;session.close();session=owner();pending=session.start(false,Set.of());stage=8;return;}
                    if(stage==8){require(!permitted,"Restart erased marker pause");world.getBlockAt(4,y,4).setType(Material.SMITHING_TABLE,false);observer=new PaperWorkplaceOperations(session,getLogger());observer.runTaskTimer(WorkplaceOperationProbe.this,1,1);pending=session.updateLoadedWorlds(Set.of(id));stage=9;return;}
                    if(!permitted)return;
                    require(scalar("SELECT COUNT(*) FROM business_workplace_anchors")==1&&scalar("SELECT COUNT(*) FROM workplace_operation_history")==5,"Anchor/history not retained");
                    require(scalar("SELECT hundredths FROM treasuries")==8500&&scalar("SELECT hundredths FROM industry_stores")==1000,"Marker changes lost real money");
                    require(scalar("SELECT COUNT(*) FROM business_employment WHERE ended_step IS NULL")==1&&scalar("SELECT COUNT(*) FROM workshop_starter_orders WHERE delivered_period IS NULL")==1,"Marker changes lost workers or cargo");
                    observer.cancel();observer=null;world.getBlockAt(4,y,4).setBlockData(original,false);placed=false;
                    require(world.getBlockAt(4,y,4).getBlockData().getAsString().equals(original.getAsString()),"Temporary block not exactly restored");
                    getLogger().info("NATIVE WORKPLACE OPERATION PASS: actual scheduled main-thread point checks, removed/wrong/restored marker, SQL owner, absent-world guard, restart, retained money/job/cargo/history; temporary block restored");
                    cancel();getServer().getPluginManager().disablePlugin(WorkplaceOperationProbe.this);
                }catch(Exception failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKPLACE OPERATION FAIL",failure);getServer().getPluginManager().disablePlugin(WorkplaceOperationProbe.this);}}
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Workplace operation fixture refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){if(observer!=null)observer.cancel();if(session!=null)session.close();if(placed){world.getBlockAt(4,y,4).setBlockData(original,false);placed=false;}if(ticket){chunk.removePluginChunkTicket(this);ticket=false;}if(executor!=null)executor.shutdown();}
}
