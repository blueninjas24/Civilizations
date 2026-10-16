import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.EconomyConfiguration;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Disposable packaged SQL-owner proof; not automatic recognition, training or physical construction. */
public final class BakeryPrivateInputProbe extends JavaPlugin {
    private boolean authorized;
    private void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;var world=getServer().getWorlds().getFirst().getUID();
        var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();var logger=getLogger();
        var finished=CompletableFuture.runAsync(()->{var db=new DatabaseManager(folder,logger);try {
            var config=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(ProductionRecipe.class.getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8));
            var economySettings=EconomyConfiguration.read(config);var industry=EconomyConfiguration.readIndustryRuntime(config);
            db.initialize();UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),merchant=UUID.randomUUID(),supplier=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Bakery native fixture","Fixture",0,64,0,1,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            db.getPopulationRepository().insertResident(new Resident(owner,"Fixture baker",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(owner,"baking",10);
            db.getPopulationRepository().insertResident(new Resident(merchant,"Fixture merchant",town,null,300,Resident.Status.ALIVE));
            var supplierSite=new BusinessSite(UUID.randomUUID(),town,world,3,64,0,1000,"Explicit private source fixture");var requirements=new JobRequirements(216,Map.of());
            db.getBusinessRepository().found(new BusinessFounding(new Business(supplier,town,"Fixture grain supplier",merchant,merchant,"warehouse",supplierSite.id(),1,0),supplierSite,requirements,requirements,0,"Explicit native source fixture"));
            db.getBusinessRepository().hire(UUID.randomUUID(),supplier,merchant,0);db.getIndustryRepository().openStore(supplier,100,Map.of(new Commodity("grain"),1L),0);
            try(var stock=db.getConnection().createStatement()){stock.execute("UPDATE industry_stock SET units=10 WHERE business_id='"+supplier+"' AND commodity='grain'");}
            new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,10000,"Explicit native fixture treasury");
            var economy=new EconomyRuntimeRepository(db.getConnection(),economySettings,FarmObservation.Policy.prototype(),216);
            economy.observe(new FarmObservation(town,world,1000,true,0),1000);economy.startOnline(1000,Set.of(world));economy.pulse(61000,false);
            try(var s=db.getConnection().createStatement()) {s.execute("UPDATE economy_stocks SET units=0 WHERE commodity='food'");}
            new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,61000,true,List.of(new WorkplaceObservation.Marker(new VillageObservation.Position(1,64,0),WorkplaceObservation.Kind.SMOKER))));
            db.close();var prototype=PopulationSession.Settings.prototype();
            var settings=new PopulationSession.Settings(0,prototype.millisPerMonth(),prototype.maximumOfflineMonths(),prototype.monthsPerBatch(),prototype.population(),prototype.migration(),prototype.recognition(),prototype.enrollment());
            BakeryInvestment.Draft draft;
            try(var session=new PopulationSession(folder,logger,settings,()->61000,economySettings,FarmObservation.Policy.prototype())) {
                session.configureIndustryRuntime(industry);session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.configureWorkplaces(EconomyConfiguration.readWorkplaceObserver(config));
                session.configureVillageSupplies(EconomyConfiguration.readVillageSupplies(config));
                session.configureWorkshopOpportunities(WorkshopOpportunityPolicy.prototype(),EconomyConfiguration.readWorkshopStarterSupplies(config));
                session.configureBakery(EconomyConfiguration.readBakeryFounding(config),EconomyConfiguration.readBakeryOpportunities(config));
                session.configureMineFounding(MineFoundingPolicy.disabled(),EconomyConfiguration.readStartupGrants(config));
                session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
                draft=session.prepareBakeryInvestment(town).get(5,TimeUnit.SECONDS).draft().orElseThrow();
                check(draft.privateInputs()!=null&&draft.privateInputs().seller().equals(supplier),"Actual private supplier selected");
                session.acceptBakeryInvestment(draft,new WorkplaceOperations.Confirmation(draft.anchor(),true,61000)).get(5,TimeUnit.SECONDS);
                check(session.currentBusinessAvailability(draft.anchor().business()).get(5,TimeUnit.SECONDS).permitted(),"Qualified paid bakery is available");
            }
            db=new DatabaseManager(folder,logger);db.initialize();var business=draft.anchor().business();
            check(new GrainHarvestRepository(db.getConnection()).stock(town)==0,"No public grain created or consumed");
            check(new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==5500,"Real startup capital and ingredient payment");
            var beforeReplay=db.getIndustryRepository().store(business).orElseThrow();
            var review=new BakeryOpportunities(db.getConnection(),EconomyConfiguration.readBakeryOpportunities(config),industry,EconomyConfiguration.readStartupGrants(config),EconomyConfiguration.readWorkshopStarterSupplies(config),EconomyConfiguration.readVillageSupplies(config),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
            new BakeryInvestment(db.getConnection(),review,EconomyConfiguration.readBakeryFounding(config),EconomyConfiguration.readBakeryOpportunities(config),industry,EconomyConfiguration.readStartupGrants(config),EconomyConfiguration.readWorkshopStarterSupplies(config),216,0)
                .accept(draft,new WorkplaceOperations.Confirmation(draft.anchor(),true,61000),61000,Set.of(world));
            check(beforeReplay.equals(db.getIndustryRepository().store(business).orElseThrow()),"Private starter restart replay creates no payment or cargo");
            var runtime=new IndustryRuntime(db.getConnection(),industry,BusinessOperationPolicy.prototype());runtime.start(61000);
            runtime.pulse(61000+industry.millisPerPeriod(),Set.of(world));
            check(db.getIndustryRepository().store(business).orElseThrow().units().get(Commodity.FOOD)==3,"Delivered grain becomes actual meals");
            var buyers=new VillageSupplyPurchases(db.getConnection(),EconomyConfiguration.readVillageSupplies(config),StudyPolicy.disabled(),BusinessOperationPolicy.prototype());
            var order=new VillageSupplyPurchases.Request(UUID.randomUUID(),town,business,Commodity.FOOD,3,1);var paid=buyers.purchase(order,Set.of(world));
            check(paid.price()==600,"Village pays actual food customer invoice");
            var store=db.getIndustryRepository().store(business).orElseThrow();
            long supplierCash=db.getIndustryRepository().store(supplier).orElseThrow().hundredths();
            check(store.hundredths()+supplierCash+new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==9500,"Cash conservation after explicit 500 startup cost");
            check(supplierCash>=300,"Private seller paid real cash");
            try(var receipts=db.getConnection().createStatement();var rows=receipts.executeQuery("SELECT COUNT(*) FROM bakery_private_starters")){check(rows.next()&&rows.getLong(1)==1,"Exactly one private starter receipt");}
            runtime.pulse(1000000,Set.of());check(store.equals(db.getIndustryRepository().store(business).orElseThrow()),"Absent world exact pause");
            check(paid.equals(buyers.purchase(order,Set.of()))&&new GrainHarvestRepository(db.getConnection()).stock(town)==0,"Historical replay cannot refill or repay");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        new BukkitRunnable(){int ticks;@Override public void run(){try {
            check(++ticks<=400,"Bakery fixture timeout");if(!finished.isDone())return;finished.join();
            getLogger().info("BAKERY_PRIVATE_INPUT_PROBE_PASSED packaged defaults, actual SQL-owner founding, private supplier payment/cargo, timed bread, paid village food, replay and absence; no world/entity mutations");cancel();getServer().shutdown();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_PRIVATE_INPUT_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,100,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"BAKERY_PRIVATE_INPUT_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
}
