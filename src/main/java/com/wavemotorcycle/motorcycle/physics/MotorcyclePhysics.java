package com.wavemotorcycle.motorcycle.physics;

import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.motorcycle.WheelieState;
import com.wavemotorcycle.motorcycle.input.MotorcycleInput;
import com.wavemotorcycle.util.MathUtil;
import org.bukkit.World;

/**
 * Custom motorcycle physics, integrated once per update tick.
 *
 * <p>The model is an arcade bicycle: a signed scalar speed along the heading,
 * steering limited by a speed-dependent maximum angle, a rear-axle-pivoted wheelie
 * rotation, ground following with step-up/cliff detection and a sub-stepped
 * bounding-box collision.
 */
public final class MotorcyclePhysics {

    /** One physics result, consumed by the controller for damage/effects/sounds. */
    public static final class StepResult {
        public boolean collided;
        public double impactSpeed;
        public boolean landed;
        public double landImpact;
        public boolean fellIntoVoid;
    }

    private final ConfigManager cfg;
    private final CollisionResolver collision = new CollisionResolver();

    public MotorcyclePhysics(ConfigManager cfg) {
        this.cfg = cfg;
    }

    public StepResult step(MotorcycleController c, MotorcycleInput in, TerrainSurface terrain, double dt) {
        StepResult r = new StepResult();
        World world = c.world();

        double s = c.speed();
        double maxSpeed = cfg.maxSpeed;

        // Traction of the surface under the main contact point.
        double contactX = c.x() + MathUtil.dirX(c.yaw()) * MotorcycleController.HALF_WHEELBASE;
        double contactZ = c.z() + MathUtil.dirZ(c.yaw()) * MotorcycleController.HALF_WHEELBASE;
        double traction = terrain.traction(world, contactX, contactZ, c.y());

        // ------------------------------------------------------------------
        // Longitudinal dynamics
        // ------------------------------------------------------------------
        boolean canDrive = c.engineRunning() && c.fuelOk();
        if (canDrive) {
            if (in.throttle) {
                s += cfg.acceleration * traction * dt;
            }
            if (in.brake) {
                if (s > 0.002) {
                    s -= cfg.braking * dt;
                } else {
                    s -= cfg.acceleration * 0.6 * traction * dt; // reverse
                }
            }
        } else if (in.brake) {
            if (s > 0.002) {
                s -= cfg.braking * dt;
            } else {
                s -= cfg.acceleration * 0.3 * traction * dt; // engine off: weak roll-back
            }
        }
        double fr = cfg.friction * dt;
        if (s > 0) {
            s = Math.max(0, s - fr);
        } else if (s < 0) {
            s = Math.min(0, s + fr);
        }
        // Wheelies cost a little acceleration.
        if (cfg.wheelieMaxAngle > 0 && in.throttle) {
            double wf = Math.min(1.0, c.wheelieAngle() / cfg.wheelieMaxAngle);
            if (wf > 0.05) {
                s = Math.min(s, maxSpeed * (1.0 - 0.25 * wf));
            }
        }
        s = MathUtil.clamp(s, -cfg.reverseSpeed, maxSpeed);

        // ------------------------------------------------------------------
        // Steering
        // ------------------------------------------------------------------
        double speedFrac = Math.min(1.0, Math.abs(s) / Math.max(0.001, maxSpeed));
        double maxSteer = cfg.maxSteerAngle * (1.0 - 0.6 * speedFrac);
        double target = 0.0;
        if (in.hasSteer) {
            double viewError = MathUtil.wrap180(in.viewYaw - c.yaw());
            target = MathUtil.clamp(viewError * 0.75, -maxSteer, maxSteer);
            double wheeliePenalty = 1.0 - 0.5 * Math.min(1.0, c.wheelieAngle() / Math.max(0.001, cfg.wheelieMaxAngle));
            target *= Math.max(0.3, wheeliePenalty);
        }
        double steerSpeed = 45.0 / 20.0 * dt; // degrees per tick
        double newSteer = MathUtil.clamp(
                c.steerAngle() + MathUtil.clamp(target - c.steerAngle(), -steerSpeed, steerSpeed),
                -cfg.maxSteerAngle, cfg.maxSteerAngle);
        c.setSteerAngle(newSteer);

        if (Math.abs(s) > 0.004) {
            double steerRad = newSteer * MathUtil.DEG_TO_RAD;
            double yawRate = Math.tan(steerRad) * (s / MotorcycleController.WHEELBASE) * cfg.turnRate;
            // Low-speed bonus makes parking possible instead of glacial.
            double lowSpeedBonus = 1.0 + 2.0 * Math.max(0.0, 1.0 - Math.abs(s) / 0.25);
            yawRate *= lowSpeedBonus;
            if (c.airborne()) {
                yawRate *= 0.35; // limited air control
            }
            c.setYaw((float) (c.yaw() + yawRate * MathUtil.RAD_TO_DEG * dt));
        }

        // ------------------------------------------------------------------
        // Wheelie
        // ------------------------------------------------------------------
        updateWheelie(c, in, s, dt, r);

        // ------------------------------------------------------------------
        // Vertical dynamics / ground following
        // ------------------------------------------------------------------
        double dirX = MathUtil.dirX(c.yaw());
        double dirZ = MathUtil.dirZ(c.yaw());
        boolean frontContact = s >= 0;
        double cx = c.x() + dirX * (frontContact ? MotorcycleController.HALF_WHEELBASE : -MotorcycleController.HALF_WHEELBASE);
        double cz = c.z() + dirZ * (frontContact ? MotorcycleController.HALF_WHEELBASE : -MotorcycleController.HALF_WHEELBASE);

        if (!c.airborne()) {
            double gy = terrain.groundY(world, cx, cz, c.y());
            if (Double.isNaN(gy)) {
                c.setAirborne(true);
                c.setVy(0);
            } else {
                double diff = gy - c.y();
                if (diff > 0.55) {
                    c.setAirborne(true);   // cliff edge
                    c.setVy(0);
                } else if (diff > 0.02) {
                    c.setPosition(c.x(), c.y() + Math.min(diff, 0.22 * dt), c.z()); // smooth step-up
                } else if (diff < -0.5) {
                    c.setAirborne(true);   // stepping off a ledge
                    c.setVy(0);
                } else {
                    c.setPosition(c.x(), gy, c.z());
                }
            }
        }

        if (c.airborne()) {
            c.setVy(MathUtil.clamp(c.vy() - cfg.gravity * dt, -1.2, 0.8));
            c.setWheelieAngle(Math.max(0, c.wheelieAngle() - 1.2 * dt));
            double ny = c.y() + c.vy() * dt;
            double gy = terrain.groundY(world, cx, cz, ny);
            if (!Double.isNaN(gy) && ny <= gy) {
                double impact = -c.vy();
                c.setPosition(c.x(), gy, c.z());
                c.setVy(0);
                c.setAirborne(false);
                if (impact > 0.10) {
                    r.landed = true;
                    r.landImpact = impact;
                }
            } else {
                c.setPosition(c.x(), ny, c.z());
            }
            if (ny < world.getMinHeight() - 16) {
                r.fellIntoVoid = true;
            }
        }

        // ------------------------------------------------------------------
        // Horizontal movement with collision
        // ------------------------------------------------------------------
        double dx = dirX * s * dt;
        double dz = dirZ * s * dt;
        if (collision.move(c, dx, dz)) {
            r.collided = true;
            r.impactSpeed = Math.abs(s);
            // Small bounce, then the controller may add damage/effects.
            c.setSpeed(-s * 0.12);
        }

        c.setSpeed(s);
        return r;
    }

    private void updateWheelie(MotorcycleController c, MotorcycleInput in, double s, double dt, StepResult r) {
        if (!cfg.wheelieEnabled) {
            if (c.wheelieAngle() > 0) {
                c.setWheelieAngle(Math.max(0, c.wheelieAngle() - 2 * dt));
                c.setWheelieState(c.wheelieAngle() <= 0 ? WheelieState.NORMAL : WheelieState.LOWERING);
            }
            return;
        }
        double a = c.wheelieAngle();
        WheelieState st = c.wheelieState();
        boolean holding = in.throttle && c.engineRunning() && c.fuelOk();

        if (in.wheelieTap && !c.airborne() && Math.abs(s) > cfg.wheelieMinSpeed && a < 5.0) {
            st = WheelieState.LIFTING;
        }

        switch (st) {
            case LIFTING:
                a += cfg.wheelieLiftForce * dt * (holding ? 1.0 : 0.35);
                if (a >= cfg.wheelieMaxAngle - 0.5) {
                    a = cfg.wheelieMaxAngle;
                    st = WheelieState.WHEELIE;
                }
                if (!holding) {
                    st = a < 1.0 ? WheelieState.NORMAL : WheelieState.LOWERING;
                }
                break;
            case WHEELIE:
                if (!holding || Math.abs(s) < 0.06) {
                    st = WheelieState.LOWERING; // losing throttle collapses the wheelie
                } else {
                    // Subtle wobble so the wheelie feels alive but stable.
                    a = cfg.wheelieMaxAngle + Math.sin(c.tickCount() * 0.5) * cfg.wheelieBalance * 40.0;
                }
                break;
            case LOWERING:
                a -= 0.45 * dt;
                if (a <= 0) {
                    a = 0;
                    st = WheelieState.NORMAL;
                }
                break;
            case NORMAL:
            default:
                break;
        }
        c.setWheelieAngle(MathUtil.clamp(a, 0, cfg.wheelieMaxAngle + 2));
        c.setWheelieState(st);
    }
}
