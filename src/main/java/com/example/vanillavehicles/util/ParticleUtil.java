package com.example.vanillavehicles.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Locale;

/**
 * Safe particle spawning. Particle names are resolved at runtime so a typo in
 * configuration can never crash the server; unknown names fall back to smoke.
 */
public final class ParticleUtil {

    private ParticleUtil() {
    }

    public static Particle resolve(String name, Particle fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public static void spawn(World world, String name, Location location, int count,
                             double offsetX, double offsetY, double offsetZ, double extra) {
        if (world == null || location == null) {
            return;
        }
        Particle particle = resolve(name, Particle.SMOKE);
        try {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        } catch (Exception ignored) {
            // Some particles require extra data; never let visuals break the tick.
        }
    }

    public static void dust(World world, Location location, int count, Color color, float size) {
        if (world == null || location == null) {
            return;
        }
        try {
            world.spawnParticle(Particle.DUST, location, count, 0.25, 0.25, 0.25, 0.0,
                    new Particle.DustOptions(color == null ? Color.WHITE : color, size));
        } catch (Exception ignored) {
        }
    }

    public static Color color(String name) {
        if (name == null) {
            return Color.WHITE;
        }
        switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "RED":
                return Color.RED;
            case "BLUE":
                return Color.BLUE;
            case "GREEN":
                return Color.GREEN;
            case "LIME":
                return Color.LIME;
            case "YELLOW":
                return Color.YELLOW;
            case "ORANGE":
                return Color.ORANGE;
            case "AQUA":
                return Color.AQUA;
            case "BLACK":
                return Color.BLACK;
            case "GRAY":
            case "GREY":
                return Color.GRAY;
            case "WHITE":
            default:
                return Color.WHITE;
        }
    }
}
