package com.blueninjas24.civilizations;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.paper.PopulationConfiguration;
import com.blueninjas24.civilizations.paper.PopulationDebugCommand;
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

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            var config = new YamlConfiguration();
            config.load(new File(getDataFolder(), "config.yml"));
            var settings = PopulationConfiguration.read(config);
            population = new PopulationSession(getDataFolder(), getLogger(), settings, System::currentTimeMillis);
            var debug = new PopulationDebugCommand(population);
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
        if (population != null) population.close();
        getLogger().info("Civilizations has been disabled.");
    }

    private void failStartup(Throwable failed) {
        getLogger().log(Level.SEVERE, "Civilizations startup failed; saved data has not been reset", failed);
        getServer().getPluginManager().disablePlugin(this);
    }
}
