package com.example.vanillavehicles.input;

/**
 * Control state of one driver. Filled either by the enhanced
 * PlayerInputEvent hook (real WASD) or by the fallback listeners that only
 * use API available since forever (hotbar gears, mouse steering, sneak brake).
 */
public class InputState {

    /** +1 forward (W) .. -1 backward (S), enhanced mode. */
    public double forward;
    /** +1 right (D) .. -1 left (A), enhanced mode. */
    public double strafe;
    public boolean jump;
    public boolean sneak;
    public boolean sprint;

    /** Fallback cruise control: -0.3 (reverse) .. 1.0 (full speed). */
    public double gear;
    /** Fallback brake (sneak hold). */
    public boolean brake;
    /** Momentary jump pulse from PlayerJumpEvent. */
    public boolean jumpPulse;

    public double lookYaw;
    public double lookPitch;
    public boolean hasLook;

    /** True once real input packets have been seen recently. */
    public boolean enhanced;
    public long lastEnhancedInput;

    public boolean consumeJumpPulse() {
        boolean value = jumpPulse;
        jumpPulse = false;
        return value;
    }

    public void reset() {
        forward = 0;
        strafe = 0;
        jump = false;
        sneak = false;
        sprint = false;
        gear = 0;
        brake = false;
        jumpPulse = false;
        hasLook = false;
        enhanced = false;
        lastEnhancedInput = 0;
    }
}
