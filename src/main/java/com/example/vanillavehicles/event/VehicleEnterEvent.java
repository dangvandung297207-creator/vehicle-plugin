package com.example.vanillavehicles.event;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player enters a vehicle. Cancellable. */
public class VehicleEnterEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vehicle vehicle;
    private final Player player;
    private final int seatIndex;
    private boolean cancelled;

    public VehicleEnterEvent(Vehicle vehicle, Player player, int seatIndex) {
        this.vehicle = vehicle;
        this.player = player;
        this.seatIndex = seatIndex;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Player getPlayer() {
        return player;
    }

    public int getSeatIndex() {
        return seatIndex;
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
