package com.example.vanillavehicles.seat;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Runtime seat: definition plus the invisible armor stand riders sit on. */
public class SeatInstance {

    public final Seat seat;
    public final int index;
    public ArmorStand stand;

    public SeatInstance(Seat seat, int index) {
        this.seat = seat;
        this.index = index;
    }

    /** Returns the player currently sitting here, or null. */
    public Player getRider() {
        if (stand == null || !stand.isValid()) {
            return null;
        }
        for (Entity passenger : stand.getPassengers()) {
            if (passenger instanceof Player) {
                return (Player) passenger;
            }
        }
        return null;
    }

    public boolean isFree() {
        return getRider() == null;
    }
}
