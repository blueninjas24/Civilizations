package com.blueninjas24.civilizations;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.paper.PopulationConfiguration;
import com.blueninjas24.civilizations.paper.EconomyConfiguration;
import com.blueninjas24.civilizations.paper.PaperFarmObserver;
import com.blueninjas24.civilizations.paper.PopulationDebugCommand;
import com.blueninjas24.civilizations.paper.VillageObserver;
import com.blueninjas24.civilizations.paper.ManagedVillagerConversionGuard;
import com.blueninjas24.civilizations.paper.ManagedVillagerLifecycle;
import com.blueninjas24.civilizations.paper.PaperResidentProjector;
import com.blueninjas24.civilizations.database.ResidentProjectionRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public final class Civilizations extends JavaPlugin {

    private PopulationSession population;
    private VillageObserver observer;
    private PaperResidentProjector projector;
    private ManagedVillagerLifecycle lifecycle;
    private com.blueninjas24.civilizations.paper.ManagedLivestockDeaths livestockDeaths;
    private com.blueninjas24.civilizations.paper.PaperLivestockPresence livestockPresence;
    private com.blueninjas24.civilizations.paper.PaperLivestockNewborns livestockNewborns;
    private com.blueninjas24.civilizations.paper.PaperLivestockAdoption livestockAdoption;
    private PaperFarmObserver farms;
    private com.blueninjas24.civilizations.paper.PaperWaterObserver waters;
    private com.blueninjas24.civilizations.paper.PaperOreObserver ores;
    private com.blueninjas24.civilizations.paper.PaperWorkplaceObserver workplaces;
    private com.blueninjas24.civilizations.paper.PaperWorkplaceOperations workplaceOperations;
    private com.blueninjas24.civilizations.paper.PaperWorkshopStartup workshopStartup;
    private com.blueninjas24.civilizations.paper.PaperBakeryStartup bakeryStartup;
    private com.blueninjas24.civilizations.paper.PaperCraftsmanStartup craftsmanStartup;
    private com.blueninjas24.civilizations.paper.PaperWarehouseStartup warehouseStartup;
    private com.blueninjas24.civilizations.paper.OrePlacementTracker orePlacements;
    private com.blueninjas24.civilizations.paper.FoodAidTransfers aid;
    private com.blueninjas24.civilizations.paper.FoodPurchaseTransfers purchases;
    private com.blueninjas24.civilizations.paper.FoodBargainingCommand bargaining;
    private com.blueninjas24.civilizations.paper.FoodBarterTransfers barters;
    private com.blueninjas24.civilizations.paper.LivestockSaleTransfers livestockSales;
    private com.blueninjas24.civilizations.paper.LivestockMarketMenu livestockMarket;
    private com.blueninjas24.civilizations.paper.PaperPenControls pens;
    private com.blueninjas24.civilizations.paper.FoodBarterCommand barterCommand;

    private com.blueninjas24.civilizations.paper.PaperFisheryStartup fisheryStartup;
    private com.blueninjas24.civilizations.paper.PaperFisheryWorkplaces fisheryWorkplaces;
    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            var config = new YamlConfiguration();
            config.load(new File(getDataFolder(), "config.yml"));
            var settings = PopulationConfiguration.read(config);
            var observerSettings = PopulationConfiguration.readObserver(config);
            var projectionSettings = PopulationConfiguration.readProjection(config);
            var penSettings=com.blueninjas24.civilizations.paper.PenProtectionSettings.read(config);
            var economyFile = new File(getDataFolder(), "economy.yml");
            if (!economyFile.exists()) saveResource("economy.yml", false);
            var economyConfig = new YamlConfiguration(); economyConfig.load(economyFile);
            var economySettings = EconomyConfiguration.read(economyConfig);
            var farmSettings = EconomyConfiguration.readFarms(economyConfig);
            var oreSettings=EconomyConfiguration.readOreObserver(economyConfig);
            var waterSettings=EconomyConfiguration.readWaterObserver(economyConfig);
            var workplaceSettings=EconomyConfiguration.readWorkplaceObserver(economyConfig);
            getLogger().info("Economy prototype settings validated: " + economySettings.secondsPerPeriod()
                    + " seconds/period, food output " + economySettings.foodOutputPercent()
                    + "%; food accounting waits for reliable field evidence.");
            var openingFunds = EconomyConfiguration.readOpeningFunds(economyConfig);
            getLogger().info("Opening village funds: " + (openingFunds.enabled() ? "enabled; grants apply once to economies without a treasury" : "disabled")
                    + "; edit economy.yml while stopped and restart to change settings.");
            population = new PopulationSession(getDataFolder(), getLogger(), settings, System::currentTimeMillis, economySettings, farmSettings,
                    EconomyConfiguration.readPriorities(economyConfig), openingFunds, EconomyConfiguration.readWellbeing(economyConfig),EconomyConfiguration.readResponses(economyConfig));
            population.configureFarmBusinesses(EconomyConfiguration.readFarmBusinesses(economyConfig));
            population.configureGrainHarvest(EconomyConfiguration.readGrainHarvest(economyConfig));
            population.configureFarmMarket(EconomyConfiguration.readFarmMarket(economyConfig));
            var livestockPolicy=EconomyConfiguration.readLivestock(economyConfig);
            population.configureLivestockMarket(EconomyConfiguration.readLivestockMarket(economyConfig));
            population.configureLivestockWorkingArea(EconomyConfiguration.readLivestockWorkingArea(economyConfig));
            population.configureLivestockWork(EconomyConfiguration.readLivestock(economyConfig),EconomyConfiguration.readLivestockRuntime(economyConfig));
            var adoptionPolicy=EconomyConfiguration.readLivestockAdoption(economyConfig);
            population.configureLivestockAdoption(adoptionPolicy);
            livestockAdoption=new com.blueninjas24.civilizations.paper.PaperLivestockAdoption(population,adoptionPolicy,getLogger());
            getServer().getPluginManager().registerEvents(livestockAdoption,this);livestockAdoption.runTaskTimer(this,1,1);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.ManagedLivestockBreedingGuard(
                    population::villageAnimals,()->livestockPolicy.enabled()&&population.status().ready()),this);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.ManagedLivestockProducts(
                    population::villageAnimals,()->livestockPolicy.enabled()&&population.status().ready()),this);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.LivestockBreedingProvenance(),this);
            livestockDeaths=new com.blueninjas24.civilizations.paper.ManagedLivestockDeaths(population,getLogger());
            getServer().getPluginManager().registerEvents(livestockDeaths,this);
            livestockDeaths.runTaskTimer(this,1,1);
            livestockPresence=new com.blueninjas24.civilizations.paper.PaperLivestockPresence(population,getLogger());
            livestockPresence.runTaskTimer(this,20,20);
            livestockNewborns=new com.blueninjas24.civilizations.paper.PaperLivestockNewborns(population,projectionSettings.nearbyRadius(),livestockPolicy::enabled,getLogger());
            livestockNewborns.runTaskTimer(this,1,5);
            getServer().getPluginManager().registerEvents(new ManagedVillagerConversionGuard(() ->
                    population.projectionView().map(PopulationSession.ProjectionView::identities)
                            .orElseGet(ResidentProjectionRepository.Snapshot::empty)), this);
            var debug = new PopulationDebugCommand(population,
                    () -> observer == null ? "Village observer not started" : observer.diagnostic()
                            + "; " + projector.diagnostic() + "; " + lifecycle.diagnostic() + "; " + farms.diagnostic()
                            + "; " + livestockDeaths.diagnostic()
                            + "; " + livestockPresence.diagnostic()
                            + "; " + livestockNewborns.diagnostic()
                            + "; " + livestockAdoption.diagnostic()
                            + "; " + (ores==null?"Ore observer not started":ores.diagnostic())
                            + "; " + (fisheryStartup==null?"Fishery startup not started":fisheryStartup.diagnostic())
                            + "; " + (fisheryWorkplaces==null?"Fishery workplace checks not started":fisheryWorkplaces.diagnostic())
                            + "; " + (waters==null?"Water observer not started":waters.diagnostic())
                            + "; " + (workplaces==null?"Workplace observer not started":workplaces.diagnostic())
                            + "; " + (workplaceOperations==null?"Workplace operation checks not started":workplaceOperations.diagnostic())
                            + "; " + (workshopStartup==null?"Automatic workshop startup not started":workshopStartup.diagnostic())
                            + "; " + (craftsmanStartup==null?"Automatic craftsman startup not started":craftsmanStartup.diagnostic())
                            + "; " + (bakeryStartup==null?"Automatic bakery startup not started":bakeryStartup.diagnostic())
                            + "; " + (warehouseStartup==null?"Automatic warehouse startup not started":warehouseStartup.diagnostic()));
            var command = Objects.requireNonNull(getCommand("civ"), "Missing civ command declaration");
            var previewPolicy = EconomyConfiguration.readAidPreview(economyConfig);
            var bargainingPolicy=EconomyConfiguration.readBargaining(economyConfig);
            aid = new com.blueninjas24.civilizations.paper.FoodAidTransfers(this,population,previewPolicy,
                    EconomyConfiguration.readDonationsEnabled(economyConfig), getServer().getLevelDirectory().resolve("players/data"));
            purchases = new com.blueninjas24.civilizations.paper.FoodPurchaseTransfers(this,population,previewPolicy,
                    EconomyConfiguration.readPurchases(economyConfig),getServer().getLevelDirectory().resolve("players/data"),bargainingPolicy);
            bargaining=new com.blueninjas24.civilizations.paper.FoodBargainingCommand(this,population,purchases,previewPolicy,
                    EconomyConfiguration.readPurchases(economyConfig),bargainingPolicy);
            barters=new com.blueninjas24.civilizations.paper.FoodBarterTransfers(this,population,previewPolicy,
                    EconomyConfiguration.readBarter(economyConfig),getServer().getLevelDirectory().resolve("players/data"));
            barterCommand=new com.blueninjas24.civilizations.paper.FoodBarterCommand(this,population,barters,previewPolicy,
                    EconomyConfiguration.readBarter(economyConfig),bargainingPolicy.offerSeconds());
            var livestockMarketPolicy=EconomyConfiguration.readLivestockMarket(economyConfig);
            livestockSales=new com.blueninjas24.civilizations.paper.LivestockSaleTransfers(this,population,livestockMarketPolicy,getServer().getLevelDirectory().resolve("players/data"));
            livestockMarket=new com.blueninjas24.civilizations.paper.LivestockMarketMenu(this,population,livestockSales,livestockMarketPolicy);
            livestockSales.runTaskTimer(this,20,5);livestockMarket.runTaskTimer(this,1,1);
            getServer().getPluginManager().registerEvents(livestockMarket,this);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.PendingLivestockItems(),this);
            pens=new com.blueninjas24.civilizations.paper.PaperPenControls(population,penSettings,getLogger());
            pens.runTaskTimer(this,1,1);getServer().getPluginManager().registerEvents(pens,this);
            var router=new com.blueninjas24.civilizations.paper.CivilizationsCommand(debug,
                    new com.blueninjas24.civilizations.paper.FoodAidPreviewCommand(population,previewPolicy),bargaining,barterCommand,livestockMarket,pens);
            command.setExecutor(router);command.setTabCompleter(router);bargaining.runTaskTimer(this,1,1);
            // Poll completed transfer stages promptly; each adapter still caps work at four players.
            // Five-second polling multiplied the wait across save/ledger stages during playtesting.
            barterCommand.runTaskTimer(this,1,1);barters.runTaskTimer(this,20,5);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.FoodAidMenu(this,population,aid,previewPolicy,purchases),this);
            aid.runTaskTimer(this,20,5);
            purchases.runTaskTimer(this,20,5);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.PendingTradeInventoryGuard(),this);
            population.configureOreTracking();
            population.configureWorkplaces(workplaceSettings);
            population.configureWorkshopFounding(EconomyConfiguration.readWorkshopFounding(economyConfig));
            population.configureCraftsman(EconomyConfiguration.readCraftsmanFounding(economyConfig),EconomyConfiguration.readCraftsmanOpportunities(economyConfig));
            population.configureBakery(EconomyConfiguration.readBakeryFounding(economyConfig),EconomyConfiguration.readBakeryOpportunities(economyConfig),EconomyConfiguration.readBakeryMarket(economyConfig));
            population.configureWarehouseOpportunities(EconomyConfiguration.readWarehouseOpportunities(economyConfig));
            population.configureWarehouseFounding(EconomyConfiguration.readWarehouseFounding(economyConfig));
            population.configureWorkplaceOperations(EconomyConfiguration.readWorkplaceOperations(economyConfig));
            population.configureWorkshopOpportunities(EconomyConfiguration.readWorkshopOpportunities(economyConfig),EconomyConfiguration.readWorkshopStarterSupplies(economyConfig));
            population.configureIndustryRuntime(EconomyConfiguration.readIndustryRuntime(economyConfig));
            population.configureWater(waterSettings);
            population.configureFisheryOpportunities(EconomyConfiguration.readFisheryOpportunities(economyConfig));
            population.configureFisheryFounding(EconomyConfiguration.readFisheryFounding(economyConfig));
            population.configureFisheryShoreline(EconomyConfiguration.readFisheryShoreline(economyConfig));
            population.configureFishing(EconomyConfiguration.readFishingResources(economyConfig));
            population.configureQuarries(EconomyConfiguration.readQuarryResources(economyConfig));
            population.configureBusinessPurchases(EconomyConfiguration.readBusinessPurchasePolicy(economyConfig));
            population.configureBusinessOperations(EconomyConfiguration.readBusinessOperations(economyConfig));
            population.configureWorkLearning(EconomyConfiguration.readWorkLearning(economyConfig));
            population.configureStudy(EconomyConfiguration.readStudy(economyConfig));
            population.configureVillageSupplies(EconomyConfiguration.readVillageSupplies(economyConfig));
            population.configureBusinessStaffing(EconomyConfiguration.readBusinessStaffing(economyConfig));
            population.configureCareerStudy(EconomyConfiguration.readCareerStudy(economyConfig));
            population.configureMineFounding(EconomyConfiguration.readMineFounding(economyConfig),EconomyConfiguration.readStartupGrants(economyConfig));
            population.configureFarmTools(EconomyConfiguration.readFarmTools(economyConfig),EconomyConfiguration.readFarmToolBudget(economyConfig));
            population.configureUpkeepContributions(EconomyConfiguration.readUpkeepContributions(economyConfig));
            population.configureAutomaticResale(EconomyConfiguration.readAutomaticResale(economyConfig));
            var editorGuard = new com.blueninjas24.civilizations.paper.OreEditorGuard(population, getLogger(),
                    EconomyConfiguration.readAdditionalOreEditors(economyConfig));
            getServer().getPluginManager().registerEvents(editorGuard, this);
            editorGuard.inspectInstalled(java.util.List.of(getServer().getPluginManager().getPlugins()));
            orePlacements=new com.blueninjas24.civilizations.paper.OrePlacementTracker(population,getLogger());
            getServer().getPluginManager().registerEvents(orePlacements,this);
            var availability = new com.blueninjas24.civilizations.paper.WorldAvailability(population, () -> getServer().getWorlds(), getLogger());
            var startup = population.start(true, availability.snapshot());
            getServer().getPluginManager().registerEvents(availability, this);
            getLogger().info("Civilization time pauses during server downtime and for absent worlds; loaded-world chunks continue simulation.");
            // Completion callbacks run on the DB owner. Poll from Paper's main thread instead.
            new BukkitRunnable() {
                @Override public void run() {
                    if (!startup.isDone()) return;
                    cancel();
                    try {
                        var status = startup.join();
                        observer = new VillageObserver(Civilizations.this, population, observerSettings);
                        farms = new PaperFarmObserver(population, farmSettings); farms.runTaskTimer(Civilizations.this, 1, 1);
                        ores=new com.blueninjas24.civilizations.paper.PaperOreObserver(population,oreSettings);ores.runTaskTimer(Civilizations.this,1,1);
                        waters=new com.blueninjas24.civilizations.paper.PaperWaterObserver(population,waterSettings,getLogger());waters.runTaskTimer(Civilizations.this,1,1);
                        fisheryStartup=new com.blueninjas24.civilizations.paper.PaperFisheryStartup(population,EconomyConfiguration.readFisheryShoreline(economyConfig),waterSettings,getLogger());fisheryStartup.runTaskTimer(Civilizations.this,1,1);
                        fisheryWorkplaces=new com.blueninjas24.civilizations.paper.PaperFisheryWorkplaces(population,EconomyConfiguration.readFisheryShoreline(economyConfig),getLogger());fisheryWorkplaces.runTaskTimer(Civilizations.this,1,1);
                        workplaces=new com.blueninjas24.civilizations.paper.PaperWorkplaceObserver(population,workplaceSettings);workplaces.runTaskTimer(Civilizations.this,1,1);
                        workplaceOperations=new com.blueninjas24.civilizations.paper.PaperWorkplaceOperations(population,getLogger());workplaceOperations.runTaskTimer(Civilizations.this,1,20);
                        workshopStartup=new com.blueninjas24.civilizations.paper.PaperWorkshopStartup(population,getLogger());workshopStartup.runTaskTimer(Civilizations.this,1,20);
                        craftsmanStartup=new com.blueninjas24.civilizations.paper.PaperCraftsmanStartup(population,getLogger());craftsmanStartup.runTaskTimer(Civilizations.this,1,20);
                        bakeryStartup=new com.blueninjas24.civilizations.paper.PaperBakeryStartup(population,getLogger());bakeryStartup.runTaskTimer(Civilizations.this,1,20);
                        warehouseStartup=new com.blueninjas24.civilizations.paper.PaperWarehouseStartup(population,getLogger());warehouseStartup.runTaskTimer(Civilizations.this,1,20);
                        lifecycle = new ManagedVillagerLifecycle(population, getLogger());
                        getServer().getPluginManager().registerEvents(lifecycle, Civilizations.this);
                        lifecycle.runTaskTimer(Civilizations.this, 1, 1);
                        projector = new PaperResidentProjector(Civilizations.this, population, projectionSettings);
                        projector.runTaskTimer(Civilizations.this, 1, 1);
                        observer.runTaskTimer(Civilizations.this, 1, 1);
                        getLogger().info("Civilizations population ready: " + status.livingPeople()
                                + " saved living residents; completed " + status.completedMonths() + " months.");
                    } catch (CompletionException failed) { failStartup(failed.getCause()); }
                }
            }.runTaskTimer(this, 1, 20);
            getLogger().info("Civilizations population starting on its database owner thread.");
        } catch (IOException | InvalidConfigurationException | RuntimeException failed) { failStartup(failed); }
    }

    @Override
    public void onDisable() {
        if(pens!=null){pens.cancel();pens.close();}
        if(livestockMarket!=null)livestockMarket.close();
        if(livestockSales!=null)livestockSales.close();
        if(fisheryWorkplaces!=null)fisheryWorkplaces.cancel();
        if(fisheryStartup!=null)fisheryStartup.cancel();
        if(waters!=null)waters.cancel();
        if(warehouseStartup!=null)warehouseStartup.cancel();
        if(workshopStartup!=null)workshopStartup.cancel();
        if(bakeryStartup!=null)bakeryStartup.cancel();
        if(craftsmanStartup!=null)craftsmanStartup.cancel();
        if(workplaceOperations!=null)workplaceOperations.cancel();
        if(barterCommand!=null)barterCommand.close();
        if(barters!=null)barters.close();
        if(bargaining!=null)bargaining.close();
        if (purchases != null) purchases.close();
        if (aid != null) aid.close();
        if (projector != null) projector.close();
        if (lifecycle != null) lifecycle.flush();
        if(livestockAdoption!=null){livestockAdoption.cancel();livestockAdoption.close();}
        if(livestockNewborns!=null){livestockNewborns.cancel();livestockNewborns.close();}
        if(livestockPresence!=null){livestockPresence.cancel();livestockPresence.flush();}
        if(livestockDeaths!=null){livestockDeaths.cancel();livestockDeaths.flush();}
        if (population != null) population.close();
        getLogger().info("Civilizations has been disabled.");
    }

    private void failStartup(Throwable failed) {
        getLogger().log(Level.SEVERE, "Civilizations startup failed; saved data has not been reset", failed);
        getServer().getPluginManager().disablePlugin(this);
    }
}
