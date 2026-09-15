package com.wavemotorcycle;

import org.bukkit.plugin.java.JavaPlugin;

public final class WaveMotorcyclePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("WaveMotorcycle enabled (skeleton).");
    }

    @Override
    public void onDisable() {
        getLogger().info("WaveMotorcycle disabled.");
    }
}
