package com.blueninjas24.civilizations;

import org.bukkit.plugin.java.JavaPlugin;

public final class Civilizations extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Civilizations is awakening...");
    }

    @Override
    public void onDisable() {
        getLogger().info("Civilizations has been disabled.");
    }
}