package com.blueninjas24.civilizations;

import org.bukkit.plugin.java.JavaPlugin;
import com.blueninjas24.civilizations.listener.VillageDiscoveryListener;

public final class Civilizations extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(
                new VillageDiscoveryListener(),
                this
        );

        getLogger().info("Civilizations is awakening...");
    }

    @Override
    public void onDisable() {
        getLogger().info("Civilizations has been disabled.");
    }
}