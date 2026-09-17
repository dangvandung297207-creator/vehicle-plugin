package com.wave100.entity;

import com.wave100.WaveConfig;
import net.minecraft.util.Mth;

/**
 * Velocity-based motorcycle physics: acceleration, braking, friction,
 * speed-sensitive steering, lean and the wheelie balance model.
 *
 * <p>The entity drives this class on the server; all world interaction
 * (collision resolution, gravity via {@code move()}) stays in the entity. The
 * same math could later be reused for client-side prediction.</p>
 */
public class WavePhysics {

    /** Turn rate at full steering lock and low speed, degrees per tick. */
    public static final float BASE_TURN_RATE = 7.0F;
    /** Speed below which steering has no effect (cannot pivot in place). */
    public static final float STEER_SPEED_GATE = 0.22F;
    /** How fast the lean angle moves toward its target, degrees per tick. */
    public static final float LEAN_RATE = 3.2F;

    /**
     * Per-tick driver input snapshot, mirrored from the client keybinds.
     */
    public static final class Input {
        public boolean forward;
        public boolean back;
        public boolean left;
        public boolean right;
        public boolean wheelie;
        public boolean brakeLight; // derived: back pressed while moving forward

        public void clear() {
            forward = back = left = right = wheelie = brakeLight = false;
        }
    }

    /** Signed forward speed in blocks per tick (negative = reversing). */
    public double speed;
    /** Current steering angle in degrees, positive = left. */
    public float steering;
    /** Applied yaw rate for the last tick, degrees (positive = left turn). */
    public float yawRate;
    /** Current lean in degrees, positive = leaning left. */
    public float lean;
    /** Wheelie pitch angle in degrees. */
    public float wheelieAngle;
    /** Wheelie angular velocity, degrees per tick. */
    public float wheelieVelocity;
    /** Front suspension compression, in model units (positive = compressed). */
    public float suspension;
    /** Front suspension velocity for springy landings. */
    public float suspensionVelocity;

    /** Smoothing helper that never overshoots. */
    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /**
     * Advances steering, longitudinal speed, yaw rate and lean one tick.
     *
     * @param in           driver input
     * @param onGround     wheels touching ground
     * @param enginePower  engine able to deliver torque (running + fuel + alive)
     * @param gear         current gear 1..4
     * @param powerFactor  engine health multiplier
     */
    public void tickDriving(Input in, boolean onGround, boolean enginePower, int gear, float powerFactor) {
        double maxSpeed = WaveConfig.Vehicle.maxSpeed();
        double acceleration = WaveConfig.Vehicle.acceleration();
        double braking = WaveConfig.Vehicle.braking();
        double friction = WaveConfig.Vehicle.friction();
        double reverseSpeed = WaveConfig.Vehicle.reverseSpeed();
        float maxSteer = (float) WaveConfig.Vehicle.steeringAngle();
        float steerResponse = (float) WaveConfig.Vehicle.steeringResponse();
        float maxLean = (float) WaveConfig.Vehicle.leanAngle();
        boolean wheelieUp = wheelieAngle > 1.0F;

        // ------------------------------------------------------------------
        // Steering: handlebars swing toward the input, then the bike yaws.
        // ------------------------------------------------------------------
        float steerInput = (in.left ? 1.0F : 0.0F) - (in.right ? 1.0F : 0.0F);
        float steerTarget = steerInput * maxSteer;
        this.steering = approach(this.steering, steerTarget, steerResponse);

        // ------------------------------------------------------------------
        // Longitudinal dynamics.
        // ------------------------------------------------------------------
        if (in.forward && enginePower) {
            double speedFrac = Math.min(1.0, Math.abs(this.speed) / maxSpeed);
            double torque = WaveEngine.GEAR_TORQUE[gear - 1];
            double force = acceleration * torque * powerFactor * (1.0 - 0.62 * speedFrac);
            if (wheelieUp) {
                force *= 0.80;
            }
            this.speed += force;
        }

        boolean brakingNow = in.back && this.speed > 0.02;
        if (in.back) {
            if (brakingNow) {
                this.speed -= braking;
            } else if (enginePower && !wheelieUp) {
                // reverse gear, much weaker than forward drive
                this.speed -= acceleration * 0.55;
            }
        }

        // engine braking + rolling friction when coasting
        if (!in.forward && !in.back) {
            this.speed *= (1.0 - friction);
        }
        if (Math.abs(this.speed) < 0.004 && !in.forward && !in.back) {
            this.speed = 0.0;
        }
        this.speed = Mth.clamp(this.speed, -reverseSpeed, maxSpeed);
        in.brakeLight = brakingNow && this.speed > 0.05;

        // ------------------------------------------------------------------
        // Yaw rate: strong at low speed, damped at high speed, none when
        // (nearly) stationary. Reversing flips the steering response like a car.
        // ------------------------------------------------------------------
        float speedFrac = (float) Math.min(1.0, Math.abs(this.speed) / maxSpeed);
        float steerEffectiveness = Mth.lerp(speedFrac, 1.0F, (float) WaveConfig.Vehicle.highSpeedSteer());
        if (!onGround) {
            steerEffectiveness *= (float) WaveConfig.Vehicle.airControl();
        }
        if (wheelieUp) {
            steerEffectiveness *= 0.35F;
        }
        float speedGate = Mth.clamp((float) Math.abs(this.speed) / STEER_SPEED_GATE, 0.0F, 1.0F);
        if (this.speed < 0.0) {
            steerEffectiveness = -steerEffectiveness;
        }
        this.yawRate = (this.steering / maxSteer) * BASE_TURN_RATE * steerEffectiveness * speedGate;

        // ------------------------------------------------------------------
        // Lean: approximate the physical lean angle atan(v * omega / g),
        // scaled down at crawl speeds so parking maneuvers stay upright.
        // ------------------------------------------------------------------
        double velocityMs = Math.abs(this.speed) * 20.0;               // m/s
        double omegaRad = Math.toRadians(this.yawRate) * 20.0;         // rad/s
        double physical = Math.toDegrees(Math.atan(velocityMs * omegaRad / 9.81)) * 0.62;
        physical *= Math.min(1.0, velocityMs / 6.0);
        if (wheelieUp) {
            physical *= 0.30;
        }
        float leanTarget = Mth.clamp((float) physical, -maxLean, maxLean);
        this.lean = approach(this.lean, leanTarget, LEAN_RATE);
    }

    /**
     * Wheelie balance physics. Holding SPACE aims for the balance point at the
     * configured max angle; throttle lifts, gravity (cosine torque) pulls the
     * front wheel down; releasing SPACE lets it fall; exceeding the safe limit
     * tips the bike over.
     *
     * @return the new wheelie state
     */
    public WheelieState tickWheelie(Input in, WheelieState state, boolean enginePower, boolean crashed) {
        if (crashed) {
            this.wheelieAngle = approach(this.wheelieAngle, 0.0F, 6.0F);
            this.wheelieVelocity = 0.0F;
            return WheelieState.NORMAL;
        }
        if (!WaveConfig.Wheelie.enabled()) {
            this.wheelieAngle = 0.0F;
            this.wheelieVelocity = 0.0F;
            return WheelieState.NORMAL;
        }

        float maxAngle = (float) WaveConfig.Wheelie.maxAngle();
        double speedFrac = Math.min(1.0, Math.abs(this.speed) / WaveConfig.Vehicle.maxSpeed());

        if (state == WheelieState.NORMAL) {
            boolean canLift = in.wheelie && in.forward && enginePower && Math.abs(this.speed) > 0.18;
            if (canLift) {
                state = WheelieState.LIFTING;
            } else {
                return WheelieState.NORMAL;
            }
        }

        if (state == WheelieState.LIFTING || state == WheelieState.WHEELIE || state == WheelieState.LOWERING) {
            double liftForce = WaveConfig.Wheelie.liftForce();
            double lift = (in.forward && enginePower) ? liftForce * (0.45 + 0.55 * speedFrac) : 0.0;
            if (speedFrac > 0.75) {
                lift *= 1.30; // full speed wheelies are wilder and riskier
            }
            // holding SPACE aims for the balance point right under the limit
            double balance = in.wheelie ? WaveConfig.Wheelie.balanceForce() * (maxAngle - this.wheelieAngle) : 0.0;
            // gravity torque fades as the bike approaches vertical (cos term),
            // which is exactly why high wheelies are unstable
            double gravity = WaveConfig.Wheelie.gravityTorque() * Math.cos(Math.toRadians(this.wheelieAngle));
            if (!in.forward) {
                gravity *= 1.35; // chopping the throttle drops the front wheel
            }

            this.wheelieVelocity += (float) (lift + balance - gravity);
            if (in.back) {
                this.wheelieVelocity -= 0.45F; // rear brake slams the front down
            }
            this.wheelieVelocity *= 0.88F; // aerodynamic + damping
            this.wheelieAngle += this.wheelieVelocity;

            if (this.wheelieAngle > maxAngle) {
                this.wheelieAngle = maxAngle;
                this.wheelieVelocity = 0.0F;
                return WheelieState.CRASH;
            }
            if (this.wheelieAngle <= 0.0F) {
                this.wheelieAngle = 0.0F;
                this.wheelieVelocity = 0.0F;
                return WheelieState.NORMAL;
            }
            if (this.wheelieVelocity > 0.15F) {
                return WheelieState.LIFTING;
            }
            if (this.wheelieVelocity < -0.15F) {
                return WheelieState.LOWERING;
            }
            return WheelieState.WHEELIE;
        }
        return state;
    }

    /**
     * Front suspension: compresses under braking and acceleration, rebounds on
     * landing, jiggles slightly over rough ground.
     */
    public void tickSuspension(Input in, boolean onGround, double speedFrac, boolean enginePower) {
        float target = 0.0F;
        if (onGround && in.brakeLight) {
            target = 0.9F;
        } else if (onGround && in.forward && enginePower && speedFrac < 0.5) {
            target = 0.45F;
        }
        this.suspensionVelocity += (target - this.suspension) * 0.22F;
        this.suspensionVelocity *= 0.82F;
        this.suspension += this.suspensionVelocity;
        this.suspension = Mth.clamp(this.suspension, -0.6F, 1.4F);
    }

    /** Adds a compression impulse on landing or impacts. */
    public void impact(double strength) {
        this.suspensionVelocity -= (float) Math.min(1.3, strength);
    }

    /** Small terrain jitter while rolling on the ground. */
    public void terrainJitter(double speedFrac) {
        this.suspensionVelocity += (float) ((Math.random() - 0.5) * 0.10 * speedFrac);
    }

    /** Kills motion after a crash (slide to a stop). */
    public void crashSlide() {
        this.speed *= 0.80;
        this.steering *= 0.7F;
        this.lean *= 0.6F;
        this.yawRate = 0.0F;
    }
}
