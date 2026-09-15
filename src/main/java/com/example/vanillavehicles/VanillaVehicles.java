package com.example.vanillavehicles;

import com.example.vanillavehicles.command.VehicleCommand;
import com.example.vanillavehicles.config.PluginConfig;
import com.example.vanillavehicles.entity.VehicleTags;
import com.example.vanillavehicles.input.InputManager;
import com.example.vanillavehicles.sound.SoundManager;
import com.example.vanillavehicles.storage.GarageManager;
import com.example.vanillavehicles.storage.VehicleStorage;
import com.example.vanillavehicles.vehicle.VehicleDefinitions;
import com.example.vanillavehicles.vehicle.VehicleManager;
import com.example.vanillavehicles.vehicle.VehicleRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main plugin class for VanillaVehicles.
 *
 * <p>Boot order: config -&gt; tags -&gt; registry (all 34 vehicle definitions)
 * -&gt; storage -&gt; managers -&gt; command -&gt; listeners -&gt; central tick
 * task -&gt; orphan cleanup -&gt; persistence restore.</p>
 */
public final class VanillaVehicles extends JavaPlugin {

    private static VanillaVehicles instance;

    private PluginConfig pluginConfig;
    private VehicleRegistry registry;
    private VehicleManager vehicleManager;
    private InputManager inputManager;
    private SoundManager soundManager;
    private VehicleStorage storage;
    private GarageManager garageManager;
    private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();

    public static VanillaVehicles getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        VehicleTags.init(this);

        pluginConfig = new PluginConfig(this);
        pluginConfig.reload();

        registry = new VehicleRegistry();
        VehicleDefinitions.registerAll(registry);

        storage = new VehicleStorage(this);
        vehicleManager = new VehicleManager(this, registry);
        inputManager = new InputManager(this);
        soundManager = new SoundManager(this);
        garageManager = new GarageManager(this);

        VehicleCommand command = new VehicleCommand(this);
        PluginCommand pluginCommand = getCommand("vehicle");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'vehicle' is missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(vehicleManager, this);
        getServer().getPluginManager().registerEvents(garageManager, this);
        inputManager.init();
        vehicleManager.start();

        // Remove entities left behind by crashes/kills, then respawn saved vehicles.
        storage.cleanupOrphans();
        vehicleManager.loadPersisted();

        getLogger().info("VanillaVehicles enabled with " + registry.size() + " vehicle types.");
    }

    @Override
    public void onDisable() {
        try {
            if (vehicleManager != null && storage != null) {
                storage.saveAll(vehicleManager.getAll());
                vehicleManager.removeAllSilent();
            }
        } catch (Exception ex) {
            getLogger().warning("Error while saving vehicles: " + ex.getMessage());
        }
        getServer().getScheduler().cancelTasks(this);
        instance = null;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public VehicleRegistry getRegistry() {
        return registry;
    }

    public VehicleManager getVehicleManager() {
        return vehicleManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public VehicleStorage getStorage() {
        return storage;
    }

    public GarageManager getGarageManager() {
        return garageManager;
    }

    /** Toggles debug output for a player. Returns the new state (true = enabled). */
    public boolean toggleDebug(Player player) {
        UUID uuid = player.getUniqueId();
        if (!debugPlayers.remove(uuid)) {
            debugPlayers.add(uuid);
            return true;
        }
        return false;
    }

    public boolean isDebug(Player player) {
        return player != null && debugPlayers.contains(player.getUniqueId());
    }
}
