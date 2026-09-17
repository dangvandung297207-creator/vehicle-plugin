package com.wave100.client;

import com.wave100.entity.EngineState;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.util.Mth;

/**
 * Drives all animated model parts from the entity's interpolated visual state.
 *
 * <p>Every value comes from {@link WaveMotorcycleEntity}'s render accessors,
 * which lerp between the last two ticks, so the result is buttery smooth even
 * though the underlying state syncs at a lower rate.</p>
 *
 * <p>Model conventions: 16 units = 1 block, front = -Z, +X = left,
 * positive steering/lean = left, positive wheelie = nose up, suspension is in
 * model units (positive = compressed), wheel spin is in wheel revolutions.</p>
 */
public final class WaveAnimationController {

    private static final float DEG_TO_RAD = (float) Math.PI / 180F;
    private static final float TWO_PI = (float) Math.PI * 2F;

    /** Steering lock for visuals, degrees (matches WaveConfig default). */
    private static final float MAX_STEER_DEG = 28F;

    /** How far the fork can slide, in model units. */
    private static final float MAX_SUSPENSION = 1.2F;

    /** Side stand: fold angle when riding, degrees. */
    private static final float STAND_FOLD_DEG = 80F;
    private static final float STAND_TILT_DEG = 20F;

    private WaveAnimationController() {
    }

    /** Applies all part animations for this frame. */
    public static void apply(WaveModel model, WaveMotorcycleEntity bike, float partialTicks) {
        // ---- steering: handlebar + fork + front wheel swivel together ----
        float steerDeg = Mth.clamp(bike.getRenderSteering(partialTicks), -MAX_STEER_DEG, MAX_STEER_DEG);
        // part space: positive yRot turns the wheel to the RIGHT, our steering
        // is positive-left, hence the negation
        model.steering.yRot = -steerDeg * DEG_TO_RAD;

        // ---- suspension: fork lower legs slide up along the raked axis ----
        float compress = Mth.clamp(bike.getRenderSuspension(partialTicks), -0.4F, MAX_SUSPENSION);
        model.forkLower.y = -compress;

        // ---- wheel spin (revolutions -> radians, forward = negative xRot) ----
        float spin = bike.getRenderWheelSpin(partialTicks) * TWO_PI;
        model.frontWheel.xRot = -spin;
        model.rearWheel.xRot = -spin;

        // ---- side stand: swings down when parked, folds back when ridden ----
        float stand = Mth.clamp(bike.getRenderStand(partialTicks), 0F, 1F);
        model.sideStand.xRot = -(1F - stand) * STAND_FOLD_DEG * DEG_TO_RAD;
        model.sideStand.zRot = stand * STAND_TILT_DEG * DEG_TO_RAD;

        // ---- engine vibration: a nervous little shake, strongest at idle ----
        EngineState engine = bike.getEngineState();
        if (engine == EngineState.IDLE || engine == EngineState.STARTING) {
            float t = bike.tickCount + partialTicks;
            model.body.x = Mth.sin(t * 2.4F) * 0.035F;
            model.body.y = Mth.sin(t * 3.7F) * 0.02F;
        } else if (engine == EngineState.RUNNING) {
            float t = bike.tickCount + partialTicks;
            model.body.x = Mth.sin(t * 3.1F) * 0.02F;
            model.body.y = Mth.sin(t * 4.6F) * 0.012F;
        } else {
            model.body.x = 0F;
            model.body.y = 0F;
        }
    }

    /** Whole-body lean in part space (radians, positive = leaning left). */
    public static float leanRadians(WaveMotorcycleEntity bike, float partialTicks) {
        return -bike.getRenderLean(partialTicks) * DEG_TO_RAD;
    }

    /** Whole-body wheelie pitch in part space (radians, positive = nose up). */
    public static float wheelieRadians(WaveMotorcycleEntity bike, float partialTicks) {
        return bike.getRenderWheelie(partialTicks) * DEG_TO_RAD;
    }

    /** Crash fall progress, 0 = upright, 1 = lying on the side. */
    public static float crashProgress(WaveMotorcycleEntity bike, float partialTicks) {
        return Mth.clamp(bike.getRenderCrash(partialTicks), 0F, 1F);
    }
}
