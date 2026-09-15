package com.example.vanillavehicles.vehicle;

/** Physics profile driving how a vehicle moves. */
public enum PhysicsType {
    /** Normal road cars and vans. */
    CAR,
    /** Two wheelers with leaning. */
    BIKE,
    /** Slow, massive road vehicles. */
    HEAVY,
    /** Watercraft with buoyancy. */
    BOAT,
    /** Arcade aircraft (hover / throttle flight). */
    AIRCRAFT,
    /** Rail vehicles with persistent throttle, minimal steering. */
    TRAIN,
    /** Slow work machines, some can turn in place. */
    CONSTRUCTION
}
