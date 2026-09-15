package com.example.vanillavehicles.api;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleDefinition;
import com.example.vanillavehicles.vehicle.VehicleType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Public API for other plugins. All methods are null-safe and return
 * empty results when the plugin is disabled.
 */
public final class VanillaVehiclesAPI {

    private VanillaVehiclesAPI() {
    }

    public static boolean isAvailable() {
        return VanillaVehicles.getInstance() != null;
    }

    /** Spawns a vehicle of the given type id. Returns null when denied. */
    public static Vehicle spawnVehicle(Player owner, String typeId, Location location) {
        if (!isAvailable()) {
            return null;
        }
        VehicleType type = VehicleType.byId(typeId);
        if (type == null) {
            return null;
        }
        return VanillaVehicles.getInstance().getVehicleManager().spawn(type, location, owner);
    }

    public static boolean removeVehicle(UUID vehicleId) {
        if (!isAvailable() || vehicleId == null) {
            return false;
        }
        for (Vehicle vehicle : VanillaVehicles.getInstance().getVehicleManager().getAll()) {
            if (vehicle.getId().equals(vehicleId)) {
                vehicle.removeQuiet();
                return true;
            }
        }
        return false;
    }

    /** Returns the vehicle a player is currently riding, or null. */
    public static Vehicle getVehicle(Player player) {
        if (!isAvailable()) {
            return null;
        }
        return VanillaVehicles.getInstance().getVehicleManager().getVehicleOf(player);
    }

    /** Returns the vehicle an entity belongs to, or null. */
    public static Vehicle getVehicle(Entity entity) {
        if (!isAvailable()) {
            return null;
        }
        return VanillaVehicles.getInstance().getVehicleManager().getVehicleOf(entity);
    }

    public static Collection<Vehicle> getVehicles() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        return VanillaVehicles.getInstance().getVehicleManager().getAll();
    }

    public static VehicleDefinition getDefinition(String typeId) {
        if (!isAvailable()) {
            return null;
        }
        return VanillaVehicles.getInstance().getRegistry().get(typeId);
    }

    public static boolean isVehicleEntity(Entity entity) {
        return DisplayFactory.isVehicleEntity(entity);
    }
}
