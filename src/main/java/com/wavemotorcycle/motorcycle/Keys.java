package com.wavemotorcycle.motorcycle;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** PersistentDataContainer / NamespacedKey registry, initialized once at enable. */
public final class Keys {

    /** UUID of the motorcycle an entity belongs to. */
    public static NamespacedKey BIKE_UUID;
    /** The model part name (only on display parts). */
    public static NamespacedKey PART;
    /** True on the invisible controller armor stand. */
    public static NamespacedKey IS_CONTROLLER;
    /** Owner UUID (on the controller). */
    public static NamespacedKey OWNER;
    /** Marker on wave key items. */
    public static NamespacedKey KEY_IS_WAVE_KEY;
    /** UUID of the bike a key is bound to (may be absent). */
    public static NamespacedKey KEY_BOUND_BIKE;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        BIKE_UUID = new NamespacedKey(plugin, "bike_uuid");
        PART = new NamespacedKey(plugin, "part");
        IS_CONTROLLER = new NamespacedKey(plugin, "controller");
        OWNER = new NamespacedKey(plugin, "owner");
        KEY_IS_WAVE_KEY = new NamespacedKey(plugin, "wave_key");
        KEY_BOUND_BIKE = new NamespacedKey(plugin, "bound_bike");
    }
}
