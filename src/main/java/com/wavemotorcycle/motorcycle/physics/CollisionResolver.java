package com.wavemotorcycle.motorcycle.physics;

import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.util.MathUtil;
import org.bukkit.Material;

/**
 * Custom bounding-box collision between the motorcycle and the block world.
 *
 * <p>The bike is approximated by its yaw-aligned bounding box (length, width and
 * height defined on {@link MotorcycleController}). Movement is sub-stepped so that
 * fast bikes cannot tunnel through walls, and any solid block intersecting the
 * box at one of several sample heights stops the bike.
 */
public final class CollisionResolver {

    /** Half of the bounding box length (along the bike heading). */
    public static final double HALF_LENGTH = 1.15;
    /** Half of the bounding box width. */
    public static final double HALF_WIDTH = 0.38;
    /** Sample heights above the ground used for the collision box. */
    private static final double[] SAMPLE_HEIGHTS = {0.12, 0.45, 0.85, 1.25};

    /**
     * Attempts to move the bike by (dx, dz). On collision the bike is left at the
     * last clear sub-step position and {@code true} is returned.
     */
    public boolean move(MotorcycleController c, double dx, double dz) {
        double dist = Math.hypot(dx, dz);
        if (dist <= 1.0E-5) {
            return false;
        }
        int steps = (int) Math.ceil(dist / 0.22);
        for (int i = 1; i <= steps; i++) {
            double f = (double) i / steps;
            double nx = c.x() + dx * f;
            double nz = c.z() + dz * f;
            if (collides(c, nx, nz, c.y())) {
                // Stay at the previous sub-step (or the start when i == 1).
                double pf = (double) (i - 1) / steps;
                c.setPosition(c.x() + dx * pf, c.y(), c.z() + dz * pf);
                return true;
            }
        }
        c.setPosition(c.x() + dx, c.y(), c.z() + dz);
        return false;
    }

    private boolean collides(MotorcycleController c, double cx, double cz, double cy) {
        double y = c.yaw();
        for (double lz : new double[]{-HALF_LENGTH, HALF_LENGTH}) {
            for (double lx : new double[]{-HALF_WIDTH, HALF_WIDTH}) {
                double wx = cx + MathUtil.rotX(lx, lz, y);
                double wz = cz + MathUtil.rotZ(lx, lz, y);
                int bx = (int) Math.floor(wx);
                int bz = (int) Math.floor(wz);
                for (double h : SAMPLE_HEIGHTS) {
                    Material m = c.world().getBlockAt(bx, (int) Math.floor(cy + h), bz).getType();
                    if (m.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
