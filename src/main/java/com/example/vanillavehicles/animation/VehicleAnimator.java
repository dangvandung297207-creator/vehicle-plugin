package com.example.vanillavehicles.animation;

import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.model.ModelMath;
import com.example.vanillavehicles.model.ModelPart;
import com.example.vanillavehicles.model.PartFlag;
import com.example.vanillavehicles.model.PartKind;
import com.example.vanillavehicles.input.InputState;
import com.example.vanillavehicles.util.ParticleUtil;
import com.example.vanillavehicles.util.SoundUtil;
import com.example.vanillavehicles.vehicle.PhysicsType;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleStats;
import com.example.vanillavehicles.vehicle.VehicleType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.joml.Vector3f;

import java.util.Map;

/**
 * All visual animation: wheels, tracks, rotors, turret, lean/bank, lamps,
 * mechanism channels and exhaust / spray / siren particles.
 *
 * <p>Only animated parts are touched every tick; static parts are refreshed
 * only while the vehicle visibly rolls (lean/bank), throttled to every
 * second tick.</p>
 */
public final class VehicleAnimator {

    private VehicleAnimator() {
    }

    private static final double DT = 0.05;

    public static void update(Vehicle vehicle, InputState input, long now) {
        VehicleStats stats = vehicle.getStats();
        Vehicle.AnimState anim = vehicle.getAnim();
        double speed = vehicle.getSpeed();

        // Distance-driven animation phases.
        anim.wheelSpin += speed * DT;
        anim.trackPhase += speed * DT;
        anim.steerVis = ModelMath.lerp(anim.steerVis, vehicle.getLastSteer(), Math.min(1, 10 * DT));

        // Rotors / propellers.
        boolean hasDriver = vehicle.getDriver() != null;
        double rotorRate = 0;
        if (vehicle.getType() == VehicleType.HELICOPTER) {
            rotorRate = hasDriver ? 30 : 4;
        } else if (vehicle.getType() == VehicleType.PLANE || vehicle.getType() == VehicleType.FIGHTER_JET) {
            rotorRate = 5 + Math.abs(speed) * 1.1;
        }
        anim.rotorAngle += rotorRate * DT;

        updateLean(vehicle, now);
        updateTurret(vehicle);

        boolean rollDirty = Math.abs(vehicle.getRollVis() - anim.rollApplied) > 0.4
                || (Math.abs(vehicle.getRollVis()) > 0.05 && (now & 1) == 0);
        if (rollDirty) {
            anim.rollApplied = vehicle.getRollVis();
        }

        double scale = stats.modelScale;
        double rollRad = Math.toRadians(vehicle.getRollVis());
        for (Map.Entry<String, Display> entry : vehicle.getParts().entrySet()) {
            ModelPart part = vehicle.getDefinition().getModel().getPart(entry.getKey());
            Display display = entry.getValue();
            if (part == null || display == null || !display.isValid()) {
                continue;
            }
            if (part.isAnimated()) {
                animatePart(vehicle, part, display, scale, rollRad);
            } else if (rollDirty) {
                refreshStaticPart(part, display, scale, vehicle.getRollVis());
            }
        }

        updateLamps(vehicle, now);
        updateEffects(vehicle, now);
    }

    // ------------------------------------------------------------------
    // Lean / bank / bob
    // ------------------------------------------------------------------

    private static void updateLean(Vehicle vehicle, long now) {
        VehicleStats stats = vehicle.getStats();
        double speedRatio = Math.min(1.0, Math.abs(vehicle.getSpeed()) / Math.max(1, stats.maxSpeed));
        double steer = vehicle.getLastSteer();
        PhysicsType physics = vehicle.getType().getPhysics();

        if (physics == PhysicsType.BIKE) {
            double target = -steer * speedRatio * 26.0 * Math.max(0.6, stats.leanFactor);
            vehicle.setRollVis(ModelMath.lerp(vehicle.getRollVis(), target, 0.25));
            vehicle.setPitchUp(ModelMath.lerp(vehicle.getPitchUp(), 0, 0.2));
        } else if (physics == PhysicsType.AIRCRAFT
                && vehicle.getType() != VehicleType.HELICOPTER) {
            vehicle.setRollVis(ModelMath.lerp(vehicle.getRollVis(), -steer * 24.0, 0.15));
        } else if (vehicle.getType() == VehicleType.HELICOPTER) {
            vehicle.setRollVis(ModelMath.lerp(vehicle.getRollVis(), -steer * 10.0, 0.15));
        } else if (physics == PhysicsType.BOAT) {
            vehicle.setRollVis(Math.sin(now * 0.08) * 1.6 - steer * speedRatio * 6.0);
            vehicle.setPitchUp(Math.sin(now * 0.06 + 1.0) * 1.2 + speedRatio * 4.0);
        } else {
            double target = -steer * speedRatio * (vehicle.isDrifting() ? 9.0 : 4.0);
            vehicle.setRollVis(ModelMath.lerp(vehicle.getRollVis(), target, 0.2));
            vehicle.setPitchUp(ModelMath.lerp(vehicle.getPitchUp(), 0, 0.2));
        }

        // Gentle hover bob for boats and helicopters, applied to the origin
        // so the whole model (including seats) moves for free.
        double bobTarget = 0;
        if (physics == PhysicsType.BOAT && vehicle.isInWater()) {
            bobTarget = Math.sin(now * 0.12) * 0.07;
        } else if (vehicle.getType() == VehicleType.HELICOPTER
                && !vehicle.isGrounded() && vehicle.getDriver() != null) {
            bobTarget = Math.sin(now * 0.15) * 0.05;
        }
        Vehicle.AnimState anim = vehicle.getAnim();
        double delta = bobTarget - anim.bobOffset;
        anim.bobOffset = bobTarget;
        if (delta != 0) {
            vehicle.getLocation().setY(vehicle.getLocation().getY() + delta);
        }
    }

    private static void updateTurret(Vehicle vehicle) {
        if (vehicle.getType() != VehicleType.TANK) {
            return;
        }
        Vehicle.AnimState anim = vehicle.getAnim();
        if (vehicle.getChannel("turretlock") > 0.5 || vehicle.getDriver() == null) {
            anim.turretYaw = ModelMath.lerp(anim.turretYaw, 0, 0.1);
            anim.barrelPitch = ModelMath.lerp(anim.barrelPitch, 0, 0.1);
            return;
        }
        double targetYaw = ModelMath.clamp(
                ModelMath.wrapDegrees(vehicle.getLookYaw() - vehicle.getHeading()), -150, 150);
        double targetPitch = ModelMath.clamp(-vehicle.getLookPitch() * 0.5, -5, 20);
        anim.turretYaw = ModelMath.lerp(anim.turretYaw, targetYaw, 0.12);
        anim.barrelPitch = ModelMath.lerp(anim.barrelPitch, targetPitch, 0.12);
    }

    // ------------------------------------------------------------------
    // Part animation
    // ------------------------------------------------------------------

    private static void animatePart(Vehicle vehicle, ModelPart part, Display display,
                                    double scale, double rollRad) {
        Vehicle.AnimState anim = vehicle.getAnim();
        if (part.has(PartFlag.WHEEL)) {
            double radius = Math.max(0.05, part.size.y * scale / 2.0);
            double angle = anim.wheelSpin / radius;
            double steer = part.has(PartFlag.STEER) ? anim.steerVis * 0.45 : 0;
            Vector3f center = ModelMath.applyRoll(scaled(part.center, scale), vehicle.getRollVis());
            display.setTransformation(ModelMath.orientedTransform(center,
                    ModelMath.eulerToAxisAngle(steer, angle, rollRad), scaled(part.size, scale)));
        } else if (part.has(PartFlag.TRACK)) {
            animateTrack(vehicle, part, display, scale);
        } else if (part.has(PartFlag.ROTOR)) {
            double angle = anim.rotorAngle;
            String name = part.name.toLowerCase();
            double yaw = 0;
            double pitch = 0;
            double roll = 0;
            if (name.contains("tail")) {
                pitch = angle * 1.6;
            } else if (name.contains("prop")) {
                roll = angle;
            } else {
                yaw = angle;
            }
            Vector3f center = ModelMath.applyRoll(scaled(part.center, scale), vehicle.getRollVis());
            display.setTransformation(ModelMath.orientedTransform(center,
                    ModelMath.eulerToAxisAngle(yaw + part.baseYaw, pitch + part.basePitch,
                            roll + part.baseRoll + rollRad),
                    scaled(part.size, scale)));
        } else if (part.has(PartFlag.TURRET)) {
            double yaw = Math.toRadians(anim.turretYaw);
            Vector3f center = ModelMath.applyRoll(scaled(part.center, scale), vehicle.getRollVis());
            display.setTransformation(ModelMath.orientedTransform(center,
                    ModelMath.eulerToAxisAngle(yaw + part.baseYaw, part.basePitch, part.baseRoll),
                    scaled(part.size, scale)));
        } else if (part.has(PartFlag.BARREL)) {
            double yaw = Math.toRadians(anim.turretYaw);
            double pitch = Math.toRadians(anim.barrelPitch);
            Vector3f center = scaled(part.center, scale);
            Vector3f hinge = part.hinge == null ? center : scaled(part.hinge, scale);
            Vector3f diff = new Vector3f(center).sub(hinge);
            Vector3f rotated = ModelMath.rotateVector(diff, yaw, pitch, 0);
            Vector3f placed = new Vector3f(hinge).add(rotated);
            // Recoil kicks the barrel backwards along its own axis.
            double recoil = vehicle.getChannel("recoil");
            if (recoil > 0.01) {
                Vector3f back = ModelMath.rotateVector(new Vector3f(0, 0, -0.35f * (float) recoil), yaw, 0, 0);
                placed.add(back);
            }
            display.setTransformation(ModelMath.orientedTransform(placed,
                    ModelMath.eulerToAxisAngle(yaw, pitch, 0), scaled(part.size, scale)));
        } else if (part.has(PartFlag.ANIM)) {
            animateChannelPart(vehicle, part, display, scale, rollRad);
        }
    }

    private static void animateTrack(Vehicle vehicle, ModelPart part, Display display, double scale) {
        int index = 0;
        try {
            String[] split = part.name.split("_");
            index = Integer.parseInt(split[split.length - 1]);
        } catch (Exception ignored) {
        }
        int half = Math.max(1, vehicle.getTrackHalfCount());
        double total = half * 0.5;
        double baseZ = part.center.z;
        double zCenter = baseZ - index * 0.5 + total / 2.0 - 0.25;
        double m = ((index * 0.5 - vehicle.getAnim().trackPhase) % total + total) % total;
        double z = zCenter - total / 2.0 + m + 0.25;
        Vector3f center = new Vector3f(part.center.x, part.center.y, (float) z);
        center = ModelMath.applyRoll(scaled(center, scale), vehicle.getRollVis());
        display.setTransformation(ModelMath.blockTransform(center, scaled(part.size, scale)));
    }

    private static void animateChannelPart(Vehicle vehicle, ModelPart part, Display display,
                                           double scale, double rollRad) {
        String channel = part.channel == null ? "" : part.channel;
        switch (channel) {
            case "ladder": {
                double c = vehicle.getChannel("ladder");
                Vector3f center = new Vector3f(
                        part.center.x, (float) (part.center.y + 1.4 * c), (float) (part.center.z - 0.9 * c));
                center = ModelMath.applyRoll(scaled(center, scale), vehicle.getRollVis());
                display.setTransformation(ModelMath.blockTransform(center, scaled(part.size, scale)));
                break;
            }
            case "fork": {
                double c = vehicle.getChannel("fork");
                Vector3f center = new Vector3f(
                        part.center.x, (float) (part.center.y + 1.1 * c), part.center.z);
                center = ModelMath.applyRoll(scaled(center, scale), vehicle.getRollVis());
                if (part.kind == PartKind.BLOCK) {
                    display.setTransformation(ModelMath.blockTransform(center, scaled(part.size, scale)));
                } else {
                    display.setTransformation(ModelMath.orientedTransform(center,
                            ModelMath.eulerToAxisAngle(part.baseYaw, part.basePitch, part.baseRoll + rollRad),
                            scaled(part.size, scale)));
                }
                break;
            }
            case "dump": {
                double c = vehicle.getChannel("dump");
                hingePitch(vehicle, part, display, scale, -0.62 * c);
                break;
            }
            case "boom": {
                // Excavator boom follows the driver's view pitch, plus the arm toggle.
                double look = vehicle.getDriver() == null ? 0
                        : ModelMath.clamp(-vehicle.getLookPitch(), -30, 45);
                double c = vehicle.getChannel("armup");
                double pitchDeg = ModelMath.clamp(-8 - look * 0.7 + c * 35.0, -55, 40);
                hingePitch(vehicle, part, display, scale, Math.toRadians(pitchDeg));
                break;
            }
            case "stick": {
                double dig = vehicle.getChannel("dig");
                double look = vehicle.getDriver() == null ? 0
                        : ModelMath.clamp(-vehicle.getLookPitch(), -30, 45);
                double pitchDeg = ModelMath.clamp(15 + look * 0.5 - dig * 55.0, -70, 60);
                hingePitch(vehicle, part, display, scale, Math.toRadians(pitchDeg));
                break;
            }
            case "bucket": {
                double dig = vehicle.getChannel("dig");
                double look = vehicle.getDriver() == null ? 0
                        : ModelMath.clamp(-vehicle.getLookPitch(), -30, 45);
                double pitchDeg = ModelMath.clamp(20 + look * 0.3 + dig * 60.0, -40, 110);
                hingePitch(vehicle, part, display, scale, Math.toRadians(pitchDeg));
                break;
            }
            case "attach": {
                double c = vehicle.getChannel("attach");
                double s = 0.05 + 0.95 * c;
                Vector3f center = new Vector3f(part.center.x,
                        (float) (part.center.y - 0.35 * (1 - c)), part.center.z);
                center = ModelMath.applyRoll(scaled(center, scale), vehicle.getRollVis());
                Vector3f size = scaled(part.size, scale).mul((float) s);
                if (part.kind == PartKind.BLOCK) {
                    display.setTransformation(ModelMath.blockTransform(center, size));
                } else {
                    display.setTransformation(ModelMath.orientedTransform(center,
                            ModelMath.eulerToAxisAngle(part.baseYaw, part.basePitch, part.baseRoll + rollRad), size));
                }
                break;
            }
            default:
                break;
        }
    }

    private static void hingePitch(Vehicle vehicle, ModelPart part, Display display,
                                   double scale, double pitchRad) {
        Vector3f center = scaled(part.center, scale);
        Vector3f hinge = part.hinge == null ? center : scaled(part.hinge, scale);
        Vector3f rotated = ModelMath.rotateVector(new Vector3f(center).sub(hinge), 0, pitchRad, 0);
        Vector3f placed = new Vector3f(hinge).add(rotated);
        display.setTransformation(ModelMath.orientedTransform(placed,
                ModelMath.eulerToAxisAngle(part.baseYaw, pitchRad + part.basePitch, part.baseRoll),
                scaled(part.size, scale)));
    }

    private static void refreshStaticPart(ModelPart part, Display display, double scale, double rollDeg) {
        Vector3f center = ModelMath.applyRoll(scaled(part.center, scale), rollDeg);
        if (part.kind == PartKind.BLOCK) {
            display.setTransformation(ModelMath.blockTransform(center, scaled(part.size, scale)));
        } else {
            display.setTransformation(ModelMath.orientedTransform(center,
                    ModelMath.eulerToAxisAngle(part.baseYaw, part.basePitch, part.baseRoll),
                    scaled(part.size, scale)));
        }
    }

    private static Vector3f scaled(Vector3f vector, double scale) {
        return new Vector3f((float) (vector.x * scale), (float) (vector.y * scale), (float) (vector.z * scale));
    }

    // ------------------------------------------------------------------
    // Lamps
    // ------------------------------------------------------------------

    private static void updateLamps(Vehicle vehicle, long now) {
        int bits = 0;
        if (vehicle.isLightsOn()) {
            bits |= 1;
        }
        if (vehicle.isBraking()) {
            bits |= 2;
        }
        if (vehicle.isReversing()) {
            bits |= 4;
        }
        if (vehicle.isSirenOn() && vehicle.getStats().siren) {
            bits |= 8;
            if ((now / 6) % 2 == 0) {
                bits |= 16;
            }
        }
        Vehicle.AnimState anim = vehicle.getAnim();
        if (bits == anim.lightBits) {
            return;
        }
        anim.lightBits = bits;
        boolean lights = (bits & 1) != 0;
        boolean braking = (bits & 2) != 0;
        boolean reversing = (bits & 4) != 0;
        boolean siren = (bits & 8) != 0;
        boolean phaseA = (bits & 16) != 0;

        for (Map.Entry<String, Display> entry : vehicle.getParts().entrySet()) {
            ModelPart part = vehicle.getDefinition().getModel().getPart(entry.getKey());
            Display display = entry.getValue();
            if (part == null || display == null || !display.isValid()) {
                continue;
            }
            if (!(display instanceof org.bukkit.entity.BlockDisplay)) {
                continue;
            }
            org.bukkit.entity.BlockDisplay block = (org.bukkit.entity.BlockDisplay) display;
            if (part.has(PartFlag.HEADLIGHT)) {
                block.setBlock(DisplayFactory.blockData(lights ? "SEA_LANTERN" : "LIGHT_GRAY_CONCRETE"));
            } else if (part.has(PartFlag.BRAKELIGHT)) {
                block.setBlock(DisplayFactory.blockData(
                        braking || (lights && part.name.contains("tail")) ? "REDSTONE_BLOCK" : "RED_CONCRETE"));
            } else if (part.has(PartFlag.REVERSELIGHT)) {
                block.setBlock(DisplayFactory.blockData(reversing ? "WHITE_CONCRETE" : "LIGHT_GRAY_CONCRETE"));
            } else if (part.has(PartFlag.EMERGENCY)) {
                if (!siren) {
                    block.setBlock(DisplayFactory.blockData("GRAY_CONCRETE"));
                } else {
                    boolean leftSide = part.center.x >= 0;
                    boolean on = phaseA == leftSide;
                    if (on) {
                        block.setBlock(DisplayFactory.blockData(leftSide ? "REDSTONE_BLOCK" : "BLUE_CONCRETE"));
                    } else {
                        block.setBlock(DisplayFactory.blockData("GRAY_CONCRETE"));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Particles / effect sounds
    // ------------------------------------------------------------------

    private static void updateEffects(Vehicle vehicle, long now) {
        World world = vehicle.getWorld();
        if (world == null) {
            return;
        }
        VehicleStats stats = vehicle.getStats();
        double speed = Math.abs(vehicle.getSpeed());
        boolean hasDriver = vehicle.getDriver() != null;

        // Exhaust smoke / boost flames.
        if (hasDriver && (speed > 3 || vehicle.isBoosting()) && now % 4 == 0) {
            Location exhaust = vehicle.getWorldLoc(new Vector3f(0.35f, 0.45f,
                    (float) (-stats.length / 2.0)));
            if (vehicle.isBoosting()) {
                ParticleUtil.spawn(world, "FLAME", exhaust, 6, 0.15, 0.15, 0.15, 0.02);
                ParticleUtil.spawn(world, "LARGE_SMOKE", exhaust, 4, 0.2, 0.2, 0.2, 0.03);
            } else {
                ParticleUtil.spawn(world, "CAMPFIRE_COSY_SMOKE", exhaust, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Drift / burnout smoke.
        if ((vehicle.isDrifting() || vehicle.isBurnouting()) && now % 3 == 0) {
            Location rear = vehicle.getWorldLoc(new Vector3f(0, 0.3f,
                    (float) (-stats.length * 0.3)));
            ParticleUtil.spawn(world, "LARGE_SMOKE", rear, 8, 0.7, 0.3, 0.7, 0.03);
            ParticleUtil.spawn(world, "POOF", rear, 6, 0.7, 0.3, 0.7, 0.03);
            if (now % 12 == 0) {
                SoundUtil.play(world, vehicle.getSoundProfile().brake, rear, 0.7f, 1.4f);
            }
        }

        // Boat spray.
        if (vehicle.getType().getPhysics() == com.example.vanillavehicles.vehicle.PhysicsType.BOAT
                && vehicle.isInWater() && speed > 5 && now % 3 == 0) {
            Location stern = vehicle.getWorldLoc(new Vector3f(0, 0.2f,
                    (float) (-stats.length / 2.0)));
            ParticleUtil.spawn(world, "SPLASH", stern, 10, 0.5, 0.2, 0.8, 0.1);
            ParticleUtil.spawn(world, "BUBBLE", stern, 4, 0.5, 0.3, 0.5, 0.1);
        }

        // Track dust.
        if ((vehicle.getType() == VehicleType.TANK || vehicle.getType() == VehicleType.BULLDOZER
                || vehicle.getType() == VehicleType.EXCAVATOR)
                && speed > 1.5 && vehicle.isGrounded() && now % 5 == 0) {
            Location rear = vehicle.getWorldLoc(new Vector3f(0, 0.2f,
                    (float) (-stats.length / 2.0)));
            ParticleUtil.spawn(world, "POOF", rear, 4, 0.8, 0.2, 0.5, 0.03);
        }

        // Locomotive chimney smoke.
        if ((vehicle.getType() == VehicleType.LOCOMOTIVE
                || vehicle.getType() == VehicleType.PASSENGER_TRAIN
                || vehicle.getType() == VehicleType.MINECART_RACER)
                && speed > 1 && now % 4 == 0) {
            Location stack = vehicle.getWorldLoc(new Vector3f(0,
                    (float) (stats.height + 0.2), (float) (stats.length * 0.22)));
            ParticleUtil.spawn(world, "CAMPFIRE_COSY_SMOKE", stack, 3, 0.15, 0.1, 0.15, 0.05);
            ParticleUtil.spawn(world, "LARGE_SMOKE", stack, 2, 0.15, 0.1, 0.15, 0.03);
        }

        // Helicopter rotor wash near the ground.
        if (vehicle.getType() == VehicleType.HELICOPTER && hasDriver && !vehicle.isGrounded() && now % 6 == 0) {
            Location below = vehicle.getLocation().clone();
            double ground = com.example.vanillavehicles.collision.CollisionHandler
                    .groundLevel(world, below.getX(), below.getY(), below.getZ());
            if (ground != Double.NEGATIVE_INFINITY && below.getY() - ground < 5) {
                below.setY(ground + 0.3);
                ParticleUtil.spawn(world, "POOF", below, 6, 2.0, 0.3, 2.0, 0.05);
            }
        }

        // Siren light beams.
        if (vehicle.isSirenOn() && vehicle.getStats().siren && now % 4 == 0) {
            Location top = vehicle.getWorldLoc(new Vector3f(0,
                    (float) (stats.height + 0.4), 0));
            boolean phaseA = (now / 6) % 2 == 0;
            ParticleUtil.dust(world, top, 4,
                    phaseA ? org.bukkit.Color.RED : org.bukkit.Color.BLUE, 1.2f);
        }

        // Headlight beams at night.
        if (vehicle.isLightsOn() && now % 8 == 0) {
            long time = world.getTime();
            if (time > 13000 && time < 23000) {
                for (int i = 1; i <= 3; i++) {
                    Location beam = vehicle.getWorldLoc(new Vector3f(0, 0.6f,
                            (float) (stats.length / 2.0 + i * 1.5)));
                    ParticleUtil.dust(world, beam, 1, org.bukkit.Color.WHITE, 0.8f);
                }
            }
        }
    }
}
