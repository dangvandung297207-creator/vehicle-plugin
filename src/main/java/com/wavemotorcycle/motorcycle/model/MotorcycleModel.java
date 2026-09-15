package com.wavemotorcycle.motorcycle.model;

import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.Keys;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.util.MathUtil;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Owns and updates every entity that renders one motorcycle: the invisible
 * controller armor stand (the rider's seat) and one ItemDisplay per model part.
 *
 * <p>Entities are created once and then moved/rotated with teleports plus
 * {@link Transformation} updates (client-side interpolation is enabled), they are
 * never destroyed and re-created per tick. Unchanged parts send no packets at all.
 */
public final class MotorcycleModel {

    // Item custom-model-data values matching the blockbench files in the resource pack.
    public static final int MODEL_DATA_BODY = 1;
    public static final int MODEL_DATA_FRONT_ASSEMBLY = 2;
    public static final int MODEL_DATA_HANDLEBAR = 3;
    public static final int MODEL_DATA_HEADLIGHT_OFF = 101;
    public static final int MODEL_DATA_HEADLIGHT_ON = 102;
    public static final int MODEL_DATA_REAR_DIM = 201;
    public static final int MODEL_DATA_REAR_BRAKE = 202;
    public static final int MODEL_DATA_FRONT_WHEEL = 301;
    public static final int MODEL_DATA_REAR_WHEEL = 302;
    public static final int MODEL_DATA_KEY = 1001;

    /** Seat top height above the ground (chassis frame). */
    public static final double SEAT_TOP = 0.95;
    /** Seat anchor (where the controller stands) in the chassis frame. */
    public static final double SEAT_Z = -0.55;

    private static final Vector3f UNIT_SCALE = new Vector3f(1, 1, 1);
    private static final Vector3f ORIGIN = new Vector3f(0, 0, 0);

    private final MotorcycleController controller;
    private final ConfigManager cfg;

    private ArmorStand stand;
    private final Map<ModelPart, ItemDisplay> displays = new EnumMap<>(ModelPart.class);

    private boolean headlightOn = false;
    private boolean brakeLight = false;

    // Per-part last applied state, used to skip unchanged network updates.
    private final Map<ModelPart, double[]> lastState = new EnumMap<>(ModelPart.class);

    public MotorcycleModel(MotorcycleController controller, ConfigManager cfg) {
        this.controller = controller;
        this.cfg = cfg;
    }

    /** Spawns the controller stand and all display parts. The controller pose must already be set. */
    public void spawn() {
        World world = controller.world();
        double anchorY = SEAT_TOP - cfg.seatCompensation;
        Location seat = controller.chassisPointToLocation(0, anchorY, SEAT_Z);
        stand = (ArmorStand) world.spawnEntity(seat, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
        });
        stand.setInvisible(true);
        stand.setSilent(true);
        stand.setInvulnerable(true);
        stand.setPersistent(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(true);
        stand.setCanMove(false);
        stand.setGravity(false);
        stand.getPersistentDataContainer().set(Keys.BIKE_UUID, PersistentDataType.STRING, controller.state().uuid().toString());
        stand.getPersistentDataContainer().set(Keys.IS_CONTROLLER, PersistentDataType.STRING, "1");
        if (controller.state().owner() != null) {
            stand.getPersistentDataContainer().set(Keys.OWNER, PersistentDataType.STRING, controller.state().owner().toString());
        }

        for (ModelPart part : ModelPart.values()) {
            Location loc = controller.chassisPointToLocation(part.anchorX(), part.anchorY(), part.anchorZ());
            ItemDisplay display = (ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            });
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);
            display.setTeleportDuration(1);
            display.setItemStack(itemFor(currentModelData(part)));
            display.getPersistentDataContainer().set(Keys.BIKE_UUID, PersistentDataType.STRING, controller.state().uuid().toString());
            display.getPersistentDataContainer().set(Keys.PART, PersistentDataType.STRING, part.name());
            displays.put(part, display);
        }
        // Force a full update so lastState matches reality (no first-tick packets spike later).
        controller.markVisualDirty(true);
    }

    /** Updates all displays for the current controller state. */
    public void update() {
        if (!isAlive()) {
            return;
        }
        MotorcycleController c = controller;
        World world = c.world();

        // Rider anchor follows the (wheelie-lifted) seat.
        double anchorY = SEAT_TOP - cfg.seatCompensation;
        Location seat = c.chassisPointToLocation(0, anchorY, SEAT_Z);
        if (stand.isValid()) {
            stand.teleportAsync(seat);
        }

        for (Map.Entry<ModelPart, ItemDisplay> entry : displays.entrySet()) {
            ModelPart part = entry.getKey();
            ItemDisplay display = entry.getValue();
            if (!display.isValid()) {
                continue;
            }
            Location loc = c.chassisPointToLocation(part.anchorX(), part.anchorY(), part.anchorZ());
            float steer = part.steers() ? (float) (c.steerAngle() * part.steerMultiplier()) : 0f;
            float spin = part.spins() ? (float) c.wheelSpin() : 0f;
            updateDisplay(part, display, loc, steer, spin);
        }
    }

    private void updateDisplay(ModelPart part, ItemDisplay display, Location loc, float steerDeg, float spinDeg) {
        int modelData = currentModelData(part);
        double[] last = lastState.get(part);
        if (!controller.visualDirty()
                && last != null
                && MathUtil.close(last[0], loc.getX(), 1.0E-4)
                && MathUtil.close(last[1], loc.getY(), 1.0E-4)
                && MathUtil.close(last[2], loc.getZ(), 1.0E-4)
                && MathUtil.close(last[3], loc.getYaw(), 0.02)
                && MathUtil.close(last[4], steerDeg, 0.02)
                && MathUtil.close(last[5], spinDeg, 0.05)
                && last[6] == modelData) {
            return; // nothing changed: skip network updates entirely
        }
        lastState.put(part, new double[]{loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), steerDeg, spinDeg, modelData});

        display.teleportAsync(loc);
        float steerRad = (float) (steerDeg * Math.PI / 180.0);
        float spinRad = (float) (spinDeg * Math.PI / 180.0);
        // leftRotation (outer): steering yaw around the local Y axis.
        // rightRotation (inner): wheel spin around the local X axis (the axle).
        Quaternionf left = new Quaternionf(0, (float) Math.sin(steerRad / 2), 0, (float) Math.cos(steerRad / 2));
        Quaternionf right = new Quaternionf((float) Math.sin(spinRad / 2), 0, 0, (float) Math.cos(spinRad / 2));
        display.setTransformation(new Transformation(ORIGIN, left, UNIT_SCALE, right));
    }

    private int currentModelData(ModelPart part) {
        return switch (part) {
            case BODY -> MODEL_DATA_BODY;
            case FRONT_ASSEMBLY -> MODEL_DATA_FRONT_ASSEMBLY;
            case HANDLEBAR -> MODEL_DATA_HANDLEBAR;
            case HEADLIGHT -> headlightOn ? MODEL_DATA_HEADLIGHT_ON : MODEL_DATA_HEADLIGHT_OFF;
            case REAR_LIGHT -> brakeLight ? MODEL_DATA_REAR_BRAKE : MODEL_DATA_REAR_DIM;
            case FRONT_WHEEL -> MODEL_DATA_FRONT_WHEEL;
            case REAR_WHEEL -> MODEL_DATA_REAR_WHEEL;
        };
    }

    /** Swaps the headlight model to its illuminated (or not) variant. */
    public void setHeadlight(boolean on) {
        if (headlightOn == on) {
            return;
        }
        headlightOn = on;
        ItemDisplay display = displays.get(ModelPart.HEADLIGHT);
        if (display != null && display.isValid()) {
            display.setItemStack(itemFor(currentModelData(ModelPart.HEADLIGHT)));
        }
        controller.markVisualDirty(true);
    }

    /** Swaps the rear light between the dim and the bright (braking) variant. */
    public void setBrakeLight(boolean braking) {
        if (brakeLight == braking) {
            return;
        }
        brakeLight = braking;
        ItemDisplay display = displays.get(ModelPart.REAR_LIGHT);
        if (display != null && display.isValid()) {
            display.setItemStack(itemFor(currentModelData(ModelPart.REAR_LIGHT)));
        }
        controller.markVisualDirty(true);
    }

    public boolean isHeadlightOn() {
        return headlightOn;
    }

    public boolean isBrakeLight() {
        return brakeLight;
    }

    private ItemStack itemFor(int modelData) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ArmorStand controllerStand() {
        return stand;
    }

    public boolean isAlive() {
        return stand != null && stand.isValid();
    }

    /** Removes every entity belonging to this bike. */
    public void remove() {
        for (ItemDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
        lastState.clear();
        if (stand != null && stand.isValid()) {
            stand.remove();
        }
        stand = null;
    }

    /** True when an entity carries one of this plugin's PDC markers. */
    public static boolean isBikeEntity(org.bukkit.entity.Entity entity, NamespacedKey key) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }
}
