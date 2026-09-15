package com.example.vanillavehicles.model;

import com.example.vanillavehicles.seat.Seat;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder-style container for all parts and seats of a vehicle.
 *
 * <p>Example:</p>
 * <pre>
 * VehicleModel car = new VehicleModel();
 * car.box("body", "RED_CONCRETE", 0, 0.6, 0, 1.8, 0.6, 3.6);
 * car.wheel("wheel_fl", 0.85, 0.35, 1.15, 0.7, 0.3, true);
 * car.seat(0.4, 0.5, 0.35, true);
 * car.lamp("head_l", "SEA_LANTERN", 0.6, 0.62, 1.82, 0.3, 0.2, 0.1, PartFlag.HEADLIGHT);
 * </pre>
 */
public class VehicleModel {

    private final List<ModelPart> parts = new ArrayList<>();
    private final List<Seat> seats = new ArrayList<>();
    private final Map<String, ModelPart> partMap = new HashMap<>();

    public VehicleModel addPart(ModelPart part) {
        parts.add(part);
        partMap.put(part.name, part);
        return this;
    }

    /** Axis-aligned block part. */
    public VehicleModel box(String name, String material,
                            double x, double y, double z,
                            double sx, double sy, double sz,
                            PartFlag... flags) {
        return addPart(ModelPart.block(name, material, x, y, z, sx, sy, sz, flags));
    }

    /** Full-bright block part (lamps, signs). */
    public VehicleModel boxBright(String name, String material,
                                  double x, double y, double z,
                                  double sx, double sy, double sz,
                                  PartFlag... flags) {
        return addPart(ModelPart.blockBright(name, material, x, y, z, sx, sy, sz, flags));
    }

    /** Lamp part (full bright block that can swap material). */
    public VehicleModel lamp(String name, String material,
                             double x, double y, double z,
                             double sx, double sy, double sz,
                             PartFlag... flags) {
        return addPart(ModelPart.blockBright(name, material, x, y, z, sx, sy, sz, flags));
    }

    /** Centered item part (used for everything that rotates). */
    public VehicleModel item(String name, String material,
                             double x, double y, double z, double scale,
                             PartFlag... flags) {
        return addPart(ModelPart.item(name, material, x, y, z, scale, flags));
    }

    public VehicleModel itemSized(String name, String material,
                                  double x, double y, double z,
                                  double sx, double sy, double sz,
                                  PartFlag... flags) {
        return addPart(ModelPart.itemSized(name, material, x, y, z, sx, sy, sz, flags));
    }

    public VehicleModel itemOriented(String name, String material,
                                     double x, double y, double z,
                                     double sx, double sy, double sz,
                                     double yawDeg, double pitchDeg, double rollDeg,
                                     PartFlag... flags) {
        return addPart(ModelPart.itemOriented(name, material, x, y, z, sx, sy, sz,
                yawDeg, pitchDeg, rollDeg, ModelPart.toSet(flags), false, null, null));
    }

    /** Hinge-animated item part driven by a named channel. */
    public VehicleModel hinge(String name, String material, String channel,
                              double x, double y, double z,
                              double sx, double sy, double sz,
                              double hingeX, double hingeY, double hingeZ) {
        EnumSet<PartFlag> flags = EnumSet.of(PartFlag.ANIM);
        return addPart(ModelPart.itemOriented(name, material, x, y, z, sx, sy, sz,
                0, 0, 0, flags, false,
                new Vector3f((float) hingeX, (float) hingeY, (float) hingeZ), channel));
    }

    /** Turret barrel: follows turret yaw + look pitch around a hinge. */
    public VehicleModel barrel(String name, String material,
                               double x, double y, double z,
                               double sx, double sy, double sz,
                               double hingeX, double hingeY, double hingeZ) {
        EnumSet<PartFlag> flags = EnumSet.of(PartFlag.BARREL);
        return addPart(new ModelPart(name, PartKind.ITEM, material, null,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) sx, (float) sy, (float) sz),
                0, 0, 0, flags, false, 64f,
                new Vector3f((float) hingeX, (float) hingeY, (float) hingeZ), null));
    }

    /** Sliding (translation-only) animated block part driven by a channel. */
    public VehicleModel slider(String name, String material, String channel,
                               double x, double y, double z,
                               double sx, double sy, double sz) {
        EnumSet<PartFlag> flags = EnumSet.of(PartFlag.ANIM);
        ModelPart part = new ModelPart(name, PartKind.BLOCK, material, null,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) sx, (float) sy, (float) sz),
                0, 0, 0, flags, false, 64f, null, channel);
        return addPart(part);
    }

    public VehicleModel text(String name, String text,
                             double x, double y, double z,
                             double scale, double yawDeg) {
        return addPart(ModelPart.text(name, text, x, y, z, scale, yawDeg));
    }

    /**
     * Adds a spinning wheel (tire + hub). The axle runs along local X.
     *
     * @param steer true for steered (front) wheels
     */
    public VehicleModel wheel(String name, String tireMaterial, String hubMaterial,
                              double x, double y, double z,
                              double diameter, double width, boolean steer) {
        if (steer) {
            addPart(ModelPart.itemSized(name, tireMaterial, x, y, z, width, diameter, diameter,
                    PartFlag.WHEEL, PartFlag.STEER));
            addPart(ModelPart.itemSized(name + "_hub", hubMaterial, x, y, z,
                    width + 0.04, diameter * 0.45, diameter * 0.45,
                    PartFlag.WHEEL, PartFlag.STEER));
        } else {
            addPart(ModelPart.itemSized(name, tireMaterial, x, y, z, width, diameter, diameter,
                    PartFlag.WHEEL));
            addPart(ModelPart.itemSized(name + "_hub", hubMaterial, x, y, z,
                    width + 0.04, diameter * 0.45, diameter * 0.45,
                    PartFlag.WHEEL));
        }
        return this;
    }

    public VehicleModel wheel(String name, double x, double y, double z,
                              double diameter, double width, boolean steer) {
        return wheel(name, "COAL_BLOCK", "IRON_BLOCK", x, y, z, diameter, width, steer);
    }

    /**
     * Adds one track side made of scrolling tread segments. Segments are
     * 0.5 m long; the animator scrolls them to fake track movement.
     */
    public VehicleModel track(String side, double x, double y, double zCenter,
                              double width, double height, int segments) {
        double start = zCenter - (segments * 0.5) / 2.0 + 0.25;
        for (int i = 0; i < segments; i++) {
            box("track_" + side + "_" + i, "BLACK_CONCRETE",
                    x, y, start + i * 0.5, width, height, 0.44, PartFlag.TRACK);
        }
        return this;
    }

    public VehicleModel seat(double x, double y, double z, boolean driver) {
        seats.add(new Seat(new Vector3f((float) x, (float) y, (float) z), driver));
        return this;
    }

    /** Row of passenger seats across the vehicle. */
    public VehicleModel seatRow(double y, double z, double... xPositions) {
        for (double x : xPositions) {
            seat(x, y, z, false);
        }
        return this;
    }

    public List<ModelPart> getParts() {
        return parts;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public ModelPart getPart(String name) {
        return partMap.get(name);
    }

    public int seatCount() {
        return seats.size();
    }
}
