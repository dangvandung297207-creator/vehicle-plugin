package com.example.vanillavehicles.entity;

import com.example.vanillavehicles.model.ModelMath;
import com.example.vanillavehicles.model.ModelPart;
import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns every entity owned by a vehicle and tags it, so the plugin can
 * always tell its own entities apart from normal Minecraft entities.
 */
public final class DisplayFactory {

    private DisplayFactory() {
    }

    private static final Map<String, BlockData> BLOCK_DATA_CACHE = new ConcurrentHashMap<>();

    public static Material resolveMaterial(String name, Material fallback) {
        if (name != null) {
            Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            if (material != null) {
                return material;
            }
        }
        return fallback;
    }

    public static BlockData blockData(String name) {
        String key = name == null ? "STONE" : name.trim().toUpperCase(Locale.ROOT);
        BlockData cached = BLOCK_DATA_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Material material = resolveMaterial(key, Material.STONE);
        if (!material.isBlock()) {
            material = Material.STONE;
        }
        BlockData data;
        try {
            data = Bukkit.createBlockData(material);
        } catch (IllegalArgumentException ex) {
            data = Bukkit.createBlockData(Material.STONE);
        }
        BLOCK_DATA_CACHE.put(key, data);
        return data;
    }

    public static ItemStack itemStack(String name) {
        Material material = resolveMaterial(name, Material.STONE);
        if (!material.isItem()) {
            material = Material.STONE;
        }
        return new ItemStack(material);
    }

    private static void tag(PersistentDataContainer container, Vehicle vehicle, String partName) {
        String id = vehicle.getId().toString();
        container.set(VehicleTags.VEHICLE_ID, PersistentDataType.STRING, id);
        container.set(VehicleTags.VEHICLE_INSTANCE, PersistentDataType.STRING, id);
        container.set(VehicleTags.VEHICLE_TYPE, PersistentDataType.STRING, vehicle.getType().getId());
        container.set(VehicleTags.VEHICLE_PART, PersistentDataType.STRING, partName);
    }

    private static Vector3f scaled(Vector3f vector, double scale) {
        return new Vector3f((float) (vector.x * scale), (float) (vector.y * scale), (float) (vector.z * scale));
    }

    /** Spawns one model part. Returns null if the world is unavailable. */
    public static Display spawnPart(Plugin plugin, Vehicle vehicle, ModelPart part, Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        double scale = vehicle.getStats().modelScale;
        Vector3f center = scaled(part.center, scale);
        Vector3f size = scaled(part.size, scale);

        try {
            switch (part.kind) {
                case BLOCK: {
                    BlockDisplay display = world.spawn(origin, BlockDisplay.class);
                    display.setTransformation(ModelMath.blockTransform(center, size));
                    display.setBlock(blockData(part.material));
                    finishDisplay(display, vehicle, part, origin);
                    return display;
                }
                case ITEM: {
                    ItemDisplay display = world.spawn(origin, ItemDisplay.class);
                    display.setItemStack(itemStack(part.material));
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    display.setTransformation(ModelMath.orientedTransform(center,
                            ModelMath.eulerToAxisAngle(part.baseYaw, part.basePitch, part.baseRoll), size));
                    finishDisplay(display, vehicle, part, origin);
                    return display;
                }
                case TEXT:
                default: {
                    TextDisplay display = world.spawn(origin, TextDisplay.class);
                    display.setText(part.text == null ? "" : part.text);
                    display.setLineWidth(200);
                    display.setShadowed(true);
                    display.setTransformation(ModelMath.orientedTransform(center,
                            ModelMath.eulerToAxisAngle(part.baseYaw, part.basePitch, part.baseRoll), size));
                    finishDisplay(display, vehicle, part, origin);
                    return display;
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not spawn part " + part.name + ": " + ex.getMessage());
            return null;
        }
    }

    private static void finishDisplay(Display display, Vehicle vehicle, ModelPart part, Location origin) {
        display.setBillboard(Display.Billboard.FIXED);
        display.setViewRange(part.viewRange);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        if (part.fullBright) {
            display.setBrightness(new Display.Brightness(15, 15));
        }
        display.setInterpolationDuration(part.isAnimated() || part.isLamp() ? 2 : 0);
        display.setInvulnerable(true);
        tag(display.getPersistentDataContainer(), vehicle, part.name);
    }

    /** Spawns the invisible armor stand a rider sits on. */
    public static ArmorStand spawnSeat(Plugin plugin, Vehicle vehicle, int index, Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        try {
            ArmorStand stand = world.spawn(origin, ArmorStand.class);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(false);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setCanPickupItems(false);
            stand.setRemoveWhenFarAway(false);
            tag(stand.getPersistentDataContainer(), vehicle, "seat");
            stand.getPersistentDataContainer().set(VehicleTags.SEAT_INDEX,
                    PersistentDataType.INTEGER, index);
            return stand;
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not spawn seat: " + ex.getMessage());
            return null;
        }
    }

    /** Spawns the clickable hitbox used to enter the vehicle. */
    public static Interaction spawnHitbox(Plugin plugin, Vehicle vehicle, Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        try {
            Interaction interaction = world.spawn(origin, Interaction.class);
            double width = Math.max(1.2, (vehicle.getStats().length + vehicle.getStats().width) / 2.0);
            double height = Math.max(1.2, vehicle.getStats().height + 0.6);
            interaction.setInteractionWidth((float) width);
            interaction.setInteractionHeight((float) height);
            interaction.setInvulnerable(true);
            interaction.setSilent(true);
            tag(interaction.getPersistentDataContainer(), vehicle, "hitbox");
            return interaction;
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not spawn hitbox: " + ex.getMessage());
            return null;
        }
    }

    /** Checks whether an entity belongs to any vehicle of this plugin. */
    public static boolean isVehicleEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            return entity.getPersistentDataContainer()
                    .has(VehicleTags.VEHICLE_INSTANCE, PersistentDataType.STRING);
        } catch (Exception ex) {
            return false;
        }
    }
}
