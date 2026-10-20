import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicitly bounded paid public path: actual supplier goods, earned labor, interrupted ack and native conflicts. */
public final class ConstructionPlacementProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();
    private final Map<Position,String> before=new LinkedHashMap<>();
    private final Set<Long> tickets=new HashSet<>();
    private DatabaseManager database;private Path folder;private World world;private ConstructionDraft draft;
    private PaperConstructionStep writer;private CompletableFuture<?> pending;private int phase,index,ticks;
    private ConstructionBlockJournal.Intent last;private boolean authorized,restored;
    private record Far(ConstructionDraft draft,ConstructionBlockJournal.Intent intent){}
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Private disposable fixture required");
            authorized=true;world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();
            int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
            for(int dx=0;dx<5;dx++){
                int cx=(x+dx)>>4,cz=z>>4;require(world.isChunkLoaded(cx,cz),"Native target must already be loaded");
                // Test-only hold: the writer itself must never acquire tickets or load chunks.
                long key=((long)cx<<32)^(cz&0xffffffffL);if(tickets.add(key))world.addPluginChunkTicket(cx,cz,this);
            }
            int y=world.getHighestBlockYAt(x,z)+1;var origin=new Position(x,y,z);
            var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");require(plugin!=null&&plugin.isEnabled(),"Production plugin active");
            var template=PaperConstructionLibrary.load(plugin).templates().get("starter_path");
            draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),origin,Transform.identity(),template,1000);
            for(var p:ConstructionPreview.requiredCells(template,Transform.identity(),origin)) {
                int cx=p.x()>>4,cz=p.z()>>4;require(world.isChunkLoaded(cx,cz),"All required test cells must already be loaded");
                long key=((long)cx<<32)^(cz&0xffffffffL);if(tickets.add(key))world.addPluginChunkTicket(cx,cz,this);
            }
            var bounds=ConstructionPreview.footprint(template,Transform.identity(),origin);
            writer=PaperConstructionStep.development(this,draft,new ApprovedArea(UUID.randomUUID(),world.getUID(),bounds));
            for(int dx=0;dx<5;dx++) {
                var p=origin.plus(new Position(dx,0,0));var state=world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString();
                require(state.equals("minecraft:air"),"Test must not replace existing blocks");before.put(p,state);
            }
            folder=getDataFolder().toPath().resolve("private-journal");require(!Files.exists(folder),"Fresh private journal required");
            pending=CompletableFuture.runAsync(()->{try{
                reopen();var c=database.getConnection();
                try(var s=c.prepareStatement("INSERT INTO settlements(id,name,world,center_x,center_y,center_z,discovered_at,population,beds) VALUES(?,'Private construction fixture','test',0,64,0,1,0,0)")){s.setString(1,draft.settlement().toString());s.executeUpdate();}
                database.getVillageIdentityRepository().observeWorld(draft.world(),"Private construction fixture");
                try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,draft.settlement().toString());s.setString(2,draft.world().toString());s.executeUpdate();}
                new ConstructionDraftRepository(c).save(draft);
                new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),System.currentTimeMillis(),true,true),System.currentTimeMillis());
                fundAndEarnPath();
            }catch(Exception failed){throw new CompletionException(failed);}},owner);
            new BukkitRunnable(){@Override public void run(){
                try{if(++ticks>1200)throw new IllegalStateException("Native placement probe timed out");if(!pending.isDone())return;advance();}
                catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_PLACEMENT_FAIL",failure);try{restore();}finally{getServer().shutdown();}}
            }}.runTaskTimer(this,1,1);
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4 placement guard/setup failed",failure);restore();if(authorized)getServer().shutdown();}
    }
    private void advance()throws Exception {
        var completed=pending.join();
        switch(phase) {
            case 0->{phase=1;prepare();}
            case 1->{
                last=(ConstructionBlockJournal.Intent)completed;
                var outcome=writer.attempt(last);require(outcome.state()==PaperConstructionStep.State.APPLIED,"Prepared block must be placed exactly once: "+outcome);
                require(writer.attempt(last).state()==PaperConstructionStep.State.ALREADY_PRESENT,"Immediate retry must not write twice");
                phase=2;pending=CompletableFuture.supplyAsync(()->{try{
                    // Close/reopen after physical write, deliberately before SQL acknowledgement.
                    if(index==0)reopen();return new ConstructionBlockJournal(database.getConnection()).find(last.operation()).orElseThrow();
                }catch(Exception failed){throw new CompletionException(failed);}},owner);
            }
            case 2->{
                last=(ConstructionBlockJournal.Intent)completed;require(last.state()==ConstructionBlockJournal.State.PREPARED,"Unacknowledged intent must survive reopen");
                require(writer.attempt(last).state()==PaperConstructionStep.State.ALREADY_PRESENT,"Recovered writer must only acknowledge exact existing output");
                phase=3;pending=CompletableFuture.supplyAsync(()->{try{return new ConstructionBlockJournal(database.getConnection()).acknowledge(last.operation(),last.after());}catch(Exception failed){throw new CompletionException(failed);}},owner);
            }
            case 3->{
                last=(ConstructionBlockJournal.Intent)completed;require(last.state()==ConstructionBlockJournal.State.APPLIED,"Native output acknowledged");
                if(++index<5){phase=1;prepare();return;}
                var p=last.at();world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.DIAMOND_BLOCK,false);
                require(writer.attempt(last).state()==PaperConstructionStep.State.CONFLICT,"Changed acknowledged block must never be replaced");
                require(world.getBlockAt(p.x(),p.y(),p.z()).getType()==Material.DIAMOND_BLOCK,"Conflicting block preserved");
                phase=4;pending=CompletableFuture.supplyAsync(()->{try{
                    require(new ConstructionBlockJournal(database.getConnection()).acknowledge(last.operation(),"minecraft:diamond_block").state()==ConstructionBlockJournal.State.CONFLICT,"Conflict retained in SQL");
                    var far=new ConstructionDraft(UUID.randomUUID(),draft.settlement(),draft.world(),new Position(2_000_000,draft.origin().y(),2_000_000),Transform.identity(),draft.template(),1000);
                    new ConstructionDraftRepository(database.getConnection()).save(far);
                    return new Far(far,new ConstructionBlockJournal(database.getConnection()).prepare(UUID.randomUUID(),far.id(),0,"minecraft:cobblestone"));
                }catch(Exception failed){throw new CompletionException(failed);}},owner);
            }
            case 4->{
                var far=(Far)completed;require(!world.isChunkLoaded(125000,125000),"Remote target starts unloaded");
                var farWriter=PaperConstructionStep.development(this,far.draft(),new ApprovedArea(UUID.randomUUID(),world.getUID(),ConstructionPreview.footprint(far.draft().template(),Transform.identity(),far.draft().origin())));
                require(farWriter.attempt(far.intent()).state()==PaperConstructionStep.State.WAIT,"Unloaded construction waits");
                require(!world.isChunkLoaded(125000,125000),"Writer must not load remote target");
                phase=5;pending=CompletableFuture.runAsync(()->{database.close();database=null;},owner);
            }
            case 5->{restore();require(restored,"All five test cells restored");phase=6;getLogger().info("P4_PLACEMENT_PASS: real protected public payment, delayed cargo, earned unemployed labor and stage escrow; five native blocks, exact retries, database-reopen crash window, changed-block conflict and unloaded target without force-load; originals restored; no ordinary writer or capacity activation");getServer().shutdown();}
            default->{}
        }
    }
    private void prepare(){
        var authority=new ConstructionMaterials.Authority(draft.id(),draft.world(),System.currentTimeMillis(),true,true);
        pending=CompletableFuture.supplyAsync(()->{try{return new ConstructionBlockJournal(database.getConnection()).preparePublic(UUID.randomUUID(),draft.id(),index,"minecraft:cobblestone",authority,System.currentTimeMillis(),Set.of(draft.world()));}catch(Exception failed){throw new CompletionException(failed);}},owner);
    }
    private void fundAndEarnPath()throws Exception {
        var c=database.getConnection();var town=draft.settlement();var supplier=UUID.randomUUID();var ownerId=UUID.randomUUID();var builder=UUID.randomUUID();
        var people=database.getPopulationRepository();
        people.insertResident(new com.blueninjas24.civilizations.population.Resident(ownerId,"Private supplier fixture",town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        people.insertResident(new com.blueninjas24.civilizations.population.Resident(builder,"Private builder fixture",town,null,300,com.blueninjas24.civilizations.population.Resident.Status.ALIVE));
        var treasury=new TreasuryRepository(c);treasury.initialize(UUID.randomUUID(),town,10000,"Explicit disposable fixture funds");
        var site=new com.blueninjas24.civilizations.industry.BusinessSite(UUID.randomUUID(),town,draft.world(),0,64,0,10,"Explicit private fixture workplace");
        var company=new com.blueninjas24.civilizations.industry.Business(supplier,town,"Private supplier",ownerId,ownerId,"warehouse",site.id(),2,0);
        var requirements=new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of());
        database.getBusinessRepository().found(new com.blueninjas24.civilizations.industry.BusinessFounding(company,site,requirements,requirements,500,"Explicit fixture opportunity"));
        database.getBusinessRepository().hire(UUID.randomUUID(),supplier,ownerId,0);
        var stone=com.blueninjas24.civilizations.economy.Commodity.STONE;var food=com.blueninjas24.civilizations.economy.Commodity.FOOD;
        database.getIndustryRepository().openStore(supplier,64,Map.of(stone,1L),0);
        try(var statement=c.prepareStatement("UPDATE industry_stock SET units=4 WHERE business_id=? AND commodity='stone'")){statement.setString(1,supplier.toString());require(statement.executeUpdate()==1,"Explicit disposable fixture stone seed");}
        var bands=new EnumMap<com.blueninjas24.civilizations.economy.CommodityAccounting.Condition,Integer>(com.blueninjas24.civilizations.economy.CommodityAccounting.Condition.class);
        for(var band:com.blueninjas24.civilizations.economy.CommodityAccounting.Condition.values())bands.put(band,100);
        var settings=new com.blueninjas24.civilizations.economy.EconomySettings(1,3,2,2,1,4,100,new com.blueninjas24.civilizations.economy.CommodityAccounting.Policy(5000,12500,20000,3),Map.of(food,100L),Map.of(food,java.math.BigDecimal.ONE),bands);
        var economy=new EconomyRuntimeRepository(c,settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        economy.observe(new com.blueninjas24.civilizations.economy.FarmObservation(town,draft.world(),1000,true,0),1000);economy.startOnline(1000,Set.of(draft.world()));economy.pulse(2000,false);
        var runtime=new com.blueninjas24.civilizations.industry.IndustryRuntimePolicy(true,1000,8,com.blueninjas24.civilizations.industry.IndustryPolicy.prototype(),Map.of(),com.blueninjas24.civilizations.industry.WorkshopSupplyPolicy.disabled(),new com.blueninjas24.civilizations.industry.WarehouseSupplyPolicy(true,16,16),com.blueninjas24.civilizations.industry.ToolUpkeepPolicy.disabled());
        var purchases=new ConstructionSupplyOrders(c,ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),runtime,com.blueninjas24.civilizations.industry.BusinessOperationPolicy.prototype());
        var request=new ConstructionSupplyOrders.Request(UUID.randomUUID(),draft.id(),supplier,ConstructionBudgetPolicy.Source.VILLAGE,null,stone,2,1);
        long before=treasury.load(town).orElseThrow().hundredths();
        var receipt=purchases.purchase(request,new ConstructionMaterials.Authority(draft.id(),draft.world(),System.currentTimeMillis(),true,true),System.currentTimeMillis(),Set.of(draft.world()));
        require(receipt.price()==50&&treasury.load(town).orElseThrow().hundredths()==before-50&&database.getIndustryRepository().store(supplier).orElseThrow().hundredths()==50,"Real supplier payment conserved");
        require(purchases.stock(draft.id()).isEmpty(),"Pending cargo cannot build");
        economy.pulse(3000,false);require(ConstructionSupplyOrders.deliveryOnly(c).deliverReady(Set.of(draft.world()),16)==1,"Earned-period delivery");
        var work=new EconomyRuntimeRepository(c,settings,com.blueninjas24.civilizations.economy.FarmObservation.Policy.prototype(),216);
        work.configureFarmBusinesses(com.blueninjas24.civilizations.industry.FarmBusinessPolicy.prototype());work.configureConstruction(new ConstructionLaborPolicy(true,1,1,2,25));work.startOnline(3000,Set.of(draft.world()));
        work.pulse(4000,false);require(new ConstructionPublicWork(c).work(draft.id(),0)==2,"Only earned unemployed builder time credited");
        work.pulse(5000,false);work.pulse(6000,false);require(new ConstructionPublicWork(c).work(draft.id(),0)==5,"Frozen path work earned over three actual food periods");
        require(purchases.stock(draft.id()).isEmpty()&&database.getIndustryRepository().store(supplier).orElseThrow().units().get(stone)==2,"Stage escrow and supplier stock conserve four seeded units");
        require(purchases.purchase(request,new ConstructionMaterials.Authority(draft.id(),draft.world(),0,false,false),0,Set.of()).price()==50,"Exact payment replay creates nothing");
    }
    private void reopen()throws Exception{if(database!=null)database.close();database=new DatabaseManager(folder.toFile(),getLogger());database.initialize();}
    private void restore(){if(!authorized||world==null||restored)return;for(var entry:before.entrySet()){
        var p=entry.getKey();if(!world.isChunkLoaded(p.x()>>4,p.z()>>4))throw new IllegalStateException("Fixture cleanup must not load a chunk");
        world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(entry.getValue()),false);
    }restored=true;world.removePluginChunkTickets(this);tickets.clear();}
    @Override public void onDisable(){restore();owner.execute(()->{if(database!=null){database.close();database=null;}});owner.shutdown();}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
