package com.wave100.entity;

import com.wave100.WaveConfig;

/**
 * Simple engine simulation: state machine, RPM curve and a 4-speed automatic
 * gearbox.
 *
 * <p>Gear speed windows (in blocks/tick, at the default 0.85 max speed):</p>
 * <pre>
 *   Gear 1:  0.00 - 0.27   (  0 - 20 km/h)  strong torque
 *   Gear 2:  0.20 - 0.47   ( 15 - 34 km/h)
 *   Gear 3:  0.40 - 0.67   ( 29 - 48 km/h)
 *   Gear 4:  0.60 - max    ( 43+ km/h)      weakest torque, highest top speed
 * </pre>
 *
 * <p>The player never shifts manually; gears modulate acceleration force and
 * the RPM band, exactly like the centrifugal clutch of a real underbone bike.</p>
 */
public final class WaveEngine {

    /** Top speed (blocks/tick) each gear reaches before upshifting. */
    public static final double[] GEAR_TOP_SPEED = {0.27, 0.47, 0.67, 0.85};
    /** Speed below which each gear downshifts. */
    public static final double[] GEAR_DOWN_SPEED = {0.0, 0.20, 0.40, 0.60};
    /** Torque multiplier per gear (gear 1 pulls hardest). */
    public static final double[] GEAR_TORQUE = {1.0, 0.72, 0.5, 0.36};

    public static final int GEAR_COUNT = 4;

    private WaveEngine() {
    }

    /**
     * Chooses the gear for the current speed with hysteresis so it does not
     * flutter around shift points.
     */
    public static int pickGear(int currentGear, double speed) {
        int gear = Math.max(1, Math.min(GEAR_COUNT, currentGear));
        double abs = Math.abs(speed);
        if (gear < GEAR_COUNT && abs > GEAR_TOP_SPEED[gear - 1]) {
            gear++;
        } else if (gear > 1 && abs < GEAR_DOWN_SPEED[gear - 1]) {
            gear--;
        }
        return gear;
    }

    /**
     * Target RPM for the given state. Blends a speed term (vehicle speed within
     * the gear's band) and a load term (throttle), then clamps into range.
     */
    public static int targetRpm(double speed, float throttle, int gear, boolean engineOn) {
        if (!engineOn) {
            return 0;
        }
        double idle = WaveConfig.Engine.idleRpm();
        double max = WaveConfig.Engine.maxRpm();
        double top = GEAR_TOP_SPEED[gear - 1];
        double speedTerm = Math.min(1.0, Math.abs(speed) / top) * 0.62;
        double loadTerm = throttle * 0.38;
        double rpm = idle + (max - idle) * Math.min(1.0, speedTerm + loadTerm);
        return (int) Math.max(idle, Math.min(max, rpm));
    }

    /**
     * Engine power multiplier from durability. Below 40/100 the engine runs
     * weakly, below 15 it refuses to run at all.
     */
    public static float healthPowerFactor(float health, float maxHealth) {
        float frac = maxHealth <= 0 ? 1.0F : health / maxHealth;
        if (frac <= 0.15F) {
            return 0.0F;
        }
        if (frac <= 0.40F) {
            return 0.6F;
        }
        return 1.0F;
    }
}
