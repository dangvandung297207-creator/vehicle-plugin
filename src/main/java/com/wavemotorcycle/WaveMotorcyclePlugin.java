package com.wavemotorcycle;

import com.wavemotorcycle.commands.WaveCommand;
import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.events.EntityEventListener;
import com.wavemotorcycle.events.PlayerEventListener;
import com.wavemotorcycle.events.WorldEventListener;
import com.wavemotorcycle.motorcycle.Keys;
import com.wavemotorcycle.motorcycle.MotorcycleManager;
import com.wavemotorcycle.motorcycle.pack.ResourcePackManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * WaveMotorcycle - a fully rideable, physics-based Honda Wave-style motorcycle
 * for Paper 26.2.
 */
public final class WaveMotorcyclePlugin extends JavaPlugin {

    private ConfigManager cfg;
    private MotorcycleManager manager;
    private ResourcePackManager packManager;

    @Override
    public void onEnable() {
        Keys.init(this);
        saveDefaultConfig();
        cfg = new ConfigManager(this);
        cfg.load();

        manager = new MotorcycleManager(this, cfg);
        packManager = new ResourcePackManager(this, cfg);

        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityEventListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldEventListener(this), this);

        PluginCommand command = getCommand("wave");
        if (command != null) {
            WaveCommand executor = new WaveCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        manager.start();
        getLogger().info("WaveMotorcycle enabled - " + manager.size() + " bike(s) active.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        getLogger().info("WaveMotorcycle disabled.");
    }

    /** Reloads config.yml / messages.yml and applies changes to live bikes. */
    public void reloadAll() {
        cfg.load();
        manager.reload();
    }

    public ConfigManager cfg() {
        return cfg;
    }

    public MotorcycleManager manager() {
        return manager;
    }

    public ResourcePackManager packManager() {
        return packManager;
    }
}
