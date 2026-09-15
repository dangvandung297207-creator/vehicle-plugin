package com.example.vanillavehicles.model;

import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * One piece of a vehicle model.
 *
 * <p>Local coordinate system (matches the display entity's local space):
 * +X = vehicle left (port), +Y = up, +Z = forward. Offsets are the part's
 * visual center in meters, relative to the vehicle origin at ground level.</p>
 */
public class ModelPart {

    public final String name;
    public final PartKind kind;
    /** Material name for BLOCK/ITEM parts. */
    public final String material;
    /** Literal text for TEXT parts. */
    public final String text;
    /** Local center of the part. */
    public final Vector3f center;
    /** Scale (size in meters for block parts). */
    public final Vector3f size;
    /** Static orientation in radians, applied to ITEM/TEXT parts. */
    public final float baseYaw;
    public final float basePitch;
    public final float baseRoll;
    public final EnumSet<PartFlag> flags;
    public final boolean fullBright;
    public final float viewRange;
    /** Pivot point for hinge animation (BARREL, ANIM parts), may be null. */
    public final Vector3f hinge;
    /** Animation channel name for ANIM parts, may be null. */
    public final String channel;

    public ModelPart(String name, PartKind kind, String material, String text,
                     Vector3f center, Vector3f size,
                     float baseYaw, float basePitch, float baseRoll,
                     EnumSet<PartFlag> flags, boolean fullBright, float viewRange,
                     Vector3f hinge, String channel) {
        this.name = name;
        this.kind = kind;
        this.material = material;
        this.text = text;
        this.center = center;
        this.size = size;
        this.baseYaw = baseYaw;
        this.basePitch = basePitch;
        this.baseRoll = baseRoll;
        this.flags = flags;
        this.fullBright = fullBright;
        this.viewRange = viewRange;
        this.hinge = hinge;
        this.channel = channel;
    }

    public static ModelPart block(String name, String material,
                                  double x, double y, double z,
                                  double sx, double sy, double sz,
                                  PartFlag... flags) {
        return new ModelPart(name, PartKind.BLOCK, material, null,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) sx, (float) sy, (float) sz),
                0, 0, 0, toSet(flags), false, 64f, null, null);
    }

    public static ModelPart blockBright(String name, String material,
                                        double x, double y, double z,
                                        double sx, double sy, double sz,
                                        PartFlag... flags) {
        return new ModelPart(name, PartKind.BLOCK, material, null,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) sx, (float) sy, (float) sz),
                0, 0, 0, toSet(flags), true, 64f, null, null);
    }

    public static ModelPart item(String name, String material,
                                 double x, double y, double z, double scale,
                                 PartFlag... flags) {
        return itemOriented(name, material, x, y, z, scale, scale, scale,
                0, 0, 0, toSet(flags), false, null, null);
    }

    public static ModelPart itemSized(String name, String material,
                                      double x, double y, double z,
                                      double sx, double sy, double sz,
                                      PartFlag... flags) {
        return itemOriented(name, material, x, y, z, sx, sy, sz,
                0, 0, 0, toSet(flags), false, null, null);
    }

    public static ModelPart itemOriented(String name, String material,
                                         double x, double y, double z,
                                         double sx, double sy, double sz,
                                         double yawDeg, double pitchDeg, double rollDeg,
                                         EnumSet<PartFlag> flags, boolean fullBright,
                                         Vector3f hinge, String channel) {
        return new ModelPart(name, PartKind.ITEM, material, null,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) sx, (float) sy, (float) sz),
                (float) Math.toRadians(yawDeg), (float) Math.toRadians(pitchDeg),
                (float) Math.toRadians(rollDeg), flags, fullBright, 64f, hinge, channel);
    }

    public static ModelPart text(String name, String text,
                                 double x, double y, double z, double scale,
                                 double yawDeg) {
        return new ModelPart(name, PartKind.TEXT, null, text,
                new Vector3f((float) x, (float) y, (float) z),
                new Vector3f((float) scale, (float) scale, (float) scale),
                (float) Math.toRadians(yawDeg), 0, 0,
                EnumSet.noneOf(PartFlag.class), false, 64f, null, null);
    }

    public static EnumSet<PartFlag> toSet(PartFlag... flags) {
        if (flags == null || flags.length == 0) {
            return EnumSet.noneOf(PartFlag.class);
        }
        return EnumSet.of(flags[0], flags);
    }

    public boolean has(PartFlag flag) {
        return flags.contains(flag);
    }

    public boolean isAnimated() {
        return has(PartFlag.WHEEL) || has(PartFlag.TRACK) || has(PartFlag.ROTOR)
                || has(PartFlag.TURRET) || has(PartFlag.BARREL) || has(PartFlag.ANIM);
    }

    public boolean isLamp() {
        return has(PartFlag.HEADLIGHT) || has(PartFlag.BRAKELIGHT)
                || has(PartFlag.REVERSELIGHT) || has(PartFlag.EMERGENCY);
    }
}
