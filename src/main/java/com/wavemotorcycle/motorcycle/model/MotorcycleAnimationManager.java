package com.wavemotorcycle.motorcycle.model;

import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.motorcycle.EngineState;

/**
 * Computes the subtle body "suspension" animation on top of the hard
 * transforms (position/rotation). Everything is smoothed per tick, so animation
 * changes blend instead of snapping.
 *
 * <p>Active animations:
 * <ul>
 *   <li>IDLE - tiny engine rumble while the engine runs.</li>
 *   <li>ACCELERATING - front dips slightly (wheelie squat the other way).</li>
 *   <li>BRAKING - front dips.</li>
 *   <li>TURN_LEFT / TURN_RIGHT - a small body lean into the turn.</li>
 *   <li>WHEELIE - the body settles a touch as the front lifts.</li>
 *   <li>LANDING - quick dip for a few ticks.</li>
 *   <li>HEADLIGHT_ON / HEADLIGHT_OFF - no geometry change, tracked for debug output.</li>
 *   <li>DAMAGED - extra wobble.</li>
 * </ul>
 */
public final class MotorcycleAnimationManager {

    public enum Animation {
        IDLE,
        ACCELERATING,
        BRAKING,
        TURN_LEFT,
        TURN_RIGHT,
        WHEELIE,
        LANDING,
        HEADLIGHT_ON,
        HEADLIGHT_OFF,
        DAMAGED
    }

    private double bodyDip;      // extra Y offset (blocks) applied to chassis parts
    private double bodyLean;     // extra pitch degrees (positive = nose up)
    private double wobblePhase;
    private int landingTicks;
    private Animation primary = Animation.IDLE;

    /** Advances the animation state by one tick. */
    public void update(MotorcycleController c, double dt) {
        // --- determine the target state ---
        double targetDip = 0.0;
        double targetLean = 0.0;
        double maxSpeed = Math.max(0.001, c.maxSpeed());

        if (c.engineState() == EngineState.RUNNING) {
            targetDip += Math.sin(c.tickCount() * 0.9) * 0.004; // idle rumble
        }
        if (c.isBraking()) {
            targetDip -= 0.012; // brake dip
        }
        if (c.isThrottling() && c.engineState() == EngineState.RUNNING) {
            targetDip += 0.010 * Math.min(1.0, Math.abs(c.speed()) / maxSpeed); // acceleration squat
        }
        double wf = c.wheelieAngle() / Math.max(0.001, c.wheelieMaxAngle());
        if (wf > 0.05) {
            targetLean += wf * 1.5; // body settles slightly while wheeling
        }
        double steerFrac = c.steerAngle() / Math.max(0.001, c.maxSteerAngle());
        if (Math.abs(steerFrac) > 0.05 && Math.abs(c.speed()) > 0.05) {
            targetDip += -steerFrac * 0.015 * Math.min(1.0, Math.abs(c.speed()) / maxSpeed); // lean into turn
        }
        if (c.health() < 40) {
            targetDip += Math.sin(c.tickCount() * 1.7) * 0.008; // damaged wobble
        }
        if (landingTicks > 0) {
            landingTicks--;
            targetDip -= 0.03 * (landingTicks / 6.0);
        }

        // --- smooth toward targets ---
        double k = 1.0 - Math.pow(0.72, dt);
        bodyDip += (targetDip - bodyDip) * k;
        bodyLean += (targetLean - bodyLean) * k;
        wobblePhase += dt;

        // --- primary animation label (for /wave debug) ---
        if (landingTicks > 0) {
            primary = Animation.LANDING;
        } else if (wf > 0.25) {
            primary = Animation.WHEELIE;
        } else if (c.isBraking()) {
            primary = Animation.BRAKING;
        } else if (c.isThrottling() && c.engineState() == EngineState.RUNNING) {
            primary = Animation.ACCELERATING;
        } else if (Math.abs(steerFrac) > 0.25 && Math.abs(c.speed()) > 0.05) {
            primary = steerFrac < 0 ? Animation.TURN_LEFT : Animation.TURN_RIGHT;
        } else if (c.headlightOn()) {
            primary = Animation.HEADLIGHT_ON;
        } else if (c.health() < 40) {
            primary = Animation.DAMAGED;
        } else {
            primary = Animation.IDLE;
        }
    }

    /** Called by the controller when a hard landing happened. */
    public void onLanding() {
        landingTicks = 6;
    }

    public double bodyDip() {
        return bodyDip;
    }

    public double bodyLean() {
        return bodyLean;
    }

    public Animation primary() {
        return primary;
    }

    /** Resets the smoothed state (used when the model is (re)spawned). */
    public void reset() {
        bodyDip = 0;
        bodyLean = 0;
        wobblePhase = 0;
        landingTicks = 0;
        primary = Animation.IDLE;
    }
}
