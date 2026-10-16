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
import java.util.concurrent.atomic.AtomicLong;

/** Packaged runtime/SQL-owner proof in a separate marked disposable DB; no world/entity/block changes. */
public final class StorageUpgradeProbe extends JavaPlugin {
    private boolean authorized;
    private void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;var world=getServer().getWorlds().getFirst().getUID();var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();
        var finished=CompletableFuture.runAsync(()->{DatabaseManager db=new DatabaseManager(folder,getLogger());try {
            var c=YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(ProductionRecipe.class.getResourceAsStream("/economy.yml")),StandardCharsets.UTF_8));
            c.set("industry-runtime.real-millis-per-period",1000);var economy=EconomyConfiguration.read(c);var industry=EconomyConfiguration.readIndustryRuntime(c);
            check(industry.storageUpgrades().enabled()&&industry.recipes().containsKey("craft_storage_fittings"),"Packaged storage/recipe defaults");
            db.initialize();var town=UUID.randomUUID();var buyer=UUID.randomUUID();var maker=UUID.randomUUID();
            db.saveSettlement(new Settlement(town,"Storage fixture","Fixture",0,64,0,2,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            var firms=new ArrayList<UUID>();
            for(var kind:List.of("warehouse","craftsman")) {
                var person=kind.equals("warehouse")?buyer:maker;var skill=kind.equals("warehouse")?"commerce":"crafting";
                db.getPopulationRepository().insertResident(new Resident(person,"Explicit fixture worker",town,null,300,Resident.Status.ALIVE));db.getPopulationRepository().recordSkillExperience(person,skill,10);
                var site=new BusinessSite(UUID.randomUUID(),town,world,firms.size()*3,64,0,1000,"Explicit existing workplace fixture");var requirements=new JobRequirements(216,Map.of(skill,1L));
                var firm=new Business(UUID.randomUUID(),town,"Fixture "+kind,person,person,kind,site.id(),2,0);
                db.getBusinessRepository().found(new BusinessFounding(firm,site,requirements,requirements,0,"Explicit native fixture"));db.getBusinessRepository().hire(UUID.randomUUID(),firm.id(),person,0);firms.add(firm.id());
            }
            UUID warehouse=firms.get(0),craft=firms.get(1);new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,30000,"Explicit disposable test capital");
            var oldCatalog=new TreeMap<>(industry.mining().storageSpacePerUnit());oldCatalog.remove(StorageUpgrades.FITTINGS);
            db.getIndustryRepository().openStore(warehouse,100,oldCatalog,6000);db.getIndustryRepository().openStore(craft,100,industry.mining().storageSpacePerUnit(),4000);
            try(var s=db.getConnection().createStatement()){s.execute("UPDATE industry_stock SET units=80 WHERE business_id='"+warehouse+"' AND commodity='coal'");s.execute("UPDATE industry_stock SET units=3 WHERE business_id='"+warehouse+"' AND commodity='timber'");s.execute("UPDATE industry_stock SET units=1 WHERE business_id='"+warehouse+"' AND commodity='stone'");}
            var timber=UUID.randomUUID();var stone=UUID.randomUUID();db.getIndustryRepository().purchase(timber,warehouse,craft,Commodity.TIMBER,3,1200,0,1,128);db.getIndustryRepository().purchase(stone,warehouse,craft,Commodity.STONE,1,300,0,1,128);
            db.getIndustryRepository().deliver(timber,1);db.getIndustryRepository().deliver(stone,1);db.getIndustryRepository().process(UUID.randomUUID(),craft,ProductionRecipe.storageFittings(),1,0);
            new EconomyRuntimeRepository(db.getConnection(),economy,FarmObservation.Policy.prototype(),216).observe(new FarmObservation(town,world,1000,true,0),1000);
            db.close();var time=new AtomicLong(1000);var defaults=PopulationSession.Settings.prototype();
            var settings=new PopulationSession.Settings(0,defaults.millisPerMonth(),defaults.maximumOfflineMonths(),defaults.monthsPerBatch(),defaults.population(),defaults.migration(),defaults.recognition(),defaults.enrollment());
            try(var session=new PopulationSession(folder,getLogger(),settings,time::get,economy,FarmObservation.Policy.prototype())) {
                session.configureIndustryRuntime(industry);session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
                time.set(2000);session.pulse().get(5,TimeUnit.SECONDS);session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);time.set(100000);session.pulse().get(5,TimeUnit.SECONDS);
            }
            db=new DatabaseManager(folder,getLogger());db.initialize();check(db.getIndustryRepository().store(warehouse).orElseThrow().capacity()==100,"Absent-world cargo remains uninstalled");
            UUID operation;try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT operation_id FROM business_storage_upgrades")){check(r.next(),"Automatic actual fitting purchase");operation=UUID.fromString(r.getString(1));check(!r.next(),"One upgrade only");}
            var pending=new StorageUpgrades(db.getConnection(),StorageUpgradePolicy.prototype(),BusinessOperationPolicy.prototype()).find(operation).orElseThrow();check(pending.installedPeriod()==null,"No early capacity");db.close();
            try(var session=new PopulationSession(folder,getLogger(),settings,time::get,economy,FarmObservation.Policy.prototype())) {
                session.configureIndustryRuntime(industry);session.configureBusinessOperations(BusinessOperationPolicy.prototype());session.start(false,Set.of(world)).get(10,TimeUnit.SECONDS);
                session.pulse().get(5,TimeUnit.SECONDS);time.set(101000);session.pulse().get(5,TimeUnit.SECONDS);
            }
            db=new DatabaseManager(folder,getLogger());db.initialize();var store=db.getIndustryRepository().store(warehouse).orElseThrow();var producer=db.getIndustryRepository().store(craft).orElseThrow();
            check(store.capacity()==200&&store.units().get(Commodity.COAL)==80&&store.spacePerUnit().equals(oldCatalog),"Delivered capacity, exact old stock/catalog retained");
            check(store.hundredths()==5500&&producer.hundredths()==4500,"Real ingredient and fitting payments");check(store.hundredths()+producer.hundredths()+new TreasuryRepository(db.getConnection()).load(town).orElseThrow().hundredths()==30000,"Cash conserved");
            check(producer.units().get(StorageUpgrades.FITTINGS)==0,"Actual produced fitting installed once");
            var upgrades=new StorageUpgrades(db.getConnection(),StorageUpgradePolicy.disabled(),BusinessOperationPolicy.prototype());var receipt=upgrades.find(operation).orElseThrow();check(receipt.installedPeriod()!=null,"Saved installation receipt");
            check(receipt.equals(upgrades.accept(receipt.draft(),1,Set.of()))&&store.equals(db.getIndustryRepository().store(warehouse).orElseThrow()),"Restart/disabled/absent replay cannot buy or expand again");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Native fixture foreign keys");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}});
        new BukkitRunnable(){int ticks;@Override public void run(){try{check(++ticks<=600,"Storage fixture timeout");if(!finished.isDone())return;finished.join();getLogger().info("STORAGE_UPGRADE_PROBE_PASSED actual ingredient purchase/crafting, SQL-owner automatic fitting sale, delayed capacity, world pause/restart, exact replay and conserved cash/stock; no world/entity mutations");cancel();getServer().shutdown();}catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"STORAGE_UPGRADE_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,20,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"STORAGE_UPGRADE_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
}
