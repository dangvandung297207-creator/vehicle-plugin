package com.example.vanillavehicles.storage;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Inventory holder binding a cargo inventory to its vehicle. */
public class CargoHolder implements InventoryHolder {

    private final Vehicle vehicle;
    private Inventory inventory;

    public CargoHolder(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
