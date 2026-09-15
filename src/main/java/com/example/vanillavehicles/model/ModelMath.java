package com.example.vanillavehicles.model;

import org.bukkit.entity.Display;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * All rotation math for the model engine in one place.
 *
 * <p>Local space: +X = vehicle left, +Y = up, +Z = forward. Entity yaw/pitch
 * rotate translations automatically, so part entities are simply teleported to
 * the vehicle origin and the local offset lives in the transformation.</p>
 *
 * <p>Roll (lean/bank) is baked into translations manually because entities
 * have no roll axis: world = Ry(-yaw) * Rx(pitch) * Rz(roll) * local.</p>
 */
public final class ModelMath {

    private ModelMath() {
    }

    /** Identity rotation, reused to avoid allocations. */
    public static AxisAngle4f identity() {
        return new AxisAngle4f(0f, 0f, 1f, 0f);
    }

    /**
     * Transforms a local offset into a world offset.
     *
     * @param yawDeg   entity yaw in degrees (vehicle heading)
     * @param pitchDeg entity pitch in degrees (Minecraft convention: negative looks up)
     * @param rollDeg  bank around the forward axis in degrees (positive banks right)
     */
    public static Vector3f toWorld(Vector3f local, double yawDeg, double pitchDeg, double rollDeg) {
        double lx = local.x;
        double ly = local.y;
        double lz = local.z;

        // Roll about local Z (forward).
        double r = Math.toRadians(rollDeg);
        double cosR = Math.cos(r);
        double sinR = Math.sin(r);
        double x1 = lx * cosR - ly * sinR;
        double y1 = lx * sinR + ly * cosR;
        double z1 = lz;

        // Pitch about X.
        double p = Math.toRadians(pitchDeg);
        double cosP = Math.cos(p);
        double sinP = Math.sin(p);
        double y2 = y1 * cosP - z1 * sinP;
        double z2 = y1 * sinP + z1 * cosP;

        // Yaw: world = Ry(-yaw) * v.
        double yw = Math.toRadians(yawDeg);
        double cosY = Math.cos(yw);
        double sinY = Math.sin(yw);
        double x3 = x1 * cosY - z2 * sinY;
        double z3 = x1 * sinY + z2 * cosY;

        return new Vector3f((float) x3, (float) y2, (float) z3);
    }

    /**
     * Applies only the roll component (rotation about local Z / forward).
     * Used for part translations, because entity yaw/pitch already rotate
     * translations but entities have no roll axis.
     */
    public static Vector3f applyRoll(Vector3f local, double rollDeg) {
        if (rollDeg == 0) {
            return new Vector3f(local);
        }
        double r = Math.toRadians(rollDeg);
        double cosR = Math.cos(r);
        double sinR = Math.sin(r);
        return new Vector3f(
                (float) (local.x * cosR - local.y * sinR),
                (float) (local.x * sinR + local.y * cosR),
                local.z);
    }

    /** Horizontal forward vector for a heading, matching Bukkit's convention. */
    public static Vector forward(double yawDeg) {
        double rad = Math.toRadians(yawDeg);
        return new Vector(-Math.sin(rad), 0, Math.cos(rad));
    }

    /** World-space right vector for a heading. */
    public static Vector right(double yawDeg) {
        double rad = Math.toRadians(yawDeg);
        return new Vector(-Math.cos(rad), 0, -Math.sin(rad));
    }

    /**
     * Composes yaw/pitch/roll (radians, applied yaw -&gt; pitch -&gt; roll) into
     * a single axis-angle rotation using dependency-free quaternion math.
     */
    public static AxisAngle4f eulerToAxisAngle(double yawRad, double pitchRad, double rollRad) {
        double cy = Math.cos(yawRad * 0.5);
        double sy = Math.sin(yawRad * 0.5);
        double cx = Math.cos(pitchRad * 0.5);
        double sx = Math.sin(pitchRad * 0.5);
        double cz = Math.cos(rollRad * 0.5);
        double sz = Math.sin(rollRad * 0.5);

        // q = qY * qX * qZ
        // qY = (0, sy, 0, cy), qX = (sx, 0, 0, cx), qZ = (0, 0, sz, cz)
        double x1 = cy * sx;
        double y1 = sy * cx;
        double z1 = -sy * sx;
        double w1 = cy * cx;

        double x = w1 * 0 + x1 * cz + y1 * sz - z1 * 0;
        double y = w1 * 0 - x1 * sz + y1 * cz + z1 * 0;
        double z = w1 * sz + x1 * 0 - y1 * 0 + z1 * cz;
        double w = w1 * cz - x1 * 0 - y1 * 0 - z1 * sz;

        double len = Math.sqrt(x * x + y * y + z * z + w * w);
        if (len < 1e-9) {
            return identity();
        }
        x /= len;
        y /= len;
        z /= len;
        w = Math.max(-1.0, Math.min(1.0, w / len));

        double angle = 2.0 * Math.acos(w);
        double s = Math.sqrt(Math.max(0.0, 1.0 - w * w));
        if (s < 1e-6 || angle < 1e-6) {
            return identity();
        }
        return new AxisAngle4f((float) angle, (float) (x / s), (float) (y / s), (float) (z / s));
    }

    /** Rotates a vector by yaw/pitch/roll radians (same order as above). */
    public static Vector3f rotateVector(Vector3f v, double yawRad, double pitchRad, double rollRad) {
        // Build the same quaternion as eulerToAxisAngle, then rotate the vector.
        double cy = Math.cos(yawRad * 0.5);
        double sy = Math.sin(yawRad * 0.5);
        double cx = Math.cos(pitchRad * 0.5);
        double sx = Math.sin(pitchRad * 0.5);
        double cz = Math.cos(rollRad * 0.5);
        double sz = Math.sin(rollRad * 0.5);

        double x1 = cy * sx;
        double y1 = sy * cx;
        double z1 = -sy * sx;
        double w1 = cy * cx;

        double qx = x1 * cz + y1 * sz;
        double qy = -x1 * sz + y1 * cz;
        double qz = w1 * sz + z1 * cz;
        double qw = w1 * cz - z1 * sz;

        // v' = q * v * q^-1
        double vx = v.x;
        double vy = v.y;
        double vz = v.z;
        double tx = 2.0 * (qy * vz - qz * vy);
        double ty = 2.0 * (qz * vx - qx * vz);
        double tz = 2.0 * (qx * vy - qy * vx);
        return new Vector3f(
                (float) (vx + qw * tx + (qy * tz - qz * ty)),
                (float) (vy + qw * ty + (qz * tx - qx * tz)),
                (float) (vz + qw * tz + (qx * ty - qy * tx)));
    }

    /** Transformation for a static axis-aligned block part. */
    public static Display.Transformation blockTransform(Vector3f center, Vector3f size) {
        Vector3f translation = new Vector3f(
                center.x - size.x / 2f,
                center.y - size.y / 2f,
                center.z - size.z / 2f);
        return new Display.Transformation(translation, identity(),
                new Vector3f(size), identity());
    }

    /** Transformation for a (possibly rotated) centered item/text part. */
    public static Display.Transformation orientedTransform(Vector3f center, AxisAngle4f leftRotation,
                                                           Vector3f size) {
        return new Display.Transformation(new Vector3f(center), leftRotation,
                new Vector3f(size), identity());
    }

    public static double wrapDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double from, double to, double factor) {
        return from + (to - from) * clamp(factor, 0.0, 1.0);
    }

    /** Frame-rate independent approach helper. */
    public static double approach(double current, double target, double maxDelta) {
        if (current < target) {
            return Math.min(target, current + maxDelta);
        }
        return Math.max(target, current - maxDelta);
    }
}
