package com.example.vanillavehicles.entity;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * PersistentDataContainer keys used to tag every entity created by this plugin.
 * These tags guarantee we never touch entities that do not belong to us.
 */
public final class VehicleTags {

    private VehicleTags() {
    }

    /** Unique id of the vehicle instance an entity belongs to. */
    public static NamespacedKey VEHICLE_ID;
    /** Vehicle type id, e.g. "car", "tank", "helicopter". */
    public static NamespacedKey VEHICLE_TYPE;
    /** Model part name, "seat", or "hitbox". */
    public static NamespacedKey VEHICLE_PART;
    /** Duplicate of the instance id, used for fast orphan scans. */
    public static NamespacedKey VEHICLE_INSTANCE;
    /** Seat index for seat entities. */
    public static NamespacedKey SEAT_INDEX;
    /** Vehicle type stored on spawner items. */
    public static NamespacedKey SPAWNER_TYPE;

    public static void init(Plugin plugin) {
        VEHICLE_ID = new NamespacedKey(plugin, "vehicle_id");
        VEHICLE_TYPE = new NamespacedKey(plugin, "vehicle_type");
        VEHICLE_PART = new NamespacedKey(plugin, "vehicle_part");
        VEHICLE_INSTANCE = new NamespacedKey(plugin, "vehicle_instance");
        SEAT_INDEX = new NamespacedKey(plugin, "seat_index");
        SPAWNER_TYPE = new NamespacedKey(plugin, "spawner_type");
    }
}
