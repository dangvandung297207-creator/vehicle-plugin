package com.wavemotorcycle.util;

/**
 * Small math helpers shared across the plugin.
 *
 * <p>Coordinate convention used by the motorcycle system: the chassis origin is the
 * ground contact point at the middle of the wheelbase, {@code +Z} is the bike's
 * forward direction, {@code +Y} is up and {@code +X} is the lateral axis.
 * Yaw follows Minecraft conventions (0 = south/+Z, 90 = west/-X).
 */
public final class MathUtil {

    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    private MathUtil() {
    }

    /** Wraps an angle in degrees to the range (-180, 180]. */
    public static double wrap180(double degrees) {
        double d = degrees % 360.0;
        if (d > 180.0) {
            d -= 360.0;
        }
        if (d <= -180.0) {
            d += 360.0;
        }
        return d;
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** X component of a Minecraft yaw direction (0 deg = +Z south, 90 deg = -X west). */
    public static double dirX(double yawDeg) {
        return -Math.sin(yawDeg * DEG_TO_RAD);
    }

    /** Z component of a Minecraft yaw direction. */
    public static double dirZ(double yawDeg) {
        return Math.cos(yawDeg * DEG_TO_RAD);
    }

    /** Rotates a point in the local X/Z plane by a Minecraft yaw. */
    public static double rotX(double x, double z, double yawDeg) {
        double r = yawDeg * DEG_TO_RAD;
        return x * Math.cos(r) - z * Math.sin(r);
    }

    /** Rotates a point in the local X/Z plane by a Minecraft yaw. */
    public static double rotZ(double x, double z, double yawDeg) {
        double r = yawDeg * DEG_TO_RAD;
        return x * Math.sin(r) + z * Math.cos(r);
    }

    /**
     * Pitch rotation around the local X axis where a positive angle lifts the +Z (front)
     * end of the bike up. Returns the new Y.
     */
    public static double pitchY(double y, double z, double pitchDeg) {
        double r = pitchDeg * DEG_TO_RAD;
        return y * Math.cos(r) + z * Math.sin(r);
    }

    /** Companion of {@link #pitchY(double, double, double)} returning the new Z. */
    public static double pitchZ(double y, double z, double pitchDeg) {
        double r = pitchDeg * DEG_TO_RAD;
        return -y * Math.sin(r) + z * Math.cos(r);
    }

    public static boolean close(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }
}
