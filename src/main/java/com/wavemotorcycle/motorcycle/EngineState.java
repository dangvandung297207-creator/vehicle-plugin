package com.wavemotorcycle.motorcycle;

/**
 * State of the motorcycle engine.
 *
 * <ul>
 *   <li>{@link #OFF} - engine not running.</li>
 *   <li>{@link #STARTING} - short crank sequence after the rider mounts.</li>
 *   <li>{@link #RUNNING} - engine provides drive power.</li>
 *   <li>{@link #STOPPING} - idle-down sequence before the engine shuts off.</li>
 * </ul>
 */
public enum EngineState {
    OFF,
    STARTING,
    RUNNING,
    STOPPING
}
