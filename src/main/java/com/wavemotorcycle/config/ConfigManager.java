package com.wavemotorcycle.config;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.util.TextUtil;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Typed access to {@code config.yml} and {@code messages.yml}.
 * Reloaded by {@code /wave reload}.
 */
public final class ConfigManager {

    private final WaveMotorcyclePlugin plugin;

    // Motorcycle physics
    public double maxSpeed;
    public double acceleration;
    public double braking;
    public double friction;
    public double reverseSpeed;
    public double turnRate;
    public double gravity;
    public double maxSteerAngle;
    public double baseTraction;
    public int updateInterval;
    public double activationRadius;

    // Wheelie
    public boolean wheelieEnabled;
    public double wheelieLiftForce;
    public double wheelieMaxAngle;
    public double wheelieBalance;
    public int wheelieDuration;
    public double wheelieMinSpeed;

    // Lights
    public boolean headlightEnabled;
    public boolean rearLightEnabled;
    public boolean brakeLightEnabled;
    public boolean beamEnabled;
    public Particle beamParticle;
    public int beamInterval;

    // Fuel
    public boolean fuelEnabled;
    public double fuelCapacity;
    public double fuelPerSecond;
    public Material fuelItem;
    public double fuelItemAmount;
    public boolean consumeKeyOnSpawn;

    // Damage
    public boolean damageEnabled;
    public boolean collisionDamage;
    public boolean fallDamage;
    public boolean fireDamage;
    public Particle exhaustParticle;
    public boolean dustEnabled;
    public int dustInterval;

    // Rider / interaction
    public double seatCompensation;
    public double mountRange;
    public double dismountMaxSpeed;

    // Sounds (resolved later by the sound manager)
    public String soundEngineIdle;
    public String soundEngineLow;
    public String soundEngineHigh;
    public String soundEngineStart;
    public String soundEngineStop;
    public String soundBrake;
    public String soundCollision;
    public String soundWheelie;
    public String soundLight;
    public String soundLanding;
    public String soundDestroyed;
    public String soundRefuel;
    public int engineSoundInterval;

    // Resource pack
    public boolean packEnabled;
    public String packUrl;
    public String packSha1;
    public boolean packRequired;

    // Model
    public double modelYawOffset;
    public boolean mirrorX;

    // Surfaces
    public final Map<String, Double> surfaceTraction = new HashMap<>();

    private FileConfiguration messages;

    public ConfigManager(WaveMotorcyclePlugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads config.yml and messages.yml into typed fields. */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        maxSpeed = c.getDouble("motorcycle.max-speed", 0.75);
        acceleration = c.getDouble("motorcycle.acceleration", 0.025);
        braking = c.getDouble("motorcycle.braking", 0.08);
        friction = c.getDouble("motorcycle.friction", 0.015);
        reverseSpeed = c.getDouble("motorcycle.reverse-speed", 0.18);
        turnRate = c.getDouble("motorcycle.turn-rate", 0.35);
        gravity = c.getDouble("motorcycle.gravity", 0.08);
        maxSteerAngle = c.getDouble("motorcycle.steering-angle", 28.0);
        baseTraction = c.getDouble("motorcycle.traction", 1.0);
        updateInterval = Math.max(1, c.getInt("tick.update-interval", 1));
        activationRadius = c.getDouble("tick.activation-radius", 96.0);

        wheelieEnabled = c.getBoolean("wheelie.enabled", true);
        wheelieLiftForce = c.getDouble("wheelie.lift-force", 0.10);
        wheelieMaxAngle = c.getDouble("wheelie.max-angle", 28.0);
        wheelieBalance = c.getDouble("wheelie.balance", 0.015);
        wheelieDuration = Math.max(0, c.getInt("wheelie.duration", 0));
        wheelieMinSpeed = c.getDouble("wheelie.min-speed", 0.20);

        headlightEnabled = c.getBoolean("lights.headlight", true);
        rearLightEnabled = c.getBoolean("lights.rear-light", true);
        brakeLightEnabled = c.getBoolean("lights.brake-light", true);
        beamEnabled = c.getBoolean("lights.beam", true);
        beamParticle = parseParticle(c.getString("lights.beam-particle", "end_rod"), Particle.END_ROD);
        beamInterval = Math.max(1, c.getInt("lights.beam-interval", 2));

        fuelEnabled = c.getBoolean("fuel.enabled", true);
        fuelCapacity = c.getDouble("fuel.capacity", 100.0);
        fuelPerSecond = c.getDouble("fuel.consumption-per-second", 0.12);
        fuelItem = parseMaterial(c.getString("fuel.fuel-item", "glass_bottle"), Material.GLASS_BOTTLE);
        fuelItemAmount = c.getDouble("fuel.fuel-item-amount", 25.0);
        consumeKeyOnSpawn = c.getBoolean("spawn.consume-key", true);

        damageEnabled = c.getBoolean("damage.enabled", true);
        collisionDamage = c.getBoolean("damage.collision-damage", true);
        fallDamage = c.getBoolean("damage.fall-damage", true);
        fireDamage = c.getBoolean("damage.fire-damage", true);
        exhaustParticle = parseParticle(c.getString("effects.exhaust-particle", "smoke"), Particle.SMOKE);
        dustEnabled = c.getBoolean("effects.dust", true);
        dustInterval = Math.max(1, c.getInt("effects.dust-interval", 6));

        seatCompensation = c.getDouble("rider.seat-y-compensation", -0.45);
        mountRange = c.getDouble("interaction.mount-range", 2.6);
        dismountMaxSpeed = c.getDouble("interaction.dismount-max-speed", 0.35);

        soundEngineIdle = c.getString("sounds.engine_idle", "minecraft:block_note_block_bass");
        soundEngineLow = c.getString("sounds.engine_low", "minecraft:block_note_block_bass");
        soundEngineHigh = c.getString("sounds.engine_high", "minecraft:block_note_block_bass");
        soundEngineStart = c.getString("sounds.engine_start", "minecraft:block_lava_extinguish");
        soundEngineStop = c.getString("sounds.engine_stop", "minecraft:block_fire_extinguish");
        soundBrake = c.getString("sounds.brake", "minecraft:block_stone_hit");
        soundCollision = c.getString("sounds.collision", "minecraft:block_stone_hit");
        soundWheelie = c.getString("sounds.wheelie", "minecraft:block_note_block_hat");
        soundLight = c.getString("sounds.headlight_toggle", "minecraft:block_lever_click");
        soundLanding = c.getString("sounds.landing", "minecraft:entity_armor_stand_fall");
        soundDestroyed = c.getString("sounds.destroyed", "minecraft:entity_tnt_primed");
        soundRefuel = c.getString("sounds.refuel", "minecraft:entity_cow_milk");
        engineSoundInterval = Math.max(2, c.getInt("sounds.engine-pulse-interval", 8));

        packEnabled = c.getBoolean("resource-pack.enabled", true);
        packUrl = c.getString("resource-pack.url", "");
        packSha1 = c.getString("resource-pack.sha1", "");
        packRequired = c.getBoolean("resource-pack.required", false);

        modelYawOffset = c.getDouble("model.yaw-offset-degrees", 0.0);
        mirrorX = c.getBoolean("model.mirror-x", false);

        surfaceTraction.clear();
        org.bukkit.configuration.ConfigurationSection surfaces = c.getConfigurationSection("surfaces");
        if (surfaces != null) {
            for (String key : surfaces.getKeys(false)) {
                surfaceTraction.put(key.toLowerCase(Locale.ROOT), surfaces.getDouble(key, 1.0));
            }
        }
        if (surfaceTraction.isEmpty()) {
            surfaceTraction.put("grass_block", 0.85);
            surfaceTraction.put("dirt", 0.90);
            surfaceTraction.put("stone", 1.00);
            surfaceTraction.put("ice", 0.35);
            surfaceTraction.put("packed_ice", 0.50);
            surfaceTraction.put("soul_sand", 0.60);
            surfaceTraction.put("soul_soil", 0.60);
            surfaceTraction.put("sand", 0.70);
            surfaceTraction.put("gravel", 0.75);
        }

        // Messages
        plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "messages.yml"));
    }

    /** Returns a message from messages.yml with {@code &} color codes. */
    public String msg(String path) {
        String s = messages.getString(path, path);
        return s.replace("<bike>", "Wave Motorcycle");
    }

    public Component msgC(String path) {
        return TextUtil.color(msg(path));
    }

    public static Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static Particle parseParticle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
