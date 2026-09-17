package com.wave100.entity;

/**
 * Wheelie state machine states.
 */
public enum WheelieState {
    /** Front wheel on the ground, normal riding. */
    NORMAL,
    /** Front wheel is being lifted. */
    LIFTING,
    /** Balanced on the rear wheel. */
    WHEELIE,
    /** Front wheel is coming back down. */
    LOWERING,
    /** Lost balance - the motorcycle has crashed. */
    CRASH
}
