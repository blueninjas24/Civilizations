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
    private PaperFarmObserver farms;
    private com.blueninjas24.civilizations.paper.PaperOreObserver ores;
    private com.blueninjas24.civilizations.paper.PaperWorkplaceObserver workplaces;
    private com.blueninjas24.civilizations.paper.OrePlacementTracker orePlacements;
    private com.blueninjas24.civilizations.paper.FoodAidTransfers aid;
    private com.blueninjas24.civilizations.paper.FoodPurchaseTransfers purchases;
    private com.blueninjas24.civilizations.paper.FoodBargainingCommand bargaining;
    private com.blueninjas24.civilizations.paper.FoodBarterTransfers barters;
    private com.blueninjas24.civilizations.paper.FoodBarterCommand barterCommand;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            var config = new YamlConfiguration();
            config.load(new File(getDataFolder(), "config.yml"));
            var settings = PopulationConfiguration.read(config);
            var observerSettings = PopulationConfiguration.readObserver(config);
            var projectionSettings = PopulationConfiguration.readProjection(config);
            var economyFile = new File(getDataFolder(), "economy.yml");
            if (!economyFile.exists()) saveResource("economy.yml", false);
            var economyConfig = new YamlConfiguration(); economyConfig.load(economyFile);
            var economySettings = EconomyConfiguration.read(economyConfig);
            var farmSettings = EconomyConfiguration.readFarms(economyConfig);
            var oreSettings=EconomyConfiguration.readOreObserver(economyConfig);
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
            getServer().getPluginManager().registerEvents(new ManagedVillagerConversionGuard(() ->
                    population.projectionView().map(PopulationSession.ProjectionView::identities)
                            .orElseGet(ResidentProjectionRepository.Snapshot::empty)), this);
            var debug = new PopulationDebugCommand(population,
                    () -> observer == null ? "Village observer not started" : observer.diagnostic()
                            + "; " + projector.diagnostic() + "; " + lifecycle.diagnostic() + "; " + farms.diagnostic()
                            + "; " + (ores==null?"Ore observer not started":ores.diagnostic())
                            + "; " + (workplaces==null?"Workplace observer not started":workplaces.diagnostic()));
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
            var router=new com.blueninjas24.civilizations.paper.CivilizationsCommand(debug,
                    new com.blueninjas24.civilizations.paper.FoodAidPreviewCommand(population,previewPolicy),bargaining,barterCommand);
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
            population.configureWorkshopOpportunities(EconomyConfiguration.readWorkshopOpportunities(economyConfig),EconomyConfiguration.readWorkshopStarterSupplies(economyConfig));
            population.configureIndustryRuntime(EconomyConfiguration.readIndustryRuntime(economyConfig));
            population.configureBusinessPurchases(EconomyConfiguration.readBusinessPurchasePolicy(economyConfig));
            population.configureBusinessOperations(EconomyConfiguration.readBusinessOperations(economyConfig));
            population.configureWorkLearning(EconomyConfiguration.readWorkLearning(economyConfig));
            population.configureStudy(EconomyConfiguration.readStudy(economyConfig));
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
                        workplaces=new com.blueninjas24.civilizations.paper.PaperWorkplaceObserver(population,workplaceSettings);workplaces.runTaskTimer(Civilizations.this,1,1);
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
        if(barterCommand!=null)barterCommand.close();
        if(barters!=null)barters.close();
        if(bargaining!=null)bargaining.close();
        if (purchases != null) purchases.close();
        if (aid != null) aid.close();
        if (projector != null) projector.close();
        if (lifecycle != null) lifecycle.flush();
        if (population != null) population.close();
        getLogger().info("Civilizations has been disabled.");
    }

    private void failStartup(Throwable failed) {
        getLogger().log(Level.SEVERE, "Civilizations startup failed; saved data has not been reset", failed);
        getServer().getPluginManager().disablePlugin(this);
    }
}
