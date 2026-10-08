package com.blueninjas24.civilizations;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.paper.PopulationConfiguration;
import com.blueninjas24.civilizations.paper.EconomyConfiguration;
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
            getLogger().info("Economy prototype settings validated: " + economySettings.secondsPerPeriod()
                    + " seconds/period, food output " + economySettings.foodOutputPercent()
                    + "%; live economy scheduling is not connected yet.");
            population = new PopulationSession(getDataFolder(), getLogger(), settings, System::currentTimeMillis);
            getServer().getPluginManager().registerEvents(new ManagedVillagerConversionGuard(() ->
                    population.projectionView().map(PopulationSession.ProjectionView::identities)
                            .orElseGet(ResidentProjectionRepository.Snapshot::empty)), this);
            var debug = new PopulationDebugCommand(population,
                    () -> observer == null ? "Village observer not started" : observer.diagnostic()
                            + "; " + projector.diagnostic() + "; " + lifecycle.diagnostic());
            var command = Objects.requireNonNull(getCommand("civ"), "Missing civ command declaration");
            command.setExecutor(debug); command.setTabCompleter(debug);
            var startup = population.start();
            // Completion callbacks run on the DB owner. Poll from Paper's main thread instead.
            new BukkitRunnable() {
                @Override public void run() {
                    if (!startup.isDone()) return;
                    cancel();
                    try {
                        var status = startup.join();
                        observer = new VillageObserver(Civilizations.this, population, observerSettings);
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
