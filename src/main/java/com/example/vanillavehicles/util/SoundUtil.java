package com.example.vanillavehicles.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Safe sound playback. Sound names are resolved at runtime so configuration
 * mistakes can never crash the server; unknown names are silently skipped.
 */
public final class SoundUtil {

    private SoundUtil() {
    }

    public static Sound resolve(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static void play(World world, String name, Location location, float volume, float pitch) {
        if (world == null || location == null) {
            return;
        }
        Sound sound = resolve(name);
        if (sound != null) {
            try {
                world.playSound(location, sound, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    public static void play(Player player, String name, float volume, float pitch) {
        if (player == null) {
            return;
        }
        Sound sound = resolve(name);
        if (sound != null) {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }
}
