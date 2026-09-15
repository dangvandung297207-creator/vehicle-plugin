package com.example.vanillavehicles.vehicle;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.model.ModelMath;
import com.example.vanillavehicles.model.ModelPart;
import com.example.vanillavehicles.model.PartFlag;
import com.example.vanillavehicles.model.VehicleModel;
import com.example.vanillavehicles.seat.SeatInstance;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One articulated follower carriage of a locomotive or passenger train.
 * Each carriage follows the recorded path of the vehicle ahead of it.
 */
public class TrainCarriage {

    /** Bumper-to-bumper spacing between coupled segments. */
    public static final double SPACING = 6.4;

    private final VanillaVehicles plugin;
    private final Vehicle parent;
    private final int index;
    private final VehicleModel model;
    private final Map<String, Display> parts = new LinkedHashMap<>();
    private final List<SeatInstance> seats = new ArrayList<>();
    private Location loc;
    private double heading;

    public TrainCarriage(VanillaVehicles plugin, Vehicle parent, int index, VehicleModel model) {
        this.plugin = plugin;
        this.parent = parent;
        this.index = index;
        this.model = model;
        this.loc = parent.getLocation().clone();
        this.heading = parent.getHeading();
    }

    public int getIndex() {
        return index;
    }

    public Location getLocation() {
        return loc;
    }

    public List<SeatInstance> getSeats() {
        return seats;
    }

    public void spawn() {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location origin = originLoc();
        for (ModelPart part : model.getParts()) {
            Display display = DisplayFactory.spawnPart(plugin, parent, part, origin);
            if (display != null) {
                parts.put(part.name, display);
            }
        }
        int seatIndex = 0;
        for (com.example.vanillavehicles.seat.Seat seat : model.getSeats()) {
            ArmorStand stand = DisplayFactory.spawnSeat(plugin, parent,
                    100 + index * 20 + seatIndex, origin);
            SeatInstance instance = new SeatInstance(seat, 100 + index * 20 + seatIndex);
            instance.stand = stand;
            seats.add(instance);
            seatIndex++;
        }
        place(loc, heading);
    }

    private Location originLoc() {
        return new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
                (float) heading, 0f);
    }

    /** Moves the carriage to a recorded path position. */
    public void place(Location target, double targetHeading) {
        loc.setX(target.getX());
        loc.setY(target.getY());
        loc.setZ(target.getZ());
        heading = targetHeading;
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location origin = originLoc();
        for (Display display : parts.values()) {
            if (display != null && display.isValid()) {
                display.teleport(origin);
            }
        }
        double scale = parent.getStats().modelScale;
        for (SeatInstance seat : seats) {
            if (seat.stand == null || !seat.stand.isValid()) {
                continue;
            }
            Vector3f off = seat.seat.offset;
            Vector3f rotated = ModelMath.toWorld(
                    new Vector3f((float) (off.x * scale), (float) (off.y * scale), (float) (off.z * scale)),
                    heading, 0, 0);
            seat.stand.teleport(new Location(world, loc.getX() + rotated.x,
                    loc.getY() + rotated.y, loc.getZ() + rotated.z, (float) heading, 0f));
        }
    }

    /** Spins carriage wheels to match the parent. */
    public void animate(double wheelSpin) {
        double scale = parent.getStats().modelScale;
        for (Map.Entry<String, Display> entry : parts.entrySet()) {
            ModelPart part = model.getPart(entry.getKey());
            Display display = entry.getValue();
            if (part == null || display == null || !display.isValid() || !part.has(PartFlag.WHEEL)) {
                continue;
            }
            Vector3f center = new Vector3f(part.center).mul((float) scale);
            Vector3f size = new Vector3f(part.size).mul((float) scale);
            display.setTransformation(ModelMath.orientedTransform(center,
                    ModelMath.eulerToAxisAngle(0, wheelSpin, 0), size));
        }
    }

    /** Respawns entities that went missing (e.g. admin cleanup commands). */
    public void repair() {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location origin = originLoc();
        for (ModelPart part : model.getParts()) {
            Display display = parts.get(part.name);
            if (display == null || !display.isValid()) {
                Display replacement = DisplayFactory.spawnPart(plugin, parent, part, origin);
                if (replacement != null) {
                    parts.put(part.name, replacement);
                    plugin.getVehicleManager().indexEntity(replacement, parent);
                }
            }
        }
        for (SeatInstance seat : seats) {
            if (seat.stand == null || !seat.stand.isValid()) {
                ArmorStand stand = DisplayFactory.spawnSeat(plugin, parent, seat.index, origin);
                if (stand != null) {
                    seat.stand = stand;
                    plugin.getVehicleManager().indexEntity(stand, parent);
                }
            }
        }
        place(loc, heading);
    }

    public void remove() {
        for (SeatInstance seat : seats) {
            if (seat.stand != null && seat.stand.isValid()) {
                for (Entity passenger : new ArrayList<>(seat.stand.getPassengers())) {
                    if (passenger instanceof Player) {
                        parent.exit((Player) passenger);
                    } else {
                        passenger.leaveVehicle();
                    }
                }
                seat.stand.remove();
            }
        }
        for (Display display : parts.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        parts.clear();
    }

    public List<Entity> entities() {
        List<Entity> entities = new ArrayList<>();
        entities.addAll(parts.values());
        for (SeatInstance seat : seats) {
            if (seat.stand != null) {
                entities.add(seat.stand);
            }
        }
        return entities;
    }
}
