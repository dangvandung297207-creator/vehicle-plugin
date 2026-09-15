package com.example.vanillavehicles.event;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a vehicle is destroyed. Cancelling keeps it alive with 1 health. */
public class VehicleDestroyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vehicle vehicle;
    private boolean cancelled;

    public VehicleDestroyEvent(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
