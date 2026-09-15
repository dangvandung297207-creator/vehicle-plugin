package com.example.vanillavehicles.event;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a vehicle is spawned. Cancelling removes the vehicle again. */
public class VehicleSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vehicle vehicle;
    private final Player player;
    private boolean cancelled;

    public VehicleSpawnEvent(Vehicle vehicle, Player player) {
        this.vehicle = vehicle;
        this.player = player;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    /** The player who spawned the vehicle, may be null (console, garage, restore). */
    public Player getPlayer() {
        return player;
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
