import com.blueninjas24.civilizations.application.BusinessDiagnostics;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit synthetic industry setup in a separate DB; not automatic resource/workshop discovery. */
public final class WorkshopProductionProbe extends JavaPlugin {
    private ExecutorService owner;
    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            if(!Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    || !getServer().getIp().equals("127.0.0.1") || getServer().getPort()!=25577 || !getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Probe requires marked localhost disposable server without players");
            var world=getServer().getWorlds().getFirst();var worldId=world.getUID();var worldName=world.getName();
            var folder=new java.io.File(getDataFolder(),"run-"+UUID.randomUUID());
            owner=Executors.newSingleThreadExecutor(r->{var thread=new Thread(r,"Civilizations-workshop-probe-SQL-owner");thread.setDaemon(true);return thread;});
            var result=CompletableFuture.runAsync(()-> {
                try { verify(folder,worldId,worldName); }
                catch(Exception failure){throw new CompletionException(failure);}
            },owner);
            new BukkitRunnable() {
                @Override public void run() {
                    if(!result.isDone())return;
                    try {result.join();getLogger().info("NATIVE WORKSHOP PASS: paid mine/warehouse/workshop chain; actual inputs -> two tools; capital conserved; replay, dead-worker pause, restart and diagnostics passed");}
                    catch(CompletionException failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE WORKSHOP FAIL",failure.getCause());}
                    cancel();getServer().getPluginManager().disablePlugin(WorkshopProductionProbe.this);
                }
            }.runTaskTimer(this,1,10);
        } catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"Native workshop probe refused",failure);getServer().getPluginManager().disablePlugin(this);}
    }
    private void verify(java.io.File folder,UUID world,String name) throws Exception {
        UUID town=UUID.randomUUID(),founder=UUID.randomUUID(),miner=UUID.randomUUID(),smithWorker=UUID.randomUUID(),deposit=UUID.randomUUID(),operation=UUID.randomUUID();
        IndustryRepository.RecipeRun saved;UUID smithId;
        var tools=new Commodity("tools");var space=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,Commodity.STONE,1L,tools,1L);
        var db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();db.saveSettlement(new Settlement(town,"Workshop fixture",name,2048,64,2048,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,name);db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var person:List.of(founder,miner,smithWorker))db.getPopulationRepository().insertResident(new Resident(person,"Fixture",town,null,300,Resident.Status.ALIVE));
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Test-only industry capital");
            var mine=found(db,town,world,founder,"mining",2048);var warehouse=found(db,town,world,founder,"warehouse",2058);
            var smith=found(db,town,world,founder,"blacksmith",2068);smithId=smith.id();
            db.getBusinessRepository().hire(UUID.randomUUID(),mine.id(),miner,0);db.getBusinessRepository().hire(UUID.randomUUID(),smith.id(),smithWorker,0);
            var industry=db.getIndustryRepository();industry.openStore(mine.id(),1024,space,0);
            industry.openStore(warehouse.id(),1024,space,3000);industry.openStore(smith.id(),1024,space,3000);
            industry.discover(deposit,mine.siteId(),4,20,Map.of(Commodity.IRON,2L,Commodity.COAL,1L,Commodity.STONE,3L));
            industry.mine(UUID.randomUUID(),mine.id(),deposit,4,0,8,8);
            for(var e:Map.of(Commodity.IRON,4L,Commodity.COAL,2L).entrySet()) {
                var a=industry.purchase(UUID.randomUUID(),mine.id(),warehouse.id(),e.getKey(),e.getValue(),100*e.getValue(),0,1,128);industry.deliver(a.id(),1);
                var b=industry.purchase(UUID.randomUUID(),warehouse.id(),smith.id(),e.getKey(),e.getValue(),200*e.getValue(),1,1,128);industry.deliver(b.id(),2);
            }
            saved=industry.process(operation,smith.id(),ProductionRecipe.smithTools(),8,2);
            if(saved.receipt().quantity()!=2 || !saved.produced().equals(Map.of(tools,2L))
                    || !saved.equals(industry.process(operation,smith.id(),ProductionRecipe.smithTools(),8,2)))throw new IllegalStateException("Recipe/replay mismatch");
            long money=new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths();
            for(var business:List.of(mine,warehouse,smith))money+=industry.store(business.id()).orElseThrow().hundredths();
            if(money!=10000 || industry.deposit(deposit).orElseThrow().remainingBatches()!=16)throw new IllegalStateException("Capital or deposit not conserved");
            var snapshot=new BusinessRuntime(db.getConnection(),8,16).refresh(0,Set.of(world));
            if(BusinessDiagnostics.describe(snapshot,town).stream().noneMatch(line->line.contains("Latest industrial production") && line.contains("produced={tools=2}")))
                throw new IllegalStateException("Actual recipe diagnostic missing");
            db.getPopulationRepository().updateResident(new Resident(smithWorker,"Fixture",town,null,300,Resident.Status.DECEASED));
            if(industry.process(UUID.randomUUID(),smith.id(),ProductionRecipe.smithTools(),8,3).receipt().quantity()!=0)throw new IllegalStateException("Dead worker produced tools");
            try(var statement=db.getConnection().createStatement();var rows=statement.executeQuery("PRAGMA integrity_check")){if(!rows.getString(1).equals("ok"))throw new IllegalStateException("Integrity failed");}
        } finally {db.close();}
        db=new DatabaseManager(folder,getLogger());
        try {
            db.initialize();var industry=db.getIndustryRepository();
            if(!saved.equals(industry.recipeRun(operation).orElseThrow()) || industry.store(smithId).orElseThrow().units().get(tools)!=2)
                throw new IllegalStateException("Restart lost recipe history or tools");
        } finally {db.close();}
    }
    private Business found(DatabaseManager db,UUID town,UUID world,UUID person,String kind,int x) throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,64,2048,1,"Synthetic native test fixture; no automatic discovery or block rights");
        var business=new Business(UUID.randomUUID(),town,kind,person,person,kind,site.id(),4,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Explicit test opportunity"));return business;
    }
    @Override public void onDisable(){if(owner!=null)owner.shutdown();}
}
