package com.example.vanillavehicles.storage;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleState;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persists active vehicles to vehicles.yml and removes orphaned entities
 * left behind by crashes or kills. Never touches untagged entities.
 */
public class VehicleStorage {

    public static class Record {
        public String typeId;
        public String world;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public UUID owner;
        public String ownerName;
        public double health;
        public List<ItemStack> cargo = new ArrayList<>();
    }

    private final VanillaVehicles plugin;
    private final File file;

    public VehicleStorage(VanillaVehicles plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vehicles.yml");
    }

    public void saveAll(Collection<Vehicle> vehicles) throws IOException {
        YamlConfiguration data = new YamlConfiguration();
        int index = 0;
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getState() != VehicleState.ACTIVE || vehicle.getWorld() == null) {
                continue;
            }
            String path = "vehicles." + index + ".";
            data.set(path + "type", vehicle.getType().getId());
            data.set(path + "world", vehicle.getWorld().getName());
            data.set(path + "x", vehicle.getLocation().getX());
            data.set(path + "y", vehicle.getLocation().getY());
            data.set(path + "z", vehicle.getLocation().getZ());
            data.set(path + "yaw", vehicle.getHeading());
            data.set(path + "owner", vehicle.getOwner() == null ? null : vehicle.getOwner().toString());
            data.set(path + "owner-name", vehicle.getOwnerName());
            data.set(path + "health", vehicle.getHealth());
            List<ItemStack> cargo = vehicle.getCargoContents();
            if (!cargo.isEmpty()) {
                data.set(path + "cargo", cargo);
            }
            index++;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        data.save(file);
    }

    public List<Record> loadAll() {
        List<Record> records = new ArrayList<>();
        if (!file.exists()) {
            return records;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("vehicles");
        if (section == null) {
            return records;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            Record record = new Record();
            record.typeId = node.getString("type");
            record.world = node.getString("world");
            if (record.typeId == null || record.world == null) {
                continue;
            }
            record.x = node.getDouble("x");
            record.y = node.getDouble("y");
            record.z = node.getDouble("z");
            record.yaw = (float) node.getDouble("yaw");
            String owner = node.getString("owner");
            try {
                record.owner = owner == null ? null : UUID.fromString(owner);
            } catch (IllegalArgumentException ex) {
                record.owner = null;
            }
            record.ownerName = node.getString("owner-name", "Console");
            record.health = node.getDouble("health", 100);
            List<?> cargo = node.getList("cargo");
            if (cargo != null) {
                for (Object entry : cargo) {
                    if (entry instanceof ItemStack) {
                        record.cargo.add((ItemStack) entry);
                    }
                }
            }
            records.add(record);
        }
        return records;
    }

    /** Removes every entity tagged by this plugin (orphan cleanup on boot). */
    public void cleanupOrphans() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (DisplayFactory.isVehicleEntity(entity)) {
                    try {
                        entity.remove();
                        removed++;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " orphaned vehicle entities.");
        }
    }
}
