package com.blueninjas24.civilizations;

import com.blueninjas24.civilizations.database.DatabaseManager;
import com.blueninjas24.civilizations.listener.VillageDiscoveryListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class Civilizations extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);

        try {
            databaseManager.initialize();
            getLogger().info("Civilizations database initialized.");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize Civilizations database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(
                new VillageDiscoveryListener(this),
                this
        );

        getLogger().info("Civilizations is awakening...");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("Civilizations has been disabled.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}