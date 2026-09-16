package com.wave100.entity;

/**
 * Engine lifecycle states.
 */
public enum EngineState {
    /** Completely off, no sound, no fuel use. */
    OFF,
    /** Starter motor cranking (short animation + sound). */
    STARTING,
    /** Running, no throttle: idles at low RPM. */
    IDLE,
    /** Running with throttle: RPM follows speed and load. */
    RUNNING,
    /** Spinning down after being switched off. */
    STOPPING;

    public boolean isOn() {
        return this == IDLE || this == RUNNING || this == STARTING;
    }
}
