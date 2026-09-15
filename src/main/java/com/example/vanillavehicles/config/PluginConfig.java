package com.example.vanillavehicles.config;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.vehicle.VehicleStats;
import com.example.vanillavehicles.vehicle.VehicleType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Reads config.yml. Per-vehicle sections overlay the built-in defaults;
 * every vehicle instance receives its own copy.
 */
public class PluginConfig {

    private final VanillaVehicles plugin;
    private final Map<VehicleType, VehicleStats> cache = new EnumMap<>(VehicleType.class);

    public PluginConfig(VanillaVehicles plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        cache.clear();
    }

    /** Returns a fresh, fully resolved stats object for the given type. */
    public VehicleStats statsFor(VehicleType type) {
        VehicleStats base = cache.get(type);
        if (base == null) {
            base = new VehicleStats(type);
            applyOverrides(type, base);
            cache.put(type, base);
        }
        return new VehicleStats(base);
    }

    private void applyOverrides(VehicleType type, VehicleStats stats) {
        FileConfiguration config = plugin.getConfig();
        String path = "vehicles." + type.getId() + ".";
        stats.enabled = config.getBoolean(path + "enabled", stats.enabled);
        stats.permission = config.getString(path + "permission", stats.permission);
        stats.maxSpeed = config.getDouble(path + "max-speed", stats.maxSpeed);
        stats.acceleration = config.getDouble(path + "acceleration", stats.acceleration);
        stats.brake = config.getDouble(path + "brake", stats.brake);
        stats.reverseSpeed = config.getDouble(path + "reverse-speed", stats.reverseSpeed);
        stats.steering = config.getDouble(path + "steering", stats.steering);
        stats.friction = config.getDouble(path + "friction", stats.friction);
        stats.maxHealth = config.getDouble(path + "health", stats.maxHealth);
        stats.modelScale = config.getDouble(path + "model-scale", stats.modelScale);
        stats.turnInPlace = config.getBoolean(path + "turn-in-place", stats.turnInPlace);
        stats.stepUp = config.getBoolean(path + "step-up", stats.stepUp);
        stats.leanFactor = config.getDouble(path + "lean-factor", stats.leanFactor);
        stats.drift = config.getBoolean(path + "drift", stats.drift);
        stats.burnout = config.getBoolean(path + "burnout", stats.burnout);
        stats.boostEnabled = config.getBoolean(path + "boost-enabled", stats.boostEnabled);
        stats.boostMultiplier = config.getDouble(path + "boost-multiplier", stats.boostMultiplier);
        stats.boostDurationTicks = (int) (config.getDouble(path + "boost-duration-seconds",
                stats.boostDurationTicks / 20.0) * 20.0);
        stats.boostCooldownTicks = (int) (config.getDouble(path + "boost-cooldown-seconds",
                stats.boostCooldownTicks / 20.0) * 20.0);
        stats.siren = config.getBoolean(path + "siren", stats.siren);
        stats.cargoSize = config.getInt(path + "cargo-size", stats.cargoSize);
        stats.carriages = config.getInt(path + "carriages", stats.carriages);
        stats.engineSound = config.getString(path + "engine-sound", stats.engineSound);
        stats.enginePitch = (float) config.getDouble(path + "engine-pitch", stats.enginePitch);
        stats.hornSound = config.getString(path + "horn-sound", stats.hornSound);
        stats.brakeSound = config.getString(path + "brake-sound", stats.brakeSound);
        stats.crashSound = config.getString(path + "crash-sound", stats.crashSound);
        stats.sirenSound = config.getString(path + "siren-sound", stats.sirenSound);
        stats.boostSound = config.getString(path + "boost-sound", stats.boostSound);
        stats.whistleSound = config.getString(path + "whistle-sound", stats.whistleSound);
    }

    public boolean isMouseSteering() {
        return plugin.getConfig().getBoolean("settings.mouse-steering", true);
    }

    public int getMaxVehiclesPerPlayer() {
        return plugin.getConfig().getInt("settings.max-vehicles-per-player", 3);
    }

    public int getMaxVehiclesTotal() {
        return plugin.getConfig().getInt("settings.max-vehicles-total", 60);
    }

    public boolean isConsumeSpawner() {
        return plugin.getConfig().getBoolean("settings.consume-spawner-item", true);
    }

    public int getSaveIntervalMinutes() {
        return plugin.getConfig().getInt("settings.save-interval-minutes", 5);
    }

    public double getTaxiRate() {
        return plugin.getConfig().getDouble("settings.taxi-rate-per-block", 0.5);
    }

    public String getGarageTitle() {
        return plugin.getConfig().getString("settings.garage-title", "Vehicle Garage");
    }
}
