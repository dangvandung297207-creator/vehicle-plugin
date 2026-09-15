package com.example.vanillavehicles.physics;

import com.example.vanillavehicles.collision.CollisionHandler;
import com.example.vanillavehicles.input.InputState;
import com.example.vanillavehicles.model.ModelMath;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleStats;
import com.example.vanillavehicles.vehicle.VehicleType;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Arcade physics for every {@link com.example.vanillavehicles.vehicle.PhysicsType}.
 * Called once per tick per active vehicle from the central update loop.
 */
public final class PhysicsEngine {

    private PhysicsEngine() {
    }

    private static final double GRAVITY = 22.0;

    public static void update(Vehicle vehicle, InputState input, double dt) {
        switch (vehicle.getType().getPhysics()) {
            case BOAT:
                updateBoat(vehicle, input, dt);
                break;
            case AIRCRAFT:
                updateAircraft(vehicle, input, dt);
                break;
            case TRAIN:
                updateTrain(vehicle, input, dt);
                break;
            case BIKE:
            case CAR:
            case HEAVY:
            case CONSTRUCTION:
            default:
                updateGround(vehicle, input, dt);
                break;
        }
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    private static boolean enhanced(InputState input) {
        return input != null && input.enhanced
                && System.currentTimeMillis() - input.lastEnhancedInput < 1500;
    }

    /** Steering demand in -1..1, positive turns left (port). */
    private static double steerInput(Vehicle vehicle, InputState input, boolean allowMouse) {
        if (input == null) {
            return 0;
        }
        if (enhanced(input)) {
            double steer = -input.strafe;
            if (allowMouse && vehicle.useMouseSteering() && input.hasLook) {
                double mouse = ModelMath.clamp(
                        -ModelMath.wrapDegrees(input.lookYaw - vehicle.getHeading()) / 50.0, -1, 1);
                if (Math.abs(mouse) > 0.12) {
                    steer = ModelMath.clamp(steer + mouse * 0.8, -1, 1);
                }
            }
            return steer;
        }
        if (!allowMouse || !vehicle.useMouseSteering() || !input.hasLook) {
            return 0;
        }
        double wrap = ModelMath.wrapDegrees(input.lookYaw - vehicle.getHeading());
        if (Math.abs(wrap) < 5) {
            return 0;
        }
        return ModelMath.clamp(-wrap / 50.0, -1, 1);
    }

    // ------------------------------------------------------------------
    // Ground vehicles
    // ------------------------------------------------------------------

    private static void updateGround(Vehicle vehicle, InputState input, double dt) {
        VehicleStats stats = vehicle.getStats();
        double speed = vehicle.getSpeed();
        double maxForward = stats.maxSpeed * (vehicle.isBoosting() ? stats.boostMultiplier : 1.0);
        double accel = stats.acceleration * (vehicle.isBoosting() ? stats.boostMultiplier : 1.0);
        double steer = steerInput(vehicle, input, true);
        vehicle.setLastSteer(steer);

        boolean braking = false;
        if (input != null && enhanced(input)) {
            double throttle = input.forward;
            if (throttle > 0.05) {
                if (input.brake && speed < 1.0 && stats.burnout) {
                    vehicle.setBurnouting(true);
                } else {
                    vehicle.setBurnouting(false);
                    double power = accel * Math.min(1.0, throttle) * (1.0 - (speed / maxForward) * 0.55);
                    speed = Math.min(maxForward, speed + Math.max(0, power) * dt);
                }
            } else if (throttle < -0.05) {
                vehicle.setBurnouting(false);
                if (speed > 0.5) {
                    speed = Math.max(0, speed - stats.brake * dt);
                    braking = true;
                } else {
                    speed = Math.max(-stats.reverseSpeed, speed + accel * 0.6 * throttle * dt);
                }
            } else {
                vehicle.setBurnouting(false);
                speed = coast(speed, stats, dt);
            }
        } else if (input != null) {
            double desired;
            if (input.brake && speed < 1.0 && speed > -0.5 && stats.maxSpeed > 0) {
                // Hold sneak at standstill to reverse (fallback brake doubles as reverse).
                desired = -stats.reverseSpeed * 0.6;
            } else if (input.gear < 0) {
                desired = input.gear * (stats.reverseSpeed / 0.3);
            } else {
                desired = input.gear * maxForward;
            }
            if (input.gear > 0.05 && input.brake && Math.abs(speed) < 1.0 && stats.burnout) {
                vehicle.setBurnouting(true);
                desired = 0;
            } else {
                vehicle.setBurnouting(false);
            }
            if (input.brake && desired >= 0) {
                speed = ModelMath.approach(speed, 0, stats.brake * 1.2 * dt);
                braking = speed > 0.5;
            } else if (speed < desired) {
                speed = Math.min(desired, speed + accel * dt);
            } else if (speed > desired) {
                speed = Math.max(desired, speed - Math.max(stats.brake, stats.friction * 2) * dt);
                braking = speed > 1.0 && desired < speed - 1.0;
            }
        } else {
            vehicle.setBurnouting(false);
            speed = coast(speed, stats, dt);
        }

        // Drift (kart): braking hard at speed while steering.
        boolean drifting = stats.drift && braking && Math.abs(speed) > 9 && Math.abs(steer) > 0.3;
        vehicle.setDrifting(drifting);
        vehicle.setBraking(braking);

        // Steering authority grows with rolling speed, fades at very high speed.
        double authority = ModelMath.clamp(Math.abs(speed) / 5.0, 0, 1)
                * (1.0 / (1.0 + Math.abs(speed) / 24.0));
        double direction = speed >= 0 ? 1 : -1;
        double heading = vehicle.getHeading();
        if (stats.turnInPlace && Math.abs(speed) < 1.0) {
            heading += steer * stats.steering * 0.9 * (dt / 0.05);
        } else {
            heading += steer * stats.steering * authority * direction * (dt / 0.05);
            if (drifting) {
                heading += steer * 2.2 * (dt / 0.05);
                speed = ModelMath.approach(speed, speed * 0.985, 4 * dt);
            }
        }
        vehicle.setHeading(heading);
        vehicle.setSpeed(speed);

        groundMove(vehicle, dt, true);
    }

    private static double coast(double speed, VehicleStats stats, double dt) {
        double drag = 0.25;
        double next = speed - Math.signum(speed) * (stats.friction + Math.abs(speed) * drag) * dt;
        if (Math.signum(next) != Math.signum(speed)) {
            return 0;
        }
        return next;
    }

    /** Shared ground translation with collision, gravity and water handling. */
    private static void groundMove(Vehicle vehicle, double dt, boolean swimPenalty) {
        VehicleStats stats = vehicle.getStats();
        Location loc = vehicle.getLocation();
        World world = vehicle.getWorld();
        double yawRad = Math.toRadians(vehicle.getHeading());
        double speed = vehicle.getSpeed();
        double nx = loc.getX() - Math.sin(yawRad) * speed * dt;
        double nz = loc.getZ() + Math.cos(yawRad) * speed * dt;
        double y = loc.getY();

        boolean inWater = CollisionHandler.isWater(world, nx, y + 0.3, nz)
                || CollisionHandler.isWater(world, nx, y + 0.9, nz);
        vehicle.setInWater(inWater);
        if (CollisionHandler.isLava(world, nx, y + 0.3, nz)) {
            vehicle.damage(2.0, null);
        }
        if (swimPenalty && inWater && vehicle.getType().getPhysics() != com.example.vanillavehicles.vehicle.PhysicsType.BOAT) {
            vehicle.setSpeed(vehicle.getSpeed() * (1.0 - 2.0 * dt));
            if (Math.abs(vehicle.getSpeed()) > 3) {
                vehicle.setSpeed(Math.signum(vehicle.getSpeed()) * 3);
            }
            speed = vehicle.getSpeed();
            nx = loc.getX() - Math.sin(yawRad) * speed * dt;
            nz = loc.getZ() + Math.cos(yawRad) * speed * dt;
        }

        double halfLen = stats.length / 2.0 * 0.85;
        double halfW = stats.width / 2.0 * 0.85;
        if (CollisionHandler.collides(world, nx, y + 0.1, nz, halfLen, halfW, stats.height, vehicle.getHeading())) {
            if (stats.stepUp && !CollisionHandler.collides(world, nx, y + 1.2, nz,
                    halfLen, halfW, stats.height, vehicle.getHeading())) {
                vehicle.getLocation().setY(y + 1.0);
                y = y + 1.0;
            } else {
                vehicle.onCrash(Math.abs(speed));
                return;
            }
        }
        loc.setX(nx);
        loc.setZ(nz);

        double ground = CollisionHandler.groundLevel(world, nx, y, nz);
        double vy = vehicle.getVy();
        if (y <= ground + 0.02 && vy <= 0) {
            if (!vehicle.isGrounded() && vy < -14) {
                vehicle.onCrash(-vy * 0.6);
            }
            loc.setY(ground);
            vehicle.setVy(0);
            vehicle.setGrounded(true);
        } else {
            vy -= GRAVITY * dt;
            double ny = y + vy * dt;
            if (ny <= ground) {
                if (vy < -14) {
                    vehicle.onCrash(-vy * 0.6);
                }
                ny = ground;
                vy = 0;
                vehicle.setGrounded(true);
            } else {
                vehicle.setGrounded(false);
            }
            loc.setY(ny);
            vehicle.setVy(vy);
        }
    }

    // ------------------------------------------------------------------
    // Boats
    // ------------------------------------------------------------------

    private static void updateBoat(Vehicle vehicle, InputState input, double dt) {
        VehicleStats stats = vehicle.getStats();
        double speed = vehicle.getSpeed();
        double steer = steerInput(vehicle, input, true);
        vehicle.setLastSteer(steer);
        vehicle.setBurnouting(false);

        if (input != null && enhanced(input)) {
            double throttle = input.forward;
            if (throttle > 0.05) {
                speed = Math.min(stats.maxSpeed, speed + stats.acceleration * throttle * dt);
            } else if (throttle < -0.05) {
                if (speed > 0.5) {
                    speed = Math.max(0, speed - stats.brake * dt);
                } else {
                    speed = Math.max(-stats.reverseSpeed, speed + stats.acceleration * 0.6 * throttle * dt);
                }
            } else {
                speed = coast(speed, stats, dt);
            }
        } else if (input != null) {
            double desired = input.gear < 0
                    ? input.gear * (stats.reverseSpeed / 0.3)
                    : input.gear * stats.maxSpeed;
            if (input.brake) {
                speed = ModelMath.approach(speed, 0, stats.brake * dt);
            } else if (speed < desired) {
                speed = Math.min(desired, speed + stats.acceleration * dt);
            } else {
                speed = Math.max(desired, speed - stats.brake * dt);
            }
        } else {
            speed = coast(speed, stats, dt);
        }

        double authority = ModelMath.clamp(Math.abs(speed) / 4.0, 0, 1);
        vehicle.setHeading(vehicle.getHeading()
                + steer * stats.steering * authority * (speed >= 0 ? 1 : -1) * (dt / 0.05));
        vehicle.setSpeed(speed);

        // Translation on the water surface.
        Location loc = vehicle.getLocation();
        World world = vehicle.getWorld();
        double yawRad = Math.toRadians(vehicle.getHeading());
        double nx = loc.getX() - Math.sin(yawRad) * speed * dt;
        double nz = loc.getZ() + Math.cos(yawRad) * speed * dt;

        double surface = CollisionHandler.liquidSurface(world, nx, loc.getY(), nz);
        boolean afloat = surface != Double.NEGATIVE_INFINITY;
        vehicle.setInWater(afloat || CollisionHandler.isWater(world, nx, loc.getY(), nz));

        double halfLen = stats.length / 2.0 * 0.85;
        double halfW = stats.width / 2.0 * 0.85;
        double checkY = afloat ? surface - 0.4 : loc.getY();
        if (CollisionHandler.collides(world, nx, checkY + 0.1, nz, halfLen, halfW,
                stats.height, vehicle.getHeading())) {
            vehicle.onCrash(Math.abs(speed));
            return;
        }
        if (!afloat) {
            // Beached: heavy drag, settle on the ground.
            vehicle.setSpeed(speed * (1.0 - 3.0 * dt));
            double ground = CollisionHandler.groundLevel(world, nx, loc.getY(), nz);
            if (ground != Double.NEGATIVE_INFINITY && loc.getY() > ground) {
                loc.setY(Math.max(ground, loc.getY() - 8 * dt));
            }
            loc.setX(nx);
            loc.setZ(nz);
            vehicle.setGrounded(true);
            return;
        }
        vehicle.setGrounded(false);
        loc.setX(nx);
        loc.setZ(nz);
        double targetY = surface - 0.25;
        loc.setY(ModelMath.lerp(loc.getY(), targetY, Math.min(1, 6 * dt)));
        vehicle.setVy(0);
    }

    // ------------------------------------------------------------------
    // Aircraft
    // ------------------------------------------------------------------

    private static void updateAircraft(Vehicle vehicle, InputState input, double dt) {
        if (vehicle.getType() == VehicleType.HELICOPTER) {
            updateHelicopter(vehicle, input, dt);
        } else {
            updatePlane(vehicle, input, dt);
        }
    }

    private static void updateHelicopter(Vehicle vehicle, InputState input, double dt) {
        VehicleStats stats = vehicle.getStats();
        double speed = vehicle.getSpeed();

        // Heading follows the driver's view (mouse flight), damped.
        if (input != null && input.hasLook) {
            double wrap = ModelMath.wrapDegrees(input.lookYaw - vehicle.getHeading());
            vehicle.setHeading(vehicle.getHeading() + ModelMath.clamp(wrap, -5, 5) * (dt / 0.05));
        }
        double steer = enhanced(input) ? -input.strafe : 0;
        vehicle.setLastSteer(steer);

        double maxForward = stats.maxSpeed * (vehicle.isBoosting() ? stats.boostMultiplier : 1.0);
        if (input != null && enhanced(input)) {
            if (input.forward > 0.05) {
                speed = Math.min(maxForward, speed + stats.acceleration * input.forward * dt);
            } else if (input.forward < -0.05) {
                speed = Math.max(-stats.reverseSpeed, speed + stats.acceleration * input.forward * dt);
            } else {
                speed = coast(speed, stats, dt);
            }
        } else if (input != null) {
            double desired = input.gear < 0
                    ? input.gear * (stats.reverseSpeed / 0.3)
                    : input.gear * maxForward;
            if (speed < desired) {
                speed = Math.min(desired, speed + stats.acceleration * dt);
            } else {
                speed = Math.max(desired, speed - stats.brake * dt);
            }
        } else {
            speed = coast(speed, stats, dt);
        }
        vehicle.setSpeed(speed);

        // Vertical: look pitch collective + Space lift + sneak descend.
        double lookPitch = input != null && input.hasLook ? vehicle.getLookPitch() : 0;
        double vyTarget = -Math.sin(Math.toRadians(ModelMath.clamp(lookPitch, -45, 45)))
                * Math.max(0, speed) * 0.9;
        boolean jump = input != null && (input.jump || input.consumeJumpPulse());
        boolean descend = input != null && (input.brake || (enhanced(input) && input.sneak));
        if (jump) {
            vyTarget += 7;
        }
        if (descend) {
            vyTarget -= 7;
        }
        // Gentle hover: hold altitude without input.
        double vy = ModelMath.lerp(vehicle.getVy(), vyTarget, Math.min(1, 3 * dt));
        if (input == null && vehicle.isGrounded() && Math.abs(vy) < 0.05) {
            vy = 0;
        }
        vehicle.setVy(vy);

        Location loc = vehicle.getLocation();
        World world = vehicle.getWorld();
        double yawRad = Math.toRadians(vehicle.getHeading());
        double nx = loc.getX() - Math.sin(yawRad) * speed * dt;
        double nz = loc.getZ() + Math.cos(yawRad) * speed * dt;
        // Lateral strafe for enhanced mode.
        if (steer != 0) {
            nx += -Math.cos(yawRad) * -steer * 7 * dt;
            nz += -Math.sin(yawRad) * -steer * 7 * dt;
        }
        double ny = loc.getY() + vy * dt;

        double ground = CollisionHandler.groundLevel(world, nx, Math.max(ny, loc.getY()), nz);
        if (ny <= ground) {
            ny = ground;
            if (vy < -12) {
                vehicle.onCrash(-vy * 0.7);
            }
            vehicle.setVy(0);
            vehicle.setGrounded(true);
        } else {
            vehicle.setGrounded(false);
        }
        if (CollisionHandler.collides(world, nx, ny + 0.1, nz,
                stats.length / 2.0 * 0.8, stats.width / 2.0 * 0.8,
                stats.height, vehicle.getHeading())) {
            vehicle.onCrash(Math.abs(speed) + Math.abs(vy));
            return;
        }
        loc.setX(nx);
        loc.setZ(nz);
        loc.setY(ny);
        vehicle.setPitchUp(ModelMath.clamp(vy * 1.5 - Math.max(0, speed) * 0.35, -14, 14));
    }

    private static void updatePlane(Vehicle vehicle, InputState input, double dt) {
        VehicleStats stats = vehicle.getStats();
        double throttle = vehicle.getThrottle();
        boolean jet = vehicle.getType() == VehicleType.FIGHTER_JET;

        if (input != null && enhanced(input)) {
            if (input.forward > 0.05) {
                throttle = Math.min(1, throttle + 0.7 * dt);
            } else if (input.forward < -0.05) {
                throttle = Math.max(0, throttle - 0.9 * dt);
            }
        } else if (input != null) {
            throttle = ModelMath.clamp(input.gear, 0, 1);
        }
        vehicle.setThrottle(throttle);

        double maxSpeed = stats.maxSpeed * (vehicle.isBoosting() ? stats.boostMultiplier : 1.0);
        double targetSpeed = throttle * maxSpeed;
        double speed = vehicle.getSpeed();
        if (speed < targetSpeed) {
            speed = Math.min(targetSpeed, speed + stats.acceleration * dt);
        } else {
            speed = Math.max(targetSpeed, speed - (stats.friction * 2 + 1.5) * dt);
        }
        if (input != null && !enhanced(input) && input.brake) {
            speed = Math.max(0, speed - stats.brake * dt);
        }
        vehicle.setSpeed(speed);

        // Steering: keyboard plus gentle mouse assist toward the view.
        double steer = steerInput(vehicle, input, true);
        vehicle.setLastSteer(steer);
        double authority = ModelMath.clamp(speed / 10.0, 0, 1);
        vehicle.setHeading(vehicle.getHeading()
                + steer * stats.steering * authority * (dt / 0.05));

        double lookPitch = input != null && input.hasLook ? vehicle.getLookPitch() : 0;
        double climb = ModelMath.clamp(-lookPitch, -35, 35);
        if (input != null && (input.jump || input.consumeJumpPulse())) {
            climb = Math.min(35, climb + 12);
        }
        double takeoff = maxSpeed * 0.45;

        Location loc = vehicle.getLocation();
        World world = vehicle.getWorld();
        double yawRad = Math.toRadians(vehicle.getHeading());

        if (vehicle.isGrounded()) {
            // Taxi / takeoff roll.
            double nx = loc.getX() - Math.sin(yawRad) * speed * dt;
            double nz = loc.getZ() + Math.cos(yawRad) * speed * dt;
            if (CollisionHandler.collides(world, nx, loc.getY() + 0.1, nz,
                    stats.length / 2.0 * 0.8, stats.width / 2.0 * 0.8,
                    stats.height, vehicle.getHeading())) {
                vehicle.onCrash(Math.abs(speed));
                return;
            }
            loc.setX(nx);
            loc.setZ(nz);
            double ground = CollisionHandler.groundLevel(world, nx, loc.getY(), nz);
            if (ground == Double.NEGATIVE_INFINITY || loc.getY() > ground + 0.3) {
                vehicle.setGrounded(false);
            } else {
                loc.setY(ground);
                if (speed > takeoff && climb > 6) {
                    vehicle.setGrounded(false);
                    loc.setY(ground + 0.5);
                }
            }
            vehicle.setPitchUp(0);
            vehicle.setVy(0);
            return;
        }

        // Airborne flight along the climb angle.
        double climbRad = Math.toRadians(climb);
        if (speed < takeoff * 0.55 && !jet) {
            // Stall: nose drops.
            climbRad = Math.toRadians(-18);
        }
        double horizontal = Math.cos(climbRad) * speed;
        double nx = loc.getX() - Math.sin(yawRad) * horizontal * dt;
        double nz = loc.getZ() + Math.cos(yawRad) * horizontal * dt;
        double ny = loc.getY() + Math.sin(climbRad) * speed * dt;
        vehicle.setVy(Math.sin(climbRad) * speed);

        if (CollisionHandler.collides(world, nx, ny + 0.1, nz,
                stats.length / 2.0 * 0.8, stats.width / 2.0 * 0.8,
                stats.height, vehicle.getHeading())) {
            vehicle.onCrash(Math.abs(speed));
            return;
        }
        double ground = CollisionHandler.groundLevel(world, nx, ny, nz);
        if (ny <= ground) {
            double impact = -vehicle.getVy();
            ny = ground;
            vehicle.setGrounded(true);
            vehicle.setVy(0);
            vehicle.setPitchUp(0);
            if (impact > 11) {
                vehicle.onCrash(impact);
            }
        } else {
            vehicle.setPitchUp(climb);
        }
        loc.setX(nx);
        loc.setZ(nz);
        loc.setY(ny);
    }

    // ------------------------------------------------------------------
    // Trains
    // ------------------------------------------------------------------

    private static void updateTrain(Vehicle vehicle, InputState input, double dt) {
        VehicleStats stats = vehicle.getStats();
        boolean singleCar = vehicle.getType() == VehicleType.MINECART_RACER;
        double target = vehicle.getTargetSpeed();

        if (input != null && enhanced(input)) {
            if (input.forward > 0.05) {
                target = Math.min(stats.maxSpeed, target + 9 * dt);
            } else if (input.forward < -0.05) {
                target = Math.max(-stats.reverseSpeed, target - 13 * dt);
            }
        } else if (input != null) {
            target = input.gear < 0
                    ? input.gear * (stats.reverseSpeed / 0.3)
                    : input.gear * stats.maxSpeed;
            if (input.brake) {
                target = 0;
            }
        }
        vehicle.setTargetSpeed(target);

        double speed = vehicle.getSpeed();
        if (speed < target) {
            speed = Math.min(target, speed + stats.acceleration * dt);
        } else if (speed > target) {
            speed = Math.max(target, speed - stats.brake * dt);
        }
        vehicle.setSpeed(speed);

        // Rails feel: tiny steering, keyboard only (no mouse steering).
        double steer = 0;
        if (input != null && enhanced(input)) {
            steer = -input.strafe;
        } else if (singleCar && input != null && input.hasLook
                && vehicle.useMouseSteering()) {
            steer = ModelMath.clamp(
                    -ModelMath.wrapDegrees(input.lookYaw - vehicle.getHeading()) / 50.0, -1, 1);
        }
        vehicle.setLastSteer(steer);
        double authority = ModelMath.clamp(Math.abs(speed) / 5.0, 0, 1);
        vehicle.setHeading(vehicle.getHeading()
                + steer * stats.steering * authority * (speed >= 0 ? 1 : -1) * (dt / 0.05));

        vehicle.setBurnouting(false);
        vehicle.setDrifting(false);
        groundMove(vehicle, dt, true);
    }
}
