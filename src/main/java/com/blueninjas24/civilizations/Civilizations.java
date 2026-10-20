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
    private com.blueninjas24.civilizations.paper.PaperLivestockSlaughter livestockSlaughter;
    private com.blueninjas24.civilizations.paper.PaperLivestockPresence livestockPresence;
    private com.blueninjas24.civilizations.paper.PaperLivestockNewborns livestockNewborns;
    private com.blueninjas24.civilizations.paper.PaperLivestockAdoption livestockAdoption;
    private PaperFarmObserver farms;
    private com.blueninjas24.civilizations.paper.PaperWaterObserver waters;
    private com.blueninjas24.civilizations.paper.PaperOreObserver ores;
    private com.blueninjas24.civilizations.paper.PaperOreObserver quarries;
    private com.blueninjas24.civilizations.paper.PaperForestObserver forests;
    private com.blueninjas24.civilizations.paper.PaperWorkplaceObserver workplaces;
    private com.blueninjas24.civilizations.paper.PaperWorkplaceOperations workplaceOperations;
    private com.blueninjas24.civilizations.paper.PaperWorkshopStartup workshopStartup;
    private com.blueninjas24.civilizations.paper.PaperBakeryStartup bakeryStartup;
    private com.blueninjas24.civilizations.paper.PaperCraftsmanStartup craftsmanStartup;
    private com.blueninjas24.civilizations.paper.PaperWarehouseStartup warehouseStartup;
    private com.blueninjas24.civilizations.paper.OrePlacementTracker orePlacements;
    private com.blueninjas24.civilizations.paper.ForestMutationTracker forestMutations;
    private com.blueninjas24.civilizations.paper.GeneratedTerrainObserver generatedTerrain;
    private com.blueninjas24.civilizations.paper.WorldEditResourceCapture worldEditCapture;
    private com.blueninjas24.civilizations.paper.FoodAidTransfers aid;
    private com.blueninjas24.civilizations.paper.FoodPurchaseTransfers purchases;
    private com.blueninjas24.civilizations.paper.FoodBargainingCommand bargaining;
    private com.blueninjas24.civilizations.paper.FoodBarterTransfers barters;
    private com.blueninjas24.civilizations.paper.LivestockSaleTransfers livestockSales;
    private com.blueninjas24.civilizations.paper.LivestockMarketMenu livestockMarket;
    private com.blueninjas24.civilizations.paper.PaperPenControls pens;
    private com.blueninjas24.civilizations.paper.PaperConstructionPreview construction;
    private com.blueninjas24.civilizations.paper.PaperBuildingProtection buildings;
    private com.blueninjas24.civilizations.paper.PaperDwellingReview dwellingReview;
    private com.blueninjas24.civilizations.paper.PaperHomeDonation homeDonations;
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
            var slaughterPolicy=EconomyConfiguration.readLivestockSlaughter(economyConfig);population.configureLivestockSlaughter(slaughterPolicy);
            livestockSlaughter=new com.blueninjas24.civilizations.paper.PaperLivestockSlaughter(population,slaughterPolicy,EconomyConfiguration.readLivestockWorkingArea(economyConfig),getLogger());
            livestockDeaths=new com.blueninjas24.civilizations.paper.ManagedLivestockDeaths(population,getLogger(),livestockSlaughter::observeDeath);
            livestockSlaughter.setDeathSink(livestockDeaths::captureControlled);livestockSlaughter.runTaskTimer(this,1,1);
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
                            + "; " + livestockSlaughter.diagnostic()
                            + "; " + population.livestockSlaughterDiagnostic()
                            + "; " + livestockPresence.diagnostic()
                            + "; " + livestockNewborns.diagnostic()
                            + "; " + livestockAdoption.diagnostic()
                            + "; " + (ores==null?"Ore observer not started":ores.diagnostic())
                            + "; " + (quarries==null?"Stone/sand observer not started":quarries.diagnostic())
                            + "; " + population.forestTrackingDiagnostic()+"; "+population.automaticForestryDiagnostic()
                            + "; " + (generatedTerrain==null?"New terrain capture not started":generatedTerrain.diagnostic())
                            + "; " + (forests==null?"Forest observer not started":forests.diagnostic())
                            + "; " + (fisheryStartup==null?"Fishery startup not started":fisheryStartup.diagnostic())
                            + "; " + (fisheryWorkplaces==null?"Fishery workplace checks not started":fisheryWorkplaces.diagnostic())
                            + "; " + (waters==null?"Water observer not started":waters.diagnostic())
                            + "; " + (workplaces==null?"Workplace observer not started":workplaces.diagnostic())
                            + "; " + (workplaceOperations==null?"Workplace operation checks not started":workplaceOperations.diagnostic())
                            + "; " + (workshopStartup==null?"Automatic workshop startup not started":workshopStartup.diagnostic())
                            + "; " + (craftsmanStartup==null?"Automatic craftsman startup not started":craftsmanStartup.diagnostic())
                            + "; " + (bakeryStartup==null?"Automatic bakery startup not started":bakeryStartup.diagnostic())
                            + "; " + (warehouseStartup==null?"Automatic warehouse startup not started":warehouseStartup.diagnostic()),task->{
                                if(isEnabled())getServer().getScheduler().runTask(Civilizations.this,task);
                            });
            var command = Objects.requireNonNull(getCommand("civ"), "Missing civ command declaration");
            debug.configureRecovery(new com.blueninjas24.civilizations.paper.PaperMineralRecovery(this,population));
            debug.configureForestRecovery(new com.blueninjas24.civilizations.paper.PaperForestRecovery(this,population,EconomyConfiguration.readForestryObserver(economyConfig).geometry()));
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
            try {
                var buildingSettings=com.blueninjas24.civilizations.paper.BuildingProtectionSettings.read(getConfig());
                buildings=new com.blueninjas24.civilizations.paper.PaperBuildingProtection(population,buildingSettings,getLogger());
                dwellingReview=new com.blueninjas24.civilizations.paper.PaperDwellingReview(population,
                        new com.blueninjas24.civilizations.construction.DwellingInspection.Policy(buildingSettings.requireCoveredBeds()),getLogger());
                buildings.configureInspection(dwellingReview);dwellingReview.runTaskTimer(this,1,1);
                if(buildingSettings.enabled()) {
                    homeDonations=new com.blueninjas24.civilizations.paper.PaperHomeDonation(population,
                            new com.blueninjas24.civilizations.construction.DwellingInspection.Policy(buildingSettings.requireCoveredBeds()),buildings::selectedHomeBounds,getLogger());
                    buildings.configureHomes(homeDonations);homeDonations.runTaskTimer(this,1,1);
                    getServer().getPluginManager().registerEvents(homeDonations,this);
                }
                buildings.runTaskTimer(this,1,1);getServer().getPluginManager().registerEvents(buildings,this);
            }catch(IllegalArgumentException invalid){getLogger().log(Level.WARNING,"Building protection disabled; fix building-protection settings and restart.",invalid);}
            try {
                var constructionLibrary=com.blueninjas24.civilizations.paper.PaperConstructionLibrary.load(this);
                population.configureConstruction(constructionLibrary.funding(),constructionLibrary.labor());
                construction=new com.blueninjas24.civilizations.paper.PaperConstructionPreview(constructionLibrary,population::constructionProtections);
                construction.runTaskTimer(this,1,1);
            }catch(IOException|InvalidConfigurationException|IllegalArgumentException invalid) {
                getLogger().log(Level.WARNING,"Construction previews disabled; fix construction/*.yml and restart. Population and economy remain active.",invalid);
            }
            var router=new com.blueninjas24.civilizations.paper.CivilizationsCommand(debug,
                    new com.blueninjas24.civilizations.paper.FoodAidPreviewCommand(population,previewPolicy),bargaining,barterCommand,livestockMarket,pens,construction,buildings);
            command.setExecutor(router);command.setTabCompleter(router);bargaining.runTaskTimer(this,1,1);
            // Poll completed transfer stages promptly; each adapter still caps work at four players.
            // Five-second polling multiplied the wait across save/ledger stages during playtesting.
            barterCommand.runTaskTimer(this,1,1);barters.runTaskTimer(this,20,5);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.FoodAidMenu(this,population,aid,previewPolicy,purchases),this);
            aid.runTaskTimer(this,20,5);
            purchases.runTaskTimer(this,20,5);
            getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.PendingTradeInventoryGuard(),this);
            population.configureOreTracking();
            population.configureForestTracking();
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
            population.configureForestry(EconomyConfiguration.readForestryResources(economyConfig));
            population.configureForestryFounding(EconomyConfiguration.readForestryFounding(economyConfig));
            population.configureQuarryFounding(EconomyConfiguration.readQuarryFounding(economyConfig));
            population.configureBusinessPurchases(EconomyConfiguration.readBusinessPurchasePolicy(economyConfig));
            population.configureBusinessOperations(EconomyConfiguration.readBusinessOperations(economyConfig));
            population.configureWorkLearning(EconomyConfiguration.readWorkLearning(economyConfig));
            population.configureStudy(EconomyConfiguration.readStudy(economyConfig));
            population.configureVillageSupplies(EconomyConfiguration.readVillageSupplies(economyConfig));
            population.configureBusinessStaffing(EconomyConfiguration.readBusinessStaffing(economyConfig));
            population.configureBusinessAllocation(EconomyConfiguration.readBusinessAllocation(economyConfig));
            population.configureCareerStudy(EconomyConfiguration.readCareerStudy(economyConfig));
            population.configureMineFounding(EconomyConfiguration.readMineFounding(economyConfig),EconomyConfiguration.readStartupGrants(economyConfig));
            population.configureFarmTools(EconomyConfiguration.readFarmTools(economyConfig),EconomyConfiguration.readFarmToolBudget(economyConfig));
            population.configureUpkeepContributions(EconomyConfiguration.readUpkeepContributions(economyConfig));
            population.configureAutomaticResale(EconomyConfiguration.readAutomaticResale(economyConfig));
            var editorPlugin=getServer().getPluginManager().getPlugin("WorldEdit");
            if(com.blueninjas24.civilizations.paper.WorldEditCapturePolicy.supportsPlugin(editorPlugin,
                    getServer().getPluginManager().getPlugin("FastAsyncWorldEdit")!=null)) {
                // WorldEdit may perform queued changes during onLoad, before our listeners exist.
                population.configureUnverifiedResourceHistory("WorldEdit bootstrap: earlier terrain requires reviewed recovery");
                worldEditCapture=new com.blueninjas24.civilizations.paper.WorldEditResourceCapture(population,getLogger());
                try {
                    worldEditCapture.register(true);
                    getServer().getPluginManager().registerEvents(new com.blueninjas24.civilizations.paper.WorldEditCommandGuard(worldEditCapture),this);
                    getLogger().info("Verified WorldEdit synchronous capture active; only fresh terrain captured this boot can qualify automatically.");
                } catch(RuntimeException|LinkageError failure) {
                    population.invalidateOreTracking("WorldEdit registration unavailable");population.invalidateForestTracking("WorldEdit registration unavailable");
                    getLogger().log(Level.WARNING,"WorldEdit resource capture failed; resource work remains guarded",failure);
                }
            }
            var editorGuard = new com.blueninjas24.civilizations.paper.OreEditorGuard(population, getLogger(),
                    EconomyConfiguration.readAdditionalOreEditors(economyConfig),plugin->worldEditCapture!=null&&worldEditCapture.certifiesPresenceOf(plugin));
            getServer().getPluginManager().registerEvents(editorGuard, this);
            editorGuard.inspectInstalled(java.util.List.of(getServer().getPluginManager().getPlugins()));
            orePlacements=new com.blueninjas24.civilizations.paper.OrePlacementTracker(population,getLogger());
            getServer().getPluginManager().registerEvents(orePlacements,this);
            forestMutations=new com.blueninjas24.civilizations.paper.ForestMutationTracker(population,getLogger());
            getServer().getPluginManager().registerEvents(forestMutations,this);
            generatedTerrain=new com.blueninjas24.civilizations.paper.GeneratedTerrainObserver(population,getLogger());
            getServer().getPluginManager().registerEvents(generatedTerrain,this);
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
                        quarries=com.blueninjas24.civilizations.paper.PaperOreObserver.quarries(population,EconomyConfiguration.readQuarryObserver(economyConfig));quarries.runTaskTimer(Civilizations.this,1,1);
                        forests=new com.blueninjas24.civilizations.paper.PaperForestObserver(population,EconomyConfiguration.readForestryObserver(economyConfig));forests.runTaskTimer(Civilizations.this,1,1);
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
        if(homeDonations!=null){homeDonations.cancel();homeDonations.close();}
        if(dwellingReview!=null){dwellingReview.cancel();dwellingReview.close();}
        if(buildings!=null){buildings.cancel();buildings.close();}
        if(construction!=null)construction.cancel();
        if(pens!=null){pens.cancel();pens.close();}
        if(livestockMarket!=null)livestockMarket.close();
        if(livestockSales!=null)livestockSales.close();
        if(fisheryWorkplaces!=null)fisheryWorkplaces.cancel();
        if(fisheryStartup!=null)fisheryStartup.cancel();
        if(waters!=null)waters.cancel();
        if(forests!=null)forests.cancel();
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
        if(livestockSlaughter!=null){livestockSlaughter.cancel();livestockSlaughter.close();}
        if(livestockNewborns!=null){livestockNewborns.cancel();livestockNewborns.close();}
        if(livestockPresence!=null){livestockPresence.cancel();livestockPresence.flush();}
        if(livestockDeaths!=null){livestockDeaths.cancel();livestockDeaths.flush();}
        if(worldEditCapture!=null)worldEditCapture.close();
        if (population != null) population.close();
        getLogger().info("Civilizations has been disabled.");
    }

    private void failStartup(Throwable failed) {
        getLogger().log(Level.SEVERE, "Civilizations startup failed; saved data has not been reset", failed);
        getServer().getPluginManager().disablePlugin(this);
    }
}
