package com.wavemotorcycle.motorcycle.physics;

import com.wavemotorcycle.config.ConfigManager;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Bisected;
import org.bukkit.block.Block;
import org.bukkit.block.BlockData;

/**
 * Ground detection and surface traction.
 *
 * <p>Ground is resolved per wheel contact point by scanning the block column
 * around the motorcycle. Full blocks and upper slabs give a ground height, lower
 * slabs are climbable. Liquids provide no ground.
 */
public final class TerrainSurface {

    private final ConfigManager cfg;

    public TerrainSurface(ConfigManager cfg) {
        this.cfg = cfg;
    }

    /**
     * Finds the ground height below a contact point, or {@code Double.NaN} when no
     * ground exists in the scanned range (e.g. the void).
     */
    public double groundY(World world, double x, double z, double referenceY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int top = (int) Math.floor(referenceY) + 2;
        int bottom = (int) Math.floor(referenceY) - 9;
        for (int y = top; y >= bottom; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            Material m = block.getType();
            if (m.isSolid()) {
                return y + 1.0;
            }
            BlockData data = block.getBlockData();
            if (data instanceof Bisected bisected) {
                return y + (bisected.getHalf() == Bisected.Half.UPPER ? 1.0 : 0.5);
            }
            // liquids (lava/water) do not support the bike
        }
        return Double.NaN;
    }

    /** The material the bike is resting on at the given ground height, or null. */
    public Material surfaceMaterial(World world, double x, double z, double groundY) {
        int y = (int) (groundY - 1.01);
        Block block = world.getBlockAt((int) Math.floor(x), y, (int) Math.floor(z));
        Material m = block.getType();
        return m == Material.AIR ? null : m;
    }

    /**
     * Traction multiplier for the surface under the contact point.
     * Slabs fall back to their base material (a stone slab behaves like stone).
     */
    public double traction(World world, double x, double z, double groundY) {
        if (Double.isNaN(groundY)) {
            return 1.0;
        }
        Material m = surfaceMaterial(world, x, z, groundY);
        if (m == null) {
            return 1.0;
        }
        String name = m.name().toLowerCase(java.util.Locale.ROOT);
        Map<String, Double> table = cfg.surfaceTraction;
        Double value = table.get(name);
        if (value == null && name.endsWith("_slab")) {
            value = table.get(name.substring(0, name.length() - "_slab".length()));
        }
        if (value == null && name.endsWith("_slabs")) {
            value = table.get(name.substring(0, name.length() - "_slabs".length()));
        }
        return (value == null ? 1.0 : value) * cfg.baseTraction;
    }
}
