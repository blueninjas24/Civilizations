import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Marked, read-only Paper quarry observer plus private-SQLite founding/production proof. Never ship. */
public final class AutomaticQuarryProbe extends JavaPlugin {
    private final AtomicLong clock=new AtomicLong(1000);
    private final List<OreSurveyArea.Chunk> tickets=new ArrayList<>();
    private final ExecutorService sqlWorker=Executors.newSingleThreadExecutor();
    private PopulationSession session;
    private PaperOreObserver observer;
    private World world;
    private UUID worldId;
    private Path directory;
    private boolean authorized;
    private int chunkX,chunkZ,minimumY,maximumY,cursor,found,stage,ticks;
    private OreScan scan;
    private final List<VillageObservation.Position> actualStone=new ArrayList<>(),actualSand=new ArrayList<>();
    private CompletableFuture<Void> startup;
    private CompletableFuture<Reviews> reviews;
    private CompletableFuture<Void> sqlProof;
    private UUID town;
    private int candidateCount;

    private record Reviews(MineralOpportunities.Review quarries,MineralOpportunities.Review minerals) {}
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    private boolean air(Material material){return material==Material.AIR||material==Material.CAVE_AIR||material==Material.VOID_AIR;}
    private List<VillageObservation.Position> sorted(List<VillageObservation.Position> positions) {
        return positions.stream().sorted(Comparator.comparingInt(VillageObservation.Position::x)
                .thenComparingInt(VillageObservation.Position::y).thenComparingInt(VillageObservation.Position::z)).toList();
    }

    @Override public void onEnable(){
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim()
                    .equals("Disposable Civilizations Phase 1 test environment")
                    &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578
                    &&getServer().getOnlinePlayers().isEmpty(),"Marked localhost fixture with zero players only");
            authorized=true;world=getServer().getWorlds().getFirst();worldId=world.getUID();
            var spawn=world.getSpawnLocation();int homeX=spawn.getBlockX(),homeY=spawn.getBlockY(),homeZ=spawn.getBlockZ();
            chunkX=Math.floorDiv(homeX,16);chunkZ=Math.floorDiv(homeZ,16);
            var offsets=List.of(new OreSurveyArea.Chunk(chunkX,chunkZ),new OreSurveyArea.Chunk(chunkX-1,chunkZ),
                    new OreSurveyArea.Chunk(chunkX+1,chunkZ),new OreSurveyArea.Chunk(chunkX,chunkZ-1),
                    new OreSurveyArea.Chunk(chunkX,chunkZ+1));
            for(var chunk:offsets) {
                if(world.addPluginChunkTicket(chunk.x(),chunk.z(),this))tickets.add(chunk);
                check(world.isChunkLoaded(chunk.x(),chunk.z()),"All five marked fixture chunks must be available");
            }
            minimumY=Math.max(world.getMinHeight(),homeY-16);maximumY=Math.min(world.getMaxHeight(),homeY+17);
            check(minimumY<maximumY,"Spawn is inside the world height");
            int[] dx={0,-1,1,0,0},dz={0,0,0,-1,1};VillageObservation.Position natural=null;
            search: for(int ci=0;ci<dx.length&&natural==null;ci++)for(int y=minimumY;y<maximumY&&natural==null;y++)
                for(int z=0;z<16&&natural==null;z++)for(int x=0;x<16;x++) {
                    int wx=(chunkX+dx[ci])*16+x,wz=(chunkZ+dz[ci])*16+z;Material m=world.getBlockAt(wx,y,wz).getType();
                    if(m!=Material.STONE&&m!=Material.DEEPSLATE&&m!=Material.SAND&&m!=Material.RED_SAND)continue;
                    for(var face:new int[][]{{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}})
                        if(world.isChunkLoaded(Math.floorDiv(wx+face[0],16),Math.floorDiv(wz+face[2],16))
                                &&air(world.getBlockAt(wx+face[0],y+face[1],wz+face[2]).getType())) {
                            natural=new VillageObservation.Position(wx,y,wz);break;
                        }
                }
            check(natural!=null,"Five already-loaded terrain slices need a natural exposed stone or sand block");
            for(var chunk:List.copyOf(tickets))world.removePluginChunkTicket(chunk.x(),chunk.z(),this);tickets.clear();
            chunkX=Math.floorDiv(natural.x(),16);chunkZ=Math.floorDiv(natural.z(),16);homeX=natural.x();homeY=natural.y()+1;homeZ=natural.z();
            minimumY=Math.max(world.getMinHeight(),homeY-2);maximumY=Math.min(world.getMaxHeight(),homeY+3);
            offsets=List.of(new OreSurveyArea.Chunk(chunkX,chunkZ),new OreSurveyArea.Chunk(chunkX-1,chunkZ),
                    new OreSurveyArea.Chunk(chunkX+1,chunkZ),new OreSurveyArea.Chunk(chunkX,chunkZ-1),
                    new OreSurveyArea.Chunk(chunkX,chunkZ+1));
            var snapshots=new HashMap<OreSurveyArea.Chunk,ChunkSnapshot>();
            for(var chunk:offsets) {
                if(world.addPluginChunkTicket(chunk.x(),chunk.z(),this))tickets.add(chunk);
                check(world.isChunkLoaded(chunk.x(),chunk.z()),"All five target-centered fixture chunks must be available");
                snapshots.put(chunk,world.getChunkAt(chunk.x(),chunk.z()).getChunkSnapshot(false,false,false,false));
            }
            check(!air(world.getBlockAt(natural.x(),natural.y(),natural.z()).getType()),"Natural quarry target remains present");
            scan=OreScan.quarries(PaperOreNeighborhood.quarries(snapshots,chunkX,chunkZ,world.getMinHeight(),world.getMaxHeight()),
                    chunkX,chunkZ,minimumY,maximumY,Set.of());

            var home=new VillageObservation.Position(homeX,homeY,homeZ);
            var secondHome=new VillageObservation.Position(homeX+2,homeY,homeZ);
            var observation=new VillageObservation(worldId,"Quarry observer fixture",1000,true,List.of(home,secondHome),
                    List.of(new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture A"),
                            new VillageObservation.Person(UUID.randomUUID(),home,true,"Fixture B")));
            directory=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());
            session=new PopulationSession(directory.toFile(),getLogger(),PopulationSession.Settings.prototype(),clock::get);
            session.configureOreTracking();
            startup=session.start(false,Set.of(worldId)).thenCompose(ignored->session.observe(observation)).thenApply(ignored->null);

            new BukkitRunnable(){
                @Override public void run(){try {
                    check(++ticks<=1200,"Automatic quarry probe timed out at stage "+stage);
                    if(stage==0) {
                        if(!startup.isDone())return;startup.join();
                        check(scan.advance(128)<=128,"Bounded immutable snapshot scan required");
                        if(!scan.complete())return;
                        stage=1;return;
                    }
                    if(stage==1) {
                        int used=0,total=256*(maximumY-minimumY);
                        while(cursor<total&&used++<128) {
                            int column=cursor%256,y=minimumY+cursor/256,x=column&15,z=column>>4;cursor++;
                            var material=world.getBlockAt(chunkX*16+x,y,chunkZ*16+z).getType();
                            boolean stone=material==Material.STONE||material==Material.DEEPSLATE;
                            boolean sand=material==Material.SAND||material==Material.RED_SAND;
                            if(!stone&&!sand)continue;
                            boolean exposed=false;
                            for(var face:new int[][]{{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}})
                                exposed|=air(world.getBlockAt(chunkX*16+x+face[0],y+face[1],chunkZ*16+z+face[2]).getType());
                            if(exposed){if(found++<512)(stone?actualStone:actualSand).add(new VillageObservation.Position(chunkX*16+x,y,chunkZ*16+z));}
                        }
                        if(cursor<total)return;
                        var snapshotEvidence=scan.observations(worldId,1000);
                        check(found>0&&found<=512,"Fixture needs at least one bounded exposed stone/sand coordinate");
                        check(snapshotEvidence.getFirst().complete()&&snapshotEvidence.getLast().complete(),"Five loaded chunks must yield a complete quarry snapshot");
                        check(snapshotEvidence.getFirst().ore().equals(sorted(actualStone))
                                &&snapshotEvidence.getLast().ore().equals(sorted(actualSand)),
                                "Independent live blocks must match centre/cardinal snapshot quarry evidence");
                        observer=PaperOreObserver.quarries(session,new OreObserverPolicy(true,2,256,20,0),clock::get);
                        observer.runTaskTimer(AutomaticQuarryProbe.this,1,1);stage=2;return;
                    }
                    if(stage==2) {
                        if(!observer.diagnostic().contains("Ore survey: "))return;
                        observer.cancel();observer=null;
                        town=session.settlementNames().keySet().stream().findFirst().orElseThrow();
                        check(session.settlementNames().size()==1,"Fixture has exactly one saved settlement");
                        clock.set(1010);
                        var coalIron=List.of(new MineralObservation(worldId,1010,true,Commodity.COAL,List.of()),
                                new MineralObservation(worldId,1010,true,Commodity.IRON,List.of()));
                        reviews=session.surveyMinerals(town,coalIron).thenCompose(ignored->{
                            var quarryView=session.currentQuarryOpportunities(town,60_000);
                            var mineralView=session.currentMineralOpportunities(town,60_000);
                            return quarryView.thenCombine(mineralView,Reviews::new);
                        });
                        stage=3;return;
                    }
                    if(stage==3) {
                        if(!reviews.isDone())return;var saved=reviews.join();
                        check(saved.quarries().status()==MineralOpportunities.Status.CURRENT
                                &&saved.quarries().capturedMillis()==1000,"Ordinary quarry observer must save a current frame on the private SQL owner");
                        check(saved.minerals().status()==MineralOpportunities.Status.CURRENT
                                &&saved.minerals().capturedMillis()==1010,"Later coal/iron frame must remain independent");
                        candidateCount=saved.quarries().candidates().size();
                        check(candidateCount>0,"Actual quarry observation must expose a viable current coordinate");
                        releaseTickets();session.close();session=null;
                        sqlProof=CompletableFuture.runAsync(()->foundAndProduce(saved.quarries()),sqlWorker);
                        stage=4;return;
                    }
                    if(stage==4) {
                        if(!sqlProof.isDone())return;sqlProof.join();
                        getLogger().info("NATIVE AUTOMATIC QUARRY PASS: ordinary Paper quarry observer matched independent read-only terrain; private SQLite kept quarry and coal/iron captures independent; actual saved candidate funded one quarry/pit; timed industry runtime sold paid quarry cargo to the warehouse, delivered paid inputs to a staffed customer, and produced customer goods; total cash conserved after startup sink; absent-world review preserved assets; no block edits");
                        cancel();getServer().getPluginManager().disablePlugin(AutomaticQuarryProbe.this);getServer().shutdown();
                    }
                }catch(Exception failure){
                    cancel();getLogger().log(java.util.logging.Level.SEVERE,"NATIVE AUTOMATIC QUARRY FAIL",failure);
                    getServer().getPluginManager().disablePlugin(AutomaticQuarryProbe.this);if(authorized)getServer().shutdown();
                }}
            }.runTaskTimer(this,1,1);
        }catch(Exception failure){
            getLogger().log(java.util.logging.Level.SEVERE,"Automatic quarry probe refused",failure);
            getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();
        }
    }

    private void foundAndProduce(MineralOpportunities.Review survey) {
        DatabaseManager db=null;
        try {
            db=new DatabaseManager(directory.toFile(),getLogger());db.initialize();
            var mineral=survey.candidates().stream().map(MineralOpportunities.Evidence::mineral)
                    .filter(good->good.equals(Commodity.STONE)||good.equals(Commodity.SAND)).findFirst().orElseThrow();
            var input=new Commodity(mineral.id());String kind=mineral.equals(Commodity.STONE)?"quarry":"sandpit";
            var ids=List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
            var owner=ids.get(0);var shopWorker=ids.get(1);var warehouseWorker=ids.get(2);var founder=ids.get(3);var other=ids.get(4);
            for(var id:ids)db.getPopulationRepository().insertResident(new Resident(id,"Private fixture resident",town,null,300,Resident.Status.ALIVE));
            db.getPopulationRepository().recordSkillExperience(founder,"mining",10);
            db.getPopulationRepository().recordSkillExperience(other,"mining",10);
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100_000,"Isolated quarry probe funds");

            var bands=new EnumMap<Condition,Integer>(Condition.class);for(var condition:Condition.values())bands.put(condition,100);
            var settings=new EconomySettings(1,3,2,2,1,4,100,new Policy(5000,12500,20000,3),
                    Map.of(Commodity.FOOD,100L,Commodity.STONE,20L,Commodity.TIMBER,20L),
                    Map.of(Commodity.FOOD,BigDecimal.ONE,Commodity.STONE,BigDecimal.ONE,Commodity.TIMBER,BigDecimal.ONE),bands);
            var economy=new EconomyRuntimeRepository(db.getConnection(),settings,FarmObservation.Policy.prototype(),216,
                    VillagePriorities.Policy.prototype(),PopulationSession.Settings.prototype().seed());
            economy.observe(new FarmObservation(town,worldId,1000,true,0),1000);
            economy.startOnline(1000,Set.of(worldId));economy.pulse(2000,false);

            var sizes=new TreeMap<Commodity,Long>();sizes.put(input,1L);sizes.put(new Commodity("cut_material"),1L);sizes.put(Commodity.COAL,1L);
            sizes.put(Commodity.STONE,1L);sizes.put(Commodity.SAND,1L);sizes.put(ToolUpkeepPolicy.TOOLS,1L);
            business(db,UUID.randomUUID(),town,worldId,owner,shopWorker,"craftsman",100,20_000,sizes);
            business(db,UUID.randomUUID(),town,worldId,owner,warehouseWorker,"warehouse",102,10_000,sizes);

            var recipe=new ProductionRecipe("probe_cutting","craftsman",Map.of(input,2L),
                    Map.of(new Commodity("cut_material"),1L),1,4);
            var defaults=IndustryPolicy.prototype();
            var mining=new IndustryPolicy(defaults.depositBatchesPerObservedOre(),defaults.batchesPerWorker(),defaults.siteBatchCapacity(),
                    defaults.storageCapacity(),defaults.localDeliveryPeriods(),defaults.localHaulUnitsPerPeriod(),defaults.miningBundle(),sizes);
            var industry=new IndustryRuntimePolicy(true,1000,8,mining,Map.of(recipe.id(),recipe),
                    new WorkshopSupplyPolicy(true,1,16,Map.of(input,100L)),new WarehouseSupplyPolicy(true,16,16));
            var operations=BusinessOperationPolicy.prototype();
            var demand=new LocalIndustryDemand(db.getConnection(),industry,operations).review(town,Set.of(worldId),16);
            check(demand.complete()&&demand.uncovered().getOrDefault(input,0L)>0,
                    "Funded staffed customer must have real uncovered quarry input demand");
            check(demand.customers().stream().anyMatch(customer->customer.estimatedCost()>0),
                    "Customer demand must have a real payable input estimate");

            var founding=new AutomaticQuarryFounding(db.getConnection(),QuarryFoundingPolicy.prototype(),QuarryPolicy.prototype(),industry,
                    operations,StartupGrantPolicy.prototype(),216,42,()->true);
            var started=founding.review(town,1,2000,Set.of(worldId));
            check(started.startsWith(mineral.equals(Commodity.STONE)?"Founded one stone quarry":"Founded one sandpit"),started);
            var firms=scalar(db,"SELECT COUNT(*) FROM businesses WHERE industry='"+kind+"'");check(firms==1,"One actual funded extraction firm expected");
            check(scalar(db,"SELECT COUNT(*) FROM startup_grants")==1&&cash(db,town)==67_500,
                    "Real startup grant and capital must debit the private village treasury");
            var business=uuid(db,"SELECT id FROM businesses WHERE industry='"+kind+"'");
            var deposit=uuid(db,"SELECT id FROM mineral_deposits WHERE site_id=(SELECT site_id FROM businesses WHERE id='"+business+"')");
            var store=db.getIndustryRepository().store(business).orElseThrow();
            check(store.hundredths()==2_000&&store.units().values().stream().allMatch(units->units==0),
                    "Founded workplace must own paid startup capital and initially empty stock");
            long remaining=scalar(db,"SELECT remaining_batches FROM mineral_deposits WHERE id='"+deposit+"'");
            var reviewAbsent=founding.review(town,1,2001,Set.of());
            check(reviewAbsent.toLowerCase(Locale.ROOT).contains("absent"),"Automatic quarry founding must pause while the resource world is absent");
            check(scalar(db,"SELECT COUNT(*) FROM businesses WHERE industry='"+kind+"'")==1
                    &&cash(db,town)==67_500&&scalar(db,"SELECT COUNT(*) FROM startup_grants")==1
                    &&scalar(db,"SELECT remaining_batches FROM mineral_deposits WHERE id='"+deposit+"'")==remaining,
                    "Absent-world review must preserve all paid startup assets and finite reserves");

            var runtime=new IndustryRuntime(db.getConnection(),industry,operations);
            runtime.configureQuarries(QuarryPolicy.prototype());runtime.start(4000);
            for(long now=5000;now<=9000;now+=1000)runtime.pulse(now,Set.of(worldId));
            check(scalar(db,"SELECT COUNT(*) FROM industry_production WHERE business_id='"+business+"'")==5,
                    "Timed quarry runtime must produce through ordinary paid work periods");
            check(scalar(db,"SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+business+"' AND buyer_id=(SELECT id FROM businesses WHERE industry='warehouse' AND settlement_id='"+town+"') AND price>0 AND delivered_period IS NOT NULL")>0,
                    "Warehouse must purchase and receive actual quarry cargo");
            check(scalar(db,"SELECT COUNT(*) FROM local_shipments WHERE buyer_id=(SELECT id FROM businesses WHERE industry='craftsman' AND settlement_id='"+town+"') AND seller_id=(SELECT id FROM businesses WHERE industry='warehouse' AND settlement_id='"+town+"') AND price>0 AND delivered_period IS NOT NULL")>0,
                    "Customer must pay for and receive warehouse cargo");
            check(scalar(db,"SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE business_id=(SELECT id FROM businesses WHERE industry='craftsman' AND settlement_id='"+town+"') AND commodity='cut_material'")>0,
                    "Actual customer production must turn delivered quarry input into output");
            long totalCash=cash(db,town)+scalar(db,"SELECT COALESCE(SUM(hundredths),0) FROM industry_stores");
            check(totalCash==99_500,"Paid quarry founding and local cargo conserve cash after the 500 startup sink: "+totalCash);
            check(scalar(db,"SELECT COUNT(*) FROM startup_grants")==1&&cash(db,town)==67_500,
                    "Timed local purchases must not repeat or alter founding grants");
        }catch(Exception failure){throw new CompletionException(failure);}
        finally{if(db!=null)db.close();}
    }

    private void business(DatabaseManager db,UUID id,UUID town,UUID world,UUID owner,UUID worker,
                          String industry,int x,long cash,Map<Commodity,Long> sizes)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,64,0,1000,"Explicit private quarry probe customer/workplace");
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,industry,owner,owner,industry,site.id(),1,0),
                site,requirements,requirements,0,"Private test fixture"));
        db.getIndustryRepository().openStore(id,1000,sizes,cash);
        db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);
    }
    private long scalar(DatabaseManager db,String query)throws Exception {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){check(r.next(),"Expected SQL scalar");return r.getLong(1);}
    }
    private UUID uuid(DatabaseManager db,String query)throws Exception {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){check(r.next(),"Expected SQL identity");return UUID.fromString(r.getString(1));}
    }
    private long cash(DatabaseManager db,UUID town)throws Exception {
        return new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
    }
    private void releaseTickets(){if(world==null)return;for(var chunk:tickets)world.removePluginChunkTicket(chunk.x(),chunk.z(),this);tickets.clear();}
    @Override public void onDisable(){
        if(observer!=null)observer.cancel();if(session!=null)session.close();releaseTickets();sqlWorker.shutdown();
    }
}
