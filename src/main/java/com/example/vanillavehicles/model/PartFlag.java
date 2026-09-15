package com.example.vanillavehicles.model;

/**
 * Behaviour flags for model parts, consumed by the animator.
 */
public enum PartFlag {
    /** Spins with vehicle speed (wheel axle along local X). */
    WHEEL,
    /** Wheel also yaws with steering. */
    STEER,
    /** Track tread segment, scrolls while moving. */
    TRACK,
    /** Spinning rotor / propeller. */
    ROTOR,
    /** Yaws with the turret. */
    TURRET,
    /** Cannon barrel, follows turret yaw + look pitch. */
    BARREL,
    /** Headlight, swaps material with the light switch. */
    HEADLIGHT,
    /** Brake light, bright while braking. */
    BRAKELIGHT,
    /** Reverse light, bright while reversing. */
    REVERSELIGHT,
    /** Emergency flasher, alternates while the siren runs. */
    EMERGENCY,
    /** Generic channel-driven part (ladder, forks, bed, arm...). */
    ANIM
}
