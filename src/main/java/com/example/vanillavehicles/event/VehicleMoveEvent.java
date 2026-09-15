package com.example.vanillavehicles.event;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a vehicle changes position. This event is intentionally NOT
 * cancellable because movement is already applied when it fires.
 */
public class VehicleMoveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vehicle vehicle;
    private final Location from;
    private final Location to;

    public VehicleMoveEvent(Vehicle vehicle, Location from, Location to) {
        this.vehicle = vehicle;
        this.from = from;
        this.to = to;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
