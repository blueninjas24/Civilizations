import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Actual paid paving only inside a marked empty host-temporary fixture. Restores every captured cell. */
public final class PaidRoadPavingProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();private final AtomicLong clock=new AtomicLong(System.currentTimeMillis());
    private final Map<Position,String> originals=new TreeMap<>();private final List<ConstructionDraft> drafts=new ArrayList<>();
    private World world;private Path folder;private DatabaseManager db;private PopulationSession session;private EconomySettings settings;private UUID seller;
    private PaperRoadWriter writer;private PaperRoadWorkSurvey survey;private PaperRoadWorkSurvey.Frame frame;private ConstructionWorkQueue.Work work;
    private PaperConstructionWorkSurvey homeSurvey;private ConstructionDraft connectionHome,connectionRoad;private PaperConstructionLibrary library;
    private PopulationSession.PreparedConstructionStep prepared;private CompletableFuture<?> pending;private ArmorStand obstruction;
    private int phase,ticks,renewals;private UUID operation;private boolean authorized,restored,afterWriteRestart,beforeWriteRestart,blockedScene,blockedEntity,firstComplete;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();for(var chunk:world.getLoadedChunks())world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");library=PaperConstructionLibrary.load(plugin);var recipe=library.templates().get("starter_path");
        var chunks=world.getLoadedChunks();require(chunks.length>0,"An initially loaded fixture chunk is required");var chunk=chunks[0];int x=(chunk.getX()<<4)+4,z=(chunk.getZ()<<4)+4;int y=world.getHighestBlockYAt(x,z);
        UUID town=UUID.randomUUID();
        for(int scenario=0;scenario<2;scenario++){
            int atX=x,atZ=z+scenario*6;var points=scenario==0?List.of(new Position(atX+2,y,atZ),new Position(atX+1,y,atZ),new Position(atX,y,atZ)):List.of(new Position(atX+1,y,atZ),new Position(atX,y,atZ));
            var source=recipe;if(scenario==0){var stage=recipe.stages().getFirst();var blocks=new TreeMap<Position,String>();stage.blocks().keySet().forEach(p->blocks.put(p,"minecraft:stone_bricks"));source=new ConstructionTemplate(recipe.id(),2,recipe.name(),recipe.attribution(),Purpose.PATH,recipe.size(),List.of(new Stage(stage.id(),stage.work(),stage.materials(),blocks)),recipe.anchors());}
            var design=LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,points,points.size()-1,3),source).orElseThrow();
            var draft=new ConstructionDraft(UUID.randomUUID(),town,world.getUID(),design.origin(),Transform.identity(),design.template(),clock.get());drafts.add(draft);
            for(var at:RoadGroundReview.requiredCells(design.geometry(),4)){
                require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"No fixture chunk may be loaded by inspection");var block=world.getBlockAt(at.x(),at.y(),at.z());
                require(at.y()==y?block.getType()==Material.GRASS_BLOCK:block.getType()==Material.AIR,"Flat empty fixture frame required");originals.put(at,block.getBlockData().getAsString());
            }
            var end=design.connection();world.getBlockAt(end.x(),end.y(),end.z()).setType(Material.DIRT_PATH,false);
        }
        folder=getDataFolder().toPath().resolve("private-road");require(!Files.exists(folder),"Fresh private fixture DB required");
        pending=CompletableFuture.runAsync(()->{try{seed();}catch(Exception e){throw new CompletionException(e);}},owner);
        new BukkitRunnable(){@Override public void run(){try{require(++ticks<1800,"Paid road timeout phase "+phase);if(pending.isDone())step();}catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_PAID_ROAD_FAIL",failure);restore();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_PAID_ROAD_SETUP_FAIL",failure);restore();if(authorized)getServer().shutdown();}}
    private void step()throws Exception{
        Object result=pending.join();
        switch(phase){
            case 0->{openSession();phase=1;pending=session.start(false,Set.of(world.getUID()));}
            case 1->{phase=2;pending=session.constructionWorkPage(null,8);}
            case 2->{var page=(ConstructionWorkQueue.Page)result;
                work=page.projects().stream().filter(w->w.draft().id().equals(drafts.get(firstComplete?1:0).id())).findFirst().orElseThrow();
                if(!firstComplete&&work.state()==ConstructionWorkQueue.State.COMPLETE){survey=new PaperRoadWorkSurvey(work,4,clock.get());phase=10;pending=CompletableFuture.completedFuture(null);return;}
                require(work.state()==ConstructionWorkQueue.State.READY||work.state()==ConstructionWorkQueue.State.RESUME,"Actual paid work ready: "+work);
                operation=work.pending().map(ConstructionBlockJournal.Intent::operation).orElseGet(UUID::randomUUID);survey=new PaperRoadWorkSurvey(work,4,clock.get());phase=3;pending=CompletableFuture.completedFuture(null);}
            case 3->{var next=survey.advance(128,clock.get());if(next.isEmpty())return;frame=next.get();require(frame.ready(),"Two-pass native road scene: "+frame.explanation());authorize();}
            case 4->{prepared=(PopulationSession.PreparedConstructionStep)result;
                if(firstComplete&&!beforeWriteRestart){beforeWriteRestart=true;restart();return;}
                if(!firstComplete&&!blockedScene){blockedScene=true;var at=frame.geometry().origin().plus(new Position(-3,1,3));var block=world.getBlockAt(at.x(),at.y(),at.z());String before=block.getBlockData().getAsString();block.setType(Material.OAK_FENCE,false);var held=writer.attempt(work,frame,prepared);require(held.state()==PaperConstructionStep.State.WAIT,"Changed clearance blocks paving");block.setBlockData(getServer().createBlockData(before),false);phase=5;pending=CompletableFuture.completedFuture(null);return;}
                phase=6;pending=CompletableFuture.completedFuture(null);}
            case 5->{if(!blockedEntity){blockedEntity=true;var at=prepared.intent().at();obstruction=world.spawn(new Location(world,at.x()+0.5,at.y()+1,at.z()+0.5),ArmorStand.class);var held=writer.attempt(work,frame,prepared);require(held.state()==PaperConstructionStep.State.WAIT,"Actual entity blocks paving");obstruction.remove();obstruction=null;}phase=6;pending=CompletableFuture.completedFuture(null);}
            case 6->{var outcome=writer.attempt(work,frame,prepared);
                if(firstComplete){require(outcome.state()==PaperConstructionStep.State.WAIT&&outcome.explanation().contains("ambiguous"),"Prepared original ground after restart must hold, never create replacement: "+outcome);var at=prepared.intent().at();require(world.getBlockAt(at.x(),at.y(),at.z()).getBlockData().getAsString().equals(prepared.intent().before()),"Ambiguous ground remains unchanged");session.close();session=null;phase=8;pending=CompletableFuture.runAsync(()->{try{verify();}catch(Exception e){throw new CompletionException(e);}},owner);return;}
                if(outcome.state()==PaperConstructionStep.State.WAIT){require(++renewals<=3,"Native owner permit must settle: "+outcome);authorize();return;}renewals=0;
                require(outcome.state()==PaperConstructionStep.State.APPLIED||outcome.state()==PaperConstructionStep.State.ALREADY_PRESENT,"Exact paid road result: "+outcome);
                if(!afterWriteRestart){afterWriteRestart=true;require(outcome.state()==PaperConstructionStep.State.APPLIED,"First cell actually paved");restart();return;}
                phase=7;pending=session.acknowledgePublicConstructionStep(operation,work.draft().id(),world.getUID(),prepared.intent().after(),clock.get());}
            case 7->{require(((ConstructionBlockJournal.Intent)result).state()==ConstructionBlockJournal.State.APPLIED,"Road acknowledgement durable");phase=2;pending=session.constructionWorkPage(null,8);}
            case 8->{restore();getLogger().info("P4_PAID_ROAD_PASS: two paid stone-brick cells replace exact natural ground under current owner gate; configured4-cell clearance; changed neighbor/entity holds; actual restart after write acknowledges without duplicate; completed road origin/cells/site release survive another restart; second paid road prepared-before-write restart holds ambiguous ground; real supplier stock/cash,3 earned work units and118 FK integrity conserved; original cells restored.");phase=9;getServer().shutdown();}
            case 10->{var next=survey.advance(128,clock.get());if(next.isEmpty())return;frame=next.get();require(frame.ready(),"Completed road still matches its full native scene");phase=11;pending=session.completeCivRoad(UUID.randomUUID(),work.draft().id(),frame.observation(),frame.buffer());}
            case 11->{var receipt=(CivRoadCompletion.Receipt)result;require(receipt.draft().equals(drafts.getFirst().id()),"Permanent road receipt belongs to original draft");firstComplete=true;
                var anchor=drafts.getFirst().origin();connectionHome=new ConstructionDraft(UUID.randomUUID(),receipt.village(),receipt.world(),anchor.plus(new Position(-2,1,1)),Transform.identity(),library.templates().get("starter_home"),clock.get());
                var cells=ConstructionPreview.requiredCells(connectionHome.template(),connectionHome.transform(),connectionHome.origin());
                var min=new Position(cells.stream().mapToInt(Position::x).min().orElseThrow(),cells.stream().mapToInt(Position::y).min().orElseThrow(),cells.stream().mapToInt(Position::z).min().orElseThrow());var max=new Position(cells.stream().mapToInt(Position::x).max().orElseThrow(),cells.stream().mapToInt(Position::y).max().orElseThrow(),cells.stream().mapToInt(Position::z).max().orElseThrow());
                phase=12;pending=session.constructionRoadStates(receipt.world(),new ConstructionPreview.Bounds(min,max));}
            case 12->{@SuppressWarnings("unchecked") var saved=(Map<Position,String>)result;require(saved.size()>=1,"Read-only SQL road origin available to connector survey");homeSurvey=new PaperConstructionWorkSurvey(new ConstructionWorkQueue.Work(connectionHome,ConstructionWorkQueue.State.READY,0,Optional.empty(),"Read-only connector proof; no saved home/project/rights",saved),clock.get());phase=13;pending=CompletableFuture.completedFuture(null);}
            case 13->{var next=homeSurvey.advance(128,clock.get());if(next.isEmpty())return;require(next.get().authority().siteCleared(),"Actual stone-brick paving supports read-only home connector: "+next.get().explanation());homeSurvey=new PaperConstructionWorkSurvey(new ConstructionWorkQueue.Work(connectionHome,ConstructionWorkQueue.State.READY,0,Optional.empty(),"No road provenance; must refuse player-like stone"),clock.get());phase=14;}
            case 14->{var next=homeSurvey.advance(128,clock.get());if(next.isEmpty())return;require(!next.get().authority().siteCleared(),"Same physical paving without saved origin grants no home access");getLogger().info("P4_CIV_ROAD_HOME_CONNECTOR_PASS: actual paved stone-brick connector accepted only with SQL road origin; same scene without origin refused; read-only, no house or capacity created.");
                var end=drafts.getFirst().origin().plus(new Position(1,0,0));var design=LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,List.of(end.plus(new Position(0,0,1)),end),1,2),library.templates().get("starter_path")).orElseThrow();
                connectionRoad=new ConstructionDraft(UUID.randomUUID(),drafts.getFirst().settlement(),world.getUID(),design.origin(),Transform.identity(),design.template(),clock.get());var cells=RoadGroundReview.requiredCells(design.geometry(),4);
                var min=new Position(cells.stream().mapToInt(Position::x).min().orElseThrow(),cells.stream().mapToInt(Position::y).min().orElseThrow(),cells.stream().mapToInt(Position::z).min().orElseThrow());var max=new Position(cells.stream().mapToInt(Position::x).max().orElseThrow(),cells.stream().mapToInt(Position::y).max().orElseThrow(),cells.stream().mapToInt(Position::z).max().orElseThrow());
                phase=15;pending=session.constructionRoadStates(world.getUID(),new ConstructionPreview.Bounds(min,max));}
            case 15->{@SuppressWarnings("unchecked") var saved=(Map<Position,String>)result;require(saved.size()==2,"Both original paved cells keep SQL provenance");survey=new PaperRoadWorkSurvey(new ConstructionWorkQueue.Work(connectionRoad,ConstructionWorkQueue.State.READY,0,Optional.empty(),"Read-only extension evidence, no project/payment/intent",saved),4,clock.get());phase=16;pending=CompletableFuture.completedFuture(null);}
            case 16->{var next=survey.advance(128,clock.get());if(next.isEmpty())return;require(next.get().ready(),"Actual road endpoint and buffered paving match saved origin: "+next.get().explanation());survey=new PaperRoadWorkSurvey(new ConstructionWorkQueue.Work(connectionRoad,ConstructionWorkQueue.State.READY,0,Optional.empty(),"No source origin; same paving must refuse"),4,clock.get());phase=17;}
            case 17->{var next=survey.advance(128,clock.get());if(next.isEmpty())return;require(!next.get().ready(),"Unregistered stone endpoint/neighbor grants no road extension");getLogger().info("P4_CIV_ROAD_EXTENSION_PASS: registered actual stone-brick endpoint and nearby paving accepted in exact buffered road scene; same scene without origin refused; no extension blocks or authority created.");restart();}
        }
    }
    private void authorize(){phase=4;var placements=work.draft().template().stages().getFirst().blocks().values().stream().toList();pending=session.prepareAuthorizedRoadStep(operation,work.draft().id(),work.nextOrdinal(),placements.get(work.nextOrdinal()),frame.geometry(),frame.observation(),frame.buffer());}
    private void restart(){session.close();openSession();phase=1;pending=session.start(false,Set.of(world.getUID()));}
    private void openSession(){var p=PopulationSession.Settings.prototype();var config=new PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment());session=new PopulationSession(folder.toFile(),getLogger(),config,clock::get,settings,FarmObservation.Policy.prototype());session.configureFarmBusinesses(FarmBusinessPolicy.prototype());session.configureConstruction(ConstructionBudgetPolicy.prototype(),new ConstructionLaborPolicy(true,1,1,100,25));writer=new PaperRoadWriter(this,session);}
    private void seed()throws Exception{
        db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();var c=db.getConnection();UUID worldId=drafts.getFirst().world();UUID town=drafts.getFirst().settlement(),ownerId=UUID.randomUUID(),builder=UUID.randomUUID();
        try(var s=c.prepareStatement("INSERT INTO settlements VALUES(?,'Private paid road fixture','fixture',0,64,0,1,0,0)")){s.setString(1,town.toString());s.executeUpdate();}
        db.getVillageIdentityRepository().observeWorld(worldId,"Private road fixture");try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,worldId.toString());s.executeUpdate();}
        for(var d:drafts){new ConstructionDraftRepository(c).save(d);new ConstructionSiteReservations(c).reserve(d.id(),new ConstructionMaterials.Authority(d.id(),d.world(),clock.get(),true,true),clock.get());}
        db.getPopulationRepository().insertResident(new Resident(ownerId,"Explicit fixture supplier",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().insertResident(new Resident(builder,"Explicit fixture builder",town,null,300,Resident.Status.ALIVE));
        new TreasuryRepository(c).initialize(UUID.randomUUID(),town,100000,"Explicit disposable fixture money");seller=UUID.randomUUID();var site=new BusinessSite(UUID.randomUUID(),town,worldId,0,64,0,10,"Explicit fixture supplier site");var company=new Business(seller,town,"Fixture warehouse",ownerId,ownerId,"warehouse",site.id(),2,0);var requirements=new JobRequirements(216,Map.of());db.getBusinessRepository().found(new BusinessFounding(company,site,requirements,requirements,500,"Explicit fixture startup"));db.getBusinessRepository().hire(UUID.randomUUID(),seller,ownerId,0);db.getIndustryRepository().openStore(seller,256,Map.of(Commodity.STONE,1L),0);
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=20 WHERE business_id=?")){s.setString(1,seller.toString());require(s.executeUpdate()==1,"Explicit finite fixture stock");}
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var band:CommodityAccounting.Condition.values())bands.put(band,100);
        settings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE),bands);
        long now=clock.get();var economy=new EconomyRuntimeRepository(c,settings,FarmObservation.Policy.prototype(),216);economy.observe(new FarmObservation(town,worldId,now,true,0),now);economy.startOnline(now,Set.of(worldId));economy.pulse(now+1000,false);
        var p=IndustryPolicy.prototype();var transport=new IndustryPolicy(p.depositBatchesPerObservedOre(),p.batchesPerWorker(),p.siteBatchCapacity(),p.storageCapacity(),1,128,p.miningBundle(),p.storageSpacePerUnit());var runtime=new IndustryRuntimePolicy(true,1000,8,transport,Map.of(),WorkshopSupplyPolicy.disabled(),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());var orders=new ConstructionSupplyOrders(c,ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),runtime,BusinessOperationPolicy.prototype());
        for(var d:drafts)orders.purchase(new ConstructionSupplyOrders.Request(UUID.randomUUID(),d.id(),seller,ConstructionBudgetPolicy.Source.VILLAGE,null,Commodity.STONE,d.template().materials().get(Commodity.STONE),1),new ConstructionMaterials.Authority(d.id(),d.world(),now+1000,true,true),now+1000,Set.of(worldId));
        economy.pulse(now+2000,false);require(ConstructionSupplyOrders.deliveryOnly(c).deliverReady(Set.of(worldId),8)==2,"Two actual paid deliveries");
        var work=new EconomyRuntimeRepository(c,settings,FarmObservation.Policy.prototype(),216);work.configureFarmBusinesses(FarmBusinessPolicy.prototype());work.configureConstruction(new ConstructionLaborPolicy(true,1,1,100,25));work.startOnline(now+2000,Set.of(worldId));work.pulse(now+3000,false);work.pulse(now+4000,false);
        for(var d:drafts)require(new ConstructionPublicWork(c).work(d.id(),0)==d.template().work(),"Real eligible labor earned for each road");clock.set(now+4000);db.close();db=null;
    }
    private void verify()throws Exception{
        db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();var c=db.getConnection();require(new TreasuryRepository(c).load(drafts.getFirst().settlement()).orElseThrow().hundredths()==99450,"No duplicate public spending after500 startup/50 actual supplies");var store=db.getIndustryRepository().store(seller).orElseThrow();require(store.hundredths()==50&&store.units().get(Commodity.STONE)==18,"Finite paid supplier goods and funds conserved");
        for(String sql:List.of("SELECT count(*)=3 FROM construction_road_ground","SELECT count(*)=2 FROM construction_block_journal WHERE state='APPLIED'","SELECT count(*)=1 FROM construction_block_journal WHERE state='PREPARED'","SELECT sum(effort)=3 FROM construction_public_work","SELECT count(*)=2 FROM residents"))try(var s=c.createStatement();var r=s.executeQuery(sql)){require(r.getInt(1)==1,"Saved real history: "+sql);}
        var roads=new CivRoadCompletion(c);require(roads.findDraft(drafts.getFirst().id()).isPresent(),"Completed road origin survives restart");require(!new ConstructionSiteReservations(c).find(drafts.getFirst().id()).orElseThrow().active(),"Completed road frees only its temporary site");
        var first=drafts.getFirst();var origin=first.origin();var bounds=ConstructionPreview.footprint(first.template(),first.transform(),origin);require(roads.expectedStates(first.world(),bounds).size()==2,"Exactly two paid paving cells retain permanent origin");
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Healthy118 private save");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Valid118 foreign keys");}db.close();db=null;
    }
    private void restore(){if(!authorized||restored||world==null)return;if(obstruction!=null){obstruction.remove();obstruction=null;}for(var e:originals.entrySet()){var at=e.getKey();require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(at.x(),at.y(),at.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){restore();if(session!=null){session.close();session=null;}owner.execute(()->{if(db!=null){db.close();db=null;}});owner.shutdown();}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
