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

/** Destructive only inside an explicitly marked, empty, disposable loopback fixture; restores captured cells. */
public final class CivHomeCompletionProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();
    private final AtomicLong clock=new AtomicLong(System.currentTimeMillis());
    private final Map<Position,String> originals=new LinkedHashMap<>();
    private final List<String> nativeStates=new ArrayList<>();
    private final UUID family=UUID.randomUUID(),completionOperation=UUID.randomUUID();
    private DatabaseManager database;private PopulationSession session;private Path folder;private World world;
    private ConstructionDraft draft;private EconomySettings settings;private UUID supplier;
    private PaperConstructionWriter writer;private PaperConstructionWorkSurvey survey;private PaperDwellingSurvey dwelling;
    private ConstructionWorkQueue.Work work;private PaperConstructionWorkSurvey.Frame frame;
    private CivBuildingCompletion.Receipt receipt;private BuildingLayout layout;
    private CompletableFuture<?> pending;private int phase,ticks,index;private boolean authorized,restored,restarted;

    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");
        authorized=true;world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");require(plugin!=null&&plugin.isEnabled(),"Production plugin active");
        var template=PaperConstructionLibrary.load(plugin).templates().get("starter_home");var transform=new Transform(ConstructionTemplate.Rotation.CLOCKWISE_90,true);
        draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),new Position(x,world.getHighestBlockYAt(x,z)+1,z),transform,template,1000);
        var held=new HashSet<Long>();
        for(var p:ConstructionPreview.requiredCells(template,transform,draft.origin())){
            int cx=p.x()>>4,cz=p.z()>>4;require(world.isChunkLoaded(cx,cz),"All fixture cells already loaded");long key=((long)cx<<32)^(cz&0xffffffffL);
            if(held.add(key))world.addPluginChunkTicket(cx,cz,this);originals.put(p,world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString());
            if(p.y()>=draft.origin().y())require(world.getBlockAt(p.x(),p.y(),p.z()).getType()==Material.AIR,"Empty fixture footprint required");
            else world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.STONE,false); // Stable disposable substrate; grass under new floors naturally becomes dirt.
        }
        for(var a:template.anchors())if(a.role()==Role.ROAD){var p=draft.origin().plus(transform.apply(a.at(),template.size()));world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.DIRT_PATH,false);}
        var prepared=PaperConstructionLibrary.prepare(template,transform);
        for(int s=0;s<template.stages().size();s++)for(var p:template.stages().get(s).blocks().keySet())nativeStates.add(prepared.stages().get(s).blocks().get(transform.apply(p,template.size())));
        folder=getDataFolder().toPath().resolve("private-home");require(!Files.exists(folder),"Fresh private database required");
        pending=CompletableFuture.runAsync(()->{try{seed();}catch(Exception e){throw new CompletionException(e);}},owner);
        new BukkitRunnable(){@Override public void run(){try{if(++ticks>3600)throw new IllegalStateException("Full home probe timeout at phase "+phase+" block "+index);if(pending.isDone())advance();}
            catch(Throwable e){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_HOME_COMPLETION_FAIL",e);try{restore();}finally{getServer().shutdown();}}}}.runTaskTimer(this,1,1);
    }catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"P4_HOME_COMPLETION_SETUP_FAIL",e);restore();if(authorized)getServer().shutdown();}}

    private void advance()throws Exception{
        var result=pending.join();switch(phase){
            case 0->{newSession();phase=1;pending=session.start(false,Set.of(draft.world()));}
            case 1->page();
            case 2->{var entries=((ConstructionWorkQueue.Page)result).projects();require(entries.size()==1,"One saved paid home");work=entries.getFirst();
                if(work.state()==ConstructionWorkQueue.State.WAIT_LABOR){clock.addAndGet(1000);phase=1;pending=session.pulse();return;}
                require(work.state()==ConstructionWorkQueue.State.READY||work.state()==ConstructionWorkQueue.State.RESUME||work.state()==ConstructionWorkQueue.State.COMPLETE,"Work ready or complete: "+work.explanation());
                survey=new PaperConstructionWorkSurvey(work,clock.get());phase=3;pending=CompletableFuture.completedFuture(null);}
            case 3->{var observed=survey.advance(128,clock.get());if(observed.isEmpty())return;frame=observed.get();require(frame.authority().siteCleared(),"Full native review: "+frame.explanation());
                if(work.state()==ConstructionWorkQueue.State.COMPLETE){phase=5;pending=session.completeCivHome(completionOperation,draft.id(),new CivBuildingCompletion.Evidence(frame.authority(),frame.cells()));return;}
                phase=4;pending=session.prepareAuthorizedPublicConstructionStep(work.pending().map(ConstructionBlockJournal.Intent::operation).orElseGet(UUID::randomUUID),draft.id(),index,nativeStates.get(index),frame.authority());}
            case 4->{var prepared=(PopulationSession.PreparedConstructionStep)result;var applied=writer.attempt(work,frame,prepared);
                require(applied.state()==(work.state()==ConstructionWorkQueue.State.RESUME?PaperConstructionStep.State.ALREADY_PRESENT:PaperConstructionStep.State.APPLIED),"Paid native block "+index+": "+applied);
                require(writer.attempt(work,frame,prepared).state()==PaperConstructionStep.State.ALREADY_PRESENT,"Exact retry does not duplicate native work");
                if(index==0&&!restarted){restarted=true;session.close();newSession();phase=1;pending=session.start(false,Set.of(draft.world()));return;}
                var intent=prepared.intent();var p=intent.at();phase=8;pending=session.acknowledgePublicConstructionStep(intent.operation(),draft.id(),draft.world(),world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString(),clock.get());}
            case 8->{require(((ConstructionBlockJournal.Intent)result).state()==ConstructionBlockJournal.State.APPLIED,"Exact native output saved");index++;page();}
            case 5->{receipt=(CivBuildingCompletion.Receipt)result;require(index==draft.template().blockCount(),"Every authored block built");phase=6;pending=session.housingPage(null,8);}
            case 6->{var entries=((PopulationSession.HousingPage)result).layouts();require(entries.size()==1,"Completed home joins ordinary housing inspection");layout=entries.getFirst();require(layout.id().equals(CivHomeLayout.from(draft).id()),"Stable transformed home identity");dwelling=new PaperDwellingSurvey(layout,clock.get(),true);phase=7;pending=CompletableFuture.completedFuture(null);}
            case 7->{var observation=dwelling.advance(128,clock.get());if(observation.isEmpty())return;phase=9;pending=session.reconcileHousing(layout,observation.get(),DwellingInspection.Policy.prototype());}
            case 9->{var report=(DwellingHousing.Report)result;require(report.assigned()==1,"Native inspected home houses actual saved family: "+report);session.close();newSession();phase=10;pending=session.start(false,Set.of(draft.world()));}
            case 10->{phase=11;pending=session.completeCivHome(completionOperation,draft.id(),new CivBuildingCompletion.Evidence(frame.authority(),frame.cells()));}
            case 11->{require(receipt.equals(result),"Exact completion survives actual session restart");session.close();session=null;phase=12;pending=CompletableFuture.runAsync(()->{try{verify();}catch(Exception e){throw new CompletionException(e);}},owner);}
            case 12->{restore();require(restored,"All captured native cells restored");getLogger().info("P4_HOME_COMPLETION_PASS: real supplier payment and delayed cargo; earned unemployed builder labor; all "+index+" rotated/mirrored native blocks through guarded writer; exact retries and actual restart before first acknowledgement; village-owned completion releases site; real two-pass native dwelling inspection assigns one whole saved household and four beds; completion restart replay conserves money, goods, people and capacity; original cells restored. Ordinary automatic scheduling remains off.");phase=13;getServer().shutdown();}
            default->{}
        }
    }
    private void page(){phase=2;pending=session.constructionWorkPage(null,8);}
    private void newSession(){var p=PopulationSession.Settings.prototype();var config=new PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());
        session=new PopulationSession(folder.toFile(),getLogger(),config,clock::get,settings,FarmObservation.Policy.prototype());session.configureFarmBusinesses(FarmBusinessPolicy.prototype());session.configureConstruction(ConstructionBudgetPolicy.prototype(),new ConstructionLaborPolicy(true,1,1,100,25));writer=new PaperConstructionWriter(this,session);}
    private void seed()throws Exception{
        database=new DatabaseManager(folder.toFile(),getLogger());database.initialize();var c=database.getConnection();var town=draft.settlement();
        try(var s=c.prepareStatement("INSERT INTO settlements(id,name,world,center_x,center_y,center_z,discovered_at,population,beds) VALUES(?,'Private home fixture','test',0,64,0,1,0,0)")){s.setString(1,town.toString());s.executeUpdate();}
        database.getVillageIdentityRepository().observeWorld(draft.world(),"Private home fixture");try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,draft.world().toString());s.executeUpdate();}
        new ConstructionDraftRepository(c).save(draft);new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),clock.get(),true,true),clock.get());
        var people=database.getPopulationRepository();UUID ownerId=UUID.randomUUID(),builder=UUID.randomUUID();people.insertHousehold(new Household(family,town,"Private home beneficiary"));
        people.insertResident(new Resident(ownerId,"Private supplier",town,null,300,Resident.Status.ALIVE));people.insertResident(new Resident(builder,"Private builder",town,family,300,Resident.Status.ALIVE));
        try(var s=c.prepareStatement("INSERT INTO household_capacity VALUES(?,0)")){s.setString(1,family.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,1000,NULL,NULL)")){
            s.setString(1,draft.id().toString());s.setString(2,family.toString());s.setString(3,town.toString());s.setString(4,draft.world().toString());s.setString(5,draft.template().fingerprint());s.executeUpdate();}
        new TreasuryRepository(c).initialize(UUID.randomUUID(),town,100000,"Explicit disposable fixture money");supplier=UUID.randomUUID();
        var site=new BusinessSite(UUID.randomUUID(),town,draft.world(),0,64,0,10,"Explicit fixture workplace");var company=new Business(supplier,town,"Fixture warehouse",ownerId,ownerId,"warehouse",site.id(),2,0);var requirements=new JobRequirements(216,Map.of());
        database.getBusinessRepository().found(new BusinessFounding(company,site,requirements,requirements,500,"Explicit fixture opportunity"));database.getBusinessRepository().hire(UUID.randomUUID(),supplier,ownerId,0);
        database.getIndustryRepository().openStore(supplier,256,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L,new Commodity("wool"),1L),0);
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=50 WHERE business_id=?")){s.setString(1,supplier.toString());require(s.executeUpdate()==3,"Explicit finite fixture stock");}
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE),bands);
        long now=clock.get();var economy=new EconomyRuntimeRepository(c,settings,FarmObservation.Policy.prototype(),216);economy.observe(new FarmObservation(town,draft.world(),now,true,0),now);economy.startOnline(now,Set.of(draft.world()));economy.pulse(now+1000,false);
        var p=IndustryPolicy.prototype();var transport=new IndustryPolicy(p.depositBatchesPerObservedOre(),p.batchesPerWorker(),p.siteBatchCapacity(),p.storageCapacity(),1,128,p.miningBundle(),p.storageSpacePerUnit());
        var runtime=new IndustryRuntimePolicy(true,1000,8,transport,Map.of(),WorkshopSupplyPolicy.disabled(),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());
        var bought=new ConstructionVillageProcurement(c,ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),runtime,BusinessOperationPolicy.prototype()).purchaseRemaining(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),now+1000,true,true),now+1001,Set.of(draft.world()));require(bought.purchases().size()==3,"All real home material purchases");
        economy.pulse(now+2000,false);require(ConstructionSupplyOrders.deliveryOnly(c).deliverReady(Set.of(draft.world()),8)==3,"Real earned-period cargo arrival");
        var workRuntime=new EconomyRuntimeRepository(c,settings,FarmObservation.Policy.prototype(),216);workRuntime.configureFarmBusinesses(FarmBusinessPolicy.prototype());workRuntime.configureConstruction(new ConstructionLaborPolicy(true,1,1,100,25));workRuntime.startOnline(now+2000,Set.of(draft.world()));workRuntime.pulse(now+3000,false);require(new ConstructionPublicWork(c).work(draft.id(),0)==36,"Foundation labor earned");clock.set(now+3000);database.close();database=null;
    }
    private void verify()throws Exception{
        database=new DatabaseManager(folder.toFile(),getLogger());database.initialize();var c=database.getConnection();var entry=new BuildingRegistry(c).find(receipt.building()).orElseThrow();require(entry.villageOwned()&&entry.donor()==null,"Village origin has no player donor");
        require(new HousingProjects(c).find(draft.id()).orElseThrow().completed(),"Completed family binding");require(!new ConstructionSiteReservations(c).find(draft.id()).orElseThrow().active(),"Completed site released");
        var unit=layout.units().getFirst().id();var qualified=new DwellingOccupancy(c).unit(unit).orElseThrow();require(qualified.beds()==4&&qualified.issues().isEmpty()&&qualified.review().consent().equals(entry.latest().operation()),"Four actually inspected housing places saved");
        try(var s=c.prepareStatement("SELECT unit_id FROM dwelling_active_occupancy WHERE household_id=?")){s.setString(1,family.toString());try(var r=s.executeQuery()){require(r.next()&&r.getString(1).equals(unit.toString()),"Saved family occupies qualified unit");}}
        require(new TreasuryRepository(c).load(draft.settlement()).orElseThrow().hundredths()==96560,"Actual home price debited exactly once after the real 500-unit warehouse founding cost");var store=database.getIndustryRepository().store(supplier).orElseThrow();require(store.hundredths()==2940&&store.units().get(Commodity.STONE)==38&&store.units().get(Commodity.TIMBER)==14&&store.units().get(new Commodity("wool"))==38,"Finite goods and money conserved");
        try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM residents WHERE status='ALIVE'")){require(r.getInt(1)==2,"No fabricated residents");}
        try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM dwelling_bed_claims")){require(r.getInt(1)==4,"Exactly four claimed bed heads");}
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Healthy private database");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Valid foreign keys");}database.close();database=null;
    }
    private void restore(){if(!authorized||world==null||restored)return;for(var entry:originals.entrySet()){var p=entry.getKey();require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Cleanup must not load chunks");world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(entry.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();if(session!=null){session.close();session=null;}owner.execute(()->{if(database!=null){database.close();database=null;}});owner.shutdown();}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
