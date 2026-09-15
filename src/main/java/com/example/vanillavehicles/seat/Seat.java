package com.example.vanillavehicles.seat;

import org.joml.Vector3f;

/**
 * Seat definition: local offset plus the driver flag.
 * Runtime state lives in {@link SeatInstance}, never here, because
 * definitions are shared between all vehicles of a type.
 */
public class Seat {

    public final Vector3f offset;
    public final boolean driver;

    public Seat(Vector3f offset, boolean driver) {
        this.offset = offset;
        this.driver = driver;
    }
}
