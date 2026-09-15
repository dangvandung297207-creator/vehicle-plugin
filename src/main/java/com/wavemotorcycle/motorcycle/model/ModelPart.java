package com.wavemotorcycle.motorcycle.model;

/**
 * The individual display parts that make up the 3D motorcycle.
 *
 * <p>Each part is a separate {@link org.bukkit.entity.ItemDisplay} rendering its own
 * blockbench model from the resource pack, so parts can be transformed
 * independently (wheel spin, steering, wheelie lift).
 *
 * <p>Local anchor points are in chassis coordinates (chassis origin = ground at the
 * middle of the wheelbase, +Z forward, +Y up, in blocks). The anchor is the point
 * the part's model is authored around; all part models are authored with the
 * ground at y=0 and the bike facing +Z.
 */
public enum ModelPart {

    /** Main frame, underbone, engine, seat, tail, exhaust. */
    BODY(0, 0, 0, 1, false, false),
    /** Front fork and front fender. */
    FRONT_ASSEMBLY(0, 0, 0.675, 1, false, true),
    /** Handlebar, grips and mirrors (steers a bit faster than the fork). */
    HANDLEBAR(0, 0, 0.675, 1.5, false, true),
    /** Headlight housing + lens (has off/on model variants). */
    HEADLIGHT(0, 0, 0.675, 1, false, true),
    /** Rear light (dim / brake model variants). */
    REAR_LIGHT(0, 0, -0.675, 1, false, false),
    /** Front wheel (steers and spins). */
    FRONT_WHEEL(0, 0.33, 0.675, 1, true, true),
    /** Rear wheel (spins only; it is the wheelie pivot axle). */
    REAR_WHEEL(0, 0.33, -0.675, 1, true, false);

    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    /** Steering multiplier applied to the bike steering angle (1 = fork, 1.5 = handlebar). */
    private final double steerMultiplier;
    /** Whether this part spins with the wheels. */
    private final boolean spins;
    /** Whether this part follows the steering rotation. */
    private final boolean steers;

    ModelPart(double anchorX, double anchorY, double anchorZ,
              double steerMultiplier, boolean spins, boolean steers) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.steerMultiplier = steerMultiplier;
        this.spins = spins;
        this.steers = steers;
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }

    public double steerMultiplier() {
        return steerMultiplier;
    }

    public boolean spins() {
        return spins;
    }

    public boolean steers() {
        return steers;
    }
}
