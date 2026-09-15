package com.wavemotorcycle.motorcycle;

/** Wheelie phase used by the physics system. */
public enum WheelieState {
    /** Front wheel on the ground. */
    NORMAL,
    /** Front end is lifting. */
    LIFTING,
    /** Front end is held at (or near) the maximum angle. */
    WHEELIE,
    /** Front end is coming back down. */
    LOWERING
}
