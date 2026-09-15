package com.example.vanillavehicles.collision;

import com.example.vanillavehicles.model.ModelMath;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.joml.Vector3f;

/**
 * Lightweight collision sampling. Only a handful of block lookups per
 * vehicle per tick - no ray tracing, no AABB sweeps.
 */
public final class CollisionHandler {

    private CollisionHandler() {
    }

    /**
     * Tests a yaw-aligned box against solid blocks. Liquid never collides.
     *
     * @param halfLen half length (local Z), halfW half width (local X)
     */
    public static boolean collides(World world, double x, double y, double z,
                                   double halfLen, double halfW, double height, double yawDeg) {
        if (world == null) {
            return false;
        }
        double[] heights = {0.15, height * 0.55, Math.max(0.3, height - 0.15)};
        double[][] corners = {
                {halfW, halfLen}, {-halfW, halfLen},
                {halfW, -halfLen}, {-halfW, -halfLen},
                {0, halfLen}, {0, -halfLen}, {0, 0}
        };
        for (double h : heights) {
            for (double[] corner : corners) {
                Vector3f world3 = ModelMath.toWorld(
                        new Vector3f((float) corner[0], 0f, (float) corner[1]), yawDeg, 0, 0);
                Block block = world.getBlockAt(
                        (int) Math.floor(x + world3.x),
                        (int) Math.floor(y + h),
                        (int) Math.floor(z + world3.z));
                if (!block.isPassable() && !block.isLiquid()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the highest solid ground at (x, z) near y. Returns
     * Double.NEGATIVE_INFINITY when airborne.
     */
    public static double groundLevel(World world, double x, double y, double z) {
        if (world == null) {
            return Double.NEGATIVE_INFINITY;
        }
        int baseY = (int) Math.floor(y);
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int yy = baseY + 1; yy >= baseY - 4; yy--) {
            if (world.getBlockAt(bx, yy, bz).getType().isSolid()) {
                return yy + 1.0;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    /** Highest liquid surface near y, or NEGATIVE_INFINITY when dry. */
    public static double liquidSurface(World world, double x, double y, double z) {
        if (world == null) {
            return Double.NEGATIVE_INFINITY;
        }
        int baseY = (int) Math.floor(y);
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int yy = baseY + 2; yy >= baseY - 3; yy--) {
            if (world.getBlockAt(bx, yy, bz).isLiquid()) {
                return yy + 1.0;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    public static boolean isWater(World world, double x, double y, double z) {
        if (world == null) {
            return false;
        }
        Block block = world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return block.isLiquid() && !block.getType().name().contains("LAVA");
    }

    public static boolean isLava(World world, double x, double y, double z) {
        if (world == null) {
            return false;
        }
        Block block = world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return block.isLiquid() && block.getType().name().contains("LAVA");
    }
}
