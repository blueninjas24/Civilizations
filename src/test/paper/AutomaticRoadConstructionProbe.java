import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Guarded actual production road scheduler fixture; never installed on a human server. */
public class AutomaticRoadConstructionProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();private final AtomicLong clock=new AtomicLong(System.currentTimeMillis());
    private final Map<Position,String> originals=new TreeMap<>();private final List<ConstructionDraft> drafts=new ArrayList<>();
    private World world;private Path folder;private DatabaseManager db;private PopulationSession session;private EconomySettings settings;private UUID seller;
    private PaperAutomaticHomePlanning homes;private UUID futureHome;private PaperConstructionLibrary library;private IndustryRuntimePolicy industryRuntime;private PaperAutomaticConstruction automatic;
    private CompletableFuture<?> pending;private int phase,ticks;private boolean authorized,restored;
    protected boolean roadFirstMode(){return false;}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();for(var chunk:world.getLoadedChunks())world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");
        var config=new org.bukkit.configuration.file.YamlConfiguration();var file=new java.io.File(plugin.getDataFolder(),"construction.yml");config.load(file);
        if(roadFirstMode())config.set("land.building-buffer",0);config.set("automatic-construction.enabled",true);config.set("automatic-construction.local-roads-enabled",true);config.set("automatic-construction.review-seconds",1);config.set("automatic-construction.block-interval-ms",0);config.save(file);library=PaperConstructionLibrary.load(plugin);
        var chunk=world.getLoadedChunks()[0];
        if(roadFirstMode()){
            // Explicit empty disposable-world preparation only. Production adapters never load chunks.
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                var prepared=world.getChunkAt(chunk.getX()+dx,chunk.getZ()+dz);world.addPluginChunkTicket(prepared.getX(),prepared.getZ(),this);
            }
        }
        int x=(chunk.getX()<<4)+(roadFirstMode()?8:4),z=(chunk.getZ()<<4)+(roadFirstMode()?8:4),y=world.getHighestBlockYAt(x,z);
        var points=List.of(new Position(x+2,y,z),new Position(x+1,y,z),new Position(x,y,z));var design=LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,points,2,3),library.templates().get("starter_path")).orElseThrow();
        drafts.add(new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),design.origin(),Transform.identity(),design.template(),clock.get()));
        for(var at:RoadGroundReview.requiredCells(design.geometry(),4)){require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Fixture cannot load chunks");var block=world.getBlockAt(at.x(),at.y(),at.z());require(at.y()==y?block.getType()==Material.GRASS_BLOCK:block.getType()==Material.AIR,"Flat natural empty fixture required");originals.put(at,block.getBlockData().getAsString());}
        var end=design.connection();world.getBlockAt(end.x(),end.y(),end.z()).setType(Material.DIRT_PATH,false);
        if(roadFirstMode()){
            for(int xx=x-8;xx<=x+8;xx++)for(int zz=z-8;zz<=z+8;zz++){require(world.isChunkLoaded(xx>>4,zz>>4),"Snapshot cannot load chunks");var at=new Position(xx,y,zz);originals.putIfAbsent(at,world.getBlockAt(xx,y,zz).getBlockData().getAsString());}
            for(var at:List.of(new Position(x+2,y+1,z),new Position(x-2,y+1,z),new Position(x,y+1,z+2),new Position(x,y+1,z-2))){var block=world.getBlockAt(at.x(),at.y(),at.z());require(block.getType()==Material.AIR,"Empty infill fixture required");originals.put(at,block.getBlockData().getAsString());block.setType(Material.OAK_FENCE,false);}
        }
        folder=getDataFolder().toPath().resolve("private-automatic-road");require(!Files.exists(folder),"Fresh private save required");pending=CompletableFuture.runAsync(()->{try{seed();}catch(Exception e){throw new CompletionException(e);}},owner);
        new BukkitRunnable(){@Override public void run(){try{require(++ticks<1800,"Automatic road timeout: "+(session==null?"startup":session.constructionWorkDiagnostic()));if(pending.isDone())step();}catch(Throwable e){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_AUTOMATIC_ROAD_FAIL",e);restore();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"P4_AUTOMATIC_ROAD_SETUP_FAIL",e);restore();if(authorized)getServer().shutdown();}}
    private void step()throws Exception{
        var result=pending.join();switch(phase){
            case 0->{var p=PopulationSession.Settings.prototype();var cfg=new PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());session=new PopulationSession(folder.toFile(),getLogger(),cfg,clock::get,settings,FarmObservation.Policy.prototype());session.configureFarmBusinesses(FarmBusinessPolicy.prototype());session.configureConstruction(ConstructionBudgetPolicy.prototype(),new ConstructionLaborPolicy(true,1,1,100,25));session.configureIndustryRuntime(industryRuntime);phase=1;pending=session.start(false,Set.of(world.getUID()));}
            case 1->{automatic=new PaperAutomaticConstruction(this,session,library,clock::get,getLogger());automatic.runTaskTimer(this,1,1);homes=new PaperAutomaticHomePlanning(session,library,new AutomaticHomePlanningSettings(true,128,10,1,roadFirstMode()?8:12),DwellingInspection.Policy.prototype(),clock::get,getLogger());homes.runTaskTimer(this,1,1);phase=2;pending=CompletableFuture.completedFuture(null);}
            case 2->{if(ticks%20==0){clock.addAndGet(1000);phase=3;var d=drafts.getFirst();pending=session.pulse().thenCompose(ignored->session.constructionRoadStates(d.world(),roadFirstMode()?new ConstructionPreview.Bounds(d.origin().plus(new Position(-12,0,-12)),d.origin().plus(new Position(12,0,12))):ConstructionPreview.footprint(d.template(),d.transform(),d.origin())));}}
            case 3->{if(((Map<?,?>)result).isEmpty()){phase=2;pending=CompletableFuture.completedFuture(null);return;}if(roadFirstMode()){phase=8;pending=session.housingProjectStatus(drafts.getFirst().settlement(),world.getUID(),DwellingInspection.Policy.prototype());}else{phase=6;pending=CompletableFuture.completedFuture(null);}}
            case 8->{var report=(HousingProjectStatus.Report)result;require(report.items().size()==1,"Exactly one actual family proposal");futureHome=report.items().getFirst().family().proposal().draft();phase=6;pending=CompletableFuture.completedFuture(null);}
            case 6->{if(ticks%20==0){clock.addAndGet(1000);phase=7;pending=session.pulse().thenCompose(ignored->session.housingProjectInspection(futureHome,world.getUID(),DwellingInspection.Policy.prototype()));}}
            case 7->{if(!((HousingProjectStatus.Item)result).siteHeld()){phase=6;pending=CompletableFuture.completedFuture(null);return;}homes.cancel();homes.close();homes=null;automatic.cancel();automatic.close();automatic=null;session.close();session=null;phase=4;pending=CompletableFuture.runAsync(()->{try{verify();}catch(Exception e){throw new CompletionException(e);}},owner);}
            case 4->{restore();getLogger().info((roadFirstMode()?"P4_ROAD_FIRST_GROWTH_PASS: ordinary housing need created the frozen road/home/family proposal; ":"P4_AUTOMATIC_ROAD_PASS: ")+" actual configured shared builder purchased finite real stock, waited for cargo and earned labor, paved the frozen native corridor and registered permanent road/site release; normal loaded-only home planning handed completed access to the original frozen home site; no home built or capacity/people added; saved cash/goods conserved; restored every fixture cell.");phase=5;getServer().shutdown();}
        }
    }
    private void seed()throws Exception{
        db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();var c=db.getConnection();UUID worldId=drafts.getFirst().world();UUID town=drafts.getFirst().settlement(),ownerId=UUID.randomUUID(),builder=UUID.randomUUID();
        try(var s=c.prepareStatement("INSERT INTO settlements VALUES(?,'Private paid road fixture','fixture',0,64,0,1,0,0)")){s.setString(1,town.toString());s.executeUpdate();}
        db.getVillageIdentityRepository().observeWorld(worldId,"Private road fixture");try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,worldId.toString());s.executeUpdate();}
        if(!roadFirstMode())for(var d:drafts){new ConstructionDraftRepository(c).save(d);new ConstructionSiteReservations(c).reserve(d.id(),new ConstructionMaterials.Authority(d.id(),d.world(),clock.get(),true,true),clock.get());}
        db.getPopulationRepository().insertResident(new Resident(ownerId,"Explicit fixture supplier",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().insertResident(new Resident(builder,"Explicit fixture builder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(c).initialize(UUID.randomUUID(),town,100000,"Explicit disposable fixture money");seller=UUID.randomUUID();var site=new BusinessSite(UUID.randomUUID(),town,worldId,0,64,0,10,"Explicit fixture supplier site");var company=new Business(seller,town,"Fixture warehouse",ownerId,ownerId,"warehouse",site.id(),2,0);var requirements=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(company,site,requirements,requirements,500,"Explicit fixture startup"));db.getBusinessRepository().hire(UUID.randomUUID(),seller,ownerId,0);db.getIndustryRepository().openStore(seller,256,Map.of(Commodity.STONE,1L),0);
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=20 WHERE business_id=?")){s.setString(1,seller.toString());require(s.executeUpdate()==1,"Explicit finite fixture stock");}
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,10000L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE),bands);
        long now=clock.get();var economy=new EconomyRuntimeRepository(c,settings,FarmObservation.Policy.prototype(),216);economy.observe(new FarmObservation(town,worldId,now,true,0),now);economy.startOnline(now,Set.of(worldId));economy.pulse(now+1000,false);
        var p=IndustryPolicy.prototype();var transport=new IndustryPolicy(p.depositBatchesPerObservedOre(),p.batchesPerWorker(),p.siteBatchCapacity(),p.storageCapacity(),1,128,p.miningBundle(),p.storageSpacePerUnit());var runtime=new IndustryRuntimePolicy(true,1000,8,transport,Map.of(),WorkshopSupplyPolicy.disabled(),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());
        industryRuntime=runtime;clock.set(now+1000);
        UUID family=UUID.randomUUID();db.getPopulationRepository().insertHousehold(new Household(family,town,"Private road beneficiary"));
        try(var s=c.prepareStatement("UPDATE residents SET household_id=? WHERE id=?")){s.setString(1,family.toString());s.setString(2,builder.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO household_capacity VALUES(?,0)")){s.setString(1,family.toString());s.executeUpdate();}
        if(roadFirstMode()){
            var end=LocalRoadDesign.geometry(drafts.getFirst()).connection();
            try(var s=c.prepareStatement("UPDATE settlements SET center_x=?,center_y=?,center_z=? WHERE id=?")){s.setInt(1,end.x());s.setInt(2,end.y()+1);s.setInt(3,end.z());s.setString(4,town.toString());s.executeUpdate();}
            db.close();db=null;return;
        }
        var homePlan=library.templates().get("starter_home");var anchor=homePlan.anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow().at();var road=drafts.getFirst();
        var end=road.origin().plus(new Position(1,0,0));
        var home=new ConstructionDraft(UUID.randomUUID(),town,worldId,new Position(end.x()-anchor.x(),end.y()-anchor.y(),end.z()-anchor.z()),Transform.identity(),homePlan,road.createdMillis());
        futureHome=home.id();new ConstructionDraftRepository(c).save(home);
        try(var s=c.prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,?,NULL,NULL)")){
            s.setString(1,home.id().toString());s.setString(2,family.toString());s.setString(3,town.toString());s.setString(4,worldId.toString());s.setString(5,homePlan.fingerprint());s.setLong(6,home.createdMillis());s.executeUpdate();
        }
        try(var s=c.prepareStatement("INSERT INTO housing_road_projects VALUES(?,?,?)")){s.setString(1,road.id().toString());s.setString(2,home.id().toString());s.setLong(3,home.createdMillis());s.executeUpdate();}
        db.close();db=null;
    }
    private void verify()throws Exception{
        db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();var c=db.getConnection();var road=drafts.getFirst();
        if(roadFirstMode()){var link=new HousingRoadProjects(c).findHome(futureHome).orElseThrow();road=new ConstructionDraftRepository(c).find(link.roadDraft()).orElseThrow();drafts.set(0,road);}
        require(new CivRoadCompletion(c).findDraft(road.id()).isPresent(),"Actual scheduler registered completion");require(!new ConstructionSiteReservations(c).find(road.id()).orElseThrow().active(),"Road site released");
        require(new ConstructionSiteReservations(c).find(futureHome).orElseThrow().active(),"Normal planner handed off original home site");
        var stock=db.getIndustryRepository().store(seller).orElseThrow();long units=road.template().materials().get(Commodity.STONE),cost=units*25;
        getLogger().info("P4_ROAD_TERMS: "+road.template().blockCount()+" cells, "+units+" stone, "+cost+" hundredths, "+road.template().work()+" work; original family/home retained");
        require(stock.units().get(Commodity.STONE)==20-units&&stock.hundredths()==cost,"Finite stock and actual prices conserved");require(new TreasuryRepository(c).load(road.settlement()).orElseThrow().hundredths()==99500-cost,"Original money conserved after500 startup and real supplies");
        for(String query:List.of("SELECT count(*)="+road.template().blockCount()+" FROM construction_block_journal WHERE state='APPLIED'","SELECT count(*)=2 FROM residents","SELECT count(*)=1 FROM construction_supply_orders","SELECT sum(effort)="+road.template().work()+" FROM construction_public_work"))try(var s=c.createStatement();var r=s.executeQuery(query)){require(r.getInt(1)==1,"Saved conservation: "+query);}
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Healthy save");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Valid foreign keys");}db.close();db=null;
    }
    private void restore(){if(!authorized||restored||world==null)return;for(var e:originals.entrySet()){var at=e.getKey();require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(at.x(),at.y(),at.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){if(homes!=null){homes.cancel();homes.close();homes=null;}if(automatic!=null){automatic.cancel();automatic.close();automatic=null;}restore();if(session!=null){session.close();session=null;}owner.execute(()->{if(db!=null)db.close();});owner.shutdown();}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException(why);}
}
