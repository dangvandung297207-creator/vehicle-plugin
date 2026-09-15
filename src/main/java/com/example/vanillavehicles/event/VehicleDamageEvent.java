package com.example.vanillavehicles.event;

import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a vehicle takes damage (crash, collision, commands). Cancellable. */
public class VehicleDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vehicle vehicle;
    private final Player damager;
    private double damage;
    private boolean cancelled;

    public VehicleDamageEvent(Vehicle vehicle, Player damager, double damage) {
        this.vehicle = vehicle;
        this.damager = damager;
        this.damage = damage;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    /** The responsible player, or null for crashes and the environment. */
    public Player getDamager() {
        return damager;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = Math.max(0.0, damage);
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
