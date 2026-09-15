package com.wavemotorcycle.motorcycle.input;

/**
 * Per-tick snapshot of the rider's input.
 *
 * <p>Keyboard detection without a client mod is limited to the states the vanilla
 * client reports to the server: the sprint state (holding SPACE), the sneak state
 * (holding SHIFT) and the player's view yaw (mouse look). The plugin maps these
 * onto motorcycle controls, see the README for the exact mapping.
 */
public final class MotorcycleInput {

    /** Empty input used for un-riden motorcycles. */
    public static final MotorcycleInput EMPTY = new MotorcycleInput();

    public boolean throttle;
    public boolean brake;
    /** True when the rider has a usable steering view. */
    public boolean hasSteer;
    /** Rider view yaw in degrees. */
    public double viewYaw;
    /** True when a wheelie trigger edge was detected this tick. */
    public boolean wheelieTap;

    private MotorcycleInput() {
    }

    public static MotorcycleInput of() {
        return new MotorcycleInput();
    }
}
